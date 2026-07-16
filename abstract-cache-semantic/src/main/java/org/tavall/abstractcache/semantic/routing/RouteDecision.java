package org.tavall.abstractcache.semantic.routing;

import java.util.List;

/**
 * Resolved ordered route for a semantic cache key.
 */
public final class RouteDecision {

    private final List<RouteStage> stages;

    public RouteDecision(List<RouteStage> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("route stages cannot be null or empty");
        }
        this.stages = List.copyOf(stages);
    }

    public List<RouteStage> stages() {
        return stages;
    }
}
