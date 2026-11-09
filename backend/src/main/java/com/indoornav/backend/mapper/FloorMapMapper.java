package com.indoornav.backend.mapper;

import com.indoornav.backend.dto.FloorMapResponse;
import com.indoornav.backend.entity.Edge;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.Obstacle;
import com.indoornav.backend.entity.PointOfInterest;
import com.indoornav.backend.entity.Room;

public final class FloorMapMapper {

    private static final String PLAN_URL_PREFIX = "/assets/floorplans/";

    private FloorMapMapper() {
    }

    public static FloorMapResponse.Plan toPlan(Floor floor) {
        return new FloorMapResponse.Plan(
                PLAN_URL_PREFIX + floor.getPlanAssetPath(),
                floor.getPlanWidth(),
                floor.getPlanHeight(),
                floor.getMetresPerUnit());
    }

    /** {@code obstacle} is the active obstacle on this node, or null when it is open. */
    public static FloorMapResponse.MapNode toNode(Node node, Obstacle obstacle) {
        return new FloorMapResponse.MapNode(
                node.getId(),
                node.getX(),
                node.getY(),
                node.getType(),
                node.getDisplayName(),
                node.isAccessible(),
                obstacle != null,
                toSummary(obstacle));
    }

    /** {@code obstacle} is the active obstacle on this edge, or null when it is open. */
    public static FloorMapResponse.MapEdge toEdge(Edge edge, long floorId, Obstacle obstacle) {
        return new FloorMapResponse.MapEdge(
                edge.getId(),
                edge.getFromNode().getId(),
                edge.getToNode().getId(),
                edge.getConnectionTypeCode(),
                edge.isAccessible(),
                edge.isBidirectional(),
                otherFloorId(edge, floorId),
                obstacle != null,
                toSummary(obstacle));
    }

    public static FloorMapResponse.MapRoom toRoom(Room room) {
        FloorMapResponse.Bounds bounds = new FloorMapResponse.Bounds(
                room.getBoundsX(), room.getBoundsY(), room.getBoundsWidth(), room.getBoundsHeight());
        return new FloorMapResponse.MapRoom(
                room.getId(), room.getName(), room.getCategory(), room.getDescription(), bounds);
    }

    public static FloorMapResponse.MapPointOfInterest toPointOfInterest(PointOfInterest point) {
        return new FloorMapResponse.MapPointOfInterest(
                point.getId(), point.getName(), point.getCategory(), point.getNode().getId(), point.getRoomId());
    }

    private static Long otherFloorId(Edge edge, long floorId) {
        Long fromFloorId = edge.getFromNode().getFloor().getId();
        Long toFloorId = edge.getToNode().getFloor().getId();
        if (fromFloorId.equals(toFloorId)) {
            return null;
        }
        return fromFloorId == floorId ? toFloorId : fromFloorId;
    }

    private static FloorMapResponse.ObstacleSummary toSummary(Obstacle obstacle) {
        if (obstacle == null) {
            return null;
        }
        return new FloorMapResponse.ObstacleSummary(obstacle.getId(), obstacle.getType(), obstacle.getDescription());
    }
}
