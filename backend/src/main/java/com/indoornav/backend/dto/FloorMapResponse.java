package com.indoornav.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.entity.ObstacleType;
import java.math.BigDecimal;
import java.util.List;

/** Everything needed to draw one floor (API.md 6.5). */
public record FloorMapResponse(
        FloorResponse floor,
        Plan plan,
        List<MapNode> nodes,
        List<MapEdge> edges,
        List<MapRoom> rooms,
        List<MapPointOfInterest> pointsOfInterest) {

    public record Plan(String url, BigDecimal width, BigDecimal height, BigDecimal metresPerUnit) {
    }

    /** The visitor-facing part of an obstacle; administrative data is never exposed here. */
    public record ObstacleSummary(Long id, ObstacleType type, String description) {
    }

    public record MapNode(
            Long id,
            BigDecimal x,
            BigDecimal y,
            NodeType type,
            String name,
            boolean accessible,
            boolean blocked,
            @JsonInclude(JsonInclude.Include.NON_NULL) ObstacleSummary obstacle) {
    }

    public record MapEdge(
            Long id,
            Long fromNodeId,
            Long toNodeId,
            String connectionType,
            boolean accessible,
            boolean bidirectional,
            Long otherFloorId,
            boolean blocked,
            @JsonInclude(JsonInclude.Include.NON_NULL) ObstacleSummary obstacle) {
    }

    public record Bounds(BigDecimal x, BigDecimal y, BigDecimal width, BigDecimal height) {
    }

    public record MapRoom(Long id, String name, String category, String description, Bounds bounds) {
    }

    public record MapPointOfInterest(Long id, String name, String category, Long nodeId, Long roomId) {
    }
}
