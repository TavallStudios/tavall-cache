package org.tavall.abstractcache.cache.metadata;

import java.util.Optional;

public interface ICacheRegistryMetaDataProvider {

    Optional<CacheRegistryMetaData> createCacheRegistryMetaData();
}
