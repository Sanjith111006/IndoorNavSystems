package com.indoornav.engine.cost;

/**
 * Time parameters supplied from connection-type reference data.
 */
public record ConnectionCost(double speedMetresPerSecond, double fixedTimeSeconds) {

    public ConnectionCost {
        if (!Double.isFinite(speedMetresPerSecond) || speedMetresPerSecond <= 0.0) {
            throw new IllegalArgumentException("Connection speed must be finite and positive.");
        }
        if (!Double.isFinite(fixedTimeSeconds) || fixedTimeSeconds < 0.0) {
            throw new IllegalArgumentException("Fixed connection time must be finite and non-negative.");
        }
    }
}
