package org.tavall.abstractcache.semantic.storage.redis;

import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;
import org.tavall.abstractcache.semantic.spi.StorageKeyUtil;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecord;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecordSerializer;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Redis-backed tier adapter for hot service tiers.
 */
public final class RedisTierAdapter implements CacheTierAdapter {

    private final CacheTier tier;
    private final JedisPooled client;
    private final String keyPrefix;

    public RedisTierAdapter(CacheTier tier, String redisUrl) {
        this.tier = tier;
        this.client = new JedisPooled(redisUrl);
        this.keyPrefix = "abstract-cache:" + tier.name() + ":";
    }

    @Override
    public CacheTier tier() {
        return tier;
    }

    @Override
    public boolean isAvailable() {
        try {
            return "PONG".equalsIgnoreCase(client.ping());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public Optional<StoredCacheRecord> get(SemanticCacheKey key) {
        String encoded = client.get(redisKey(key));
        if (encoded == null) {
            return Optional.empty();
        }
        return Optional.of(StoredCacheRecordSerializer.deserialize(Base64.getDecoder().decode(encoded)));
    }

    @Override
    public void put(StoredCacheRecord record) {
        String encoded = Base64.getEncoder().encodeToString(StoredCacheRecordSerializer.serialize(record));
        client.set(redisKey(record.key()), encoded);
    }

    @Override
    public void remove(SemanticCacheKey key) {
        client.del(redisKey(key));
    }

    @Override
    public List<StoredCacheRecord> scanExpired(Instant now, int limit) {
        List<StoredCacheRecord> expired = new ArrayList<>();
        for (StoredCacheRecord record : snapshotRecords()) {
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
        List<StoredCacheRecord> records = new ArrayList<>();
        ScanParams params = new ScanParams().match(keyPrefix + "*").count(100);
        String cursor = ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<String> result = client.scan(cursor, params);
            cursor = result.getCursor();
            for (String key : result.getResult()) {
                String encoded = client.get(key);
                if (encoded != null) {
                    records.add(StoredCacheRecordSerializer.deserialize(Base64.getDecoder().decode(encoded)));
                }
            }
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

        return records;
    }

    /**
     * Clears all records owned by this adapter key prefix.
     */
    public void clear() {
        List<String> keys = new ArrayList<>();
        ScanParams params = new ScanParams().match(keyPrefix + "*").count(100);
        String cursor = ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<String> result = client.scan(cursor, params);
            cursor = result.getCursor();
            keys.addAll(result.getResult());
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

        if (!keys.isEmpty()) {
            client.del(keys.toArray(String[]::new));
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private String redisKey(SemanticCacheKey key) {
        return keyPrefix + StorageKeyUtil.storageKey(key);
    }
}
