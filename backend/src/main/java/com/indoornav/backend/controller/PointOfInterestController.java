package com.indoornav.backend.controller;

import com.indoornav.backend.dto.PointOfInterestResponse;
import com.indoornav.backend.service.DestinationSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points-of-interest")
public class PointOfInterestController {

    private static final String CATEGORY_SLUG = "[a-z][a-z0-9-]*";

    private final DestinationSearchService destinationSearchService;

    public PointOfInterestController(DestinationSearchService destinationSearchService) {
        this.destinationSearchService = destinationSearchService;
    }

    @GetMapping
    public List<PointOfInterestResponse> search(
            @RequestParam @Positive long buildingId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @Size(max = 50) @Pattern(regexp = CATEGORY_SLUG) String category,
            @RequestParam(defaultValue = "30") @Min(1) @Max(50) int limit) {
        return destinationSearchService.search(buildingId, query, category, limit);
    }

    @GetMapping("/categories")
    public List<String> listCategories(@RequestParam @Positive long buildingId) {
        return destinationSearchService.listCategories(buildingId);
    }
}
