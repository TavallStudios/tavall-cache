package org.tavall.abstractcache.semantic.spi;

import org.tavall.abstractcache.semantic.model.CacheTag;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stable storage key generation for semantic cache records.
 */
public final class StorageKeyUtil {

    private StorageKeyUtil() {
    }

    /**
     * Builds a deterministic storage key for a semantic cache key.
     *
     * @param key semantic key
     * @return stable storage key
     */
    public static String storageKey(SemanticCacheKey key) {
        StringBuilder builder = new StringBuilder();
        builder.append(key.fingerprint()).append('|');
        builder.append(key.domain() == null ? "" : key.domain().name()).append('|');
        builder.append(key.source() == null ? "" : key.source().name()).append('|');
        builder.append(key.version() == null ? "" : key.version().name()).append('|');
        key.tags().stream()
                .sorted()
                .map(CacheTag::value)
                .forEach(tag -> builder.append(tag).append(','));
        return sha256(builder.toString());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte current : hashed) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
