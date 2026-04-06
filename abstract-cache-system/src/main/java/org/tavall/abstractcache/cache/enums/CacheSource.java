package org.tavall.abstractcache.cache.enums;

/**
 * Source marker describing the subsystem that generated the cache key.
 */
public enum CacheSource {
    AI_SCANNER,
    SCAN_ERROR_TRACKER,
    QR_CODE_GENERATOR,
    TRACKING_NUMBER_GENERATOR,
    DELIVERY_STATE_TRACKER,
    USER_ACCOUNT_SERVICE,
    ROUTE_PLANNER
}
