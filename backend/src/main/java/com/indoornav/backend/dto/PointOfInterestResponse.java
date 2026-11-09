package com.indoornav.backend.dto;

public record PointOfInterestResponse(
        Long id,
        String name,
        String category,
        Long nodeId,
        Long floorId,
        String floorDisplayName) {
}
