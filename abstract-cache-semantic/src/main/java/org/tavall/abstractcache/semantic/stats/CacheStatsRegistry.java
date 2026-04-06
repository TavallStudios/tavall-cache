package org.tavall.abstractcache.semantic.stats;

import org.tavall.abstractcache.cache.interfaces.ICacheStats;
import org.tavall.abstractcache.cache.metadata.CacheMetaData;
import org.tavall.abstractcache.semantic.model.CacheTier;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Global cache stats registry for application-wide cache inspection.
 */
public final class CacheStatsRegistry {

    private static final CacheStatsRegistry INSTANCE = new CacheStatsRegistry();

    private final ConcurrentMap<String, CacheStatsProvider> providers;

    private CacheStatsRegistry() {
        this.providers = new ConcurrentHashMap<>();
    }

    /**
     * @return global registry instance
     */
    public static CacheStatsRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a named stats provider.
     *
     * @param provider stats provider
     */
    public void register(CacheStatsProvider provider) {
        if (provider == null) {
            return;
        }
        CacheStatsProvider existing = providers.putIfAbsent(provider.cacheName(), provider);
        if (existing != null && existing != provider) {
            throw new IllegalStateException("cache stats provider already registered: " + provider.cacheName());
        }
    }

    /**
     * Unregisters a stats provider by name.
     *
     * @param cacheName cache name
     */
    public void unregister(String cacheName) {
        if (cacheName != null) {
            providers.remove(cacheName);
        }
    }

    /**
     * Returns a snapshot for a single registered provider.
     *
     * @param cacheName cache name
     * @return stats snapshot when registered
     */
    public Optional<ICacheStats> snapshot(String cacheName) {
        CacheStatsProvider provider = providers.get(cacheName);
        return provider == null ? Optional.empty() : Optional.ofNullable(provider.snapshotStats());
    }

    /**
     * Builds an application-wide aggregate snapshot.
     *
     * @return aggregate snapshot
     */
    public ApplicationCacheStats snapshot() {
        CacheMetaData occupancy = CacheMetaData.empty();
        Map<String, ICacheStats> byCache = new ConcurrentHashMap<>();
        Map<CacheTier, CacheMetaData> occupancyByTier = new EnumMap<>(CacheTier.class);
        long hits = 0;
        long misses = 0;
        long promotions = 0;
        long demotions = 0;
        long terminalEvictions = 0;
        long adapterWriteFailures = 0;
        long adapterAvailabilitySkips = 0;

        for (CacheStatsProvider provider : providers.values()) {
            ICacheStats stats = provider.snapshotStats();
            if (stats == null) {
                continue;
            }
            byCache.put(provider.cacheName(), stats);
            occupancy = occupancy.plus(stats);

            if (stats instanceof SemanticCacheStats semanticStats) {
                for (Map.Entry<CacheTier, CacheMetaData> entry : semanticStats.occupancyByTier().entrySet()) {
                    occupancyByTier.merge(entry.getKey(), entry.getValue(), CacheMetaData::plus);
                }
                hits += semanticStats.hits();
                misses += semanticStats.misses();
                promotions += semanticStats.promotions();
                demotions += semanticStats.demotions();
                terminalEvictions += semanticStats.terminalEvictions();
                adapterWriteFailures += semanticStats.adapterWriteFailures();
                adapterAvailabilitySkips += semanticStats.adapterAvailabilitySkips();
            }
        }

        return new ApplicationCacheStats(
                occupancy,
                byCache,
                occupancyByTier,
                hits,
                misses,
                promotions,
                demotions,
                terminalEvictions,
                adapterWriteFailures,
                adapterAvailabilitySkips
        );
    }
}
