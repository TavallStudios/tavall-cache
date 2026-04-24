package org.tavall.abstractcache.cache;

import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.enums.CacheVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheKeyTest {

    @Test
    void testConstructorAndGetters() {
        String rawKey = "user-123";
        CacheKey<String> key = new CacheKey<>(
                rawKey,
                CacheType.MEMORY,
                CacheDomain.SCANS,
                CacheSource.AI_SCANNER,
                CacheVersion.V1_0
        );

        assertEquals(rawKey, key.getRawCacheKey());
        assertEquals(CacheType.MEMORY, key.getCacheType());
        assertEquals(CacheDomain.SCANS, key.getCacheDomain());
        assertEquals(CacheSource.AI_SCANNER, key.getSource());
        assertEquals(CacheVersion.V1_0, key.getVersion());
        assertTrue(key.getCreatedAt() > 0);
    }

    @Test
    void testEquality_CorrectlyDistinguishesDomains() {
        CacheKey<String> key1 = new CacheKey<>("key", CacheType.MEMORY, CacheDomain.SCANS);
        CacheKey<String> key2 = new CacheKey<>("key", CacheType.MEMORY, CacheDomain.QR);

        assertNotEquals(key1, key2);
        assertNotEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void testAccessCountIncrement() {
        CacheKey<String> key = new CacheKey<>("user-123", CacheType.MEMORY, CacheDomain.SCANS);

        assertEquals(0, key.getAccessCount());
        assertEquals(1, key.incrementAccessCount());
        assertEquals(1, key.getAccessCount());
    }

    @Test
    void testHashCode_Matches_Equality_Contract() {
        CacheKey<String> key1 = new CacheKey<>("user-session", CacheType.MEMORY, CacheDomain.SCANS);
        CacheKey<String> key2 = new CacheKey<>("user-session", CacheType.MEMORY, CacheDomain.SCANS);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }
}
