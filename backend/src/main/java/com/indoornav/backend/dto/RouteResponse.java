package com.indoornav.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.engine.model.RoutePreference;
import com.indoornav.engine.result.RouteStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * Result of {@code POST /api/navigation/route} (API.md 6.9). {@code status} and
 * {@code preference} are always present; the other fields depend on the status.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteResponse(
        RouteStatus status,
        RoutePreference preference,
        List<PathNode> pathNodes,
        Double distanceMeters,
        Long estimatedDurationSeconds,
        List<FloorTransition> floorTransitions,
        List<Instruction> instructions,
        List<Warning> warnings,
        NoRoute noRoute) {

    public enum InstructionType {
        START,
        WALK,
        FLOOR_CHANGE,
        ARRIVE
    }

    /**
     * The turn that starts a walk step. API.md also allows STRAIGHT, but straight continuations
     * are merged into the previous walk step, so this backend never sends it.
     */
    public enum Turn {
        LEFT,
        RIGHT
    }

    public enum WarningCode {
        DETOUR,
        STAIRS_REQUIRED
    }

    public enum NoRouteReason {
        OBSTACLE,
        ACCESSIBILITY_CONSTRAINT,
        DISCONNECTED
    }

    public record PathNode(Long nodeId, Long floorId, BigDecimal x, BigDecimal y, NodeType type, String name) {
    }

    /** {@code pathIndex} is the index in {@code pathNodes} of the node where the change starts. */
    public record FloorTransition(Long fromFloorId, Long toFloorId, String connectionType, int pathIndex) {
    }

    /** Fields that do not apply to a step's type are omitted. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Instruction(
            InstructionType type,
            String text,
            Double distanceMeters,
            Long floorId,
            Long toFloorId,
            String connectionType,
            Turn turn,
            int startPathIndex,
            int endPathIndex) {
    }

    /** {@code extraDistanceMeters} is always present, and null when it does not apply. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Warning(WarningCode code, String message, List<Long> obstacleIds, Double extraDistanceMeters) {
    }

    public record ObstacleSummary(Long id, ObstacleType type, String description) {
    }

    public record NoRoute(NoRouteReason reasonCode, String message, List<ObstacleSummary> obstacles) {
    }

    public static RouteResponse found(
            RoutePreference preference,
            List<PathNode> pathNodes,
            double distanceMeters,
            long estimatedDurationSeconds,
            List<FloorTransition> floorTransitions,
            List<Instruction> instructions,
            List<Warning> warnings) {
        return new RouteResponse(
                RouteStatus.FOUND,
                preference,
                pathNodes,
                distanceMeters,
                estimatedDurationSeconds,
                floorTransitions,
                instructions,
                warnings,
                null);
    }

    public static RouteResponse noRoute(RoutePreference preference, NoRoute noRoute) {
        return new RouteResponse(RouteStatus.NO_ROUTE, preference, null, null, null, null, null, null, noRoute);
    }
}
