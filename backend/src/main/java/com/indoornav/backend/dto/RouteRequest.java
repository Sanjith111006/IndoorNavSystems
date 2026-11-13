package com.indoornav.backend.dto;

import com.indoornav.engine.model.RoutePreference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Body of {@code POST /api/navigation/route} (API.md 6.9). */
public record RouteRequest(
        @NotNull @Positive Long startNodeId,
        @NotNull @Positive Long destinationNodeId,
        RoutePreference preference) {

    public RouteRequest {
        preference = preference == null ? RoutePreference.SHORTEST : preference;
    }
}
