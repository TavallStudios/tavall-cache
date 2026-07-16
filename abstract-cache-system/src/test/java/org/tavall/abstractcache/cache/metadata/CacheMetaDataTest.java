package org.tavall.abstractcache.cache.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheMetaDataTest {

    @Test
    void testEmpty_ReturnsZeroedSnapshot() {
        CacheMetaData metadata = CacheMetaData.empty();

        assertEquals(0, metadata.getTotalEntries());
        assertEquals(0, metadata.getValidEntries());
        assertEquals(0, metadata.getExpiredEntries());
    }

    @Test
    void testPlus_AggregatesSnapshots() {
        CacheMetaData left = new CacheMetaData(2, 1, 1);
        CacheMetaData right = new CacheMetaData(3, 2, 1);

        CacheMetaData combined = left.plus(right);

        assertEquals(5, combined.getTotalEntries());
        assertEquals(3, combined.getValidEntries());
        assertEquals(2, combined.getExpiredEntries());
    }
}
