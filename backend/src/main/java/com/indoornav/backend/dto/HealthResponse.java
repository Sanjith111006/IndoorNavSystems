package com.indoornav.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Health output (API.md 6.1). When down, the standard error fields are added, except that
 * {@code status} stays the UP or DOWN value monitors read rather than the HTTP status number.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
        String status,
        String database,
        Instant timestamp,
        String code,
        String message,
        String path,
        String requestId) {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    public static HealthResponse up() {
        return new HealthResponse(UP, UP, null, null, null, null, null);
    }

    public static HealthResponse down(Instant timestamp, String code, String message, String path, String requestId) {
        return new HealthResponse(DOWN, DOWN, timestamp, code, message, path, requestId);
    }
}
