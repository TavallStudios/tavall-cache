package org.tavall.abstractcache.cache.user;

import org.tavall.abstractcache.cache.CacheKey;
import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.enums.CacheVersion;

public class UserCacheKey<K> extends CacheKey<K> {

    private final long expirationTime;

    public UserCacheKey(
            K key,
            CacheType cacheType,
            CacheDomain cacheDomain,
            CacheSource source,
            CacheVersion version,
            long expirationTime
    ) {
        super(key, cacheType, cacheDomain, source, version);
        this.expirationTime = expirationTime;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public boolean isExpired(long currentTime) {
        return currentTime > expirationTime;
    }
}
