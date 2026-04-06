package org.tavall.abstractcache.cache;

import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheValueTest {

    @Test
    void testValueRetrieval() {
        String data = "Payload";
        long expiry = System.currentTimeMillis() + 10_000;
        ICacheValue<String> cacheValue = new CacheValue<>(data, expiry);
        ICacheValue<String> sameValue = new CacheValue<>(data, expiry + 500);

        assertEquals(data, cacheValue.getValue());
        assertEquals(cacheValue, sameValue);
    }

    @Test
    void testExpiration() {
        long past = System.currentTimeMillis() - 1_000;
        long future = System.currentTimeMillis() + 10_000;

        CacheValue<String> expiredVal = new CacheValue<>("Old", past);
        CacheValue<String> freshVal = new CacheValue<>("New", future);

        assertTrue(expiredVal.isExpired());
        assertFalse(freshVal.isExpired());
    }

    @Test
    void testExpirationWithScheduler() throws InterruptedException {
        long ttlMs = 100;
        long safetyMargin = 150;
        CacheValue<String> shortLivedVal = new CacheValue<>("Transient", System.currentTimeMillis() + ttlMs);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean expirationDetected = new AtomicBoolean(false);

        try {
            scheduler.schedule(() -> {
                if (shortLivedVal.isExpired()) {
                    expirationDetected.set(true);
                }
                latch.countDown();
            }, ttlMs + safetyMargin, TimeUnit.MILLISECONDS);

            boolean finishedInTime = latch.await(2, TimeUnit.SECONDS);
            assertTrue(finishedInTime);
            assertTrue(expirationDetected.get());
        } finally {
            scheduler.shutdownNow();
        }
    }
}
