package org.tavall.abstractcache.semantic.spi;

import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Storage adapter for a single semantic cache tier.
 */
public interface CacheTierAdapter {

    /**
     * @return tier handled by this adapter
     */
    CacheTier tier();

    /**
     * @return {@code true} when this adapter is currently available
     */
    boolean isAvailable();

    /**
     * Reads a stored record.
     *
     * @param key semantic cache key
     * @return stored record when present
     */
    Optional<StoredCacheRecord> get(SemanticCacheKey key);

    /**
     * Writes a stored record.
     *
     * @param record stored record
     */
    void put(StoredCacheRecord record);

    /**
     * Removes a stored record.
     *
     * @param key semantic cache key
     */
    void remove(SemanticCacheKey key);

    /**
     * Returns expired records up to the supplied limit.
     *
     * @param now current instant
     * @param limit max record count
     * @return expired records
     */
    List<StoredCacheRecord> scanExpired(Instant now, int limit);

    /**
     * Returns a detached snapshot of stored records.
     *
     * @return current stored records
     */
    List<StoredCacheRecord> snapshotRecords();

    /**
     * Releases any adapter-owned resources.
     */
    default void close() {
        // No-op by default.
    }
}
