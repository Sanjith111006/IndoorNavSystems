package com.indoornav.backend.service;

import com.indoornav.backend.dto.BuildingDetailResponse;
import com.indoornav.backend.dto.BuildingSummaryResponse;
import com.indoornav.backend.dto.FloorResponse;
import com.indoornav.backend.dto.LabelledNodeResponse;
import com.indoornav.backend.entity.Building;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.mapper.BuildingMapper;
import com.indoornav.backend.repository.BuildingRepository;
import com.indoornav.backend.repository.FloorRepository;
import com.indoornav.backend.repository.NodeRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final NodeRepository nodeRepository;

    public BuildingService(
            BuildingRepository buildingRepository,
            FloorRepository floorRepository,
            NodeRepository nodeRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.nodeRepository = nodeRepository;
    }

    public List<BuildingSummaryResponse> listBuildings() {
        Map<Long, Long> floorCounts = floorRepository.countFloorsPerBuilding().stream()
                .collect(Collectors.toMap(
                        FloorRepository.FloorCount::getBuildingId, FloorRepository.FloorCount::getFloorCount));
        return buildingRepository.findAllByOrderByNameAsc().stream()
                .map(building -> BuildingMapper.toSummary(
                        building, floorCounts.getOrDefault(building.getId(), 0L)))
                .toList();
    }

    public BuildingDetailResponse getBuilding(long buildingId) {
        Building building = findBuilding(buildingId);
        List<Floor> floors = floorRepository.findByBuildingIdOrderByLevelAsc(buildingId);
        return BuildingMapper.toDetail(building, floors.size(), defaultFloorId(buildingId, floors));
    }

    public List<FloorResponse> listFloors(long buildingId) {
        findBuilding(buildingId);
        return floorRepository.findByBuildingIdOrderByLevelAsc(buildingId).stream()
                .map(BuildingMapper::toFloor)
                .toList();
    }

    public List<LabelledNodeResponse> listLabelledNodes(long buildingId, String query, int limit) {
        findBuilding(buildingId);
        String pattern = SearchPatterns.contains(SearchPatterns.normaliseQuery(query));
        return nodeRepository.findLabelled(buildingId, pattern, Limit.of(limit));
    }

    /** The lowest floor with a building entrance, otherwise the lowest floor (API.md APA-1). */
    private Long defaultFloorId(long buildingId, List<Floor> floorsByLevel) {
        if (floorsByLevel.isEmpty()) {
            throw new IllegalStateException("Building " + buildingId + " has no floors");
        }
        List<Floor> entranceFloors = floorRepository.findFloorsContainingNodeType(
                buildingId, NodeType.BUILDING_ENTRANCE, Limit.of(1));
        Floor defaultFloor = entranceFloors.isEmpty() ? floorsByLevel.get(0) : entranceFloors.get(0);
        return defaultFloor.getId();
    }

    private Building findBuilding(long buildingId) {
        return buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building"));
    }
}
