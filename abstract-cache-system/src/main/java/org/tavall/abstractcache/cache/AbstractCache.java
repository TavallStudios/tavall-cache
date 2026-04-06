package org.tavall.abstractcache.cache;

import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.enums.CacheVersion;
import org.tavall.abstractcache.cache.interfaces.ICacheKey;
import org.tavall.abstractcache.cache.interfaces.ICacheValue;

/**
 * Base class for cache implementations that need consistent key and value construction.
 *
 * @param <K> raw key type
 * @param <V> payload type
 */
public abstract class AbstractCache<K, V> {

    /**
     * @return cache type to apply to generated keys
     */
    public abstract CacheType getCacheType();

    /**
     * @return cache domain to apply to generated keys
     */
    public abstract CacheDomain getCacheDomain();

    /**
     * @return source marker to apply to generated keys
     */
    public abstract CacheSource getSource();

    /**
     * @return version marker to apply to generated keys
     */
    public abstract CacheVersion getVersion();

    // --- Key factories ---

    /**
     * Creates a key using only the raw key.
     *
     * @param rawKey domain key
     * @return key wrapper
     */
    public ICacheKey<K> createKey(K rawKey) {
        return new CacheKey<>(rawKey);
    }

    /**
     * Creates a key with explicit type.
     *
     * @param rawKey domain key
     * @param type cache type
     * @return key wrapper
     */
    public ICacheKey<K> createKey(K rawKey, CacheType type) {
        return new CacheKey<>(rawKey, type);
    }

    /**
     * Creates a key with explicit type and domain.
     *
     * @param rawKey domain key
     * @param type cache type
     * @param domain cache domain
     * @return key wrapper
     */
    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheDomain domain) {
        return new CacheKey<>(rawKey, type, domain);
    }

    /**
     * Creates a key with explicit type and source.
     *
     * @param rawKey domain key
     * @param type cache type
     * @param source cache source
     * @return key wrapper
     */
    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheSource source) {
        return new CacheKey<>(rawKey, type, source);
    }

    /**
     * Creates a key with explicit type and version.
     *
     * @param rawKey domain key
     * @param type cache type
     * @param version cache version
     * @return key wrapper
     */
    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheVersion version) {
        return new CacheKey<>(rawKey, type, version);
    }

    /**
     * Creates a key with explicit type, domain, and source.
     *
     * @param rawKey domain key
     * @param type cache type
     * @param domain cache domain
     * @param source cache source
     * @return key wrapper
     */
    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheDomain domain, CacheSource source) {
        return new CacheKey<>(rawKey, type, domain, source);
    }

    /**
     * Creates a key with explicit type, source, and version.
     *
     * @param rawKey domain key
     * @param type cache type
     * @param source cache source
     * @param version cache version
     * @return key wrapper
     */
    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheSource source, CacheVersion version) {
        return new CacheKey<>(rawKey, type, source, version);
    }

    /**
     * Creates a key with explicit type, domain, and version.
     *
     * @param rawKey domain key
     * @param type cache type
     * @param domain cache domain
     * @param version cache version
     * @return key wrapper
     */
    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheDomain domain, CacheVersion version) {
        return new CacheKey<>(rawKey, type, domain, version);
    }

    /**
     * Creates a fully specified key.
     *
     * @param rawKey domain key
     * @param type cache type
     * @param domain cache domain
     * @param source cache source
     * @param version cache version
     * @return key wrapper
     */
    public ICacheKey<K> createKey(
            K rawKey,
            CacheType type,
            CacheDomain domain,
            CacheSource source,
            CacheVersion version
    ) {
        return new CacheKey<>(rawKey, type, domain, source, version);
    }

    // --- Value factories ---

    /**
     * Creates a cache value with an explicit expiration timestamp.
     *
     * @param value payload
     * @param expirationTime expiration epoch milliseconds
     * @return value wrapper
     */
    protected ICacheValue<V> createValue(V value, long expirationTime) {
        return new CacheValue<>(value, expirationTime);
    }

    /**
     * Creates a cache value that expires immediately.
     *
     * @param value payload
     * @return value wrapper
     */
    protected ICacheValue<V> createValue(V value) {
        return createValue(value, System.currentTimeMillis());
    }
}
