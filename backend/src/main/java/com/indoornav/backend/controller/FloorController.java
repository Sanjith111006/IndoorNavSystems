package com.indoornav.backend.controller;

import com.indoornav.backend.dto.FloorMapResponse;
import com.indoornav.backend.service.FloorMapService;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/floors")
public class FloorController {

    private final FloorMapService floorMapService;

    public FloorController(FloorMapService floorMapService) {
        this.floorMapService = floorMapService;
    }

    @GetMapping("/{floorId}/map")
    public FloorMapResponse getFloorMap(@PathVariable @Positive long floorId) {
        return floorMapService.getFloorMap(floorId);
    }
}
