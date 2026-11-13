package com.indoornav.backend.routing;

import com.indoornav.backend.config.RoutingProperties;
import com.indoornav.backend.entity.ConnectionType;
import com.indoornav.backend.entity.Edge;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.Obstacle;
import com.indoornav.backend.entity.ObstacleStatus;
import com.indoornav.backend.entity.PointOfInterest;
import com.indoornav.backend.repository.ConnectionTypeRepository;
import com.indoornav.backend.repository.EdgeRepository;
import com.indoornav.backend.repository.FloorRepository;
import com.indoornav.backend.repository.NodeRepository;
import com.indoornav.backend.repository.ObstacleRepository;
import com.indoornav.backend.repository.PointOfInterestRepository;
import com.indoornav.engine.cost.ConnectionCost;
import com.indoornav.engine.cost.RoutingCostConfiguration;
import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.model.NavigationEdge;
import com.indoornav.engine.model.NavigationNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Reads a building's graph and active obstacles and maps them to the engine's model. */
@Component
public class RoutingGraphLoader {

    private final FloorRepository floorRepository;
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final ConnectionTypeRepository connectionTypeRepository;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final ObstacleRepository obstacleRepository;
    private final RoutingProperties routingProperties;

    public RoutingGraphLoader(
            FloorRepository floorRepository,
            NodeRepository nodeRepository,
            EdgeRepository edgeRepository,
            ConnectionTypeRepository connectionTypeRepository,
            PointOfInterestRepository pointOfInterestRepository,
            ObstacleRepository obstacleRepository,
            RoutingProperties routingProperties) {
        this.floorRepository = floorRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.connectionTypeRepository = connectionTypeRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.obstacleRepository = obstacleRepository;
        this.routingProperties = routingProperties;
    }

    /** One read-only transaction, so the graph and obstacles come from one consistent snapshot. */
    @Transactional(readOnly = true)
    public RoutingSnapshot load(long buildingId) {
        List<Floor> floors = floorRepository.findByBuildingIdOrderByLevelAsc(buildingId);
        List<Node> nodes = nodeRepository.findByBuildingId(buildingId);
        List<Edge> edges = edgeRepository.findByBuildingId(buildingId);
        List<ConnectionType> connectionTypes = connectionTypeRepository.findAll();
        Map<Long, String> poiNames = pointOfInterestRepository.findInBuilding(buildingId).stream()
                .collect(Collectors.toMap(point -> point.getNode().getId(), PointOfInterest::getName));
        List<Obstacle> obstacles = obstacleRepository.findByBuildingIdAndStatus(buildingId, ObstacleStatus.ACTIVE);

        NavigationGraph graph = new NavigationGraph(
                nodes.stream().map(RoutingGraphLoader::toEngineNode).toList(),
                edges.stream().map(RoutingGraphLoader::toEngineEdge).toList(),
                floors.stream().collect(Collectors.toMap(Floor::getId, floor -> floor.getMetresPerUnit().doubleValue())));

        return new RoutingSnapshot(
                graph,
                costConfiguration(connectionTypes),
                nodes.stream().collect(Collectors.toMap(Node::getId, node -> toRoutingNode(node, poiNames))),
                floors.stream().collect(Collectors.toMap(Floor::getId, Floor::getDisplayName)),
                connectionTypes.stream().collect(Collectors.toMap(ConnectionType::getCode, ConnectionType::getName)),
                obstacles.stream().map(RoutingGraphLoader::toActiveObstacle).toList());
    }

    private RoutingCostConfiguration costConfiguration(List<ConnectionType> connectionTypes) {
        Map<com.indoornav.engine.model.ConnectionType, ConnectionCost> costs = connectionTypes.stream()
                .collect(Collectors.toMap(
                        type -> new com.indoornav.engine.model.ConnectionType(type.getCode()),
                        type -> new ConnectionCost(type.getSpeedMps().doubleValue(), type.getFixedSeconds().doubleValue())));
        return new RoutingCostConfiguration(
                costs, routingProperties.avoidStairsMultiplier(), routingProperties.preferElevatorMultiplier());
    }

    private static NavigationNode toEngineNode(Node node) {
        return new NavigationNode(
                node.getId(),
                node.getFloor().getId(),
                node.getX().doubleValue(),
                node.getY().doubleValue(),
                com.indoornav.engine.model.NodeType.valueOf(node.getType().name()),
                node.isAccessible());
    }

    private static NavigationEdge toEngineEdge(Edge edge) {
        return new NavigationEdge(
                edge.getId(),
                edge.getFromNode().getId(),
                edge.getToNode().getId(),
                edge.getDistanceMeters().doubleValue(),
                edge.getTimeSeconds() == null ? null : edge.getTimeSeconds().doubleValue(),
                new com.indoornav.engine.model.ConnectionType(edge.getConnectionTypeCode()),
                edge.isAccessible(),
                edge.isBidirectional());
    }

    private static RoutingSnapshot.RoutingNode toRoutingNode(Node node, Map<Long, String> poiNames) {
        return new RoutingSnapshot.RoutingNode(
                node.getId(),
                node.getFloor().getId(),
                node.getX(),
                node.getY(),
                node.getType(),
                node.getDisplayName(),
                poiNames.get(node.getId()));
    }

    private static RoutingSnapshot.ActiveObstacle toActiveObstacle(Obstacle obstacle) {
        return new RoutingSnapshot.ActiveObstacle(
                obstacle.getId(),
                obstacle.getType(),
                obstacle.getDescription(),
                obstacle.getEdge() == null ? null : obstacle.getEdge().getId(),
                obstacle.getNode() == null ? null : obstacle.getNode().getId());
    }
}
