package com.indoornav.backend.mapper;

import com.indoornav.backend.dto.ObstacleResponse;
import com.indoornav.backend.dto.ObstacleTargetType;
import com.indoornav.backend.entity.Edge;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.Obstacle;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ObstacleMapper {

    private ObstacleMapper() {
    }

    /** {@code poiNamesByNodeId} supplies a label for nodes that have no display name. */
    public static ObstacleResponse toResponse(Obstacle obstacle, Map<Long, String> poiNamesByNodeId) {
        Edge edge = obstacle.getEdge();
        Node node = obstacle.getNode();
        boolean isEdgeTarget = edge != null;
        return new ObstacleResponse(
                obstacle.getId(),
                obstacle.getBuildingId(),
                isEdgeTarget ? ObstacleTargetType.EDGE : ObstacleTargetType.NODE,
                isEdgeTarget ? edge.getId() : null,
                isEdgeTarget ? null : node.getId(),
                floorIds(edge, node),
                targetSummary(edge, node, poiNamesByNodeId),
                obstacle.getType(),
                obstacle.getDescription(),
                obstacle.getStatus(),
                obstacle.getCreatedAt(),
                obstacle.getUpdatedAt(),
                obstacle.getVersion());
    }

    /** The target's nodes, from which the labels and floors are derived. */
    public static Stream<Node> targetNodes(Obstacle obstacle) {
        Edge edge = obstacle.getEdge();
        return edge != null ? Stream.of(edge.getFromNode(), edge.getToNode()) : Stream.of(obstacle.getNode());
    }

    private static List<Long> floorIds(Edge edge, Node node) {
        if (edge == null) {
            return List.of(node.getFloor().getId());
        }
        return Stream.of(edge.getFromNode(), edge.getToNode())
                .map(endpoint -> endpoint.getFloor().getId())
                .distinct()
                .toList();
    }

    private static String targetSummary(Edge edge, Node node, Map<Long, String> poiNamesByNodeId) {
        if (edge == null) {
            return label(node, poiNamesByNodeId);
        }
        return label(edge.getFromNode(), poiNamesByNodeId) + " to " + label(edge.getToNode(), poiNamesByNodeId);
    }

    private static String label(Node node, Map<Long, String> poiNamesByNodeId) {
        if (node.getDisplayName() != null) {
            return node.getDisplayName();
        }
        return poiNamesByNodeId.getOrDefault(node.getId(), "Node " + node.getId());
    }
}
