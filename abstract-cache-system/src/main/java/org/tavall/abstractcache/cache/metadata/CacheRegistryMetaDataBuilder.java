package org.tavall.abstractcache.cache.metadata;

import org.tavall.dependency.metadata.DependencySource;

public final class CacheRegistryMetaDataBuilder {

    private Class<?> cacheClass;
    private String cacheName;
    private Class<?> keyClass;
    private Class<?> valueClass;
    private Class<?> bucketClass;
    private Class<?> cacheEntryMetaDataClass;
    private DependencySource dependencySource = DependencySource.CACHE;
    private MemoryPolicy memoryPolicy = MemoryPolicy.HOT;
    private boolean persistent;
    private boolean distributed;
    private boolean profileEnabled;
    private String ownerModule;

    public CacheRegistryMetaDataBuilder cacheClass(Class<?> cacheClass) {
        this.cacheClass = cacheClass;
        return this;
    }

    public CacheRegistryMetaDataBuilder cacheName(String cacheName) {
        this.cacheName = cacheName;
        return this;
    }

    public CacheRegistryMetaDataBuilder keyClass(Class<?> keyClass) {
        this.keyClass = keyClass;
        return this;
    }

    public CacheRegistryMetaDataBuilder valueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
        return this;
    }

    public CacheRegistryMetaDataBuilder bucketClass(Class<?> bucketClass) {
        this.bucketClass = bucketClass;
        return this;
    }

    public CacheRegistryMetaDataBuilder cacheEntryMetaDataClass(Class<?> cacheEntryMetaDataClass) {
        this.cacheEntryMetaDataClass = cacheEntryMetaDataClass;
        return this;
    }

    public CacheRegistryMetaDataBuilder dependencySource(DependencySource dependencySource) {
        if (dependencySource != null) {
            this.dependencySource = dependencySource;
        }
        return this;
    }

    public CacheRegistryMetaDataBuilder memoryPolicy(MemoryPolicy memoryPolicy) {
        if (memoryPolicy != null) {
            this.memoryPolicy = memoryPolicy;
        }
        return this;
    }

    public CacheRegistryMetaDataBuilder persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    public CacheRegistryMetaDataBuilder distributed(boolean distributed) {
        this.distributed = distributed;
        return this;
    }

    public CacheRegistryMetaDataBuilder profileEnabled(boolean profileEnabled) {
        this.profileEnabled = profileEnabled;
        return this;
    }

    public CacheRegistryMetaDataBuilder ownerModule(String ownerModule) {
        this.ownerModule = ownerModule;
        return this;
    }

    public CacheRegistryMetaData build() {
        return new CacheRegistryMetaData(
                cacheClass,
                cacheName,
                keyClass,
                valueClass,
                bucketClass,
                cacheEntryMetaDataClass,
                dependencySource,
                memoryPolicy,
                persistent,
                distributed,
                profileEnabled,
                ownerModule
        );
    }
}
