package org.tavall.abstractcache.semantic.model;

import org.tavall.abstractcache.cache.CacheKey;
import org.tavall.abstractcache.cache.enums.CacheDomain;
import org.tavall.abstractcache.cache.enums.CacheSource;
import org.tavall.abstractcache.cache.enums.CacheType;
import org.tavall.abstractcache.cache.enums.CacheVersion;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Semantic cache identity derived from caller-supplied context fingerprint and metadata.
 */
public final class SemanticCacheKey extends CacheKey<String> {

    private final Set<CacheTag> tags;

    public SemanticCacheKey(
            String fingerprint,
            CacheDomain domain,
            CacheSource source,
            CacheVersion version,
            Set<CacheTag> tags
    ) {
        super(requireFingerprint(fingerprint), CacheType.HYBRID, domain, source, version);
        this.tags = Set.copyOf(tags == null ? Set.of() : new LinkedHashSet<>(tags));
    }

    public String fingerprint() {
        return getRawCacheKey();
    }

    public Set<CacheTag> tags() {
        return tags;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SemanticCacheKey that)) {
            return false;
        }
        return Objects.equals(fingerprint(), that.fingerprint())
                && getCacheDomain() == that.getCacheDomain()
                && getSource() == that.getSource()
                && getVersion() == that.getVersion()
                && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fingerprint(), getCacheDomain(), getSource(), getVersion(), tags);
    }

    @Override
    public String toString() {
        return "SemanticCacheKey{" +
                "fingerprint='" + fingerprint() + '\'' +
                ", cacheType=" + getCacheType() +
                ", domain=" + getCacheDomain() +
                ", source=" + getSource() +
                ", version=" + getVersion() +
                ", tags=" + tags +
                '}';
    }

    public CacheDomain domain() {
        return getCacheDomain();
    }

    public CacheSource source() {
        return getSource();
    }

    public CacheVersion version() {
        return getVersion();
    }

    private static String requireFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            throw new IllegalArgumentException("fingerprint cannot be null or blank");
        }
        return fingerprint;
    }
}
