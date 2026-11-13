package com.indoornav.backend.service;

import com.indoornav.backend.dto.RouteResponse.Instruction;
import com.indoornav.backend.dto.RouteResponse.InstructionType;
import com.indoornav.backend.dto.RouteResponse.Turn;
import com.indoornav.backend.routing.RoutingSnapshot;
import com.indoornav.backend.routing.RoutingSnapshot.RoutingNode;
import com.indoornav.engine.model.NavigationEdge;
import com.indoornav.engine.result.RouteResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Turns a found path into ordered steps with structured fields and default English text
 * (ARCHITECTURE.md 6.5, AD-10).
 *
 * <p>Same-floor edges join one walk step until the path bends by more than
 * {@link #TURN_THRESHOLD_DEGREES}; the next walk step then starts with that turn (PRD FR-28).
 * Consecutive vertical edges of the same connection type become one floor change, so riding an
 * elevator up two floors is one step.
 */
@Component
public class InstructionBuilder {

    /** Bends below this read as "keep going"; corridors are rarely drawn perfectly straight. */
    static final double TURN_THRESHOLD_DEGREES = 30.0;

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
        Turn turn = startsAfterSameFloorEdge(snapshot, nodeIds, firstEdge) ? turnAt(snapshot, nodeIds, firstEdge) : null;
        int nextEdge = firstEdge;
        double distance = 0.0;
        do {
            distance += edge(snapshot, route, nextEdge).distanceMetres();
            nextEdge++;
        } while (nextEdge < route.edgeIds().size()
                && !isVertical(snapshot, nodeIds, nextEdge)
                && turnAt(snapshot, nodeIds, nextEdge) == null);

        steps.add(new Instruction(
                InstructionType.WALK,
                walkText(turn, distance, snapshot.node(nodeIds.get(nextEdge)).label()),
                RouteFormatting.oneDecimal(distance),
                floorOf(snapshot, nodeIds, firstEdge),
                null,
                null,
                turn,
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
                null,
                firstEdge,
                nextEdge));
        return nextEdge;
    }

    private static String walkText(Turn turn, double distance, String label) {
        String walk = "walk " + RouteFormatting.wholeMetres(distance) + (label == null ? "." : " to " + label + ".");
        if (turn == null) {
            return Character.toUpperCase(walk.charAt(0)) + walk.substring(1);
        }
        return "Turn " + turn.name().toLowerCase(Locale.ROOT) + " and " + walk;
    }

    /**
     * The turn made at node {@code nodeIndex}, or null when the path keeps going within the
     * threshold. Plan coordinates have y pointing down, so a positive cross product is a
     * clockwise turn on the map, which is a right turn. An edge with no horizontal length (two
     * nodes at the same point) has no direction and never counts as a turn.
     */
    static Turn turnAt(RoutingSnapshot snapshot, List<Long> nodeIds, int nodeIndex) {
        RoutingNode previous = snapshot.node(nodeIds.get(nodeIndex - 1));
        RoutingNode current = snapshot.node(nodeIds.get(nodeIndex));
        RoutingNode next = snapshot.node(nodeIds.get(nodeIndex + 1));
        double inX = current.x().doubleValue() - previous.x().doubleValue();
        double inY = current.y().doubleValue() - previous.y().doubleValue();
        double outX = next.x().doubleValue() - current.x().doubleValue();
        double outY = next.y().doubleValue() - current.y().doubleValue();
        if ((inX == 0.0 && inY == 0.0) || (outX == 0.0 && outY == 0.0)) {
            return null;
        }
        double cross = inX * outY - inY * outX;
        double dot = inX * outX + inY * outY;
        double bendDegrees = Math.toDegrees(Math.atan2(cross, dot));
        if (Math.abs(bendDegrees) < TURN_THRESHOLD_DEGREES) {
            return null;
        }
        return bendDegrees > 0 ? Turn.RIGHT : Turn.LEFT;
    }

    /** A turn needs a known heading, which a walk has only if the edge before it was on this floor. */
    private static boolean startsAfterSameFloorEdge(RoutingSnapshot snapshot, List<Long> nodeIds, int edgeIndex) {
        return edgeIndex > 0 && !isVertical(snapshot, nodeIds, edgeIndex - 1);
    }

    /** The start is chosen by the visitor, so the text never claims to know their position. */
    private static Instruction start(RoutingSnapshot snapshot, List<Long> nodeIds) {
        String label = snapshot.node(nodeIds.getFirst()).label();
        String text = label == null ? "Start at your chosen starting point." : "Start at " + label + ".";
        return new Instruction(InstructionType.START, text, null, floorOf(snapshot, nodeIds, 0), null, null, null, 0, 0);
    }

    private static Instruction arrive(RoutingSnapshot snapshot, List<Long> nodeIds, int lastIndex) {
        String label = snapshot.node(nodeIds.get(lastIndex)).label();
        String text = label == null ? "Arrive at your destination." : "Arrive at " + label + ".";
        return new Instruction(
                InstructionType.ARRIVE, text, null, floorOf(snapshot, nodeIds, lastIndex), null, null, null,
                lastIndex, lastIndex);
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
