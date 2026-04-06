package org.tavall.abstractcache.semantic.spi;

/**
 * Codec for persisting semantic cache payloads.
 *
 * @param <V> payload type
 */
public interface CacheCodec<V> {

    /**
     * @return stable codec identifier
     */
    String codecId();

    /**
     * Encodes the payload to bytes.
     *
     * @param value payload
     * @return encoded payload bytes
     */
    byte[] encode(V value);

    /**
     * Decodes the payload from bytes.
     *
     * @param bytes payload bytes
     * @return decoded payload
     */
    V decode(byte[] bytes);
}
