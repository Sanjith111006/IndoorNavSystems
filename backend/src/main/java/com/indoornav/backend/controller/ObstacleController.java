package com.indoornav.backend.controller;

import com.indoornav.backend.config.OpenApiConfig;
import com.indoornav.backend.dto.ObstacleCreateRequest;
import com.indoornav.backend.dto.ObstacleResponse;
import com.indoornav.backend.dto.ObstacleUpdateRequest;
import com.indoornav.backend.entity.ObstacleStatus;
import com.indoornav.backend.service.ObstacleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/obstacles")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class ObstacleController {

    private final ObstacleService obstacleService;

    public ObstacleController(ObstacleService obstacleService) {
        this.obstacleService = obstacleService;
    }

    @GetMapping
    public List<ObstacleResponse> list(
            @RequestParam @Positive long buildingId,
            @RequestParam(required = false) ObstacleStatus status) {
        return obstacleService.list(buildingId, status);
    }

    @PostMapping
    public ResponseEntity<ObstacleResponse> create(@Valid @RequestBody ObstacleCreateRequest request) {
        ObstacleResponse created = obstacleService.create(request);
        return ResponseEntity.created(URI.create("/api/obstacles/" + created.id())).body(created);
    }

    @PutMapping("/{obstacleId}")
    public ObstacleResponse update(
            @PathVariable @Positive long obstacleId, @Valid @RequestBody ObstacleUpdateRequest request) {
        return obstacleService.update(obstacleId, request);
    }

    @DeleteMapping("/{obstacleId}")
    public ResponseEntity<Void> delete(@PathVariable @Positive long obstacleId) {
        obstacleService.delete(obstacleId);
        return ResponseEntity.noContent().build();
    }
}
