package org.tavall.abstractcache.semantic.spi;

import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheVersion;
import org.tavall.abstractcache.semantic.model.CacheTag;
import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.routing.RouteStage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared binary serializer for durable semantic cache records.
 */
public final class StoredCacheRecordSerializer {

    private static final int FORMAT_VERSION = 1;

    private StoredCacheRecordSerializer() {
    }

    /**
     * Serializes a stored cache record to bytes.
     *
     * @param record stored record
     * @return serialized bytes
     */
    public static byte[] serialize(StoredCacheRecord record) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(output)) {
                data.writeInt(FORMAT_VERSION);
                writeNullableEnum(data, record.key().domain());
                writeNullableEnum(data, record.key().source());
                writeNullableEnum(data, record.key().version());
                data.writeUTF(record.key().fingerprint());
                List<CacheTag> tags = record.key().tags().stream().sorted(Comparator.naturalOrder()).toList();
                data.writeInt(tags.size());
                for (CacheTag tag : tags) {
                    data.writeUTF(tag.value());
                }
                data.writeUTF(record.codecId());
                data.writeLong(record.createdAt().toEpochMilli());
                data.writeLong(record.expiresAt().toEpochMilli());
                data.writeInt(record.routeIndex());
                data.writeInt(record.routeStages().size());
                for (RouteStage stage : record.routeStages()) {
                    data.writeUTF(stage.tier().name());
                    data.writeLong(stage.ttl().toMillis());
                }
                byte[] payload = record.payloadBytes();
                data.writeInt(payload.length);
                data.write(payload);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to serialize cache record", exception);
        }
    }

    /**
     * Deserializes a stored cache record from bytes.
     *
     * @param bytes serialized bytes
     * @return deserialized record
     */
    public static StoredCacheRecord deserialize(byte[] bytes) {
        try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int formatVersion = data.readInt();
            if (formatVersion != FORMAT_VERSION) {
                throw new IOException("unsupported cache file version: " + formatVersion);
            }

            CacheDomain domain = readNullableEnum(data, CacheDomain.class);
            CacheSource source = readNullableEnum(data, CacheSource.class);
            CacheVersion version = readNullableEnum(data, CacheVersion.class);
            String fingerprint = data.readUTF();
            int tagCount = data.readInt();
            Set<CacheTag> tags = new LinkedHashSet<>();
            for (int index = 0; index < tagCount; index++) {
                tags.add(CacheTag.of(data.readUTF()));
            }
            String codecId = data.readUTF();
            Instant createdAt = Instant.ofEpochMilli(data.readLong());
            Instant expiresAt = Instant.ofEpochMilli(data.readLong());
            int routeIndex = data.readInt();
            int routeSize = data.readInt();
            List<RouteStage> routeStages = new ArrayList<>(routeSize);
            for (int index = 0; index < routeSize; index++) {
                CacheTier tier = CacheTier.valueOf(data.readUTF());
                Duration ttl = Duration.ofMillis(data.readLong());
                routeStages.add(new RouteStage(tier, ttl));
            }
            int payloadLength = data.readInt();
            byte[] payload = data.readNBytes(payloadLength);

            SemanticCacheKey key = new SemanticCacheKey(fingerprint, domain, source, version, tags);
            return new StoredCacheRecord(key, payload, codecId, createdAt, expiresAt, routeIndex, routeStages);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to deserialize cache record", exception);
        }
    }

    private static <E extends Enum<E>> void writeNullableEnum(DataOutputStream data, E value) throws IOException {
        data.writeBoolean(value != null);
        if (value != null) {
            data.writeUTF(value.name());
        }
    }

    private static <E extends Enum<E>> E readNullableEnum(DataInputStream data, Class<E> type) throws IOException {
        boolean present = data.readBoolean();
        return present ? Enum.valueOf(type, data.readUTF()) : null;
    }
}
