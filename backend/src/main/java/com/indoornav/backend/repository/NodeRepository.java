package com.indoornav.backend.repository;

import com.indoornav.backend.dto.LabelledNodeResponse;
import com.indoornav.backend.entity.Node;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NodeRepository extends JpaRepository<Node, Long> {

    List<Node> findByFloor_IdOrderByIdAsc(Long floorId);

    /** A node is labelled when it has a display name or a linked POI (API.md 6.8). */
    @Query("""
            select new com.indoornav.backend.dto.LabelledNodeResponse(
                n.id, coalesce(n.displayName, p.name), n.type, f.id, f.displayName, n.x, n.y)
            from Node n
            join n.floor f
            left join PointOfInterest p on p.node = n
            where n.buildingId = :buildingId
              and coalesce(n.displayName, p.name) is not null
              and lower(coalesce(n.displayName, p.name)) like :containsPattern escape '\\'
            order by coalesce(n.displayName, p.name) asc, n.id asc
            """)
    List<LabelledNodeResponse> findLabelled(
            @Param("buildingId") Long buildingId,
            @Param("containsPattern") String containsPattern,
            Limit limit);
}
