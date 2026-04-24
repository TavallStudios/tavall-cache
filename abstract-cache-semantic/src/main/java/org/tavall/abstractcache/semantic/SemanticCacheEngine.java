package org.tavall.abstractcache.semantic;

import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.cache.interfaces.ICacheStats;
import org.tavall.abstractcache.cache.metadata.CacheMetaData;
import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheEntry;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.routing.CacheRoutingPolicy;
import org.tavall.abstractcache.semantic.routing.RouteDecision;
import org.tavall.abstractcache.semantic.routing.RouteStage;
import org.tavall.abstractcache.semantic.spi.CacheCodec;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecord;
import org.tavall.abstractcache.semantic.stats.CacheStatsRegistry;
import org.tavall.abstractcache.semantic.stats.SemanticCacheStats;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Default semantic cache engine implementation.
 */
final class SemanticCacheEngine implements SemanticCache {

    private static final Duration READ_ROUTE_TTL = Duration.ofSeconds(1);

    private final String cacheName;
    private final Map<CacheTier, CacheTierAdapter> adapters;
    private final CacheRoutingPolicy routingPolicy;
    private final Clock clock;
    private final Duration maintenanceInterval;
    private final int scanBatchSize;
    private final ScheduledExecutorService scheduler;
    private final boolean ownsScheduler;
    private final LongAdder hits;
    private final LongAdder misses;
    private final Map<CacheTier, LongAdder> hitsByTier;
    private final LongAdder promotions;
    private final LongAdder demotions;
    private final LongAdder terminalEvictions;
    private final LongAdder adapterWriteFailures;
    private final LongAdder adapterAvailabilitySkips;

    SemanticCacheEngine(
            String cacheName,
            Map<CacheTier, CacheTierAdapter> adapters,
            CacheRoutingPolicy routingPolicy,
            Clock clock,
            Duration maintenanceInterval,
            ScheduledExecutorService scheduler,
            int scanBatchSize
    ) {
        this.cacheName = Objects.requireNonNull(cacheName, "cacheName cannot be null");
        this.adapters = new EnumMap<>(adapters);
        this.routingPolicy = Objects.requireNonNull(routingPolicy, "routingPolicy cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.maintenanceInterval = Objects.requireNonNull(maintenanceInterval, "maintenanceInterval cannot be null");
        this.scanBatchSize = scanBatchSize;
        this.hits = new LongAdder();
        this.misses = new LongAdder();
        this.hitsByTier = createTierCounterMap();
        this.promotions = new LongAdder();
        this.demotions = new LongAdder();
        this.terminalEvictions = new LongAdder();
        this.adapterWriteFailures = new LongAdder();
        this.adapterAvailabilitySkips = new LongAdder();

        if (!maintenanceInterval.isZero() && !maintenanceInterval.isNegative()) {
            if (scheduler == null) {
                this.scheduler = Executors.newSingleThreadScheduledExecutor();
                this.ownsScheduler = true;
            } else {
                this.scheduler = scheduler;
                this.ownsScheduler = false;
            }
            this.scheduler.scheduleWithFixedDelay(
                    this::runMaintenanceCycle,
                    maintenanceInterval.toMillis(),
                    maintenanceInterval.toMillis(),
                    TimeUnit.MILLISECONDS
            );
        } else {
            this.scheduler = scheduler;
            this.ownsScheduler = false;
        }

        CacheStatsRegistry.getInstance().register(this);
    }

    @Override
    public <V> void put(SemanticCacheKey key, V value, Duration ttl, CacheCodec<V> codec) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(ttl, "ttl cannot be null");
        Objects.requireNonNull(codec, "codec cannot be null");
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }

