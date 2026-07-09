package org.tavall.abstractcache.semantic;

import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.routing.CacheRoutingPolicy;
import org.tavall.abstractcache.semantic.routing.DefaultCacheRoutingPolicy;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Builder for semantic cache engine instances.
 */
public final class SemanticCacheBuilder {

    private String cacheName;
    private final Map<CacheTier, CacheTierAdapter> adapters;
    private CacheRoutingPolicy routingPolicy;
    private Clock clock;
    private Duration maintenanceInterval;
    private ScheduledExecutorService scheduler;
    private int scanBatchSize;

    public SemanticCacheBuilder() {
        this.cacheName = "semantic-cache-" + UUID.randomUUID();
        this.adapters = new EnumMap<>(CacheTier.class);
        this.routingPolicy = DefaultCacheRoutingPolicy.standard();
        this.clock = Clock.systemUTC();
        this.maintenanceInterval = Duration.ofSeconds(30);
        this.scanBatchSize = 1_000;
    }

    public SemanticCacheBuilder cacheName(String cacheName) {
        if (cacheName == null || cacheName.isBlank()) {
            throw new IllegalArgumentException("cacheName cannot be null or blank");
        }
        this.cacheName = cacheName;
        return this;
    }

    public SemanticCacheBuilder withAdapter(CacheTierAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter cannot be null");
        adapters.put(adapter.tier(), adapter);
        return this;
    }

    public SemanticCacheBuilder withHotMemoryTier() {
        return withAdapter(instantiateAdapter(
                "org.tavall.abstractcache.semantic.storage.memory.HotMemoryTierAdapter"
        ));
    }

    public SemanticCacheBuilder withLocalDiskTier(Path baseDirectory) {
        return withAdapter(instantiateAdapter(
                "org.tavall.abstractcache.semantic.storage.disk.LocalDiskTierAdapter",
                new Class<?>[]{Path.class},
                baseDirectory
        ));
    }

    public SemanticCacheBuilder withInMemoryServiceTier(CacheTier tier) {
        return withAdapter(instantiateAdapter(
                "org.tavall.abstractcache.semantic.storage.memory.InMemoryServiceTierAdapter",
                new Class<?>[]{CacheTier.class},
                tier
        ));
    }

    public SemanticCacheBuilder withRedisTier(CacheTier tier, String redisUrl) {
        return withAdapter(instantiateAdapter(
                "org.tavall.abstractcache.semantic.storage.redis.RedisTierAdapter",
                new Class<?>[]{CacheTier.class, String.class},
                tier,
                redisUrl
        ));
    }

    public SemanticCacheBuilder withPostgresTier(CacheTier tier, String jdbcUrl, String username, String password) {
        return withAdapter(instantiateAdapter(
                "org.tavall.abstractcache.semantic.storage.postgres.PostgresTierAdapter",
                new Class<?>[]{CacheTier.class, String.class, String.class, String.class},
                tier,
                jdbcUrl,
                username,
                password
        ));
    }

    public SemanticCacheBuilder withMongoTier(CacheTier tier, String connectionString, String databaseName) {
        return withAdapter(instantiateAdapter(
                "org.tavall.abstractcache.semantic.storage.mongo.MongoTierAdapter",
                new Class<?>[]{CacheTier.class, String.class, String.class},
                tier,
                connectionString,
                databaseName
        ));
    }

    public SemanticCacheBuilder withQdrantTier(CacheTier tier, String baseUrl, String collectionName) {
        return withAdapter(instantiateAdapter(
                "org.tavall.abstractcache.semantic.storage.qdrant.QdrantTierAdapter",
                new Class<?>[]{CacheTier.class, String.class, String.class},
                tier,
                baseUrl,
                collectionName
        ));
    }

    public SemanticCacheBuilder withRoutingPolicy(CacheRoutingPolicy routingPolicy) {
        this.routingPolicy = Objects.requireNonNull(routingPolicy, "routingPolicy cannot be null");
        return this;
    }

    public SemanticCacheBuilder withClock(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        return this;
    }

    public SemanticCacheBuilder withMaintenanceInterval(Duration maintenanceInterval) {
        this.maintenanceInterval = Objects.requireNonNull(maintenanceInterval, "maintenanceInterval cannot be null");
        return this;
    }

    public SemanticCacheBuilder withScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    public SemanticCacheBuilder withScanBatchSize(int scanBatchSize) {
        if (scanBatchSize <= 0) {
            throw new IllegalArgumentException("scanBatchSize must be positive");
        }
        this.scanBatchSize = scanBatchSize;
        return this;
    }

    public SemanticCache build() {
        if (!adapters.containsKey(CacheTier.HOT_MEMORY)) {
            adapters.put(
                    CacheTier.HOT_MEMORY,
                    instantiateAdapter("org.tavall.abstractcache.semantic.storage.memory.HotMemoryTierAdapter")
            );
        }
        return new SemanticCacheEngine(
                cacheName,
                adapters,
                routingPolicy,
                clock,
                maintenanceInterval,
                scheduler,
                scanBatchSize
        );
    }

    private CacheTierAdapter instantiateAdapter(String className) {
        return instantiateAdapter(className, new Class<?>[0]);
    }

    private CacheTierAdapter instantiateAdapter(String className, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Class<?> adapterClass = Class.forName(className);
            Object adapter = adapterClass.getConstructor(parameterTypes).newInstance(arguments);
            return (CacheTierAdapter) adapter;
        } catch (ReflectiveOperationException | ClassCastException ex) {
            throw new IllegalStateException(
                    "Cache storage adapter is unavailable on the Maven classpath: " + className,
                    ex
            );
        }
    }
}
