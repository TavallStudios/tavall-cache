package org.tavall.abstractcache.semantic.stats;

import org.tavall.abstractcache.cache.interfaces.ICacheStats;
import org.tavall.abstractcache.cache.metadata.CacheMetaData;
import org.tavall.abstractcache.semantic.model.CacheTier;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Detailed semantic cache stats snapshot.
 */
public final class SemanticCacheStats implements ICacheStats {

    private final CacheMetaData occupancy;
    private final Map<CacheTier, CacheMetaData> occupancyByTier;
    private final long hits;
    private final long misses;
    private final Map<CacheTier, Long> hitsByTier;
    private final long promotions;
    private final long demotions;
    private final long terminalEvictions;
    private final long adapterWriteFailures;
    private final long adapterAvailabilitySkips;

    public SemanticCacheStats(
            CacheMetaData occupancy,
            Map<CacheTier, CacheMetaData> occupancyByTier,
            long hits,
            long misses,
            Map<CacheTier, Long> hitsByTier,
            long promotions,
            long demotions,
            long terminalEvictions,
            long adapterWriteFailures,
            long adapterAvailabilitySkips
    ) {
        this.occupancy = occupancy;
        this.occupancyByTier = Collections.unmodifiableMap(new EnumMap<>(occupancyByTier));
        this.hits = hits;
        this.misses = misses;
        this.hitsByTier = Collections.unmodifiableMap(new EnumMap<>(hitsByTier));
        this.promotions = promotions;
        this.demotions = demotions;
        this.terminalEvictions = terminalEvictions;
        this.adapterWriteFailures = adapterWriteFailures;
        this.adapterAvailabilitySkips = adapterAvailabilitySkips;
    }

    @Override
    public int getTotalEntries() {
        return occupancy.getTotalEntries();
    }

    @Override
    public int getValidEntries() {
        return occupancy.getValidEntries();
    }

    @Override
    public int getExpiredEntries() {
        return occupancy.getExpiredEntries();
    }

    public CacheMetaData occupancy() {
        return occupancy;
    }

    public Map<CacheTier, CacheMetaData> occupancyByTier() {
        return occupancyByTier;
    }

    public long hits() {
        return hits;
    }

    public long misses() {
        return misses;
    }

    public Map<CacheTier, Long> hitsByTier() {
        return hitsByTier;
    }

    public long promotions() {
        return promotions;
    }

    public long demotions() {
        return demotions;
    }

    public long terminalEvictions() {
        return terminalEvictions;
    }

    public long adapterWriteFailures() {
        return adapterWriteFailures;
    }

    public long adapterAvailabilitySkips() {
        return adapterAvailabilitySkips;
    }
}