        RouteDecision routeDecision = routingPolicy.decide(key, ttl);
        byte[] payloadBytes = codec.encode(value);
        Instant now = clock.instant();
        StoredCacheRecord seedRecord = new StoredCacheRecord(
                key,
                payloadBytes,
                codec.codecId(),
                now,
                now.plus(routeDecision.stages().get(0).ttl()),
                0,
                routeDecision.stages()
        );
        if (!writeToFirstHealthyStage(seedRecord, 0, now)) {
            throw new IllegalStateException("no healthy adapter available for semantic cache write");
        }
    }

    @Override
    public <V> Optional<ICacheValue<V>> get(SemanticCacheKey key, CacheCodec<V> codec) {
        return Optional.ofNullable(getEntry(key, codec).orElse(null));
    }

    @Override
    public <V> Optional<SemanticCacheEntry<V>> getEntry(SemanticCacheKey key, CacheCodec<V> codec) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(codec, "codec cannot be null");

        Instant now = clock.instant();
        RouteDecision routeDecision = routingPolicy.decide(key, READ_ROUTE_TTL);
        for (RouteStage stage : routeDecision.stages()) {
            CacheTierAdapter adapter = adapters.get(stage.tier());
            if (adapter == null || !adapter.isAvailable()) {
                if (adapter == null || !adapter.isAvailable()) {
                    adapterAvailabilitySkips.increment();
                }
                continue;
            }

            Optional<StoredCacheRecord> maybeRecord = adapter.get(key);
            if (maybeRecord.isEmpty()) {
                continue;
            }

            StoredCacheRecord record = maybeRecord.get();
            if (record.isExpired(now)) {
                continue;
            }

            hits.increment();
            hitsByTier.get(record.currentTier()).increment();
            boolean promoted = promoteRecord(record, now);
            V value = codec.decode(record.payloadBytes());
            return Optional.of(new SemanticCacheEntry<>(
                    key,
                    value,
                    record.createdAt(),
                    record.expiresAt(),
                    promoted,
                    record.routeIndex(),
                    record.codecId(),
                    record.routeStages()
            ));
        }

        misses.increment();
        return Optional.empty();
    }

    @Override
    public boolean invalidate(SemanticCacheKey key) {
        boolean removed = false;
        for (CacheTierAdapter adapter : adapters.values()) {
            if (adapter == null) {
                continue;
            }
            if (adapter.get(key).isPresent()) {
                adapter.remove(key);
                removed = true;
            }
        }
        return removed;
    }

    @Override
    public int invalidateByFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return 0;
        }
        int removed = 0;
        for (CacheTierAdapter adapter : adapters.values()) {
            if (adapter == null) {
                continue;
            }
            List<StoredCacheRecord> snapshot = adapter.snapshotRecords();
            for (StoredCacheRecord record : snapshot) {
                if (fingerprint.equals(record.key().fingerprint())) {
                    adapter.remove(record.key());
                    removed++;
                }
            }
        }
        return removed;
    }

    @Override
    public void runMaintenanceCycle() {
        Instant now = clock.instant();
        for (CacheTierAdapter adapter : adapters.values()) {
            if (adapter == null || !adapter.isAvailable()) {
                continue;
            }
            List<StoredCacheRecord> expiredRecords = adapter.scanExpired(now, scanBatchSize);
            for (StoredCacheRecord record : expiredRecords) {
                demoteOrEvict(record, adapter, now);
            }
        }
    }

    @Override
    public String cacheName() {
        return cacheName;
    }

    @Override
    public SemanticCacheStats snapshotStats() {
        Instant now = clock.instant();
        Map<CacheTier, CacheMetaData> occupancyByTier = new EnumMap<>(CacheTier.class);
        CacheMetaData total = CacheMetaData.empty();

        for (CacheTierAdapter adapter : adapters.values()) {
            if (adapter == null) {
                continue;
            }
            int totalEntries = 0;
            int validEntries = 0;
            int expiredEntries = 0;

            for (StoredCacheRecord record : adapter.snapshotRecords()) {
                totalEntries++;
                if (record.isExpired(now)) {
                    expiredEntries++;
                } else {
                    validEntries++;
                }
            }

            CacheMetaData metadata = new CacheMetaData(totalEntries, validEntries, expiredEntries);
            occupancyByTier.put(adapter.tier(), metadata);
            total = total.plus(metadata);
        }

        Map<CacheTier, Long> hitCounts = new EnumMap<>(CacheTier.class);
        for (Map.Entry<CacheTier, LongAdder> entry : hitsByTier.entrySet()) {
            hitCounts.put(entry.getKey(), entry.getValue().sum());
        }

        return new SemanticCacheStats(
                total,
                occupancyByTier,
                hits.sum(),
                misses.sum(),
                hitCounts,
                promotions.sum(),
                demotions.sum(),
                terminalEvictions.sum(),
                adapterWriteFailures.sum(),
                adapterAvailabilitySkips.sum()
        );
    }

    @Override
    public void close() {
        CacheStatsRegistry.getInstance().unregister(cacheName);
        for (CacheTierAdapter adapter : adapters.values()) {
            if (adapter != null) {
                adapter.close();
            }
        }
        if (ownsScheduler && scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private boolean promoteRecord(StoredCacheRecord record, Instant now) {
        if (record.routeIndex() == 0) {
            return false;
        }
        for (int index = 0; index < record.routeIndex(); index++) {
            CacheTierAdapter adapter = adapters.get(record.routeStages().get(index).tier());
            if (adapter == null || !adapter.isAvailable()) {
                adapterAvailabilitySkips.increment();
                continue;
            }
            try {
                StoredCacheRecord promotedRecord = record.moveToStage(
                        index,
                        now.plus(record.routeStages().get(index).ttl())
                );
                adapter.put(promotedRecord);
                promotions.increment();
                return true;
            } catch (RuntimeException exception) {
                adapterWriteFailures.increment();
            }
        }
        return false;
    }

    private void demoteOrEvict(StoredCacheRecord record, CacheTierAdapter currentAdapter, Instant now) {
        for (int index = record.routeIndex() + 1; index < record.routeStages().size(); index++) {
            CacheTierAdapter nextAdapter = adapters.get(record.routeStages().get(index).tier());
            if (nextAdapter == null || !nextAdapter.isAvailable()) {
                adapterAvailabilitySkips.increment();
                continue;
            }
            try {
                StoredCacheRecord demotedRecord = record.moveToStage(
                        index,
                        now.plus(record.routeStages().get(index).ttl())
                );
                nextAdapter.put(demotedRecord);
                currentAdapter.remove(record.key());
                demotions.increment();
                return;
            } catch (RuntimeException exception) {
                adapterWriteFailures.increment();
            }
        }

        currentAdapter.remove(record.key());
        terminalEvictions.increment();
    }

    private boolean writeToFirstHealthyStage(StoredCacheRecord record, int startingIndex, Instant now) {
        for (int index = startingIndex; index < record.routeStages().size(); index++) {
            CacheTierAdapter adapter = adapters.get(record.routeStages().get(index).tier());
            if (adapter == null || !adapter.isAvailable()) {
                adapterAvailabilitySkips.increment();
                continue;
            }
            try {
                StoredCacheRecord currentRecord = index == record.routeIndex()
                        ? record
                        : record.moveToStage(index, now.plus(record.routeStages().get(index).ttl()));
                adapter.put(currentRecord);
                return true;
            } catch (RuntimeException exception) {
                adapterWriteFailures.increment();
            }
        }
        return false;
    }

    private Map<CacheTier, LongAdder> createTierCounterMap() {
        Map<CacheTier, LongAdder> counters = new EnumMap<>(CacheTier.class);
        for (CacheTier tier : CacheTier.values()) {
            counters.put(tier, new LongAdder());
        }
        return counters;
    }
}
