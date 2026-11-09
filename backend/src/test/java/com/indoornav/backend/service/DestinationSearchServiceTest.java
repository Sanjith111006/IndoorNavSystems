package com.indoornav.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indoornav.backend.TestEntities;
import com.indoornav.backend.dto.PointOfInterestResponse;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.exception.FieldValidationException;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.repository.BuildingRepository;
import com.indoornav.backend.repository.PointOfInterestRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class DestinationSearchServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private PointOfInterestRepository pointOfInterestRepository;

    private DestinationSearchService service;

    @BeforeEach
    void setUp() {
        service = new DestinationSearchService(buildingRepository, pointOfInterestRepository);
    }

    @Test
    void searchEscapesWildcardsAndLowersTheText() {
        when(buildingRepository.existsById(1L)).thenReturn(true);

        service.search(1L, "  50%_OFF  ", null, 30);

        verify(pointOfInterestRepository).search(
                eq(1L), eq("%50\\%\\_off%"), eq("50\\%\\_off%"), isNull(), eq(Limit.of(30)));
    }

    @Test
    void blankQueryBrowsesEverything() {
        when(buildingRepository.existsById(1L)).thenReturn(true);

        service.search(1L, "   ", "information", 10);

        verify(pointOfInterestRepository).search(1L, "%", "%", "information", Limit.of(10));
    }

    @Test
    void queryOverSixtyCharactersIsRejected() {
        when(buildingRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.search(1L, "x".repeat(61), null, 30))
                .isInstanceOfSatisfying(
                        FieldValidationException.class,
                        exception -> assertThat(exception.field()).isEqualTo("query"));
    }

    @Test
    void floorComesFromTheLinkedNode() {
        Floor floor = TestEntities.floor(10L, 0);
        Node node = TestEntities.node(42L, floor, NodeType.POINT_OF_INTEREST, null);
        when(buildingRepository.existsById(1L)).thenReturn(true);
        when(pointOfInterestRepository.search(any(), any(), any(), any(), any()))
                .thenReturn(List.of(TestEntities.pointOfInterest(5L, node, "Help desk", "information")));

        PointOfInterestResponse result = service.search(1L, "help", null, 30).get(0);

        assertThat(result.nodeId()).isEqualTo(42L);
        assertThat(result.floorId()).isEqualTo(10L);
        assertThat(result.floorDisplayName()).isEqualTo("Level 0");
    }

    @Test
    void unknownBuildingIsNotFoundRatherThanEmpty() {
        when(buildingRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.search(99L, null, null, 30)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.listCategories(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
