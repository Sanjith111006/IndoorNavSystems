package com.indoornav.engine.model;

import java.util.Objects;
import java.util.Set;

/**
 * A routing query and its caller-supplied blocked graph elements.
 */
public record RouteRequest(
        long startNodeId,
        long destinationNodeId,
        RoutePreference preference,
        Set<Long> blockedNodeIds,
        Set<Long> blockedEdgeIds) {

    public RouteRequest {
        if (startNodeId <= 0 || destinationNodeId <= 0) {
            throw new IllegalArgumentException("Start and destination IDs must be positive.");
        }
        Objects.requireNonNull(preference, "preference must not be null");
        blockedNodeIds = Set.copyOf(Objects.requireNonNull(blockedNodeIds, "blockedNodeIds must not be null"));
        blockedEdgeIds = Set.copyOf(Objects.requireNonNull(blockedEdgeIds, "blockedEdgeIds must not be null"));
    }

    public static RouteRequest of(long startNodeId, long destinationNodeId, RoutePreference preference) {
        return new RouteRequest(startNodeId, destinationNodeId, preference, Set.of(), Set.of());
    }
}
