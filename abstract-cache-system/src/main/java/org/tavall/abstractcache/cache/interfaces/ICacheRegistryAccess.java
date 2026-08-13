package org.tavall.abstractcache.cache.interfaces;

import org.tavall.abstractcache.cache.metadata.CacheRegistryMetaData;
import org.tavall.dependency.IDependencyInjectableInterface;

import java.util.Set;

/**
 * Dependency-facing access contract for cache registry metadata.
 *
 * <p>Cache classes are the registry keys. Registration is intentionally idempotent: metadata is
 * published only when its cache class is non-null and that class is not already registered.
 * Lookup methods are null-safe and return the current registry view without creating metadata.</p>
 */
public interface ICacheRegistryAccess extends IDependencyInjectableInterface {

    /**
     * Registers cache metadata when it identifies a cache class that is not already present.
     *
     * <p>A null metadata object or metadata without a cache class is ignored. Existing metadata for
     * the same cache class is preserved rather than replaced.</p>
     *
     * @param cacheRegistryMetaData metadata to publish
     * @return this registry access object for fluent registration
     */
    ICacheRegistryAccess registerCacheIfAbsent(CacheRegistryMetaData cacheRegistryMetaData);

    /**
     * Resolves metadata registered for a cache class.
     *
     * @param cacheClass cache implementation class to resolve
     * @return registered metadata, or {@code null} when the class is null or not registered
     */
    CacheRegistryMetaData getCacheRegistryMetaData(Class<?> cacheClass);

    /**
     * Tests whether metadata is registered for a cache class.
     *
     * @param cacheClass cache implementation class to inspect
     * @return {@code true} when the non-null class has a registry entry
     */
    boolean hasCacheRegistryMetaData(Class<?> cacheClass);

    /**
     * Returns a snapshot of cache classes currently represented in the registry.
     *
     * @return registered cache classes
     */
    Set<Class<?>> getRegisteredCacheClasses();
}
