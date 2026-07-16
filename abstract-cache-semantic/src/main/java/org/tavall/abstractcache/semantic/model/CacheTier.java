package org.tavall.abstractcache.semantic.model;

/**
 * Logical cache tiers used by the semantic cache engine.
 */
public enum CacheTier {
    HOT_MEMORY,
    LOCAL_DISK,
    LOCAL_HOT_SERVICE,
    REMOTE_HOT,
    LOCAL_COLD_SERVICE,
    REMOTE_COLD
}
