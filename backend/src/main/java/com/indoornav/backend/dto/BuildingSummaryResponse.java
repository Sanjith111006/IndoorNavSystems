package com.indoornav.backend.dto;

public record BuildingSummaryResponse(
        Long id,
        String name,
        String description,
        String buildingType,
        String address,
        long floorCount) {
}
