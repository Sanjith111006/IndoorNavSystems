package com.indoornav.backend.service;

import com.indoornav.backend.dto.FloorMapResponse;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.Obstacle;
import com.indoornav.backend.entity.ObstacleStatus;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.mapper.BuildingMapper;
import com.indoornav.backend.mapper.FloorMapMapper;
import com.indoornav.backend.repository.EdgeRepository;
import com.indoornav.backend.repository.FloorRepository;
import com.indoornav.backend.repository.NodeRepository;
import com.indoornav.backend.repository.ObstacleRepository;
import com.indoornav.backend.repository.PointOfInterestRepository;
import com.indoornav.backend.repository.RoomRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assembles one floor's map. Obstacles are read on every call, so the state is never stale. */
@Service
@Transactional(readOnly = true)
public class FloorMapService {

    private final FloorRepository floorRepository;
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final RoomRepository roomRepository;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final ObstacleRepository obstacleRepository;

    public FloorMapService(
            FloorRepository floorRepository,
            NodeRepository nodeRepository,
            EdgeRepository edgeRepository,
            RoomRepository roomRepository,
            PointOfInterestRepository pointOfInterestRepository,
            ObstacleRepository obstacleRepository) {
        this.floorRepository = floorRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.roomRepository = roomRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.obstacleRepository = obstacleRepository;
    }

    public FloorMapResponse getFloorMap(long floorId) {
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor"));

        Map<Long, Obstacle> obstaclesByEdgeId = new HashMap<>();
        Map<Long, Obstacle> obstaclesByNodeId = new HashMap<>();
        for (Obstacle obstacle : obstacleRepository.findByBuildingIdAndStatus(
                floor.getBuildingId(), ObstacleStatus.ACTIVE)) {
            if (obstacle.getEdge() != null) {
                obstaclesByEdgeId.put(obstacle.getEdge().getId(), obstacle);
            } else {
                obstaclesByNodeId.put(obstacle.getNode().getId(), obstacle);
            }
        }

        List<FloorMapResponse.MapNode> nodes = nodeRepository.findByFloor_IdOrderByIdAsc(floorId).stream()
                .map(node -> FloorMapMapper.toNode(node, obstaclesByNodeId.get(node.getId())))
                .toList();
        List<FloorMapResponse.MapEdge> edges = edgeRepository.findTouchingFloor(floorId).stream()
                .map(edge -> FloorMapMapper.toEdge(edge, floorId, obstaclesByEdgeId.get(edge.getId())))
                .toList();
        List<FloorMapResponse.MapRoom> rooms = roomRepository.findByFloorIdOrderByIdAsc(floorId).stream()
                .map(FloorMapMapper::toRoom)
                .toList();
        List<FloorMapResponse.MapPointOfInterest> points = pointOfInterestRepository.findOnFloor(floorId).stream()
                .map(FloorMapMapper::toPointOfInterest)
                .toList();

        return new FloorMapResponse(
                BuildingMapper.toFloor(floor), FloorMapMapper.toPlan(floor), nodes, edges, rooms, points);
    }
}
