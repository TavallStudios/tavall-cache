package org.tavall.abstractcache.cache.metadata;

public final class CacheEntryMetaDataBuilder {

    private MemoryPolicy memoryPolicy = MemoryPolicy.HOT;
    private boolean persistent;
    private boolean distributed;
    private boolean profileEnabled;

    public CacheEntryMetaDataBuilder memoryPolicy(MemoryPolicy memoryPolicy) {
        if (memoryPolicy != null) {
            this.memoryPolicy = memoryPolicy;
        }
        return this;
    }

    public CacheEntryMetaDataBuilder persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    public CacheEntryMetaDataBuilder distributed(boolean distributed) {
        this.distributed = distributed;
        return this;
    }

    public CacheEntryMetaDataBuilder profileEnabled(boolean profileEnabled) {
        this.profileEnabled = profileEnabled;
        return this;
    }

    public CacheEntryMetaData build() {
        return new CacheEntryMetaData(
                memoryPolicy,
                persistent,
                distributed,
                profileEnabled
        );
    }
}
