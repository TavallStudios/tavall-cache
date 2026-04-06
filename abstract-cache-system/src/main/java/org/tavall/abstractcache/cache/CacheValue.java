package org.tavall.abstractcache.cache;

import org.tavall.abstractcache.cache.interfaces.ICacheValue;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * Wrapper for cache payload and its expiration timestamp.
 *
 * @param <V> payload type
 */
public class CacheValue<V> implements ICacheValue<V> {

    private final V value;
    private final long expirationTime;

    /**
     * Creates a value wrapper.
     *
     * @param value payload
     * @param expirationTime expiration epoch milliseconds
     */
    public CacheValue(V value, long expirationTime) {
        this.value = value;
        this.expirationTime = expirationTime;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public OptionalLong getExpirationTimeMillis() {
        return OptionalLong.of(expirationTime);
    }

    /**
     * @return expiration epoch milliseconds
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * Checks if the wrapper is expired relative to current wall clock.
     *
     * @return {@code true} when expired
     */
    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    @Override
    public boolean isExpired(long currentTime) {
        return currentTime > expirationTime;
    }

    /**
     * Checks if the wrapper is expired relative to the supplied timestamp.
     *
     * @param currentTime current epoch milliseconds
     * @return {@code true} when expired
     */
    public boolean isValueExpired(long currentTime) {
        return isExpired(currentTime);
    }

    /**
     * Compares wrappers by payload equality.
     *
     * @param other comparison target
     * @return {@code true} when payload values are equal
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CacheValue<?> that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    /**
     * Computes hash code using only payload value.
     *
     * @return payload hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Returns a debug-friendly value wrapper representation.
     *
     * @return wrapper string
     */
    @Override
    public String toString() {
        return "CacheValue{" +
                "value=" + value +
                ", expirationTime=" + expirationTime +
                '}';
    }
}
