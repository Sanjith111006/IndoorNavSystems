package com.indoornav.backend.service;

import com.indoornav.backend.dto.ObstacleCreateRequest;
import com.indoornav.backend.dto.ObstacleResponse;
import com.indoornav.backend.dto.ObstacleTargetType;
import com.indoornav.backend.dto.ObstacleUpdateRequest;
import com.indoornav.backend.entity.ConnectionType;
import com.indoornav.backend.entity.Edge;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.entity.Obstacle;
import com.indoornav.backend.entity.ObstacleStatus;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.backend.entity.PointOfInterest;
import com.indoornav.backend.exception.ConflictException;
import com.indoornav.backend.exception.FieldValidationException;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.mapper.ObstacleMapper;
import com.indoornav.backend.repository.BuildingRepository;
import com.indoornav.backend.repository.EdgeRepository;
import com.indoornav.backend.repository.NodeRepository;
import com.indoornav.backend.repository.ObstacleRepository;
import com.indoornav.backend.repository.PointOfInterestRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrator management of obstacles. Each public method is one transaction. */
@Service
public class ObstacleService {

    private static final Logger log = LoggerFactory.getLogger(ObstacleService.class);
    private static final String OBSTACLE = "Obstacle";
    private static final String ALREADY_ACTIVE = "An active obstacle already exists on this target.";
    private static final String STALE_VERSION = "This obstacle was changed by someone else.";

    private final ObstacleRepository obstacleRepository;
    private final BuildingRepository buildingRepository;
    private final EdgeRepository edgeRepository;
    private final NodeRepository nodeRepository;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final Clock clock;

