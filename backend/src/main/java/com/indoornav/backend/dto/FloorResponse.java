package com.indoornav.backend.dto;

public record FloorResponse(Long id, Long buildingId, int level, String displayName) {
}
