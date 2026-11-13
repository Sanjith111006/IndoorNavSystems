package com.indoornav.backend.service;

import com.indoornav.backend.dto.RouteResponse.Instruction;
import com.indoornav.backend.dto.RouteResponse.InstructionType;
import com.indoornav.backend.routing.RoutingSnapshot;
import com.indoornav.engine.model.NavigationEdge;
import com.indoornav.engine.result.RouteResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Turns a found path into ordered steps with structured fields and default English text
 * (ARCHITECTURE.md 6.5, AD-10).
 *
 * <p>Consecutive same-floor edges become one walk step. Consecutive vertical edges of the same
 * connection type become one floor change, so riding an elevator up two floors is one step. Left
 * and right turn wording (PRD FR-28, Should) is not produced yet.
 */
@Component
public class InstructionBuilder {

    public List<Instruction> build(RoutingSnapshot snapshot, RouteResult route) {
        List<Long> nodeIds = route.nodeIds();
        int lastIndex = nodeIds.size() - 1;
        if (route.edgeIds().isEmpty()) {
            return List.of(arrive(snapshot, nodeIds, lastIndex));
        }

        List<Instruction> steps = new ArrayList<>();
        steps.add(start(snapshot, nodeIds));
        int edgeIndex = 0;
        while (edgeIndex < route.edgeIds().size()) {
            edgeIndex = isVertical(snapshot, nodeIds, edgeIndex)
                    ? addFloorChange(snapshot, route, edgeIndex, steps)
                    : addWalk(snapshot, route, edgeIndex, steps);
        }
        steps.add(arrive(snapshot, nodeIds, lastIndex));
        return steps;
    }

    /** Returns the index of the first edge after the walk. */
    private int addWalk(RoutingSnapshot snapshot, RouteResult route, int firstEdge, List<Instruction> steps) {
        List<Long> nodeIds = route.nodeIds();
        int nextEdge = firstEdge;
        double distance = 0.0;
        while (nextEdge < route.edgeIds().size() && !isVertical(snapshot, nodeIds, nextEdge)) {
            distance += edge(snapshot, route, nextEdge).distanceMetres();
            nextEdge++;
        }
        String label = snapshot.node(nodeIds.get(nextEdge)).label();
        String text = "Walk " + RouteFormatting.wholeMetres(distance) + (label == null ? "." : " to " + label + ".");
        steps.add(new Instruction(
                InstructionType.WALK,
                text,
                RouteFormatting.oneDecimal(distance),
                floorOf(snapshot, nodeIds, firstEdge),
                null,
                null,
                firstEdge,
                nextEdge));
        return nextEdge;
    }

    /** Returns the index of the first edge after the floor change. */
    private int addFloorChange(RoutingSnapshot snapshot, RouteResult route, int firstEdge, List<Instruction> steps) {
        List<Long> nodeIds = route.nodeIds();
        String connectionCode = edge(snapshot, route, firstEdge).connectionType().code();
        int nextEdge = firstEdge;
        while (nextEdge < route.edgeIds().size()
                && isVertical(snapshot, nodeIds, nextEdge)
                && edge(snapshot, route, nextEdge).connectionType().code().equals(connectionCode)) {
            nextEdge++;
        }
        long toFloorId = floorOf(snapshot, nodeIds, nextEdge);
        String text = "Take the " + snapshot.connectionName(connectionCode) + " to " + snapshot.floorName(toFloorId) + ".";
        steps.add(new Instruction(
                InstructionType.FLOOR_CHANGE,
                text,
                null,
                floorOf(snapshot, nodeIds, firstEdge),
                toFloorId,
                connectionCode,
                firstEdge,
                nextEdge));
        return nextEdge;
    }

    /** The start is chosen by the visitor, so the text never claims to know their position. */
    private static Instruction start(RoutingSnapshot snapshot, List<Long> nodeIds) {
        String label = snapshot.node(nodeIds.getFirst()).label();
        String text = label == null ? "Start at your chosen starting point." : "Start at " + label + ".";
        return new Instruction(InstructionType.START, text, null, floorOf(snapshot, nodeIds, 0), null, null, 0, 0);
    }

    private static Instruction arrive(RoutingSnapshot snapshot, List<Long> nodeIds, int lastIndex) {
        String label = snapshot.node(nodeIds.get(lastIndex)).label();
        String text = label == null ? "Arrive at your destination." : "Arrive at " + label + ".";
        return new Instruction(
                InstructionType.ARRIVE, text, null, floorOf(snapshot, nodeIds, lastIndex), null, null, lastIndex, lastIndex);
    }

    private static boolean isVertical(RoutingSnapshot snapshot, List<Long> nodeIds, int edgeIndex) {
        return floorOf(snapshot, nodeIds, edgeIndex) != floorOf(snapshot, nodeIds, edgeIndex + 1);
    }

    private static long floorOf(RoutingSnapshot snapshot, List<Long> nodeIds, int nodeIndex) {
        return snapshot.node(nodeIds.get(nodeIndex)).floorId();
    }

    private static NavigationEdge edge(RoutingSnapshot snapshot, RouteResult route, int edgeIndex) {
        return snapshot.graph().edge(route.edgeIds().get(edgeIndex));
    }
}
