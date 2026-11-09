package com.indoornav.backend.dto;

import com.indoornav.backend.entity.ObstacleStatus;
import com.indoornav.backend.entity.ObstacleType;
import java.time.Instant;
import java.util.List;

/** The obstacle resource shown only to administrators (API.md 7.2). */
public record ObstacleResponse(
        Long id,
        Long buildingId,
        ObstacleTargetType targetType,
        Long edgeId,
        Long nodeId,
        List<Long> floorIds,
        String targetSummary,
        ObstacleType obstacleType,
        String description,
        ObstacleStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
