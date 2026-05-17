package org.tavall.abstractcache.semantic.stats;

import org.tavall.abstractcache.cache.interfaces.ICacheStats;
import org.tavall.abstractcache.cache.metadata.CacheMetaData;

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

        for (CacheStatsProvider provider : providers.values()) {
            ICacheStats stats = provider.snapshotStats();
            if (stats == null) {
                continue;
            }
            byCache.put(provider.cacheName(), stats);
            occupancy = occupancy.plus(stats);
        }

        return new ApplicationCacheStats(occupancy, byCache);
    }
}
