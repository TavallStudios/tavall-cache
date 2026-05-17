package org.tavall.abstractcache.semantic;

import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.semantic.model.SemanticCacheEntry;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheCodec;
import org.tavall.abstractcache.semantic.stats.CacheStatsProvider;
import org.tavall.abstractcache.semantic.stats.SemanticCacheStats;

import java.time.Duration;
import java.util.Optional;

/**
 * Public semantic cache contract.
 */
public interface SemanticCache extends CacheStatsProvider, AutoCloseable {

    /**
     * Stores a semantic cache entry.
     *
     * @param key semantic key
     * @param value payload value
     * @param ttl initial time to live
     * @param codec payload codec
     * @param <V> payload type
     */
    <V> void put(SemanticCacheKey key, V value, Duration ttl, CacheCodec<V> codec);

    /**
     * Reads a semantic cache entry.
     *
     * @param key semantic key
     * @param codec payload codec
     * @param <V> payload type
     * @return cache hit when present
     */
    <V> Optional<ICacheValue<V>> get(SemanticCacheKey key, CacheCodec<V> codec);

    /**
     * Reads a semantic cache entry with semantic route metadata preserved.
     *
     * @param key semantic key
     * @param codec payload codec
     * @param <V> payload type
     * @return semantic cache entry when present
     */
    <V> Optional<SemanticCacheEntry<V>> getEntry(SemanticCacheKey key, CacheCodec<V> codec);

    /**
     * Invalidates a semantic cache key across all tiers.
     *
     * @param key semantic key
     * @return {@code true} when any tier contained the entry
     */
    boolean invalidate(SemanticCacheKey key);

    /**
     * Invalidates entries by fingerprint across all tiers.
     *
     * @param fingerprint semantic fingerprint
     * @return number of removed records
     */
    int invalidateByFingerprint(String fingerprint);

    /**
     * Runs a maintenance cycle to demote or evict expired entries.
     */
    void runMaintenanceCycle();

    @Override
    SemanticCacheStats snapshotStats();

    @Override
    void close();
}
