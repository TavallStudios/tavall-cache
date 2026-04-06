package org.tavall.abstractcache.cache.enums;

/**
 * Domain marker used to partition cache entries by business concern.
 */
public enum CacheDomain {
    SCANS,
    SCAN_ERRORS,
    QR,
    TRACKING,
    DELIVERY,
    USER,
    ROUTES
}
