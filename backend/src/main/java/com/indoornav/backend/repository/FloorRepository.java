package com.indoornav.backend.repository;

import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.NodeType;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FloorRepository extends JpaRepository<Floor, Long> {

    interface FloorCount {
        Long getBuildingId();

        long getFloorCount();
    }

    List<Floor> findByBuildingIdOrderByLevelAsc(Long buildingId);

    long countByBuildingId(Long buildingId);

    @Query("select f.buildingId as buildingId, count(f) as floorCount from Floor f group by f.buildingId")
    List<FloorCount> countFloorsPerBuilding();

    @Query("""
            select f from Node n join n.floor f
            where n.buildingId = :buildingId and n.type = :nodeType
            order by f.level asc
            """)
    List<Floor> findFloorsContainingNodeType(
            @Param("buildingId") Long buildingId, @Param("nodeType") NodeType nodeType, Limit limit);
}
