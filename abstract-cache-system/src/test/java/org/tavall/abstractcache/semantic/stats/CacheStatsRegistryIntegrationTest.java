package org.tavall.abstractcache.semantic.stats;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tavall.abstractcache.semantic.stats.fixtures.PlayerProfileCache;
import org.tavall.dependency.DependencyLoaderAccess;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheStatsRegistryIntegrationTest {

    @AfterEach
    void tearDown() {
        DependencyLoaderAccess.clear();
        CacheStatsRegistry.getInstance().clear();
    }

    @Test
    void registerBuffersCacheRegistryMetaDataUntilDiIsReady() {
        assertDoesNotThrow(PlayerProfileCache::new);

        assertTrue(CacheStatsRegistry.getInstance().snapshot(PlayerProfileCache.CACHE_NAME).isPresent());
        assertTrue(CacheStatsRegistry.getInstance().hasPendingCacheRegistryMetaData(PlayerProfileCache.class));
    }
}
