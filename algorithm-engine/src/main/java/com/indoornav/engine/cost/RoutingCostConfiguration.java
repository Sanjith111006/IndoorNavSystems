package com.indoornav.engine.cost;

import com.indoornav.engine.model.ConnectionType;
import com.indoornav.engine.model.NavigationEdge;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable caller-provided routing costs and positive stairs surcharges. An edge time override takes precedence.
 */
public record RoutingCostConfiguration(
        Map<ConnectionType, ConnectionCost> connectionCosts,
        double avoidStairsMultiplier,
        double preferElevatorStairsMultiplier) {

    public RoutingCostConfiguration {
        Objects.requireNonNull(connectionCosts, "connectionCosts must not be null");
        var copiedCosts = new HashMap<ConnectionType, ConnectionCost>();
        copiedCosts.putAll(connectionCosts);
        if (copiedCosts.isEmpty() || copiedCosts.containsKey(null) || copiedCosts.containsValue(null)) {
            throw new IllegalArgumentException("At least one non-null connection cost is required.");
        }
        connectionCosts = Map.copyOf(copiedCosts);
        if (!Double.isFinite(avoidStairsMultiplier) || avoidStairsMultiplier < 1.0) {
            throw new IllegalArgumentException("Avoid-stairs multiplier must be finite and at least one.");
        }
        if (!Double.isFinite(preferElevatorStairsMultiplier) || preferElevatorStairsMultiplier < 1.0) {
            throw new IllegalArgumentException("Elevator-preference multiplier must be finite and at least one.");
        }
    }

    public double travelTimeSeconds(NavigationEdge edge) {
        Objects.requireNonNull(edge, "edge must not be null");
        if (edge.timeSeconds() != null) {
            return edge.timeSeconds();
        }
        ConnectionCost cost = connectionCosts.get(edge.connectionType());
        if (cost == null) {
            throw new IllegalArgumentException("No cost exists for connection type " + edge.connectionType().code() + ".");
        }
        return (edge.distanceMetres() / cost.speedMetresPerSecond()) + cost.fixedTimeSeconds();
    }

    public double fastestHorizontalSpeedMetresPerSecond() {
        return connectionCosts.entrySet().stream()
                .filter(entry -> !entry.getKey().isStairs() && !entry.getKey().isElevator())
                .mapToDouble(entry -> entry.getValue().speedMetresPerSecond())
                .max()
                .orElseThrow(() -> new IllegalStateException("At least one horizontal connection cost is required."));
    }
}
