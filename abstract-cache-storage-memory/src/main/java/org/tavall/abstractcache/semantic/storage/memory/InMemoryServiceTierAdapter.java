package org.tavall.abstractcache.semantic.storage.memory;

import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Generic in-memory adapter useful for service-tier emulation and testing.
 */
public class InMemoryServiceTierAdapter implements CacheTierAdapter {

    private final CacheTier tier;
    private final ConcurrentMap<SemanticCacheKey, StoredCacheRecord> records;
    private volatile boolean available;

    public InMemoryServiceTierAdapter(CacheTier tier) {
        this.tier = tier;
        this.records = new ConcurrentHashMap<>();
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

    /**
     * Updates adapter availability. Intended for tests and controlled failover scenarios.
     *
     * @param available new availability flag
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public Optional<StoredCacheRecord> get(SemanticCacheKey key) {
        return Optional.ofNullable(records.get(key));
    }

    @Override
    public void put(StoredCacheRecord record) {
        records.put(record.key(), record);
    }

    @Override
    public void remove(SemanticCacheKey key) {
        records.remove(key);
    }

    @Override
    public List<StoredCacheRecord> scanExpired(Instant now, int limit) {
        List<StoredCacheRecord> expired = new ArrayList<>();
        for (StoredCacheRecord record : records.values()) {
            if (record.isExpired(now)) {
                expired.add(record);
                if (expired.size() >= limit) {
                    break;
                }
            }
        }
        return expired;
    }

    @Override
    public List<StoredCacheRecord> snapshotRecords() {
        return new ArrayList<>(records.values());
    }
}
