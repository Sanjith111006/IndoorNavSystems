package com.indoornav.backend.mapper;

import com.indoornav.backend.dto.BuildingDetailResponse;
import com.indoornav.backend.dto.BuildingSummaryResponse;
import com.indoornav.backend.dto.FloorResponse;
import com.indoornav.backend.entity.Building;
import com.indoornav.backend.entity.Floor;

public final class BuildingMapper {

    private BuildingMapper() {
    }

    public static BuildingSummaryResponse toSummary(Building building, long floorCount) {
        return new BuildingSummaryResponse(
                building.getId(),
                building.getName(),
                building.getDescription(),
                building.getBuildingType(),
                building.getAddress(),
                floorCount);
    }

    public static BuildingDetailResponse toDetail(Building building, long floorCount, Long defaultFloorId) {
        return new BuildingDetailResponse(
                building.getId(),
                building.getName(),
                building.getDescription(),
                building.getBuildingType(),
                building.getAddress(),
                floorCount,
                defaultFloorId);
    }

    public static FloorResponse toFloor(Floor floor) {
        return new FloorResponse(floor.getId(), floor.getBuildingId(), floor.getLevel(), floor.getDisplayName());
    }
}
