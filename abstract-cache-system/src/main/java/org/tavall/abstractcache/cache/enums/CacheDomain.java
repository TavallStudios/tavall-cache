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
    ROUTES,
    COMPANIONS,
    TROOPS,
    KINGDOMS,
    ECONOMY,
    GUILDS,
    CASTLES,
    EVENTS,
    INFRASTRUCTURE,
    DEBUG,
    PLAYER_PROFILE,
    WAR_STATE,
    INVENTORY_BACKUP,
    COMMAND_MODE,
    TOURNAMENT
}
