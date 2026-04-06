package org.tavall.abstractcache.cache.user;

import org.tavall.abstractcache.cache.CacheValue;
import org.tavall.abstractcache.cache.enums.CacheType;

public class UserCacheValue<V> extends CacheValue<V> {

    private final CacheType cacheType;

    public UserCacheValue(V value, CacheType cacheType, long expirationTime) {
        super(value, expirationTime);
        this.cacheType = cacheType;
    }

    public CacheType getCacheType() {
        return cacheType;
    }
}
