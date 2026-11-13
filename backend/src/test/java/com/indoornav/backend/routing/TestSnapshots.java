package com.indoornav.backend.routing;

import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.backend.routing.RoutingSnapshot.ActiveObstacle;
import com.indoornav.backend.routing.RoutingSnapshot.RoutingNode;
import com.indoornav.engine.cost.ConnectionCost;
import com.indoornav.engine.cost.RoutingCostConfiguration;
import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.model.ConnectionType;
import com.indoornav.engine.model.NavigationEdge;
import com.indoornav.engine.model.NavigationNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small three-floor graph for routing tests, with costs matching the V6 reference data.
 *
 * <pre>
 * Ground (10): Entrance(1) -10- (2) -10- Cafe(3) -10- Stairs(6, not accessible)
 *                  \                    /  \
 *                   15 - (4) ------- 15     12 - Lift(7)
 * Level 1 (11): Stairs(8) -10- Lounge(10) -12- Lift(9)       Level 2 (12): Lift(12)
 * Vertical: 6-8 stairs 5 m (not accessible), 7-9 elevator 5 m, 9-12 elevator 5 m.
 * Node 11 on the ground floor has no connections.
 * </pre>
 *
 * SHORTEST from 1 to 10 takes the stairs (45 m); FASTEST and ACCESSIBLE take the elevator (49 m).
 */
public final class TestSnapshots {

    public static final long GROUND = 10L;
    public static final long LEVEL_ONE = 11L;
    public static final long LEVEL_TWO = 12L;
    public static final long ENTRANCE = 1L;
    public static final long CAFE = 3L;
    public static final long GROUND_STAIRS = 6L;
    public static final long LOUNGE = 10L;
    public static final long ISOLATED = 11L;
    public static final long LEVEL_TWO_LIFT = 12L;
    public static final long CORRIDOR_2_TO_3 = 102L;
    public static final long CORRIDOR_1_TO_4 = 103L;
    public static final long ELEVATOR_GROUND_TO_ONE = 108L;

    private static final List<RoutingNode> NODES = List.of(
            node(1, GROUND, 0, 0, NodeType.BUILDING_ENTRANCE, "Entrance", null),
            node(2, GROUND, 10, 0, NodeType.CORRIDOR_JUNCTION, null, null),
            node(3, GROUND, 20, 0, NodeType.POINT_OF_INTEREST, null, "Cafe"),
            node(4, GROUND, 10, 10, NodeType.CORRIDOR_JUNCTION, null, null),
            node(6, GROUND, 30, 0, NodeType.STAIR_LANDING, "Ground stairs", null),
            node(7, GROUND, 30, 5, NodeType.ELEVATOR_ENTRANCE, "Ground lift", null),
            node(8, LEVEL_ONE, 30, 0, NodeType.STAIR_LANDING, "Level 1 stairs", null),
            node(9, LEVEL_ONE, 30, 5, NodeType.ELEVATOR_ENTRANCE, "Level 1 lift", null),
            node(10, LEVEL_ONE, 40, 0, NodeType.POINT_OF_INTEREST, null, "Lounge"),
            node(11, GROUND, 50, 50, NodeType.CORRIDOR_JUNCTION, null, null),
            node(12, LEVEL_TWO, 30, 5, NodeType.ELEVATOR_ENTRANCE, "Level 2 lift", null));

    private static final List<NavigationEdge> EDGES = List.of(
            edge(101, 1, 2, 10, ConnectionType.CORRIDOR, true),
            edge(CORRIDOR_2_TO_3, 2, 3, 10, ConnectionType.CORRIDOR, true),
            edge(CORRIDOR_1_TO_4, 1, 4, 15, ConnectionType.CORRIDOR, true),
            edge(104, 4, 3, 15, ConnectionType.CORRIDOR, true),
            edge(105, 3, 6, 10, ConnectionType.CORRIDOR, true),
            edge(106, 3, 7, 12, ConnectionType.CORRIDOR, true),
            edge(107, 6, 8, 5, ConnectionType.STAIRS, false),
            edge(ELEVATOR_GROUND_TO_ONE, 7, 9, 5, ConnectionType.ELEVATOR, true),
            edge(109, 8, 10, 10, ConnectionType.CORRIDOR, true),
            edge(110, 9, 10, 12, ConnectionType.CORRIDOR, true),
            edge(111, 9, 12, 5, ConnectionType.ELEVATOR, true));

    private TestSnapshots() {
    }

    public static RoutingSnapshot open() {
        return withObstacles(List.of());
    }

    public static RoutingSnapshot withObstacles(List<ActiveObstacle> obstacles) {
        List<NavigationNode> engineNodes = new ArrayList<>();
        for (RoutingNode node : NODES) {
            boolean isAccessible = node.type() != NodeType.STAIR_LANDING;
            engineNodes.add(new NavigationNode(
                    node.id(), node.floorId(), node.x().doubleValue(), node.y().doubleValue(),
                    com.indoornav.engine.model.NodeType.valueOf(node.type().name()), isAccessible));
        }
        NavigationGraph graph = new NavigationGraph(
                engineNodes, EDGES, Map.of(GROUND, 1.0, LEVEL_ONE, 1.0, LEVEL_TWO, 1.0));
        RoutingCostConfiguration costs = new RoutingCostConfiguration(
                Map.of(
                        ConnectionType.CORRIDOR, new ConnectionCost(1.2, 0.0),
                        ConnectionType.STAIRS, new ConnectionCost(0.25, 0.0),
                        ConnectionType.ELEVATOR, new ConnectionCost(4.0, 5.0)),
                10.0,
                2.0);
        Map<Long, RoutingNode> nodesById = new HashMap<>();
        NODES.forEach(node -> nodesById.put(node.id(), node));
        return new RoutingSnapshot(
                graph,
                costs,
                nodesById,
                Map.of(GROUND, "Ground", LEVEL_ONE, "Level 1", LEVEL_TWO, "Level 2"),
                Map.of("CORRIDOR", "Corridor", "STAIRS", "Stairs", "ELEVATOR", "Elevator"),
                obstacles);
    }

    public static ActiveObstacle onEdge(long obstacleId, long edgeId, ObstacleType type) {
        return new ActiveObstacle(obstacleId, type, "Closed for the test", edgeId, null);
    }

    public static ActiveObstacle onNode(long obstacleId, long nodeId) {
        return new ActiveObstacle(obstacleId, ObstacleType.CONSTRUCTION_AREA, "Closed for the test", null, nodeId);
    }

    private static RoutingNode node(
            long id, long floorId, int x, int y, NodeType type, String displayName, String poiName) {
        return new RoutingNode(id, floorId, BigDecimal.valueOf(x), BigDecimal.valueOf(y), type, displayName, poiName);
    }

    private static NavigationEdge edge(long id, long from, long to, double distance, ConnectionType type, boolean accessible) {
        return new NavigationEdge(id, from, to, distance, null, type, accessible, true);
    }
}
