package org.tavall.abstractcache.semantic.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Extensible normalized cache tag used by routing policies.
 */
public final class CacheTag implements Comparable<CacheTag> {

    private final String value;

    private CacheTag(String value) {
        this.value = value;
    }

    /**
     * Creates a normalized cache tag.
     *
     * @param value tag value
     * @return normalized tag
     */
    public static CacheTag of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("cache tag cannot be null or blank");
        }
        String normalized = value.trim()
                .replaceAll("\\s+", "_")
                .toUpperCase(Locale.ROOT);
        return new CacheTag(normalized);
    }

    /**
     * @return normalized tag value
     */
    public String value() {
        return value;
    }

    @Override
    public int compareTo(CacheTag other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CacheTag that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
