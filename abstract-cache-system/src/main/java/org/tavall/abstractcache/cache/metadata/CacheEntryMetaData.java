package org.tavall.abstractcache.cache.metadata;

public final class CacheEntryMetaData {

    private final MemoryPolicy memoryPolicy;
    private final boolean persistent;
    private final boolean distributed;
    private final boolean profileEnabled;

    CacheEntryMetaData(
            MemoryPolicy memoryPolicy,
            boolean persistent,
            boolean distributed,
            boolean profileEnabled
    ) {
        this.memoryPolicy = memoryPolicy;
        this.persistent = persistent;
        this.distributed = distributed;
        this.profileEnabled = profileEnabled;
    }

    public static CacheEntryMetaDataBuilder builder() {
        return new CacheEntryMetaDataBuilder();
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
}
