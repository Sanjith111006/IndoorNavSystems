package com.indoornav.backend.repository;

import com.indoornav.backend.entity.PointOfInterest;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, Long> {

    @Query("""
            select p from PointOfInterest p
            join fetch p.node n
            where n.floor.id = :floorId
            order by p.id asc
            """)
    List<PointOfInterest> findOnFloor(@Param("floorId") Long floorId);

    /**
     * Patterns are lower-case LIKE patterns with wildcards in the search text already escaped.
     * The cast tells PostgreSQL the parameter's type when no category is given.
     */
    @Query("""
            select p from PointOfInterest p
            join fetch p.node n
            join fetch n.floor
            where n.buildingId = :buildingId
              and lower(p.name) like :containsPattern escape '\\'
              and (cast(:category as string) is null or p.category = :category)
            order by case when lower(p.name) like :prefixPattern escape '\\' then 0 else 1 end asc,
                     p.name asc, p.id asc
            """)
    List<PointOfInterest> search(
            @Param("buildingId") Long buildingId,
            @Param("containsPattern") String containsPattern,
            @Param("prefixPattern") String prefixPattern,
            @Param("category") String category,
            Limit limit);

    @Query("""
            select distinct p.category from PointOfInterest p
            join p.node n
            where n.buildingId = :buildingId
            order by p.category asc
            """)
    List<String> findCategories(@Param("buildingId") Long buildingId);

    @Query("select p from PointOfInterest p join p.node n where n.buildingId = :buildingId")
    List<PointOfInterest> findInBuilding(@Param("buildingId") Long buildingId);

    @Query("select p from PointOfInterest p where p.node.id in :nodeIds")
    List<PointOfInterest> findByNodeIds(@Param("nodeIds") Collection<Long> nodeIds);
}
