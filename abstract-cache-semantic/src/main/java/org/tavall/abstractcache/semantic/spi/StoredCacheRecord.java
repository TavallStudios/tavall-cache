package org.tavall.abstractcache.semantic.spi;

import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.routing.RouteStage;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Serialized semantic cache record stored in tier adapters.
 */
public final class StoredCacheRecord {

    private final SemanticCacheKey key;
    private final byte[] payloadBytes;
    private final String codecId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final int routeIndex;
    private final List<RouteStage> routeStages;

    public StoredCacheRecord(
            SemanticCacheKey key,
            byte[] payloadBytes,
            String codecId,
            Instant createdAt,
            Instant expiresAt,
            int routeIndex,
            List<RouteStage> routeStages
    ) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.payloadBytes = Objects.requireNonNull(payloadBytes, "payloadBytes cannot be null").clone();
        this.codecId = Objects.requireNonNull(codecId, "codecId cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.routeStages = List.copyOf(routeStages);
        if (this.routeStages.isEmpty()) {
            throw new IllegalArgumentException("routeStages cannot be empty");
        }
        if (routeIndex < 0 || routeIndex >= this.routeStages.size()) {
            throw new IllegalArgumentException("routeIndex out of bounds");
        }
        this.routeIndex = routeIndex;
    }

    public SemanticCacheKey key() {
        return key;
    }

    public byte[] payloadBytes() {
        return payloadBytes.clone();
    }

    public String codecId() {
        return codecId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public int routeIndex() {
        return routeIndex;
    }

    public List<RouteStage> routeStages() {
        return routeStages;
    }

    public CacheTier currentTier() {
        return routeStages.get(routeIndex).tier();
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public StoredCacheRecord moveToStage(int newRouteIndex, Instant newExpiresAt) {
        return new StoredCacheRecord(key, payloadBytes, codecId, createdAt, newExpiresAt, newRouteIndex, routeStages);
    }
}
