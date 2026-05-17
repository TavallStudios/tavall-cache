package org.tavall.abstractcache.semantic.storage.mongo;

import com.mongodb.MongoCommandException;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.types.Binary;
import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;
import org.tavall.abstractcache.semantic.spi.StorageKeyUtil;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecord;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecordSerializer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB-backed tier adapter for remote cold storage.
 */
public final class MongoTierAdapter implements CacheTierAdapter {

    private static final String DEFAULT_COLLECTION = "semantic_cache_records";

    private final CacheTier tier;
    private final MongoClient client;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;

    public MongoTierAdapter(CacheTier tier, String connectionString, String databaseName) {
        this(tier, connectionString, databaseName, DEFAULT_COLLECTION);
    }

    public MongoTierAdapter(CacheTier tier, String connectionString, String databaseName, String collectionName) {
        this.tier = tier;
        this.client = MongoClients.create(
                MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(connectionString))
                        .build()
        );
        this.database = client.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
        initialize();
    }

    @Override
    public CacheTier tier() {
        return tier;
    }

    @Override
    public boolean isAvailable() {
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public Optional<StoredCacheRecord> get(SemanticCacheKey key) {
        Document document = collection.find(filterFor(key)).first();
        if (document == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(document));
    }

    @Override
    public void put(StoredCacheRecord record) {
        collection.replaceOne(
                filterFor(record.key()),
                serialize(record),
                new ReplaceOptions().upsert(true)
        );
    }

    @Override
    public void remove(SemanticCacheKey key) {
        collection.deleteOne(filterFor(key));
    }

    @Override
    public List<StoredCacheRecord> scanExpired(Instant now, int limit) {
        FindIterable<Document> documents = collection.find(
                        Filters.and(
                                Filters.eq("adapterTier", tier.name()),
                                Filters.lt("expiresAtMillis", now.toEpochMilli())
                        )
                )
                .sort(new Document("expiresAtMillis", 1))
                .limit(limit);
        return deserializeAll(documents);
    }

    @Override
    public List<StoredCacheRecord> snapshotRecords() {
        return deserializeAll(collection.find(Filters.eq("adapterTier", tier.name())));
    }

    public void clear() {
        collection.deleteMany(Filters.eq("adapterTier", tier.name()));
    }

    @Override
    public void close() {
        client.close();
    }

    private void initialize() {
        createIndexIfPossible(
                Indexes.compoundIndex(Indexes.ascending("adapterTier"), Indexes.ascending("storageKey")),
                new IndexOptions().unique(true)
        );
        createIndexIfPossible(
                Indexes.compoundIndex(Indexes.ascending("adapterTier"), Indexes.ascending("expiresAtMillis")),
                null
        );
        createIndexIfPossible(Indexes.ascending("fingerprint"), null);
    }

    private void createIndexIfPossible(org.bson.conversions.Bson index, IndexOptions options) {
        try {
            if (options == null) {
                collection.createIndex(index);
            } else {
                collection.createIndex(index, options);
            }
        } catch (MongoCommandException exception) {
            if (!isLowDiskSpaceError(exception)) {
                throw exception;
            }
        }
    }

    private boolean isLowDiskSpaceError(MongoCommandException exception) {
        return exception.getErrorCode() == 14031
                || (exception.getErrorMessage() != null && exception.getErrorMessage().contains("OutOfDiskSpace"));
    }

    private Document filterFor(SemanticCacheKey key) {
        return new Document("adapterTier", tier.name())
                .append("storageKey", StorageKeyUtil.storageKey(key));
    }

    private Document serialize(StoredCacheRecord record) {
        return new Document("adapterTier", tier.name())
                .append("storageKey", StorageKeyUtil.storageKey(record.key()))
                .append("fingerprint", record.key().fingerprint())
                .append("expiresAtMillis", record.expiresAt().toEpochMilli())
                .append("recordBytes", new Binary(StoredCacheRecordSerializer.serialize(record)));
    }

    private List<StoredCacheRecord> deserializeAll(FindIterable<Document> documents) {
        List<StoredCacheRecord> records = new ArrayList<>();
        for (Document document : documents) {
            records.add(deserialize(document));
        }
        return records;
    }

    private StoredCacheRecord deserialize(Document document) {
        Binary binary = document.get("recordBytes", Binary.class);
        if (binary == null) {
            throw new IllegalStateException("mongo cache record is missing payload bytes");
        }
        return StoredCacheRecordSerializer.deserialize(binary.getData());
    }
}
