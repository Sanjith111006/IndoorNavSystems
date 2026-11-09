package com.indoornav.backend.dto;

public record BuildingDetailResponse(
        Long id,
        String name,
        String description,
        String buildingType,
        String address,
        long floorCount,
        Long defaultFloorId) {
}
