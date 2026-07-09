package org.tavall.abstractcache.semantic.stats;

import org.tavall.abstractcache.cache.interfaces.ICacheStats;
import org.tavall.abstractcache.cache.metadata.CacheMetaData;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aggregated application-wide cache stats snapshot.
 */
public final class ApplicationCacheStats implements ICacheStats {

    private final CacheMetaData occupancy;
    private final Map<String, ICacheStats> byCache;

    public ApplicationCacheStats(CacheMetaData occupancy, Map<String, ICacheStats> byCache) {
        this.occupancy = occupancy;
        this.byCache = Collections.unmodifiableMap(new TreeMap<>(byCache));
    }

    @Override
    public int getTotalEntries() {
        return occupancy.getTotalEntries();
    }

    @Override
    public int getValidEntries() {
        return occupancy.getValidEntries();
    }

    @Override
    public int getExpiredEntries() {
        return occupancy.getExpiredEntries();
    }

    public CacheMetaData occupancy() {
        return occupancy;
    }

    public Map<String, ICacheStats> byCache() {
        return byCache;
    }
}
