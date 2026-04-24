package org.tavall.abstractcache.semantic.routing;

import org.tavall.abstractcache.semantic.model.SemanticCacheKey;

import java.time.Duration;

/**
 * Resolves semantic cache routes from metadata and TTL.
 */
public interface CacheRoutingPolicy {

    /**
     * Resolves the route for a semantic cache key.
     *
     * @param key semantic key
     * @param initialTtl initial caller-supplied TTL
     * @return route decision
     */
    RouteDecision decide(SemanticCacheKey key, Duration initialTtl);
}
