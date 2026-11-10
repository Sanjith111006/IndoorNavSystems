package com.indoornav.engine.model;

import java.util.Objects;

/**
 * An immutable connection between two nodes with a positive traversal distance.
 */
public record NavigationEdge(
        long id,
        long fromNodeId,
        long toNodeId,
        double distanceMetres,
        ConnectionType connectionType,
        boolean accessible,
        boolean bidirectional) {

    public NavigationEdge {
        if (id <= 0 || fromNodeId <= 0 || toNodeId <= 0) {
            throw new IllegalArgumentException("Edge and endpoint IDs must be positive.");
        }
        if (fromNodeId == toNodeId) {
            throw new IllegalArgumentException("An edge must connect two distinct nodes.");
        }
        if (!Double.isFinite(distanceMetres) || distanceMetres <= 0.0) {
            throw new IllegalArgumentException("Edge distance must be finite and positive.");
        }
        Objects.requireNonNull(connectionType, "connectionType must not be null");
    }
}
