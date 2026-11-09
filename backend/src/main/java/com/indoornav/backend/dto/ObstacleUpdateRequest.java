package com.indoornav.backend.dto;

import com.indoornav.backend.entity.ObstacleStatus;
import com.indoornav.backend.entity.ObstacleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ObstacleUpdateRequest(
        @NotNull ObstacleType obstacleType,
        @NotBlank @Size(min = 3, max = 200) String description,
        @NotNull ObstacleStatus status,
        @NotNull @PositiveOrZero Long version) {

    public ObstacleUpdateRequest {
        description = description == null ? null : description.strip();
    }
}
