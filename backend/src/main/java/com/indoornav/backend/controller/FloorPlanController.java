package com.indoornav.backend.controller;

import com.indoornav.backend.service.FloorPlanService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** The one route exempt from {@code no-store}: plan files change only with releases. */
@RestController
public class FloorPlanController {

    private static final String SVG_MEDIA_TYPE = "image/svg+xml";
    private static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";
    private static final String NO_SCRIPTS_POLICY = "default-src 'none'; style-src 'unsafe-inline'; sandbox";
    private static final Duration CACHE_LIFETIME = Duration.ofHours(1);

    private final FloorPlanService floorPlanService;

    public FloorPlanController(FloorPlanService floorPlanService) {
        this.floorPlanService = floorPlanService;
    }

    /** Headers go on the servlet response directly to replace the filter's {@code no-store}. */
    @GetMapping(value = "/assets/floorplans/{*path}", produces = SVG_MEDIA_TYPE)
    public ResponseEntity<byte[]> getFloorPlan(
            @PathVariable String path,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            HttpServletResponse response) {
        FloorPlanService.FloorPlan plan = floorPlanService.load(path.substring(1));

        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(CACHE_LIFETIME).getHeaderValue());
        response.setHeader(HttpHeaders.ETAG, plan.entityTag());
        response.setHeader(CONTENT_SECURITY_POLICY_HEADER, NO_SCRIPTS_POLICY);

        if (plan.entityTag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok(plan.content());
    }
}
