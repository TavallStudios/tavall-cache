package org.tavall.abstractcache.cache.interfaces;

import java.util.OptionalLong;

/**
 * Read-only wrapper interface for cached values.
 *
 * @param <V> payload type
 */
public interface ICacheValue<V> {

    /**
     * Returns the wrapped payload.
     *
     * @return payload
     */
    V getValue();

    /**
     * Returns the expiration timestamp for this value when one exists.
     *
     * @return expiration epoch milliseconds, or empty when this wrapper does not expire
     */
    default OptionalLong getExpirationTimeMillis() {
        return OptionalLong.empty();
    }

    /**
     * Determines whether the value is expired relative to the supplied timestamp.
     *
     * @param currentTimeMillis current epoch milliseconds
     * @return {@code true} when this wrapper has an expiration time in the past
     */
    default boolean isExpired(long currentTimeMillis) {
        OptionalLong expiration = getExpirationTimeMillis();
        return expiration.isPresent() && currentTimeMillis > expiration.getAsLong();
    }

    /**
     * Determines whether the value is expired relative to the current wall clock.
     *
     * @return {@code true} when expired
     */
    default boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }
}
