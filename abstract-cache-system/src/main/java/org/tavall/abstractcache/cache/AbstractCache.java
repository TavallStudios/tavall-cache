package org.tavall.abstractcache.cache;

import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.enums.CacheVersion;
import org.tavall.abstractcache.cache.interfaces.ICacheKey;
import org.tavall.abstractcache.cache.interfaces.ICacheStats;
import org.tavall.abstractcache.cache.interfaces.ICacheValue;
import org.tavall.abstractcache.cache.metadata.CacheMetaData;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Thread-safe, metadata-aware cache with TTL-based expiration.
 *
 * @param <K> raw key type
 * @param <V> payload type
 */
public abstract class AbstractCache<K, V> {

    protected final ConcurrentHashMap<ICacheKey<K>, ICacheValue<V>> cache = new ConcurrentHashMap<>();
    protected final long defaultTtlMillis;
    protected final AtomicInteger expiredEntries = new AtomicInteger();

    /**
     * Constructs an AbstractCache with a given default TTL.
     *
     * @param defaultTtl the default time-to-live value
     * @param unit       the time unit for the TTL
     */
    protected AbstractCache(long defaultTtl, TimeUnit unit) {
        this.defaultTtlMillis = unit.toMillis(defaultTtl);
    }

    // --- Key factories ---

    public ICacheKey<K> createKey(K rawKey) {
        return new CacheKey<>(rawKey);
    }

    public ICacheKey<K> createKey(K rawKey, CacheType type) {
        return new CacheKey<>(rawKey, type);
    }

    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheDomain domain) {
        return new CacheKey<>(rawKey, type, domain);
    }

    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheSource source) {
        return new CacheKey<>(rawKey, type, source);
    }

    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheVersion version) {
        return new CacheKey<>(rawKey, type, version);
    }

    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheDomain domain, CacheSource source) {
        return new CacheKey<>(rawKey, type, domain, source);
    }

    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheSource source, CacheVersion version) {
        return new CacheKey<>(rawKey, type, source, version);
    }

    public ICacheKey<K> createKey(K rawKey, CacheType type, CacheDomain domain, CacheVersion version) {
        return new CacheKey<>(rawKey, type, domain, version);
    }

    public ICacheKey<K> createKey(
            K rawKey,
            CacheType type,
            CacheDomain domain,
            CacheSource source,
            CacheVersion version
    ) {
        return new CacheKey<>(rawKey, type, domain, source, version);
    }

    // --- Value factories ---

    protected ICacheValue<V> createValue(V value, long expirationTime) {
        return new CacheValue<>(value, expirationTime);
    }

    // --- Cache operations ---

    public V get(K rawKey, Function<ICacheKey<K>, V> loader) {
        return get(createKey(rawKey), loader, defaultTtlMillis);
    }

    public V get(ICacheKey<K> key, Function<ICacheKey<K>, V> loader) {
        return get(key, loader, defaultTtlMillis);
    }

    public V get(ICacheKey<K> key, Function<ICacheKey<K>, V> loader, long ttlMillis) {
        ICacheValue<V> entry = cache.get(key);

        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }

        V newValue = loader.apply(key);
        if (newValue != null) {
            put(key, newValue, ttlMillis);
        }

        return newValue;
    }

    public void put(K rawKey, V value) {
        put(createKey(rawKey), value, defaultTtlMillis);
    }

    public void put(ICacheKey<K> key, V value) {
        put(key, value, defaultTtlMillis);
    }

    public void put(ICacheKey<K> key, V value, long ttlMillis) {
        if (key == null || value == null) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        cache.put(key, createValue(value, expiresAt));
    }

    public V getIfPresent(
            K rawKey,
            CacheDomain domain,
            CacheType type,
            CacheVersion version,
            CacheSource source
    ) {
        ICacheKey<K> fullKey = buildKey(rawKey, domain, type, version, source);
        ICacheValue<V> entry = cache.get(fullKey);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }

        if (entry != null) {
            expiredEntries.incrementAndGet();
            cache.remove(fullKey);
        }

        return null;
    }

    public V remove(
            K rawKey,
            CacheDomain domain,
            CacheType type,
            CacheVersion version,
            CacheSource source
    ) {
        ICacheKey<K> key = buildKey(rawKey, domain, type, version, source);
        ICacheValue<V> removed = cache.remove(key);
        return removed != null ? removed.getValue() : null;
    }

    public boolean containsKey(
            K rawKey,
            CacheDomain domain,
            CacheType type,
            CacheVersion version,
            CacheSource source
    ) {
        return getIfPresent(rawKey, domain, type, version, source) != null;
    }

    public void clear() {
        cache.clear();
        expiredEntries.set(0);
    }

    public int size() {
        return cache.size();
    }

    public int cleanupExpired() {
        int removed = 0;
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<ICacheKey<K>, ICacheValue<V>>> it = cache.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ICacheKey<K>, ICacheValue<V>> entry = it.next();
            if (entry.getValue().isExpired(now)) {
                it.remove();
                expiredEntries.incrementAndGet();
                removed++;
            }
        }
        return removed;
    }

    public ICacheStats getCacheStats() {
        int total = cache.size();
        int expired = expiredEntries.get();
        return createStats(total, total - expired, expired);
    }

    protected ICacheKey<K> buildKey(
            K rawKey,
            CacheDomain domain,
            CacheType type,
            CacheVersion version,
            CacheSource source
    ) {
        return new CacheKey<>(rawKey, type, domain, source, version);
    }

    protected ICacheStats createStats(int total, int alive, int expired) {
        return new CacheMetaData(total, alive, expired);
    }
}
