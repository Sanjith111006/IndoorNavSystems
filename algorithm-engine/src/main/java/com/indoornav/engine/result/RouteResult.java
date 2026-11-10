package com.indoornav.engine.result;

import java.util.List;
import java.util.Objects;

/**
 * Immutable complete route metrics, or a normal absence of a route.
 */
public record RouteResult(
        RouteStatus status,
        List<Long> nodeIds,
        List<Long> edgeIds,
        double totalDistanceMetres,
        double totalEstimatedTimeSeconds,
        List<FloorTransition> floorTransitions,
        boolean usesStairs) {

    public RouteResult {
        Objects.requireNonNull(status, "status must not be null");
        nodeIds = List.copyOf(Objects.requireNonNull(nodeIds, "nodeIds must not be null"));
        edgeIds = List.copyOf(Objects.requireNonNull(edgeIds, "edgeIds must not be null"));
        floorTransitions = List.copyOf(Objects.requireNonNull(floorTransitions, "floorTransitions must not be null"));
        if (!Double.isFinite(totalDistanceMetres) || totalDistanceMetres < 0.0
                || !Double.isFinite(totalEstimatedTimeSeconds) || totalEstimatedTimeSeconds < 0.0) {
            throw new IllegalArgumentException("Route metrics must be finite and non-negative.");
        }
        if (status == RouteStatus.NO_ROUTE && (!nodeIds.isEmpty() || !edgeIds.isEmpty() || totalDistanceMetres != 0.0
                || totalEstimatedTimeSeconds != 0.0 || !floorTransitions.isEmpty() || usesStairs)) {
            throw new IllegalArgumentException("A no-route result cannot contain route data.");
        }
    }

    public static RouteResult noRoute() {
        return new RouteResult(RouteStatus.NO_ROUTE, List.of(), List.of(), 0.0, 0.0, List.of(), false);
    }
}
