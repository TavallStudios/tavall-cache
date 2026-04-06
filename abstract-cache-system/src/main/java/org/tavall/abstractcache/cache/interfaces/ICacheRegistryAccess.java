package org.tavall.abstractcache.cache.interfaces;

import org.tavall.abstractcache.cache.metadata.CacheRegistryMetaData;
import org.tavall.dependency.IDependencyInjectableInterface;

import java.util.Set;

public interface ICacheRegistryAccess extends IDependencyInjectableInterface {

    ICacheRegistryAccess registerCacheIfAbsent(CacheRegistryMetaData cacheRegistryMetaData);

    CacheRegistryMetaData getCacheRegistryMetaData(Class<?> cacheClass);

    boolean hasCacheRegistryMetaData(Class<?> cacheClass);

    Set<Class<?>> getRegisteredCacheClasses();
}
