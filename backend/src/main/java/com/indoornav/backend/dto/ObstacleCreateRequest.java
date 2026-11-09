package com.indoornav.backend.dto;

import com.indoornav.backend.entity.ObstacleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** The building and status are absent on purpose: the server derives both (API.md APD-2). */
public record ObstacleCreateRequest(
        @NotNull ObstacleTargetType targetType,
        @NotNull @Positive Long targetId,
        @NotNull ObstacleType obstacleType,
        @NotBlank @Size(min = 3, max = 200) String description) {

    public ObstacleCreateRequest {
        description = description == null ? null : description.strip();
    }
}
