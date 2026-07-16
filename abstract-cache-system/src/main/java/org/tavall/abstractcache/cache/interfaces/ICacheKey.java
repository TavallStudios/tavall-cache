package org.tavall.abstractcache.cache.interfaces;

import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.enums.CacheVersion;

/**
 * Read-only view of a cache key and its metadata.
 *
 * @param <K> raw key type
 */
public interface ICacheKey<K> {

    /**
     * Returns the original domain key object.
     *
     * @return raw key
     */
    K getRawCacheKey();

    /**
     * Returns the storage strategy associated with this key.
     *
     * @return cache type or {@code null} if unspecified
     */
    CacheType getCacheType();

    /**
     * Returns the domain partition for this key.
     *
     * @return cache domain or {@code null} if unspecified
     */
    CacheDomain getCacheDomain();

    /**
     * Returns the producing subsystem for this key.
     *
     * @return cache source or {@code null} if unspecified
     */
    CacheSource getSource();

    /**
     * Returns the version tag for this key.
     *
     * @return cache version or {@code null} if unspecified
     */
    CacheVersion getVersion();
}
