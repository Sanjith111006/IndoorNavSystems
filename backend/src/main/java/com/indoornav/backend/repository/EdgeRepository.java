package com.indoornav.backend.repository;

import com.indoornav.backend.entity.Edge;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EdgeRepository extends JpaRepository<Edge, Long> {

    @Query("""
            select e from Edge e
            join fetch e.fromNode source
            join fetch e.toNode target
            where source.floor.id = :floorId or target.floor.id = :floorId
            order by e.id asc
            """)
    List<Edge> findTouchingFloor(@Param("floorId") Long floorId);

    @Query("select e from Edge e join fetch e.fromNode join fetch e.toNode where e.id = :edgeId")
    Optional<Edge> findWithNodesById(@Param("edgeId") Long edgeId);
}
