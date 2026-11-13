package com.indoornav.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.indoornav.backend.TestEntities;
import com.indoornav.backend.dto.RouteRequest;
import com.indoornav.backend.dto.RouteResponse;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.exception.NodesNotInSameBuildingException;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.exception.RouteTimeoutException;
import com.indoornav.backend.repository.NodeRepository;
import com.indoornav.backend.routing.RoutingGraphLoader;
import com.indoornav.backend.routing.RoutingSnapshot;
import com.indoornav.backend.routing.TestSnapshots;
import com.indoornav.engine.model.RoutePreference;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RouteServiceTest {

    private final NodeRepository nodeRepository = mock(NodeRepository.class);
    private final RoutingGraphLoader graphLoader = mock(RoutingGraphLoader.class);
    private final RoutePlanner planner = mock(RoutePlanner.class);
    private final RoutingSnapshot snapshot = TestSnapshots.open();
    private RouteService service;

    @BeforeEach
    void setUp() {
        service = new RouteService(nodeRepository, graphLoader, planner, Duration.ofMillis(200));
        Floor floor = TestEntities.floor(10L, 0);
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(TestEntities.node(1L, floor, NodeType.BUILDING_ENTRANCE, "A")));
        when(nodeRepository.findById(2L)).thenReturn(Optional.of(TestEntities.node(2L, floor, NodeType.ROOM_ENTRANCE, "B")));
        when(graphLoader.load(TestEntities.BUILDING_ID)).thenReturn(snapshot);
    }

    @AfterEach
    void tearDown() {
        service.destroy();
    }

    @Test
    void plansAgainstAFreshSnapshotOfTheNodesBuilding() {
        RouteResponse expected = RouteResponse.noRoute(RoutePreference.FASTEST, null);
        when(planner.plan(snapshot, 1L, 2L, RoutePreference.FASTEST)).thenReturn(expected);

        assertThat(service.route(new RouteRequest(1L, 2L, RoutePreference.FASTEST))).isSameAs(expected);
    }

    @Test
    void missingPreferenceMeansShortest() {
        RouteResponse expected = RouteResponse.noRoute(RoutePreference.SHORTEST, null);
        when(planner.plan(snapshot, 1L, 2L, RoutePreference.SHORTEST)).thenReturn(expected);

        assertThat(service.route(new RouteRequest(1L, 2L, null))).isSameAs(expected);
    }

    @Test
    void unknownNodeIsNotFoundBeforeAnyGraphIsLoaded() {
        when(nodeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.route(new RouteRequest(1L, 99L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(graphLoader, planner);
    }

    @Test
    void nodesInDifferentBuildingsAreRejected() {
        Node elsewhere = TestEntities.node(3L, TestEntities.floor(20L, 0), NodeType.ROOM_ENTRANCE, "C");
        TestEntities.set(elsewhere, "buildingId", 2L);
        when(nodeRepository.findById(3L)).thenReturn(Optional.of(elsewhere));

        assertThatThrownBy(() -> service.route(new RouteRequest(1L, 3L, null)))
                .isInstanceOf(NodesNotInSameBuildingException.class);
        verifyNoInteractions(graphLoader, planner);
    }

    @Test
    void calculationPastTheTimeLimitIsAbandoned() {
        CountDownLatch neverReleasedDuringTheCall = new CountDownLatch(1);
        when(planner.plan(any(), anyLong(), anyLong(), any())).thenAnswer(invocation -> {
            neverReleasedDuringTheCall.await();
            return null;
        });
        try {
            assertThatThrownBy(() -> service.route(new RouteRequest(1L, 2L, null)))
                    .isInstanceOf(RouteTimeoutException.class);
        } finally {
            neverReleasedDuringTheCall.countDown();
        }
    }

    @Test
    void failureInsideThePlannerIsRethrownUnchanged() {
        IllegalArgumentException failure = new IllegalArgumentException("bad graph data");
        when(planner.plan(any(), anyLong(), anyLong(), any())).thenThrow(failure);

        assertThatThrownBy(() -> service.route(new RouteRequest(1L, 2L, null))).isSameAs(failure);
    }
}
