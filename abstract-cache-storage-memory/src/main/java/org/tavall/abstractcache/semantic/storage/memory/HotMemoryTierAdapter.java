package org.tavall.abstractcache.semantic.storage.memory;

import org.tavall.abstractcache.semantic.model.CacheTier;

/**
 * Dedicated hot-memory adapter for the semantic cache engine.
 */
public final class HotMemoryTierAdapter extends InMemoryServiceTierAdapter {

    public HotMemoryTierAdapter() {
        super(CacheTier.HOT_MEMORY);
    }
}
