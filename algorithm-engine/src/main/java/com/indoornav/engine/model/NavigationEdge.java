package com.indoornav.engine.model;

import java.util.Objects;

/**
 * An immutable connection between two nodes with a positive distance and an optional time override.
 */
public record NavigationEdge(
        long id,
        long fromNodeId,
        long toNodeId,
        double distanceMetres,
        Double timeSeconds,
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
        if (timeSeconds != null && (!Double.isFinite(timeSeconds) || timeSeconds <= 0.0)) {
            throw new IllegalArgumentException("Explicit edge time must be finite and positive when supplied.");
        }
        Objects.requireNonNull(connectionType, "connectionType must not be null");
    }
}
