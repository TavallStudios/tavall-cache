package org.tavall.abstractcache.cache.maps;

import org.tavall.abstractcache.cache.CacheKey;
import org.tavall.abstractcache.cache.CacheValue;
import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.interfaces.ICacheKey;
import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheMapTest {

    private CacheMap cacheMap;

    @BeforeEach
    void setUp() {
        cacheMap = CacheMap.getCacheMap();
        cacheMap.clear();
    }

    @Test
    void testAdd_NewKey_ShouldCreateList() {
        ICacheKey<String> key = new CacheKey<>("new-key", CacheType.MEMORY, CacheDomain.SCANS);
        ICacheValue<String> value = new CacheValue<>("data", System.currentTimeMillis() + 5000);

        cacheMap.add(key, value);

        assertTrue(cacheMap.containsDomainKey(key));
        assertEquals(1, cacheMap.get(key).size());
    }

    @Test
    void testAdd_ExistingKey_ShouldAppend() {
        ICacheKey<String> key = new CacheKey<>("existing-key", CacheType.MEMORY);
        ICacheValue<String> val1 = new CacheValue<>("v1", 10_000);
        ICacheValue<String> val2 = new CacheValue<>("v2", 10_000);

        cacheMap.add(key, val1);
        cacheMap.add(key, val2);

        List<ICacheValue<?>> bucket = cacheMap.get(key);

        assertEquals(1, cacheMap.size());
        assertNotNull(bucket);
        assertEquals(2, bucket.size());
        assertTrue(bucket.contains(val1));
        assertTrue(bucket.contains(val2));
    }

    @Test
    void testFindByDomain() {
        CacheKey<String> k1 = new CacheKey<>("d1", CacheType.MEMORY, CacheDomain.SCANS);
        CacheKey<String> k2 = new CacheKey<>("d2", CacheType.MEMORY, CacheDomain.QR);

        cacheMap.add(k1, new CacheValue<>("s", 10_000));
        cacheMap.add(k2, new CacheValue<>("q", 10_000));

        List<List<ICacheValue<?>>> scanResults = cacheMap.findByDomain(CacheDomain.SCANS);
        assertEquals(1, scanResults.size());

        List<List<ICacheValue<?>>> qrResults = cacheMap.findByDomain(CacheDomain.QR);
        assertEquals(1, qrResults.size());
    }

    @Test
    void testContainsPayloadAndRemoval() {
        CacheKey<String> key = new CacheKey<>("d3", CacheType.MEMORY, CacheDomain.TRACKING);
        CacheValue<String> value = new CacheValue<>("payload", 10_000);

        cacheMap.add(key, value);
        assertTrue(cacheMap.containsPayload(key, "payload"));

        cacheMap.removeValue(value);
        assertFalse(cacheMap.containsPayload(key, "payload"));
    }
}
