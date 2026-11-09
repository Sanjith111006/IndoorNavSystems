package com.indoornav.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indoornav.backend.TestEntities;
import com.indoornav.backend.dto.ObstacleCreateRequest;
import com.indoornav.backend.dto.ObstacleResponse;
import com.indoornav.backend.dto.ObstacleTargetType;
import com.indoornav.backend.dto.ObstacleUpdateRequest;
import com.indoornav.backend.entity.Edge;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.entity.Obstacle;
import com.indoornav.backend.entity.ObstacleStatus;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.backend.exception.ConflictException;
import com.indoornav.backend.exception.FieldValidationException;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.repository.BuildingRepository;
import com.indoornav.backend.repository.EdgeRepository;
import com.indoornav.backend.repository.NodeRepository;
import com.indoornav.backend.repository.ObstacleRepository;
import com.indoornav.backend.repository.PointOfInterestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObstacleServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    @Mock
    private ObstacleRepository obstacleRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private EdgeRepository edgeRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private PointOfInterestRepository pointOfInterestRepository;

    private ObstacleService service;
    private Floor lowerFloor;
    private Floor upperFloor;
    private Node lobby;
    private Node hallway;
    private Edge corridor;

    @BeforeEach
    void setUp() {
        service = new ObstacleService(
                obstacleRepository,
                buildingRepository,
                edgeRepository,
                nodeRepository,
                pointOfInterestRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        lowerFloor = TestEntities.floor(10L, 0);
        upperFloor = TestEntities.floor(11L, 1);
        lobby = TestEntities.node(1L, lowerFloor, NodeType.BUILDING_ENTRANCE, "Lobby");
        hallway = TestEntities.node(2L, lowerFloor, NodeType.CORRIDOR_JUNCTION, null);
        corridor = TestEntities.edge(7L, lobby, hallway, "CORRIDOR");
        lenient().when(obstacleRepository.saveAndFlush(any(Obstacle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(pointOfInterestRepository.findByNodeIds(any())).thenReturn(List.of());
    }

    @Test
    void createDerivesTheBuildingAndStartsActive() {
        when(edgeRepository.findWithNodesById(7L)).thenReturn(Optional.of(corridor));

        ObstacleResponse response = service.create(
                new ObstacleCreateRequest(ObstacleTargetType.EDGE, 7L, ObstacleType.BLOCKED_CORRIDOR, "Closed"));

        assertThat(response.buildingId()).isEqualTo(TestEntities.BUILDING_ID);
        assertThat(response.status()).isEqualTo(ObstacleStatus.ACTIVE);
        assertThat(response.targetType()).isEqualTo(ObstacleTargetType.EDGE);
        assertThat(response.edgeId()).isEqualTo(7L);
        assertThat(response.nodeId()).isNull();
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void summaryLabelsNodesFromDisplayNameThenPoiThenId() {
        when(edgeRepository.findWithNodesById(7L)).thenReturn(Optional.of(corridor));

        ObstacleResponse withoutPoi = service.create(
                new ObstacleCreateRequest(ObstacleTargetType.EDGE, 7L, ObstacleType.BLOCKED_CORRIDOR, "Closed"));
        assertThat(withoutPoi.targetSummary()).isEqualTo("Lobby to Node 2");

        when(pointOfInterestRepository.findByNodeIds(any()))
                .thenReturn(List.of(TestEntities.pointOfInterest(3L, hallway, "Help desk", "information")));
        ObstacleResponse withPoi = service.create(
                new ObstacleCreateRequest(ObstacleTargetType.EDGE, 7L, ObstacleType.BLOCKED_CORRIDOR, "Closed"));
        assertThat(withPoi.targetSummary()).isEqualTo("Lobby to Help desk");
    }

    @Test
    void verticalConnectionReportsBothFloors() {
        Node upper = TestEntities.node(3L, upperFloor, NodeType.ELEVATOR_ENTRANCE, "Upper lift");
        Edge elevator = TestEntities.edge(8L, lobby, upper, "ELEVATOR");
        when(edgeRepository.findWithNodesById(8L)).thenReturn(Optional.of(elevator));

        ObstacleResponse response = service.create(
                new ObstacleCreateRequest(ObstacleTargetType.EDGE, 8L, ObstacleType.BROKEN_ELEVATOR, "Out of service"));

        assertThat(response.floorIds()).containsExactly(10L, 11L);
    }

    @Test
    void createOnNodeSetsNodeTarget() {
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(lobby));

        ObstacleResponse response = service.create(
                new ObstacleCreateRequest(ObstacleTargetType.NODE, 1L, ObstacleType.CONSTRUCTION_AREA, "Works"));

        assertThat(response.targetType()).isEqualTo(ObstacleTargetType.NODE);
        assertThat(response.nodeId()).isEqualTo(1L);
        assertThat(response.edgeId()).isNull();
        assertThat(response.floorIds()).containsExactly(10L);
    }

    @Test
    void createWithUnknownTargetIsNotFound() {
        when(edgeRepository.findWithNodesById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                        new ObstacleCreateRequest(ObstacleTargetType.EDGE, 99L, ObstacleType.BLOCKED_CORRIDOR, "Closed")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(obstacleRepository, never()).saveAndFlush(any());
    }

    @Test
    void secondActiveObstacleOnSameTargetConflicts() {
        when(edgeRepository.findWithNodesById(7L)).thenReturn(Optional.of(corridor));
        when(obstacleRepository.existsByEdge_IdAndStatus(7L, ObstacleStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                        new ObstacleCreateRequest(ObstacleTargetType.EDGE, 7L, ObstacleType.BLOCKED_CORRIDOR, "Closed")))
                .isInstanceOf(ConflictException.class);
        verify(obstacleRepository, never()).saveAndFlush(any());
    }

    @Test
    void brokenElevatorIsRejectedOnACorridor() {
        when(edgeRepository.findWithNodesById(7L)).thenReturn(Optional.of(corridor));

        assertThatThrownBy(() -> service.create(
                        new ObstacleCreateRequest(ObstacleTargetType.EDGE, 7L, ObstacleType.BROKEN_ELEVATOR, "Broken")))
                .isInstanceOfSatisfying(
                        FieldValidationException.class,
                        exception -> assertThat(exception.field()).isEqualTo("obstacleType"));
    }

    @Test
    void closedStaircaseIsRejectedOnANonStairNode() {
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> service.create(
                        new ObstacleCreateRequest(ObstacleTargetType.NODE, 1L, ObstacleType.CLOSED_STAIRCASE, "Closed")))
                .isInstanceOf(FieldValidationException.class);
    }

    @Test
    void closedStaircaseIsAcceptedOnAStairLanding() {
        Node landing = TestEntities.node(4L, lowerFloor, NodeType.STAIR_LANDING, "Stairs A");
        when(nodeRepository.findById(4L)).thenReturn(Optional.of(landing));

        ObstacleResponse response = service.create(
                new ObstacleCreateRequest(ObstacleTargetType.NODE, 4L, ObstacleType.CLOSED_STAIRCASE, "Closed"));

        assertThat(response.obstacleType()).isEqualTo(ObstacleType.CLOSED_STAIRCASE);
    }

    @Test
    void updateChangesFieldsAndTimestamp() {
        Obstacle obstacle = existingObstacle(ObstacleStatus.ACTIVE, 2L);
        when(obstacleRepository.findWithTargetById(4L)).thenReturn(Optional.of(obstacle));

        ObstacleResponse response = service.update(4L, new ObstacleUpdateRequest(
                ObstacleType.CONSTRUCTION_AREA, "Reopened", ObstacleStatus.RESOLVED, 2L));

        assertThat(response.status()).isEqualTo(ObstacleStatus.RESOLVED);
        assertThat(response.description()).isEqualTo("Reopened");
        assertThat(response.updatedAt()).isEqualTo(NOW);
        verify(obstacleRepository).saveAndFlush(obstacle);
    }

    @Test
    void updateWithStaleVersionConflictsAndChangesNothing() {
        Obstacle obstacle = existingObstacle(ObstacleStatus.ACTIVE, 5L);
        when(obstacleRepository.findWithTargetById(4L)).thenReturn(Optional.of(obstacle));

        assertThatThrownBy(() -> service.update(4L, new ObstacleUpdateRequest(
                        ObstacleType.CONSTRUCTION_AREA, "Changed", ObstacleStatus.RESOLVED, 4L)))
                .isInstanceOf(ConflictException.class);

        assertThat(obstacle.getDescription()).isEqualTo("Closed");
        verify(obstacleRepository, never()).saveAndFlush(any());
    }

    @Test
    void reactivatingWhileAnotherIsActiveConflicts() {
        Obstacle obstacle = existingObstacle(ObstacleStatus.RESOLVED, 1L);
        when(obstacleRepository.findWithTargetById(4L)).thenReturn(Optional.of(obstacle));
        when(obstacleRepository.existsByEdge_IdAndStatus(7L, ObstacleStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.update(4L, new ObstacleUpdateRequest(
                        ObstacleType.BLOCKED_CORRIDOR, "Closed again", ObstacleStatus.ACTIVE, 1L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updatingAnActiveObstacleDoesNotConflictWithItself() {
        Obstacle obstacle = existingObstacle(ObstacleStatus.ACTIVE, 1L);
        when(obstacleRepository.findWithTargetById(4L)).thenReturn(Optional.of(obstacle));

        ObstacleResponse response = service.update(4L, new ObstacleUpdateRequest(
                ObstacleType.BLOCKED_CORRIDOR, "New wording", ObstacleStatus.ACTIVE, 1L));

        assertThat(response.description()).isEqualTo("New wording");
    }

    @Test
    void updateOfUnknownObstacleIsNotFound() {
        when(obstacleRepository.findWithTargetById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new ObstacleUpdateRequest(
                        ObstacleType.BLOCKED_CORRIDOR, "Closed", ObstacleStatus.ACTIVE, 0L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteOfUnknownObstacleIsNotFound() {
        when(obstacleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listOfUnknownBuildingIsNotFound() {
        when(buildingRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.list(99L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listWithNoStatusReturnsAllAndSkipsPoiLookupWhenEmpty() {
        when(buildingRepository.existsById(1L)).thenReturn(true);
        when(obstacleRepository.findByBuildingIdOrderByUpdatedAtDescIdDesc(1L)).thenReturn(List.of());

        assertThat(service.list(1L, null)).isEmpty();
        verify(pointOfInterestRepository, never()).findByNodeIds(any());
    }

    private Obstacle existingObstacle(ObstacleStatus status, long version) {
        Obstacle obstacle = Obstacle.onEdge(corridor, ObstacleType.BLOCKED_CORRIDOR, "Closed", NOW.minusSeconds(60));
        TestEntities.set(obstacle, "id", 4L);
        TestEntities.set(obstacle, "status", status);
        TestEntities.set(obstacle, "version", version);
        return obstacle;
    }
}
