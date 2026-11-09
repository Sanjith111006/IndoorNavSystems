package com.indoornav.backend.service;

import com.indoornav.backend.dto.PointOfInterestResponse;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.PointOfInterest;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.repository.BuildingRepository;
import com.indoornav.backend.repository.PointOfInterestRepository;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DestinationSearchService {

    private final BuildingRepository buildingRepository;
    private final PointOfInterestRepository pointOfInterestRepository;

    public DestinationSearchService(
            BuildingRepository buildingRepository,
            PointOfInterestRepository pointOfInterestRepository) {
        this.buildingRepository = buildingRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
    }

    public List<PointOfInterestResponse> search(long buildingId, String query, String category, int limit) {
        requireBuilding(buildingId);
        String normalisedQuery = SearchPatterns.normaliseQuery(query);
        return pointOfInterestRepository
                .search(
                        buildingId,
                        SearchPatterns.contains(normalisedQuery),
                        SearchPatterns.startsWith(normalisedQuery),
                        category,
                        Limit.of(limit))
                .stream()
                .map(DestinationSearchService::toResponse)
                .toList();
    }

    public List<String> listCategories(long buildingId) {
        requireBuilding(buildingId);
        return pointOfInterestRepository.findCategories(buildingId);
    }

    private void requireBuilding(long buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new ResourceNotFoundException("Building");
        }
    }

    private static PointOfInterestResponse toResponse(PointOfInterest point) {
        Node node = point.getNode();
        return new PointOfInterestResponse(
                point.getId(),
                point.getName(),
                point.getCategory(),
                node.getId(),
                node.getFloor().getId(),
                node.getFloor().getDisplayName());
    }
}
