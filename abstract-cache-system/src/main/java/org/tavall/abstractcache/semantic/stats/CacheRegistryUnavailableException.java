package org.tavall.abstractcache.semantic.stats;

public final class CacheRegistryUnavailableException extends IllegalStateException {

    public CacheRegistryUnavailableException(Class<?> cacheClass) {
        super("Cache registry is unavailable for " + (cacheClass == null ? "unknown-cache" : cacheClass.getName()));
    }
}
