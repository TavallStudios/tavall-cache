package org.tavall.abstractcache.cache.maps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.abstractcache.cache.CacheKey;
import org.tavall.abstractcache.cache.CacheValue;
import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.metadata.CacheMetaData;
import org.tavall.abstractcache.semantic.stats.CacheStatsRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheMapStatsTest {

    private CacheMap cacheMap;

    @BeforeEach
    void setUp() {
        cacheMap = CacheMap.getCacheMap();
        cacheMap.clear();
    }

    @Test
    void testSnapshotStats_CountsValidAndExpiredValues() {
        CacheKey<String> key = new CacheKey<>("stats-key", CacheType.MEMORY, CacheDomain.SCANS);
        cacheMap.add(key, new CacheValue<>("valid", System.currentTimeMillis() + 10_000));
        cacheMap.add(key, new CacheValue<>("expired", System.currentTimeMillis() - 10_000));

        CacheMetaData metadata = cacheMap.snapshotStats();

        assertEquals(2, metadata.getTotalEntries());
        assertEquals(1, metadata.getValidEntries());
        assertEquals(1, metadata.getExpiredEntries());
    }

    @Test
    void testRegistry_IncludesLegacyCacheMap() {
        assertTrue(CacheStatsRegistry.getInstance().snapshot(CacheMap.CACHE_NAME).isPresent());
    }
}
