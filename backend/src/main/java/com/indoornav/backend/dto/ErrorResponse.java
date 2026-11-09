package com.indoornav.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** The single error body for every non-2xx response (API.md Section 4.2). */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String requestId,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldProblem> details) {

    public record FieldProblem(String field, String message) {
    }
}
