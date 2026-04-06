package org.tavall.abstractcache.cache.metadata;

import org.tavall.dependency.metadata.DependencySource;

public final class CacheRegistryMetaData {

    private final Class<?> cacheClass;
    private final String cacheName;
    private final Class<?> keyClass;
    private final Class<?> valueClass;
    private final Class<?> bucketClass;
    private final Class<?> cacheEntryMetaDataClass;
    private final DependencySource dependencySource;
    private final MemoryPolicy memoryPolicy;
    private final boolean persistent;
    private final boolean distributed;
    private final boolean profileEnabled;
    private final String ownerModule;

    CacheRegistryMetaData(
            Class<?> cacheClass,
            String cacheName,
            Class<?> keyClass,
            Class<?> valueClass,
            Class<?> bucketClass,
            Class<?> cacheEntryMetaDataClass,
            DependencySource dependencySource,
            MemoryPolicy memoryPolicy,
            boolean persistent,
            boolean distributed,
            boolean profileEnabled,
            String ownerModule
    ) {
        this.cacheClass = cacheClass;
        this.cacheName = cacheName;
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.bucketClass = bucketClass;
        this.cacheEntryMetaDataClass = cacheEntryMetaDataClass;
        this.dependencySource = dependencySource;
        this.memoryPolicy = memoryPolicy;
        this.persistent = persistent;
        this.distributed = distributed;
        this.profileEnabled = profileEnabled;
        this.ownerModule = ownerModule;
    }

    public static CacheRegistryMetaDataBuilder builder() {
        return new CacheRegistryMetaDataBuilder();
    }

    public Class<?> getCacheClass() {
        return cacheClass;
    }

    public String getCacheName() {
        return cacheName;
    }

    public Class<?> getKeyClass() {
        return keyClass;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    public Class<?> getBucketClass() {
        return bucketClass;
    }

    public Class<?> getCacheEntryMetaDataClass() {
        return cacheEntryMetaDataClass;
    }

    public DependencySource getDependencySource() {
        return dependencySource;
    }

    public MemoryPolicy getMemoryPolicy() {
        return memoryPolicy;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isDistributed() {
        return distributed;
    }

    public boolean isProfileEnabled() {
        return profileEnabled;
    }

    public String getOwnerModule() {
        return ownerModule;
    }
}
