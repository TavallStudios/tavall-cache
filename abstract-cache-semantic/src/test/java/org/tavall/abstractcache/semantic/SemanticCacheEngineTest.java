package org.tavall.abstractcache.semantic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.abstractcache.cache.CacheKey;
import org.tavall.abstractcache.cache.CacheValue;
import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.enums.CacheVersion;
import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.cache.maps.CacheMap;
import org.tavall.abstractcache.semantic.model.CacheTag;
import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheEntry;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.model.StandardCacheTags;
import org.tavall.abstractcache.semantic.spi.CacheCodec;
import org.tavall.abstractcache.semantic.stats.ApplicationCacheStats;
import org.tavall.abstractcache.semantic.stats.CacheStatsRegistry;
import org.tavall.abstractcache.semantic.stats.SemanticCacheStats;
import org.tavall.abstractcache.semantic.storage.memory.HotMemoryTierAdapter;
import org.tavall.abstractcache.semantic.storage.memory.InMemoryServiceTierAdapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticCacheEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void testSemanticReuse_ReturnsSharedResultForEquivalentContext() {
        SemanticCache cache = new SemanticCacheBuilder()
                .cacheName("semantic-reuse-test")
                .withHotMemoryTier()
                .build();

        try {
            SemanticCacheKey playerAContext = semanticKey("combat-context", StandardCacheTags.COMBAT);
            SemanticCacheKey playerBContext = semanticKey("combat-context", StandardCacheTags.COMBAT);

            cache.put(playerAContext, "maneuver", Duration.ofMinutes(1), stringCodec());

            Optional<ICacheValue<String>> hit = cache.get(playerBContext, stringCodec());

            assertTrue(hit.isPresent());
            assertEquals("maneuver", hit.get().getValue());
            assertInstanceOf(SemanticCacheEntry.class, hit.get());
            SemanticCacheEntry<String> entry = cache.getEntry(playerBContext, stringCodec()).orElseThrow();
            assertEquals(CacheTier.HOT_MEMORY, entry.currentTier());
            assertEquals(CacheType.HYBRID, playerBContext.getCacheType());
        } finally {
            cache.close();
        }
    }

    @Test
    void testBreedingTag_RoutesDirectlyToLocalDisk() {
        SemanticCache cache = new SemanticCacheBuilder()
                .cacheName("semantic-breeding-test")
                .withLocalDiskTier(tempDir.resolve("disk-route"))
                .build();

        try {
            SemanticCacheKey key = semanticKey("breeding-context", StandardCacheTags.BREEDING);

            cache.put(key, "offspring-plan", Duration.ofMinutes(1), stringCodec());

            Optional<SemanticCacheEntry<String>> hit = cache.getEntry(key, stringCodec());
            SemanticCacheStats stats = cache.snapshotStats();

            assertTrue(hit.isPresent());
            assertEquals(CacheTier.LOCAL_DISK, hit.get().currentTier());
            assertEquals(1, stats.occupancyByTier().get(CacheTier.LOCAL_DISK).getTotalEntries());
        } finally {
            cache.close();
        }
    }

    @Test
    void testExpiredCombatEntry_DemotesThenPromotesBackToHotMemory() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-10T00:00:00Z"));
        HotMemoryTierAdapter hotMemory = new HotMemoryTierAdapter();
        InMemoryServiceTierAdapter localHotService = new InMemoryServiceTierAdapter(CacheTier.LOCAL_HOT_SERVICE);

        SemanticCache cache = new SemanticCacheBuilder()
                .cacheName("semantic-demotion-test")
                .withAdapter(hotMemory)
                .withAdapter(localHotService)
                .withClock(clock)
                .withMaintenanceInterval(Duration.ZERO)
                .build();

        try {
            SemanticCacheKey key = semanticKey("combat-demotion", StandardCacheTags.COMBAT);
            cache.put(key, "path", Duration.ofSeconds(5), stringCodec());

            clock.advance(Duration.ofSeconds(6));
            cache.runMaintenanceCycle();

            SemanticCacheStats afterDemotion = cache.snapshotStats();
            assertEquals(0, afterDemotion.occupancyByTier().get(CacheTier.HOT_MEMORY).getValidEntries());
            assertEquals(1, afterDemotion.occupancyByTier().get(CacheTier.LOCAL_HOT_SERVICE).getValidEntries());

            Optional<SemanticCacheEntry<String>> hit = cache.getEntry(key, stringCodec());
            SemanticCacheStats afterPromotion = cache.snapshotStats();

            assertTrue(hit.isPresent());
            assertEquals(CacheTier.LOCAL_HOT_SERVICE, hit.get().currentTier());
            assertTrue(hit.get().promoted());
            assertEquals(1L, afterPromotion.promotions());
            assertEquals(1, afterPromotion.occupancyByTier().get(CacheTier.HOT_MEMORY).getValidEntries());
        } finally {
            cache.close();
        }
    }

    @Test
    void testUnavailablePreferredTier_FallsBackToLocalDisk() {
        HotMemoryTierAdapter hotMemory = new HotMemoryTierAdapter();
        hotMemory.setAvailable(false);

        SemanticCache cache = new SemanticCacheBuilder()
                .cacheName("semantic-fallback-test")
                .withAdapter(hotMemory)
                .withLocalDiskTier(tempDir.resolve("fallback-disk"))
                .build();

        try {
            SemanticCacheKey key = semanticKey("combat-fallback", StandardCacheTags.COMBAT);
            cache.put(key, "fallback-value", Duration.ofMinutes(1), stringCodec());

            Optional<SemanticCacheEntry<String>> hit = cache.getEntry(key, stringCodec());
            SemanticCacheStats stats = cache.snapshotStats();

            assertTrue(hit.isPresent());
            assertEquals(CacheTier.LOCAL_DISK, hit.get().currentTier());
            assertFalse(hit.get().promoted());
            assertTrue(stats.adapterAvailabilitySkips() > 0);
        } finally {
            cache.close();
        }
    }

    @Test
    void testRegistry_AggregatesLegacyAndSemanticCacheStats() {
        CacheMap legacyCache = CacheMap.getCacheMap();
        legacyCache.clear();
        CacheKey<String> legacyKey = new CacheKey<>("legacy", CacheType.MEMORY, CacheDomain.USER);
        legacyCache.add(legacyKey, new CacheValue<>("still-valid", System.currentTimeMillis() + 10_000));
        legacyCache.add(legacyKey, new CacheValue<>("already-expired", System.currentTimeMillis() - 10_000));

        SemanticCache cache = new SemanticCacheBuilder()
                .cacheName("semantic-registry-test")
                .withHotMemoryTier()
                .build();

        try {
            cache.put(semanticKey("registry-context", CacheTag.of("UTILITY")), "value", Duration.ofMinutes(1), stringCodec());

            ApplicationCacheStats snapshot = CacheStatsRegistry.getInstance().snapshot();

            assertNotNull(snapshot.byCache().get(CacheMap.CACHE_NAME));
            assertNotNull(snapshot.byCache().get("semantic-registry-test"));
            assertEquals(3, snapshot.getTotalEntries());
            assertEquals(2, snapshot.getValidEntries());
            assertEquals(1, snapshot.getExpiredEntries());
        } finally {
            cache.close();
            legacyCache.clear();
        }
    }

    private SemanticCacheKey semanticKey(String fingerprint, CacheTag tag) {
        return new SemanticCacheKey(
                fingerprint,
                CacheDomain.TRACKING,
                CacheSource.ROUTE_PLANNER,
                CacheVersion.V1_0,
                Set.of(tag)
        );
    }

    private CacheCodec<String> stringCodec() {
        return new CacheCodec<>() {
            @Override
            public String codecId() {
                return "utf8-string";
            }

            @Override
            public byte[] encode(String value) {
                return value.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public String decode(byte[] bytes) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        };
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}
