package org.tavall.abstractcache.semantic.storage.postgres;

import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.spi.CacheTierAdapter;
import org.tavall.abstractcache.semantic.spi.StorageKeyUtil;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecord;
import org.tavall.abstractcache.semantic.spi.StoredCacheRecordSerializer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL-backed tier adapter for cold service storage.
 */
public final class PostgresTierAdapter implements CacheTierAdapter {

    private final CacheTier tier;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String tableName;

    public PostgresTierAdapter(CacheTier tier, String jdbcUrl, String username, String password) {
        this(tier, jdbcUrl, username, password, "semantic_cache_records");
    }

    public PostgresTierAdapter(CacheTier tier, String jdbcUrl, String username, String password, String tableName) {
        this.tier = tier;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.tableName = tableName;
        initialize();
    }

    @Override
    public CacheTier tier() {
        return tier;
    }

    @Override
    public boolean isAvailable() {
        try (Connection connection = newConnection()) {
            return connection.isValid(2);
        } catch (SQLException exception) {
            return false;
        }
    }

    @Override
    public Optional<StoredCacheRecord> get(SemanticCacheKey key) {
        String sql = "select record_bytes from " + tableName + " where adapter_tier = ? and storage_key = ?";
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tier.name());
            statement.setString(2, StorageKeyUtil.storageKey(key));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(StoredCacheRecordSerializer.deserialize(resultSet.getBytes(1)));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to read postgres cache record", exception);
        }
    }

    @Override
    public void put(StoredCacheRecord record) {
        String sql = "insert into " + tableName + " (adapter_tier, storage_key, fingerprint, expires_at_millis, record_bytes) "
                + "values (?, ?, ?, ?, ?) "
                + "on conflict (adapter_tier, storage_key) do update set fingerprint = excluded.fingerprint, "
                + "expires_at_millis = excluded.expires_at_millis, record_bytes = excluded.record_bytes";
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tier.name());
            statement.setString(2, StorageKeyUtil.storageKey(record.key()));
            statement.setString(3, record.key().fingerprint());
            statement.setLong(4, record.expiresAt().toEpochMilli());
            statement.setBytes(5, StoredCacheRecordSerializer.serialize(record));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to write postgres cache record", exception);
        }
    }

    @Override
    public void remove(SemanticCacheKey key) {
        String sql = "delete from " + tableName + " where adapter_tier = ? and storage_key = ?";
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tier.name());
            statement.setString(2, StorageKeyUtil.storageKey(key));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to remove postgres cache record", exception);
        }
    }

    @Override
    public List<StoredCacheRecord> scanExpired(Instant now, int limit) {
        String sql = "select record_bytes from " + tableName + " where adapter_tier = ? and expires_at_millis < ? order by expires_at_millis asc limit ?";
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tier.name());
            statement.setLong(2, now.toEpochMilli());
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<StoredCacheRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(StoredCacheRecordSerializer.deserialize(resultSet.getBytes(1)));
                }
                return records;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to scan expired postgres cache records", exception);
        }
    }

    @Override
    public List<StoredCacheRecord> snapshotRecords() {
        String sql = "select record_bytes from " + tableName + " where adapter_tier = ?";
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tier.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<StoredCacheRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(StoredCacheRecordSerializer.deserialize(resultSet.getBytes(1)));
                }
                return records;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to snapshot postgres cache records", exception);
        }
    }

    public void clear() {
        String sql = "delete from " + tableName + " where adapter_tier = ?";
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tier.name());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to clear postgres cache records", exception);
        }
    }

    private void initialize() {
        String sql = "create table if not exists " + tableName + " ("
                + "adapter_tier varchar(64) not null,"
                + "storage_key varchar(128) not null,"
                + "fingerprint text not null,"
                + "expires_at_millis bigint not null,"
                + "record_bytes bytea not null,"
                + "primary key (adapter_tier, storage_key)"
                + ")";
        try (Connection connection = newConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            statement.execute("create index if not exists idx_" + tableName + "_expires on " + tableName + " (adapter_tier, expires_at_millis)");
            statement.execute("create index if not exists idx_" + tableName + "_fingerprint on " + tableName + " (fingerprint)");
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to initialize postgres cache table", exception);
        }
    }

    private Connection newConnection() throws SQLException {
        if (username == null || username.isBlank()) {
            return DriverManager.getConnection(jdbcUrl);
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