    public ObstacleService(
            ObstacleRepository obstacleRepository,
            BuildingRepository buildingRepository,
            EdgeRepository edgeRepository,
            NodeRepository nodeRepository,
            PointOfInterestRepository pointOfInterestRepository,
            Clock clock) {
        this.obstacleRepository = obstacleRepository;
        this.buildingRepository = buildingRepository;
        this.edgeRepository = edgeRepository;
        this.nodeRepository = nodeRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ObstacleResponse> list(long buildingId, ObstacleStatus status) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new ResourceNotFoundException("Building");
        }
        List<Obstacle> obstacles = status == null
                ? obstacleRepository.findByBuildingIdOrderByUpdatedAtDescIdDesc(buildingId)
                : obstacleRepository.findByBuildingIdAndStatusOrderByUpdatedAtDescIdDesc(buildingId, status);
        Map<Long, String> poiNames = poiNamesFor(obstacles);
        return obstacles.stream()
                .map(obstacle -> ObstacleMapper.toResponse(obstacle, poiNames))
                .toList();
    }

    @Transactional
    public ObstacleResponse create(ObstacleCreateRequest request) {
        Obstacle obstacle = request.targetType() == ObstacleTargetType.EDGE
                ? newEdgeObstacle(request)
                : newNodeObstacle(request);
        Obstacle saved = obstacleRepository.saveAndFlush(obstacle);
        log.info("Created obstacle {} of type {}", saved.getId(), saved.getType());
        return toResponse(saved);
    }

    @Transactional
    public ObstacleResponse update(long obstacleId, ObstacleUpdateRequest request) {
        Obstacle obstacle = findObstacle(obstacleId);
        if (!obstacle.getVersion().equals(request.version())) {
            throw new ConflictException(STALE_VERSION);
        }
        requireCompatibleType(request.obstacleType(), obstacle.getEdge(), obstacle.getNode());
        if (isReactivation(obstacle, request) && hasActiveObstacleOnTarget(obstacle)) {
            throw new ConflictException(ALREADY_ACTIVE);
        }
        obstacle.update(request.obstacleType(), request.description(), request.status(), clock.instant());
        // Flushing now bumps the version, so the response carries the value clients must send next.
        Obstacle saved = obstacleRepository.saveAndFlush(obstacle);
        log.info("Updated obstacle {} to status {}", saved.getId(), saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public void delete(long obstacleId) {
        Obstacle obstacle = obstacleRepository.findById(obstacleId)
                .orElseThrow(() -> new ResourceNotFoundException(OBSTACLE));
        obstacleRepository.delete(obstacle);
        log.info("Deleted obstacle {}", obstacleId);
    }

    private Obstacle newEdgeObstacle(ObstacleCreateRequest request) {
        Edge edge = edgeRepository.findWithNodesById(request.targetId())
                .orElseThrow(() -> new ResourceNotFoundException("Edge"));
        requireCompatibleType(request.obstacleType(), edge, null);
        if (obstacleRepository.existsByEdge_IdAndStatus(edge.getId(), ObstacleStatus.ACTIVE)) {
            throw new ConflictException(ALREADY_ACTIVE);
        }
        return Obstacle.onEdge(edge, request.obstacleType(), request.description(), clock.instant());
    }

    private Obstacle newNodeObstacle(ObstacleCreateRequest request) {
        Node node = nodeRepository.findById(request.targetId())
                .orElseThrow(() -> new ResourceNotFoundException("Node"));
        requireCompatibleType(request.obstacleType(), null, node);
        if (obstacleRepository.existsByNode_IdAndStatus(node.getId(), ObstacleStatus.ACTIVE)) {
            throw new ConflictException(ALREADY_ACTIVE);
        }
        return Obstacle.onNode(node, request.obstacleType(), request.description(), clock.instant());
    }

    /**
     * Only the two types tied to a specific kind of connection are restricted. The documents name
     * no target rule for the others, so they may be placed on any edge or node.
     */
    private static void requireCompatibleType(ObstacleType type, Edge edge, Node node) {
        boolean isCompatible = switch (type) {
            case BROKEN_ELEVATOR -> isTargetKind(edge, node, ConnectionType.ELEVATOR, NodeType.ELEVATOR_ENTRANCE);
            case CLOSED_STAIRCASE -> isTargetKind(edge, node, ConnectionType.STAIRS, NodeType.STAIR_LANDING);
            default -> true;
        };
        if (!isCompatible) {
            throw new FieldValidationException("obstacleType", "does not apply to this target");
        }
    }

    private static boolean isTargetKind(Edge edge, Node node, String connectionTypeCode, NodeType nodeType) {
        return edge != null
                ? connectionTypeCode.equals(edge.getConnectionTypeCode())
                : node.getType() == nodeType;
    }

    private static boolean isReactivation(Obstacle obstacle, ObstacleUpdateRequest request) {
        return obstacle.getStatus() == ObstacleStatus.RESOLVED && request.status() == ObstacleStatus.ACTIVE;
    }

    private boolean hasActiveObstacleOnTarget(Obstacle obstacle) {
        return obstacle.getEdge() != null
                ? obstacleRepository.existsByEdge_IdAndStatus(obstacle.getEdge().getId(), ObstacleStatus.ACTIVE)
                : obstacleRepository.existsByNode_IdAndStatus(obstacle.getNode().getId(), ObstacleStatus.ACTIVE);
    }

    private Obstacle findObstacle(long obstacleId) {
        return obstacleRepository.findWithTargetById(obstacleId)
                .orElseThrow(() -> new ResourceNotFoundException(OBSTACLE));
    }

    private ObstacleResponse toResponse(Obstacle obstacle) {
        return ObstacleMapper.toResponse(obstacle, poiNamesFor(List.of(obstacle)));
    }

    private Map<Long, String> poiNamesFor(List<Obstacle> obstacles) {
        Set<Long> nodeIds = obstacles.stream()
                .flatMap(ObstacleMapper::targetNodes)
                .map(Node::getId)
                .collect(Collectors.toSet());
        if (nodeIds.isEmpty()) {
            return Map.of();
        }
        return pointOfInterestRepository.findByNodeIds(nodeIds).stream()
                .collect(Collectors.toMap(point -> point.getNode().getId(), PointOfInterest::getName));
    }
}
