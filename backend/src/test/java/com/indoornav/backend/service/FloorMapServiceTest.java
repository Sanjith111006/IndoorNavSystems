package com.indoornav.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.indoornav.backend.TestEntities;
import com.indoornav.backend.dto.FloorMapResponse;
import com.indoornav.backend.entity.Edge;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.entity.Obstacle;
import com.indoornav.backend.entity.ObstacleStatus;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.repository.EdgeRepository;
import com.indoornav.backend.repository.FloorRepository;
import com.indoornav.backend.repository.NodeRepository;
import com.indoornav.backend.repository.ObstacleRepository;
import com.indoornav.backend.repository.PointOfInterestRepository;
import com.indoornav.backend.repository.RoomRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FloorMapServiceTest {

    private static final long GROUND_ID = 10L;
    private static final long UPPER_ID = 11L;
    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private EdgeRepository edgeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PointOfInterestRepository pointOfInterestRepository;

    @Mock
    private ObstacleRepository obstacleRepository;

    private FloorMapService service;
    private final Floor ground = TestEntities.floor(GROUND_ID, 0);
    private final Floor upper = TestEntities.floor(UPPER_ID, 1);
    private final Node a = TestEntities.node(1L, ground, NodeType.CORRIDOR_JUNCTION, null);
    private final Node b = TestEntities.node(2L, ground, NodeType.ELEVATOR_ENTRANCE, "Lift");
    private final Node c = TestEntities.node(3L, upper, NodeType.ELEVATOR_ENTRANCE, "Upper lift");
    private final Edge corridor = TestEntities.edge(7L, a, b, "CORRIDOR");
    private final Edge elevator = TestEntities.edge(8L, c, b, "ELEVATOR");

    @BeforeEach
    void setUp() {
        service = new FloorMapService(
                floorRepository,
                nodeRepository,
                edgeRepository,
                roomRepository,
                pointOfInterestRepository,
                obstacleRepository);
    }

    @Test
    void marksBlockedEdgesAndNodesFromActiveObstacles() {
        stubFloor(List.of(
                Obstacle.onEdge(corridor, ObstacleType.BLOCKED_CORRIDOR, "Cleaning", NOW),
                Obstacle.onNode(b, ObstacleType.CONSTRUCTION_AREA, "Works", NOW)));

        FloorMapResponse map = service.getFloorMap(GROUND_ID);

        assertThat(map.edges().get(0).blocked()).isTrue();
        assertThat(map.edges().get(0).obstacle().description()).isEqualTo("Cleaning");
        assertThat(map.edges().get(1).blocked()).isFalse();
        assertThat(map.edges().get(1).obstacle()).isNull();
        assertThat(map.nodes().get(0).blocked()).isFalse();
        assertThat(map.nodes().get(1).blocked()).isTrue();
        assertThat(map.nodes().get(1).obstacle().type()).isEqualTo(ObstacleType.CONSTRUCTION_AREA);
    }

    @Test
    void reportsTheOtherFloorOfAVerticalConnectionInEitherDirection() {
        stubFloor(List.of());

        FloorMapResponse map = service.getFloorMap(GROUND_ID);

        assertThat(map.edges().get(0).otherFloorId()).isNull();
        assertThat(map.edges().get(1).otherFloorId()).isEqualTo(UPPER_ID);
    }

    @Test
    void buildsThePlanUrlUnderTheAssetRoute() {
        stubFloor(List.of());

        FloorMapResponse map = service.getFloorMap(GROUND_ID);

        assertThat(map.plan().url()).isEqualTo("/assets/floorplans/test/level-0.svg");
        assertThat(map.floor().displayName()).isEqualTo("Level 0");
    }

    @Test
    void unknownFloorIsNotFound() {
        when(floorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFloorMap(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubFloor(List<Obstacle> activeObstacles) {
        when(floorRepository.findById(GROUND_ID)).thenReturn(Optional.of(ground));
        when(obstacleRepository.findByBuildingIdAndStatus(TestEntities.BUILDING_ID, ObstacleStatus.ACTIVE))
                .thenReturn(activeObstacles);
        when(nodeRepository.findByFloor_IdOrderByIdAsc(GROUND_ID)).thenReturn(List.of(a, b));
        when(edgeRepository.findTouchingFloor(GROUND_ID)).thenReturn(List.of(corridor, elevator));
        when(roomRepository.findByFloorIdOrderByIdAsc(GROUND_ID)).thenReturn(List.of());
        when(pointOfInterestRepository.findOnFloor(GROUND_ID)).thenReturn(List.of());
    }
}
