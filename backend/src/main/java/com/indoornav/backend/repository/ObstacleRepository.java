package com.indoornav.backend.repository;

import com.indoornav.backend.entity.Obstacle;
import com.indoornav.backend.entity.ObstacleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObstacleRepository extends JpaRepository<Obstacle, Long> {

    List<Obstacle> findByBuildingIdAndStatus(Long buildingId, ObstacleStatus status);

    @EntityGraph(attributePaths = {"edge.fromNode", "edge.toNode", "node"})
    List<Obstacle> findByBuildingIdOrderByUpdatedAtDescIdDesc(Long buildingId);

    @EntityGraph(attributePaths = {"edge.fromNode", "edge.toNode", "node"})
    List<Obstacle> findByBuildingIdAndStatusOrderByUpdatedAtDescIdDesc(Long buildingId, ObstacleStatus status);

    @EntityGraph(attributePaths = {"edge.fromNode", "edge.toNode", "node"})
    Optional<Obstacle> findWithTargetById(Long id);

    boolean existsByEdge_IdAndStatus(Long edgeId, ObstacleStatus status);

    boolean existsByNode_IdAndStatus(Long nodeId, ObstacleStatus status);
}
