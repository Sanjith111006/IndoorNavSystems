package com.indoornav.backend.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Routing settings from the {@code ROUTING_*} variables (API.md 12.1; defaults from ARCHITECTURE.md
 * AA-6 and API.md APA-4).
 *
 * <p>Multipliers only ever raise stair costs, so they must be at least 1: a discount would make
 * the A* heuristic overestimate.
 */
@Validated
@ConfigurationProperties(prefix = "app.routing")
public record RoutingProperties(
        @DecimalMin("1.0") double avoidStairsMultiplier,
        @DecimalMin("1.0") double preferElevatorMultiplier,
        @Min(1) int timeoutSeconds) {
}
