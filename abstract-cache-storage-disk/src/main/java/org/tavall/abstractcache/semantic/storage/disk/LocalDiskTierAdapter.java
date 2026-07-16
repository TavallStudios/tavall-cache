package org.tavall.abstractcache.semantic.storage.disk;

import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;
import org.tavall.abstractcache.semantic.spi.StorageKeyUtil;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecord;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecordSerializer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.nio.file.Files;

/**
 * Local disk-backed adapter for the semantic cache engine.
 */
public final class LocalDiskTierAdapter implements CacheTierAdapter {

    private final Path baseDirectory;

    public LocalDiskTierAdapter(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public CacheTier tier() {
        return CacheTier.LOCAL_DISK;
    }

    @Override
    public boolean isAvailable() {
        try {
            Files.createDirectories(baseDirectory);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    @Override
    public Optional<StoredCacheRecord> get(SemanticCacheKey key) {
        Path file = resolvePath(key);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(readRecord(file));
        } catch (IOException exception) {
            remove(key);
            return Optional.empty();
        }
    }

    @Override
    public void put(StoredCacheRecord record) {
        try {
            Path file = resolvePath(record.key());
            Files.createDirectories(file.getParent());
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tempFile, StoredCacheRecordSerializer.serialize(record));
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write local disk cache record", exception);
        }
    }

    @Override
    public void remove(SemanticCacheKey key) {
        try {
            Files.deleteIfExists(resolvePath(key));
        } catch (IOException exception) {
            throw new IllegalStateException("failed to remove local disk cache record", exception);
        }
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
        if (!Files.exists(baseDirectory)) {
            return List.of();
        }
        List<StoredCacheRecord> records = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(baseDirectory)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".cache"))
                    .sorted()
                    .toList();
            for (Path file : files) {
                try {
                    records.add(readRecord(file));
                } catch (IOException exception) {
                    Files.deleteIfExists(file);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to snapshot local disk cache records", exception);
        }
        return records;
    }

    private Path resolvePath(SemanticCacheKey key) {
        String domainSegment = key.domain() == null ? "UNKNOWN" : key.domain().name();
        return baseDirectory
                .resolve(tier().name())
                .resolve(domainSegment)
                .resolve(StorageKeyUtil.storageKey(key) + ".cache");
    }

    private StoredCacheRecord readRecord(Path file) throws IOException {
        try {
            return StoredCacheRecordSerializer.deserialize(Files.readAllBytes(file));
        } catch (RuntimeException exception) {
            throw new IOException("failed to deserialize cache record", exception);
        }
    }
}
