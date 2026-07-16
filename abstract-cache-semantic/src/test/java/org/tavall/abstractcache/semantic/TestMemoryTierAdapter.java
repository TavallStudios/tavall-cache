package org.tavall.abstractcache.semantic;

import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class TestMemoryTierAdapter implements CacheTierAdapter {
    private final CacheTier tier;
    private final ConcurrentMap<SemanticCacheKey, StoredCacheRecord> recordsByKey;
    private boolean available;

    TestMemoryTierAdapter(CacheTier tier) {
        this.tier = tier;
        this.recordsByKey = new ConcurrentHashMap<>();
        this.available = true;
    }

    @Override
    public CacheTier tier() {
        return tier;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public Optional<StoredCacheRecord> get(SemanticCacheKey key) {
        return Optional.ofNullable(recordsByKey.get(key));
    }

    @Override
    public void put(StoredCacheRecord record) {
        recordsByKey.put(record.key(), record);
    }

    @Override
    public void remove(SemanticCacheKey key) {
        recordsByKey.remove(key);
    }

    @Override
    public List<StoredCacheRecord> scanExpired(Instant now, int limit) {
        return recordsByKey.values().stream()
                .filter(record -> record.isExpired(now))
                .limit(limit)
                .toList();
    }

    @Override
    public List<StoredCacheRecord> snapshotRecords() {
        return List.copyOf(recordsByKey.values());
    }
}
