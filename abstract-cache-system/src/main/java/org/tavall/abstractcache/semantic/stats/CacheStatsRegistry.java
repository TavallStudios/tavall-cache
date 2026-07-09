package org.tavall.abstractcache.semantic.stats;

import org.tavall.abstractcache.cache.interfaces.ICacheRegistryAccess;
import org.tavall.abstractcache.cache.interfaces.ICacheStats;
import org.tavall.abstractcache.cache.metadata.CacheRegistryMetaData;
import org.tavall.abstractcache.cache.metadata.CacheMetaData;
import org.tavall.abstractcache.cache.metadata.ICacheRegistryMetaDataProvider;
import org.tavall.dependency.DependencyLoaderAccess;
import org.tavall.logging.Log;

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
    private final ConcurrentMap<Class<?>, CacheRegistryMetaData> pendingCacheRegistryMetaData;

    private CacheStatsRegistry() {
        this.providers = new ConcurrentHashMap<>();
        this.pendingCacheRegistryMetaData = new ConcurrentHashMap<>();
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

        CacheStatsProvider existingProvider = providers.putIfAbsent(provider.cacheName(), provider);
        if (existingProvider != null && existingProvider != provider) {
            CacheStatsProviderAlreadyRegisteredException exception =
                    new CacheStatsProviderAlreadyRegisteredException(provider.cacheName());

            Log.exception(exception);
            return;
        }

        registerCacheRegistryMetaDataIfAvailable(provider);
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

    public void clear() {
        providers.clear();
        pendingCacheRegistryMetaData.clear();
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

    public boolean hasPendingCacheRegistryMetaData(Class<?> cacheClass) {
        return cacheClass != null && pendingCacheRegistryMetaData.containsKey(cacheClass);
    }

    public void flushPendingCacheRegistryMetaData() {
        ICacheRegistryAccess cacheRegistryAccess = DependencyLoaderAccess.findInstance(ICacheRegistryAccess.class);

        if (cacheRegistryAccess == null) {
            return;
        }

        flushPendingCacheRegistryMetaData(cacheRegistryAccess);
    }

    public void flushPendingCacheRegistryMetaData(ICacheRegistryAccess cacheRegistryAccess) {
        if (cacheRegistryAccess == null || pendingCacheRegistryMetaData.isEmpty()) {
            return;
        }

        for (Map.Entry<Class<?>, CacheRegistryMetaData> pendingEntry : pendingCacheRegistryMetaData.entrySet()) {
            Class<?> cacheClass = pendingEntry.getKey();
            CacheRegistryMetaData cacheRegistryMetaData = pendingEntry.getValue();

            if (cacheClass == null || cacheRegistryMetaData == null) {
                continue;
            }

            cacheRegistryAccess.registerCacheIfAbsent(cacheRegistryMetaData);
            pendingCacheRegistryMetaData.remove(cacheClass, cacheRegistryMetaData);
        }
    }

    private void registerCacheRegistryMetaDataIfAvailable(CacheStatsProvider provider) {
        if (!(provider instanceof ICacheRegistryMetaDataProvider cacheRegistryMetaDataProvider)) {
            return;
        }

        Optional<CacheRegistryMetaData> optionalCacheRegistryMetaData =
                cacheRegistryMetaDataProvider.createCacheRegistryMetaData();

        if (optionalCacheRegistryMetaData.isEmpty()) {
            return;
        }

        CacheRegistryMetaData cacheRegistryMetaData = optionalCacheRegistryMetaData.get();
        Class<?> cacheClass = cacheRegistryMetaData.getCacheClass();

        if (cacheClass == null) {
            return;
        }

        ICacheRegistryAccess cacheRegistryAccess = DependencyLoaderAccess.findInstance(ICacheRegistryAccess.class);
        if (cacheRegistryAccess == null) {
            pendingCacheRegistryMetaData.putIfAbsent(cacheClass, cacheRegistryMetaData);

            CacheRegistryUnavailableException exception = new CacheRegistryUnavailableException(cacheClass);
            Log.exception(exception);
            return;
        }

        cacheRegistryAccess.registerCacheIfAbsent(cacheRegistryMetaData);
        pendingCacheRegistryMetaData.remove(cacheClass);
    }
}
