package com.indoornav.engine.model;

import java.util.Objects;

/**
 * An immutable navigable point on a floor plan.
 */
public record NavigationNode(
        long id,
        long floorId,
        double x,
        double y,
        NodeType type,
        boolean accessible) {

    public NavigationNode {
        if (id <= 0 || floorId <= 0) {
            throw new IllegalArgumentException("Node and floor IDs must be positive.");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Node coordinates must be finite.");
        }
        Objects.requireNonNull(type, "type must not be null");
    }
}
