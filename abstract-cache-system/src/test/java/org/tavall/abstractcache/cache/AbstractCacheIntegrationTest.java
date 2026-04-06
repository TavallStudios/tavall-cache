package org.tavall.abstractcache.cache;

import org.junit.jupiter.api.Test;
import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.enums.CacheVersion;
import org.tavall.abstractcache.cache.interfaces.ICacheKey;
import org.tavall.abstractcache.cache.interfaces.ICacheStats;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractCacheIntegrationTest {

    @Test
    void getCachesLoadedValueUntilExpired() {
        TestCache cache = new TestCache(200);
        AtomicInteger loaderCalls = new AtomicInteger();

        String first = cache.get("alpha", key -> {
            loaderCalls.incrementAndGet();
            return "payload";
        });
        String second = cache.get("alpha", key -> {
            loaderCalls.incrementAndGet();
            return "fallback";
        });

        assertEquals("payload", first);
        assertEquals("payload", second);
        assertEquals(1, loaderCalls.get());
    }

    @Test
    void getIfPresentExpiresAndTracksEntries() throws InterruptedException {
        TestCache cache = new TestCache(1);
        ICacheKey<String> key = cache.createKey(
                "beta",
                CacheType.MEMORY,
                CacheDomain.USER,
                CacheSource.LOCAL,
                CacheVersion.V1_0
        );

        cache.put(key, "value", 1);
        Thread.sleep(5);

        assertNull(cache.getIfPresent(
                "beta",
                CacheDomain.USER,
                CacheType.MEMORY,
                CacheVersion.V1_0,
                CacheSource.LOCAL
        ));

        ICacheStats stats = cache.getCacheStats();
        assertEquals(0, stats.getValidEntries());
        assertEquals(1, stats.getExpiredEntries());
    }

    @Test
    void cleanupExpiredRemovesEntries() throws InterruptedException {
        TestCache cache = new TestCache(1);
        ICacheKey<String> key = cache.createKey("gamma", CacheType.MEMORY);

        cache.put(key, "value", 1);
        Thread.sleep(5);

        assertEquals(1, cache.cleanupExpired());
    }

    private static final class TestCache extends AbstractCache<String, String> {
        private TestCache(long ttlMillis) {
            super(ttlMillis, TimeUnit.MILLISECONDS);
        }
    }
}
