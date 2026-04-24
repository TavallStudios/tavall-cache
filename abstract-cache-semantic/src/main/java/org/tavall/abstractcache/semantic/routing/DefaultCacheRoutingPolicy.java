package org.tavall.abstractcache.semantic.routing;

import org.tavall.abstractcache.semantic.model.CacheTag;
import org.tavall.abstractcache.semantic.model.CacheTier;
import org.tavall.abstractcache.semantic.model.SemanticCacheKey;
import org.tavall.abstractcache.semantic.model.StandardCacheTags;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Default tag-aware routing policy shipped with the library.
 */
public final class DefaultCacheRoutingPolicy implements CacheRoutingPolicy {

    private final Map<CacheTag, List<RouteStage>> tagTemplates;
    private final List<RouteStage> fallbackTemplate;

    private DefaultCacheRoutingPolicy(Map<CacheTag, List<RouteStage>> tagTemplates, List<RouteStage> fallbackTemplate) {
        this.tagTemplates = Map.copyOf(tagTemplates);
        this.fallbackTemplate = List.copyOf(fallbackTemplate);
    }

    /**
     * Creates the default library routing policy.
     *
     * @return default routing policy
     */
    public static DefaultCacheRoutingPolicy standard() {
        Map<CacheTag, List<RouteStage>> templates = new LinkedHashMap<>();
        templates.put(
                StandardCacheTags.COMBAT,
                List.of(
                        new RouteStage(CacheTier.HOT_MEMORY, Duration.ZERO),
                        new RouteStage(CacheTier.LOCAL_HOT_SERVICE, Duration.ofMinutes(5)),
                        new RouteStage(CacheTier.REMOTE_HOT, Duration.ofMinutes(5)),
                        new RouteStage(CacheTier.LOCAL_DISK, Duration.ofMinutes(15))
                )
        );
        templates.put(
                StandardCacheTags.BREEDING,
                List.of(
                        new RouteStage(CacheTier.LOCAL_DISK, Duration.ZERO),
                        new RouteStage(CacheTier.LOCAL_COLD_SERVICE, Duration.ofDays(1)),
                        new RouteStage(CacheTier.REMOTE_COLD, Duration.ofDays(7))
                )
        );
        return new DefaultCacheRoutingPolicy(
                templates,
                List.of(
                        new RouteStage(CacheTier.HOT_MEMORY, Duration.ZERO),
                        new RouteStage(CacheTier.LOCAL_DISK, Duration.ofMinutes(30))
                )
        );
    }

    @Override
    public RouteDecision decide(SemanticCacheKey key, Duration initialTtl) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(initialTtl, "initialTtl cannot be null");
        if (initialTtl.isNegative() || initialTtl.isZero()) {
            throw new IllegalArgumentException("initialTtl must be positive");
        }

        List<List<RouteStage>> matchingTemplates = new ArrayList<>();
        for (CacheTag tag : key.tags()) {
            List<RouteStage> template = tagTemplates.get(tag);
            if (template != null) {
                matchingTemplates.add(template);
            }
        }

        if (matchingTemplates.isEmpty()) {
            return new RouteDecision(materialize(fallbackTemplate, initialTtl));
        }

        matchingTemplates.sort(Comparator.comparingInt(template -> tierPriority(template.get(0).tier())));
        List<RouteStage> merged = new ArrayList<>();
        Set<CacheTier> seen = EnumSet.noneOf(CacheTier.class);

        for (List<RouteStage> template : matchingTemplates) {
            List<RouteStage> materialized = materialize(template, initialTtl);
            for (RouteStage stage : materialized) {
                if (seen.add(stage.tier())) {
                    merged.add(stage);
                }
            }
        }

        return new RouteDecision(merged);
    }

    private List<RouteStage> materialize(List<RouteStage> template, Duration initialTtl) {
        List<RouteStage> stages = new ArrayList<>(template.size());
        for (int index = 0; index < template.size(); index++) {
            RouteStage templateStage = template.get(index);
            stages.add(new RouteStage(templateStage.tier(), index == 0 ? initialTtl : templateStage.ttl()));
        }
        return stages;
    }

    private int tierPriority(CacheTier tier) {
        return switch (tier) {
            case HOT_MEMORY -> 0;
            case LOCAL_HOT_SERVICE -> 1;
            case REMOTE_HOT -> 2;
            case LOCAL_DISK -> 3;
            case LOCAL_COLD_SERVICE -> 4;
            case REMOTE_COLD -> 5;
        };
    }
}
