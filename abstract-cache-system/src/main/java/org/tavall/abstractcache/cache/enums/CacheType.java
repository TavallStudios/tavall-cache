package org.tavall.abstractcache.cache.enums;

/**
 * Logical storage strategy for a cache key.
 */
public enum CacheType {
    MEMORY,
    REDIS,
    DISK,
    HYBRID,
    DATABASE
}
