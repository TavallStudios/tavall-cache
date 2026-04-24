package org.tavall.abstractcache.semantic.stats;

import org.tavall.abstractcache.cache.interfaces.ICacheStats;

/**
 * Named source of cache statistics for registry aggregation.
 */
public interface CacheStatsProvider {

    /**
     * @return unique cache name
     */
    String cacheName();

    /**
     * @return current cache stats snapshot
     */
    ICacheStats snapshotStats();
}
