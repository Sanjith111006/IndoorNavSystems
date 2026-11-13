package com.indoornav.backend.service;

import static com.indoornav.backend.routing.TestSnapshots.CAFE;
import static com.indoornav.backend.routing.TestSnapshots.CORRIDOR_1_TO_4;
import static com.indoornav.backend.routing.TestSnapshots.CORRIDOR_2_TO_3;
import static com.indoornav.backend.routing.TestSnapshots.ELEVATOR_GROUND_TO_ONE;
import static com.indoornav.backend.routing.TestSnapshots.ENTRANCE;
import static com.indoornav.backend.routing.TestSnapshots.GROUND_STAIRS;
import static com.indoornav.backend.routing.TestSnapshots.ISOLATED;
import static com.indoornav.backend.routing.TestSnapshots.LOUNGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.indoornav.backend.dto.RouteResponse;
import com.indoornav.backend.dto.RouteResponse.NoRouteReason;
import com.indoornav.backend.dto.RouteResponse.WarningCode;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.backend.routing.RoutingSnapshot;
import com.indoornav.backend.routing.TestSnapshots;
import com.indoornav.engine.algorithm.AStarRouteFinder;
import com.indoornav.engine.model.RoutePreference;
import com.indoornav.engine.result.RouteStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoutePlannerTest {

    private final RoutePlanner planner = new RoutePlanner(new AStarRouteFinder(), new InstructionBuilder());

    @Test
    void shortestTakesTheStairsAndReportsTotals() {
        RouteResponse route = planner.plan(TestSnapshots.open(), ENTRANCE, LOUNGE, RoutePreference.SHORTEST);

        assertThat(route.status()).isEqualTo(RouteStatus.FOUND);
        assertThat(route.preference()).isEqualTo(RoutePreference.SHORTEST);
        assertThat(pathIds(route)).containsExactly(1L, 2L, 3L, 6L, 8L, 10L);
        assertThat(route.distanceMeters()).isEqualTo(45.0);
        assertThat(route.floorTransitions()).singleElement().satisfies(transition -> {
            assertThat(transition.connectionType()).isEqualTo("STAIRS");
            assertThat(transition.pathIndex()).isEqualTo(3);
        });
        assertThat(route.warnings()).isEmpty();
        assertThat(route.noRoute()).isNull();
    }

    @Test
    void fastestDiffersFromShortestByTakingTheElevator() {
        RouteResponse route = planner.plan(TestSnapshots.open(), ENTRANCE, LOUNGE, RoutePreference.FASTEST);

        assertThat(pathIds(route)).containsExactly(1L, 2L, 3L, 7L, 9L, 10L);
        assertThat(route.floorTransitions().getFirst().connectionType()).isEqualTo("ELEVATOR");
        // 32 m of corridor at 1.2 m/s, 5 m of elevator at 4 m/s plus 5 s, then 12 m of corridor.
        assertThat(route.estimatedDurationSeconds()).isEqualTo(Math.round(32 / 1.2 + 6.25 + 12 / 1.2));
    }

    @Test
    void accessibleAvoidsNodesAndEdgesThatAreNotStepFree() {
        RouteResponse route = planner.plan(TestSnapshots.open(), ENTRANCE, LOUNGE, RoutePreference.ACCESSIBLE);

        assertThat(pathIds(route)).doesNotContain(GROUND_STAIRS, 8L).contains(7L, 9L);
    }

    @Test
    void startEqualToDestinationIsAZeroLengthRouteWithOneArriveStep() {
        RouteResponse route = planner.plan(TestSnapshots.open(), CAFE, CAFE, RoutePreference.SHORTEST);

        assertThat(route.status()).isEqualTo(RouteStatus.FOUND);
        assertThat(pathIds(route)).containsExactly(CAFE);
        assertThat(route.distanceMeters()).isZero();
        assertThat(route.estimatedDurationSeconds()).isZero();
        assertThat(route.instructions()).singleElement()
                .satisfies(step -> assertThat(step.type()).isEqualTo(RouteResponse.InstructionType.ARRIVE));
    }

    @Test
    void blockedEdgeOnTheUsualRouteGivesADetourWarningWithExtraDistance() {
        RoutingSnapshot snapshot = TestSnapshots.withObstacles(
                List.of(TestSnapshots.onEdge(7L, CORRIDOR_2_TO_3, ObstacleType.BLOCKED_CORRIDOR)));

        RouteResponse route = planner.plan(snapshot, ENTRANCE, CAFE, RoutePreference.SHORTEST);

        assertThat(pathIds(route)).containsExactly(1L, 4L, 3L);
        assertThat(route.warnings()).singleElement().satisfies(warning -> {
            assertThat(warning.code()).isEqualTo(WarningCode.DETOUR);
            assertThat(warning.obstacleIds()).containsExactly(7L);
            assertThat(warning.extraDistanceMeters()).isCloseTo(10.0, within(1.0e-9));
            assertThat(warning.message()).contains("about 10 m");
        });
    }

    @Test
    void obstacleOffTheRouteGivesNoWarning() {
        RoutingSnapshot snapshot = TestSnapshots.withObstacles(
                List.of(TestSnapshots.onEdge(7L, CORRIDOR_1_TO_4, ObstacleType.BLOCKED_CORRIDOR)));

        RouteResponse route = planner.plan(snapshot, ENTRANCE, CAFE, RoutePreference.SHORTEST);

        assertThat(pathIds(route)).containsExactly(1L, 2L, 3L);
        assertThat(route.warnings()).isEmpty();
    }

    @Test
    void avoidStairsWarnsWhenStairsAreUnavoidable() {
        RoutingSnapshot snapshot = TestSnapshots.withObstacles(
                List.of(TestSnapshots.onEdge(9L, ELEVATOR_GROUND_TO_ONE, ObstacleType.BROKEN_ELEVATOR)));

        RouteResponse route = planner.plan(snapshot, ENTRANCE, LOUNGE, RoutePreference.AVOID_STAIRS);

        assertThat(route.floorTransitions().getFirst().connectionType()).isEqualTo("STAIRS");
        assertThat(route.warnings()).extracting(RouteResponse.Warning::code)
                .containsExactlyInAnyOrder(WarningCode.STAIRS_REQUIRED, WarningCode.DETOUR);
    }

    @Test
    void avoidStairsWithAStepFreeOptionNeitherUsesStairsNorWarns() {
        RouteResponse route = planner.plan(TestSnapshots.open(), ENTRANCE, LOUNGE, RoutePreference.AVOID_STAIRS);

        assertThat(route.floorTransitions().getFirst().connectionType()).isEqualTo("ELEVATOR");
        assertThat(route.warnings()).isEmpty();
    }

    @Test
    void brokenOnlyElevatorMeansNoAccessibleRouteBecauseOfTheObstacle() {
        RoutingSnapshot snapshot = TestSnapshots.withObstacles(
                List.of(TestSnapshots.onEdge(9L, ELEVATOR_GROUND_TO_ONE, ObstacleType.BROKEN_ELEVATOR)));

        RouteResponse route = planner.plan(snapshot, ENTRANCE, LOUNGE, RoutePreference.ACCESSIBLE);

        assertThat(route.status()).isEqualTo(RouteStatus.NO_ROUTE);
        assertThat(route.pathNodes()).isNull();
        assertThat(route.noRoute().reasonCode()).isEqualTo(NoRouteReason.OBSTACLE);
        assertThat(route.noRoute().obstacles()).singleElement().satisfies(obstacle -> {
            assertThat(obstacle.id()).isEqualTo(9L);
            assertThat(obstacle.type()).isEqualTo(ObstacleType.BROKEN_ELEVATOR);
        });
        // SHORTEST still succeeds through the stairs (PRD 8.3 scenario 4).
        assertThat(planner.plan(snapshot, ENTRANCE, LOUNGE, RoutePreference.SHORTEST).status())
                .isEqualTo(RouteStatus.FOUND);
    }

    @Test
    void blockedDestinationNodeIsAnObstacleReason() {
        RoutingSnapshot snapshot = TestSnapshots.withObstacles(List.of(TestSnapshots.onNode(4L, CAFE)));

        RouteResponse route = planner.plan(snapshot, ENTRANCE, CAFE, RoutePreference.SHORTEST);

        assertThat(route.noRoute().reasonCode()).isEqualTo(NoRouteReason.OBSTACLE);
        assertThat(route.noRoute().obstacles()).extracting(RouteResponse.ObstacleSummary::id).containsExactly(4L);
    }

    @Test
    void stepFreeRouteImpossibleWithoutObstaclesIsAnAccessibilityReason() {
        RouteResponse route = planner.plan(TestSnapshots.open(), GROUND_STAIRS, ENTRANCE, RoutePreference.ACCESSIBLE);

        assertThat(route.noRoute().reasonCode()).isEqualTo(NoRouteReason.ACCESSIBILITY_CONSTRAINT);
        assertThat(route.noRoute().obstacles()).isEmpty();
    }

    @Test
    void unreachableDestinationIsDisconnected() {
        RouteResponse route = planner.plan(TestSnapshots.open(), ENTRANCE, ISOLATED, RoutePreference.SHORTEST);

        assertThat(route.noRoute().reasonCode()).isEqualTo(NoRouteReason.DISCONNECTED);
    }

    private static List<Long> pathIds(RouteResponse route) {
        return route.pathNodes().stream().map(RouteResponse.PathNode::nodeId).toList();
    }
}
