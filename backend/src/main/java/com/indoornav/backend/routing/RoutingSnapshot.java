package com.indoornav.backend.routing;

import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.engine.cost.RoutingCostConfiguration;
import com.indoornav.engine.graph.NavigationGraph;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One building's graph and active obstacles, read in a single transaction (ARCHITECTURE.md 6.3).
 *
 * <p>Holds plain values rather than entities, so routing runs outside the transaction and the
 * snapshot is built for every request and never cached (ARCHITECTURE.md AD-3).
 */
public record RoutingSnapshot(
        NavigationGraph graph,
        RoutingCostConfiguration costs,
        Map<Long, RoutingNode> nodes,
        Map<Long, String> floorNames,
        Map<String, String> connectionTypeNames,
        List<ActiveObstacle> activeObstacles) {

    /** The node fields a response needs. {@code poiName} labels nodes without a display name. */
    public record RoutingNode(
            long id,
            long floorId,
            BigDecimal x,
            BigDecimal y,
            NodeType type,
            String displayName,
            String poiName) {

        /** The display name, otherwise the linked POI's name, otherwise null. */
        public String label() {
            return displayName != null ? displayName : poiName;
        }
    }

    /** Exactly one of {@code edgeId} and {@code nodeId} is set, as in the obstacles table. */
    public record ActiveObstacle(long id, ObstacleType type, String description, Long edgeId, Long nodeId) {
    }

    public RoutingSnapshot {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(costs, "costs must not be null");
        nodes = Map.copyOf(nodes);
        floorNames = Map.copyOf(floorNames);
        connectionTypeNames = Map.copyOf(connectionTypeNames);
        activeObstacles = List.copyOf(activeObstacles);
    }

    public RoutingNode node(long nodeId) {
        RoutingNode node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node " + nodeId + " is not in this snapshot");
        }
        return node;
    }

    public String floorName(long floorId) {
        return floorNames.getOrDefault(floorId, "another floor");
    }

    /** A lower-case name for instruction text, such as "elevator" for the ELEVATOR code. */
    public String connectionName(String code) {
        String name = connectionTypeNames.getOrDefault(code, code.replace('_', ' '));
        return name.toLowerCase(Locale.ROOT);
    }

    public Set<Long> blockedNodeIds() {
        return activeObstacles.stream()
                .map(ActiveObstacle::nodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<Long> blockedEdgeIds() {
        return activeObstacles.stream()
                .map(ActiveObstacle::edgeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
