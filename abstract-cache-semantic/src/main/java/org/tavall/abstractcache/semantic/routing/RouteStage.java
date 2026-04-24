package org.tavall.abstractcache.semantic.routing;

import org.tavall.abstractcache.semantic.model.CacheTier;

import java.time.Duration;
import java.util.Objects;

/**
 * Single tier stage in a semantic cache route.
 */
public final class RouteStage {

    private final CacheTier tier;
    private final Duration ttl;

    public RouteStage(CacheTier tier, Duration ttl) {
        this.tier = Objects.requireNonNull(tier, "tier cannot be null");
        this.ttl = Objects.requireNonNull(ttl, "ttl cannot be null");
        if (ttl.isNegative()) {
            throw new IllegalArgumentException("ttl cannot be negative");
        }
    }

    public CacheTier tier() {
        return tier;
    }

    public Duration ttl() {
        return ttl;
    }
}
