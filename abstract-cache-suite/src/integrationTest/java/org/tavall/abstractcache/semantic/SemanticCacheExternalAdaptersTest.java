package org.tavall.abstractcache.semantic;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheVersion;
import org.tavall.abstractcache.semantic.model.CacheTag;
import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheEntry;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.model.StandardCacheTags;
import org.tavall.abstractcache.semantic.routing.CacheRoutingPolicy;
import org.tavall.abstractcache.semantic.routing.RouteDecision;
import org.tavall.abstractcache.semantic.routing.RouteStage;
import org.tavall.abstractcache.semantic.spi.CacheCodec;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;
import org.tavall.abstractcache.semantic.storage.disk.LocalDiskTierAdapter;
import org.tavall.abstractcache.semantic.storage.memory.HotMemoryTierAdapter;
import org.tavall.abstractcache.semantic.storage.mongo.MongoTierAdapter;
import org.tavall.abstractcache.semantic.storage.postgres.PostgresTierAdapter;
import org.tavall.abstractcache.semantic.storage.qdrant.QdrantTierAdapter;
import org.tavall.abstractcache.semantic.storage.redis.RedisTierAdapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SemanticCacheExternalAdaptersTest {

    Path tempDir;

    private LocalServiceHarness.RedisService redisService;
    private LocalServiceHarness.PostgresService postgresService;
    private LocalServiceHarness.MongoService mongoService;

    @BeforeAll
    void startServices() throws Exception {
        tempDir = Files.createTempDirectory("semantic-cache-it-");
        Path servicesRoot = tempDir.resolve("services");
        redisService = LocalServiceHarness.startRedis(servicesRoot);
        postgresService = LocalServiceHarness.startPostgres(servicesRoot);
        mongoService = LocalServiceHarness.startMongo(servicesRoot);
    }

    @AfterAll
    void stopServices() {
        closeQuietly(mongoService);
        closeQuietly(postgresService);
        closeQuietly(redisService);
        deleteQuietly(tempDir);
    }

    @Test
    void testTieredTtlDemotion_TraversesEveryLiveTierAndPromotesBackToHotMemory() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-10T00:00:00Z"));
        HotMemoryTierAdapter hotMemory = new HotMemoryTierAdapter();
        RedisTierAdapter localHot = new RedisTierAdapter(CacheTier.LOCAL_HOT_SERVICE, redisService.url());
        RedisTierAdapter remoteHot = new RedisTierAdapter(CacheTier.REMOTE_HOT, redisService.url());
        LocalDiskTierAdapter localDisk = new LocalDiskTierAdapter(tempDir.resolve("full-route-disk"));
        PostgresTierAdapter localCold = new PostgresTierAdapter(
                CacheTier.LOCAL_COLD_SERVICE,
                postgresService.jdbcUrl(),
                postgresService.username(),
                postgresService.password(),
                "semantic_cache_local_cold_" + uniqueSuffix()
        );
        MongoTierAdapter remoteCold = new MongoTierAdapter(
                CacheTier.REMOTE_COLD,
                mongoService.connectionString(),
                "abstract_cache_integration",
                "semantic_cache_remote_cold_" + uniqueSuffix()
        );

        SemanticCache cache = new SemanticCacheBuilder()
                .cacheName("semantic-live-tier-chain")
                .withAdapter(hotMemory)
                .withAdapter(localHot)
                .withAdapter(remoteHot)
                .withAdapter(localDisk)
                .withAdapter(localCold)
                .withAdapter(remoteCold)
                .withRoutingPolicy(fullStackRoutingPolicy())
                .withClock(clock)
                .withMaintenanceInterval(Duration.ZERO)
                .build();

        SemanticCacheKey key = semanticKey("full-stack-context", CacheTag.of("FULL_STACK"));
        try {
            cache.put(key, "global-combat-solution", Duration.ofSeconds(1), stringCodec());
            assertTierOccupancy(hotMemory, localHot, remoteHot, localDisk, localCold, remoteCold,
                    1, 0, 0, 0, 0, 0);

            advanceAndDemote(clock, cache);
            assertTierOccupancy(hotMemory, localHot, remoteHot, localDisk, localCold, remoteCold,
                    0, 1, 0, 0, 0, 0);

            advanceAndDemote(clock, cache);
            assertTierOccupancy(hotMemory, localHot, remoteHot, localDisk, localCold, remoteCold,
                    0, 0, 1, 0, 0, 0);

            advanceAndDemote(clock, cache);
            assertTierOccupancy(hotMemory, localHot, remoteHot, localDisk, localCold, remoteCold,
                    0, 0, 0, 1, 0, 0);

            advanceAndDemote(clock, cache);
            assertTierOccupancy(hotMemory, localHot, remoteHot, localDisk, localCold, remoteCold,
                    0, 0, 0, 0, 1, 0);

            advanceAndDemote(clock, cache);
            assertTierOccupancy(hotMemory, localHot, remoteHot, localDisk, localCold, remoteCold,
                    0, 0, 0, 0, 0, 1);

            SemanticCacheEntry<String> entry = cache.getEntry(key, stringCodec()).orElseThrow();
            Optional<ICacheValue<String>> hit = cache.get(key, stringCodec());

            assertTrue(hit.isPresent());
            assertEquals("global-combat-solution", hit.get().getValue());
            assertInstanceOf(SemanticCacheEntry.class, hit.get());
            assertEquals(CacheTier.REMOTE_COLD, entry.currentTier());
            assertTrue(entry.promoted());
            assertEquals("global-combat-solution", entry.value());
            assertTierOccupancy(hotMemory, localHot, remoteHot, localDisk, localCold, remoteCold,
                    1, 0, 0, 0, 0, 1);
        } finally {
            cache.invalidate(key);
            localHot.clear();
            remoteHot.clear();
            localCold.clear();
            remoteCold.clear();
            cache.close();
        }
    }

    @Test
    void testBreedingRoute_UsesLocalDiskPostgresAndMongoAsColdChain() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-10T02:00:00Z"));
        LocalDiskTierAdapter localDisk = new LocalDiskTierAdapter(tempDir.resolve("breeding-route-disk"));
        PostgresTierAdapter localCold = new PostgresTierAdapter(
                CacheTier.LOCAL_COLD_SERVICE,
                postgresService.jdbcUrl(),
                postgresService.username(),
                postgresService.password(),
                "semantic_cache_breeding_local_cold_" + uniqueSuffix()
        );
        MongoTierAdapter remoteCold = new MongoTierAdapter(
                CacheTier.REMOTE_COLD,
                mongoService.connectionString(),
                "abstract_cache_integration",
                "semantic_cache_breeding_remote_cold_" + uniqueSuffix()
        );

        SemanticCache cache = new SemanticCacheBuilder()
                .cacheName("semantic-breeding-live-chain")
                .withAdapter(localDisk)
                .withAdapter(localCold)
                .withAdapter(remoteCold)
                .withRoutingPolicy(breedingRoutingPolicy())
                .withClock(clock)
                .withMaintenanceInterval(Duration.ZERO)
                .build();

        SemanticCacheKey key = semanticKey("breeding-stack-context", StandardCacheTags.BREEDING);
        try {
            cache.put(key, "offspring-plan", Duration.ofSeconds(1), stringCodec());
            assertTierOccupancy(null, null, null, localDisk, localCold, remoteCold,
                    0, 0, 0, 1, 0, 0);

            advanceAndDemote(clock, cache);
            assertTierOccupancy(null, null, null, localDisk, localCold, remoteCold,
                    0, 0, 0, 0, 1, 0);

            advanceAndDemote(clock, cache);
            assertTierOccupancy(null, null, null, localDisk, localCold, remoteCold,
                    0, 0, 0, 0, 0, 1);

            Optional<SemanticCacheEntry<String>> hit = cache.getEntry(key, stringCodec());

            assertTrue(hit.isPresent());
            assertEquals(CacheTier.REMOTE_COLD, hit.get().currentTier());
            assertTrue(hit.get().promoted());
            assertEquals("offspring-plan", hit.get().value());
            assertTierOccupancy(null, null, null, localDisk, localCold, remoteCold,
                    0, 0, 0, 1, 0, 1);
        } finally {
            cache.invalidate(key);
            localCold.clear();
            remoteCold.clear();
            cache.close();
        }
    }

    @Test
    void testQdrantRemoteCold_StoresAfterDemotionAndPromotesBackToLocalDisk() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-10T04:00:00Z"));
        LocalDiskTierAdapter localDisk = new LocalDiskTierAdapter(tempDir.resolve("qdrant-route-disk"));
        QdrantTierAdapter remoteCold = new QdrantTierAdapter(
                CacheTier.REMOTE_COLD,
                "http://127.0.0.1:6333",
                "semantic_cache_qdrant_" + uniqueSuffix()
        );

        SemanticCache cache = new SemanticCacheBuilder()
                .cacheName("semantic-qdrant-live-chain")
                .withAdapter(localDisk)
                .withAdapter(remoteCold)
                .withRoutingPolicy(qdrantColdRoutingPolicy())
                .withClock(clock)
                .withMaintenanceInterval(Duration.ZERO)
                .build();

        SemanticCacheKey key = semanticKey("qdrant-stack-context", CacheTag.of("QDRANT"));
        try {
            cache.put(key, "vector-backed-plan", Duration.ofSeconds(1), stringCodec());
            assertTierOccupancy(null, null, null, localDisk, null, remoteCold,
                    0, 0, 0, 1, 0, 0);

            advanceAndDemote(clock, cache);
            assertTierOccupancy(null, null, null, localDisk, null, remoteCold,
                    0, 0, 0, 0, 0, 1);

            Optional<SemanticCacheEntry<String>> hit = cache.getEntry(key, stringCodec());

            assertTrue(hit.isPresent());
            assertEquals(CacheTier.REMOTE_COLD, hit.get().currentTier());
            assertTrue(hit.get().promoted());
            assertEquals("vector-backed-plan", hit.get().value());
            assertTierOccupancy(null, null, null, localDisk, null, remoteCold,
                    0, 0, 0, 1, 0, 1);
        } finally {
            cache.invalidate(key);
            remoteCold.dropCollection();
            cache.close();
        }
    }

    private void advanceAndDemote(MutableClock clock, SemanticCache cache) {
        clock.advance(Duration.ofSeconds(2));
        cache.runMaintenanceCycle();
    }

    private void assertTierOccupancy(
            HotMemoryTierAdapter hotMemory,
            RedisTierAdapter localHot,
            RedisTierAdapter remoteHot,
            LocalDiskTierAdapter localDisk,
            PostgresTierAdapter localCold,
            CacheTierAdapter remoteCold,
            int expectedHotMemory,
            int expectedLocalHot,
            int expectedRemoteHot,
            int expectedLocalDisk,
            int expectedLocalCold,
            int expectedRemoteCold
    ) {
        if (hotMemory != null) {
            assertEquals(expectedHotMemory, hotMemory.snapshotRecords().size());
        }
        if (localHot != null) {
            assertEquals(expectedLocalHot, localHot.snapshotRecords().size());
        }
        if (remoteHot != null) {
            assertEquals(expectedRemoteHot, remoteHot.snapshotRecords().size());
        }
        if (localDisk != null) {
            assertEquals(expectedLocalDisk, localDisk.snapshotRecords().size());
        }
        if (localCold != null) {
            assertEquals(expectedLocalCold, localCold.snapshotRecords().size());
        }
        if (remoteCold != null) {
            assertEquals(expectedRemoteCold, remoteCold.snapshotRecords().size());
        }
    }

    private CacheRoutingPolicy fullStackRoutingPolicy() {
        return (key, initialTtl) -> new RouteDecision(List.of(
                new RouteStage(CacheTier.HOT_MEMORY, initialTtl),
                new RouteStage(CacheTier.LOCAL_HOT_SERVICE, Duration.ofSeconds(1)),
                new RouteStage(CacheTier.REMOTE_HOT, Duration.ofSeconds(1)),
                new RouteStage(CacheTier.LOCAL_DISK, Duration.ofSeconds(1)),
                new RouteStage(CacheTier.LOCAL_COLD_SERVICE, Duration.ofSeconds(1)),
                new RouteStage(CacheTier.REMOTE_COLD, Duration.ofSeconds(1))
        ));
    }

    private CacheRoutingPolicy breedingRoutingPolicy() {
        return (key, initialTtl) -> new RouteDecision(List.of(
                new RouteStage(CacheTier.LOCAL_DISK, initialTtl),
                new RouteStage(CacheTier.LOCAL_COLD_SERVICE, Duration.ofSeconds(1)),
                new RouteStage(CacheTier.REMOTE_COLD, Duration.ofSeconds(1))
        ));
    }

    private CacheRoutingPolicy qdrantColdRoutingPolicy() {
        return (key, initialTtl) -> new RouteDecision(List.of(
                new RouteStage(CacheTier.LOCAL_DISK, initialTtl),
                new RouteStage(CacheTier.REMOTE_COLD, Duration.ofSeconds(1))
        ));
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

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception exception) {
            throw new IllegalStateException("failed to stop integration test service", exception);
        }
    }

    private void deleteQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception exception) {
                            throw new IllegalStateException("failed to clean integration temp directory", exception);
                        }
                    });
        } catch (Exception exception) {
            throw new IllegalStateException("failed to clean integration temp directory", exception);
        }
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
