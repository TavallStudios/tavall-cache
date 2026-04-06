package org.tavall.abstractcache.semantic.stats;

public final class CacheStatsProviderAlreadyRegisteredException extends IllegalStateException {

    public CacheStatsProviderAlreadyRegisteredException(String cacheName) {
        super("cache stats provider already registered: " + cacheName);
    }
}
