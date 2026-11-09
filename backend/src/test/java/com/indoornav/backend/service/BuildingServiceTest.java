package com.indoornav.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.indoornav.backend.TestEntities;
import com.indoornav.backend.dto.BuildingDetailResponse;
import com.indoornav.backend.dto.BuildingSummaryResponse;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.repository.BuildingRepository;
import com.indoornav.backend.repository.FloorRepository;
import com.indoornav.backend.repository.NodeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private NodeRepository nodeRepository;

    private BuildingService service;
    private final Floor basement = TestEntities.floor(9L, -1);
    private final Floor ground = TestEntities.floor(10L, 0);

    @BeforeEach
    void setUp() {
        service = new BuildingService(buildingRepository, floorRepository, nodeRepository);
    }

    @Test
    void defaultFloorIsTheLowestWithAnEntrance() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(TestEntities.building(1L, "Sample")));
        when(floorRepository.findByBuildingIdOrderByLevelAsc(1L)).thenReturn(List.of(basement, ground));
        when(floorRepository.findFloorsContainingNodeType(any(), any(NodeType.class), any()))
                .thenReturn(List.of(ground));

        BuildingDetailResponse response = service.getBuilding(1L);

        assertThat(response.defaultFloorId()).isEqualTo(10L);
        assertThat(response.floorCount()).isEqualTo(2);
    }

    @Test
    void defaultFloorFallsBackToTheLowestFloor() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(TestEntities.building(1L, "Sample")));
        when(floorRepository.findByBuildingIdOrderByLevelAsc(1L)).thenReturn(List.of(basement, ground));
        when(floorRepository.findFloorsContainingNodeType(any(), any(NodeType.class), any())).thenReturn(List.of());

        assertThat(service.getBuilding(1L).defaultFloorId()).isEqualTo(9L);
    }

    @Test
    void unknownBuildingIsNotFound() {
        when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBuilding(99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.listFloors(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listCountsFloorsAndTreatsMissingCountAsZero() {
        FloorRepository.FloorCount count = new FloorRepository.FloorCount() {
            @Override
            public Long getBuildingId() {
                return 1L;
            }

            @Override
            public long getFloorCount() {
                return 3;
            }
        };
        when(floorRepository.countFloorsPerBuilding()).thenReturn(List.of(count));
        when(buildingRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                TestEntities.building(1L, "A"), TestEntities.building(2L, "B")));

        List<BuildingSummaryResponse> buildings = service.listBuildings();

        assertThat(buildings).extracting(BuildingSummaryResponse::floorCount).containsExactly(3L, 0L);
    }
}
