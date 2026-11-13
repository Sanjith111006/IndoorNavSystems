package com.indoornav.backend.service;

import static com.indoornav.backend.routing.TestSnapshots.CAFE;
import static com.indoornav.backend.routing.TestSnapshots.ENTRANCE;
import static com.indoornav.backend.routing.TestSnapshots.GROUND;
import static com.indoornav.backend.routing.TestSnapshots.LEVEL_ONE;
import static com.indoornav.backend.routing.TestSnapshots.LEVEL_TWO;
import static com.indoornav.backend.routing.TestSnapshots.LEVEL_TWO_LIFT;
import static com.indoornav.backend.routing.TestSnapshots.LOUNGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.indoornav.backend.dto.RouteResponse.Instruction;
import com.indoornav.backend.dto.RouteResponse.InstructionType;
import com.indoornav.backend.dto.RouteResponse.Turn;
import com.indoornav.backend.routing.RoutingSnapshot;
import com.indoornav.backend.routing.TestSnapshots;
import com.indoornav.engine.algorithm.AStarRouteFinder;
import com.indoornav.engine.model.RoutePreference;
import com.indoornav.engine.model.RouteRequest;
import com.indoornav.engine.result.RouteResult;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InstructionBuilderTest {

    private final InstructionBuilder builder = new InstructionBuilder();

    @Test
    void groupsWalkingAndNamesTheFloorChange() {
        List<Instruction> steps = stepsFor(ENTRANCE, LOUNGE, RoutePreference.FASTEST);

        assertThat(steps).extracting(Instruction::type).containsExactly(
                InstructionType.START,
                InstructionType.WALK,
                InstructionType.FLOOR_CHANGE,
                InstructionType.WALK,
                InstructionType.ARRIVE);
        assertThat(steps).extracting(Instruction::text).containsExactly(
                "Start at Entrance.",
                "Walk 32 m to Ground lift.",
                "Take the elevator to Level 1.",
                "Walk 12 m to Lounge.",
                "Arrive at Lounge.");
    }

    @Test
    void stepsCarryStructuredFieldsAndPathRanges() {
        List<Instruction> steps = stepsFor(ENTRANCE, LOUNGE, RoutePreference.FASTEST);

        Instruction walk = steps.get(1);
        assertThat(walk.distanceMeters()).isEqualTo(32.0);
        assertThat(walk.floorId()).isEqualTo(GROUND);
        assertThat(walk.toFloorId()).isNull();
        assertThat(walk.startPathIndex()).isZero();
        assertThat(walk.endPathIndex()).isEqualTo(3);

        Instruction floorChange = steps.get(2);
        assertThat(floorChange.floorId()).isEqualTo(GROUND);
        assertThat(floorChange.toFloorId()).isEqualTo(LEVEL_ONE);
        assertThat(floorChange.connectionType()).isEqualTo("ELEVATOR");
        assertThat(floorChange.distanceMeters()).isNull();
        assertThat(floorChange.startPathIndex()).isEqualTo(3);
        assertThat(floorChange.endPathIndex()).isEqualTo(4);

        Instruction arrive = steps.getLast();
        assertThat(arrive.startPathIndex()).isEqualTo(5).isEqualTo(arrive.endPathIndex());
    }

    @Test
    void oneElevatorRideAcrossTwoFloorsIsOneStep() {
        List<Instruction> steps = stepsFor(ENTRANCE, LEVEL_TWO_LIFT, RoutePreference.FASTEST);

        assertThat(steps).filteredOn(step -> step.type() == InstructionType.FLOOR_CHANGE).singleElement()
                .satisfies(step -> {
                    assertThat(step.toFloorId()).isEqualTo(LEVEL_TWO);
                    assertThat(step.text()).isEqualTo("Take the elevator to Level 2.");
                });
    }

    @Test
    void stairsAreNamedFromReferenceData() {
        List<Instruction> steps = stepsFor(ENTRANCE, LOUNGE, RoutePreference.SHORTEST);

        assertThat(steps.get(2).text()).isEqualTo("Take the stairs to Level 1.");
    }

    @Test
    void textNeverClaimsToKnowTheVisitorsPosition() {
        List<Instruction> steps = stepsFor(2L, 4L, RoutePreference.SHORTEST);

        assertThat(steps.getFirst().text()).isEqualTo("Start at your chosen starting point.");
        assertThat(steps.getLast().text()).isEqualTo("Arrive at your destination.");
        assertThat(steps).extracting(Instruction::text)
                .noneMatch(text -> text.contains("You are here") || text.contains("live"));
    }

    @Test
    void bendOverTheThresholdStartsAStepWithALeftTurn() {
        // 1 (0,0) -> 4 (10,10) heads down-right on the plan; 4 -> 3 (20,0) heads up-right: a left turn.
        List<Instruction> steps = stepsAvoidingCorridor(ENTRANCE, CAFE);

        assertThat(steps).extracting(Instruction::text).containsExactly(
                "Start at Entrance.", "Walk 15 m.", "Turn left and walk 15 m to Cafe.", "Arrive at Cafe.");
        assertThat(steps.get(1).turn()).isNull();
        assertThat(steps.get(2).turn()).isEqualTo(Turn.LEFT);
        assertThat(steps.get(2).startPathIndex()).isEqualTo(1);
    }

    @Test
    void theSameCornerWalkedTheOtherWayIsARightTurn() {
        List<Instruction> steps = stepsAvoidingCorridor(CAFE, ENTRANCE);

        assertThat(steps.get(2).turn()).isEqualTo(Turn.RIGHT);
        assertThat(steps.get(2).text()).isEqualTo("Turn right and walk 15 m to Entrance.");
    }

    @Test
    void gentleBendBelowTheThresholdStaysOneStep() {
        // 2 (10,0) -> 3 (20,0) -> 7 (30,5) bends by about 27 degrees.
        List<Instruction> steps = stepsFor(ENTRANCE, LOUNGE, RoutePreference.FASTEST);

        assertThat(steps).filteredOn(step -> step.type() == InstructionType.WALK)
                .extracting(Instruction::turn).containsOnlyNulls();
    }

    @Test
    void walkAfterAFloorChangeHasNoTurnBecauseItsHeadingIsUnknown() {
        List<Instruction> steps = stepsFor(ENTRANCE, LOUNGE, RoutePreference.FASTEST);

        assertThat(steps.get(3).type()).isEqualTo(InstructionType.WALK);
        assertThat(steps.get(3).turn()).isNull();
    }

    private static List<Instruction> stepsFor(long start, long destination, RoutePreference preference) {
        return stepsFor(RouteRequest.of(start, destination, preference));
    }

    /** Blocks the direct corridor 2-3, so the route bends through node 4. */
    private static List<Instruction> stepsAvoidingCorridor(long start, long destination) {
        return stepsFor(new RouteRequest(
                start, destination, RoutePreference.SHORTEST, Set.of(), Set.of(TestSnapshots.CORRIDOR_2_TO_3)));
    }

    private static List<Instruction> stepsFor(RouteRequest request) {
        RoutingSnapshot snapshot = TestSnapshots.open();
        RouteResult route = new AStarRouteFinder().findRoute(snapshot.graph(), request, snapshot.costs());
        return new InstructionBuilder().build(snapshot, route);
    }
}
