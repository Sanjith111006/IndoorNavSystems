package com.indoornav.backend.controller;

import com.indoornav.backend.dto.BuildingDetailResponse;
import com.indoornav.backend.dto.BuildingSummaryResponse;
import com.indoornav.backend.dto.FloorResponse;
import com.indoornav.backend.dto.LabelledNodeResponse;
import com.indoornav.backend.service.BuildingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    public List<BuildingSummaryResponse> listBuildings() {
        return buildingService.listBuildings();
    }

    @GetMapping("/{buildingId}")
    public BuildingDetailResponse getBuilding(@PathVariable @Positive long buildingId) {
        return buildingService.getBuilding(buildingId);
    }

    @GetMapping("/{buildingId}/floors")
    public List<FloorResponse> listFloors(@PathVariable @Positive long buildingId) {
        return buildingService.listFloors(buildingId);
    }

    @GetMapping("/{buildingId}/nodes")
    public List<LabelledNodeResponse> listNodes(
            @PathVariable @Positive long buildingId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return buildingService.listLabelledNodes(buildingId, query, limit);
    }
}
