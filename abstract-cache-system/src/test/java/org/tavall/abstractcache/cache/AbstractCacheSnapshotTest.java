package org.tavall.abstractcache.cache;

import org.junit.jupiter.api.Test;
import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.enums.CacheVersion;
import org.tavall.abstractcache.cache.interfaces.ICacheKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AbstractCacheSnapshotTest {
    private final ProbeCache cache = new ProbeCache();

    @Test
    void snapshotsExposeOnlyImmutableCacheViews() {
        cache.save("alpha", "one");
        cache.save("beta", "two");

        Map<ICacheKey<String>, String> entries = cache.snapshotEntries();
        List<String> values = cache.snapshotValues();

        assertEquals(2, entries.size());
        assertTrue(values.containsAll(List.of("one", "two")));
        assertThrows(
                UnsupportedOperationException.class,
                () -> entries.clear()
        );
    }

    @Test
    void filteredRemovalUsesTypedKeysAndValues() {
        cache.save("alpha", "one");
        cache.save("beta", "two");

        int removed = cache.removeIf((key, value) ->
                key.getRawCacheKey().startsWith("a") && value.equals("one")
        );

        assertEquals(1, removed);
        assertFalse(cache.contains("alpha"));
        assertTrue(cache.contains("beta"));
    }

    private static final class ProbeCache extends AbstractCache<String, String> {
        private static final CacheDomain DOMAIN = CacheDomain.DEBUG;
        private static final CacheType TYPE = CacheType.MEMORY;
        private static final CacheSource SOURCE = CacheSource.LOCAL;
        private static final CacheVersion VERSION = CacheVersion.V1_0;

        private ProbeCache() {
            super(1L, TimeUnit.HOURS);
        }

        private void save(String key, String value) {
            put(cacheKey(key), value);
        }

        private boolean contains(String key) {
            return containsKey(key, DOMAIN, TYPE, VERSION, SOURCE);
        }

        private ICacheKey<String> cacheKey(String key) {
            return createKey(key, TYPE, DOMAIN, SOURCE, VERSION);
        }
    }
}
