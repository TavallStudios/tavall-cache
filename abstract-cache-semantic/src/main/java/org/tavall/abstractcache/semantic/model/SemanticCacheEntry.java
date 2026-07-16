package org.tavall.abstractcache.semantic.model;

import org.tavall.abstractcache.cache.CacheValue;
import org.tavall.abstractcache.semantic.routing.RouteStage;

import java.time.Instant;
import java.util.List;

/**
 * Decoded semantic cache entry and its route metadata.
 *
 * @param <V> payload type
 */
public final class SemanticCacheEntry<V> extends CacheValue<V> {

    private final SemanticCacheKey key;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final boolean promoted;
    private final int routeIndex;
    private final String codecId;
    private final List<RouteStage> routeStages;

    public SemanticCacheEntry(
            SemanticCacheKey key,
            V value,
            Instant createdAt,
            Instant expiresAt,
            boolean promoted,
            int routeIndex,
            String codecId,
            List<RouteStage> routeStages
    ) {
        super(value, expiresAt.toEpochMilli());
        this.key = key;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.promoted = promoted;
        this.routeIndex = routeIndex;
        this.codecId = codecId;
        this.routeStages = List.copyOf(routeStages);
    }

    public SemanticCacheKey key() {
        return key;
    }

    public V value() {
        return getValue();
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean promoted() {
        return promoted;
    }

    public int routeIndex() {
        return routeIndex;
    }

    public String codecId() {
        return codecId;
    }

    public List<RouteStage> routeStages() {
        return routeStages;
    }

    public CacheTier currentTier() {
        return routeStages.get(routeIndex).tier();
    }
}
