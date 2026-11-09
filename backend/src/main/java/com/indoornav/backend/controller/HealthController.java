package com.indoornav.backend.controller;

import com.indoornav.backend.dto.HealthResponse;
import com.indoornav.backend.exception.ErrorCode;
import com.indoornav.backend.security.RequestIdFilter;
import com.indoornav.backend.service.HealthService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;
    private final Clock clock;

    public HealthController(HealthService healthService, Clock clock) {
        this.healthService = healthService;
        this.clock = clock;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health(HttpServletRequest request) {
        if (healthService.isDatabaseUp()) {
            return ResponseEntity.ok(HealthResponse.up());
        }
        ErrorCode code = ErrorCode.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(code.status()).body(HealthResponse.down(
                clock.instant(),
                code.name(),
                code.defaultMessage(),
                request.getRequestURI(),
                RequestIdFilter.currentRequestId()));
    }
}
