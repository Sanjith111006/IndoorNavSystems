package com.indoornav.backend.service;

import com.indoornav.backend.dto.RouteResponse;
import com.indoornav.backend.dto.RouteResponse.NoRouteReason;
import com.indoornav.backend.dto.RouteResponse.WarningCode;
import com.indoornav.backend.routing.RoutingSnapshot;
import com.indoornav.backend.routing.RoutingSnapshot.ActiveObstacle;
import com.indoornav.engine.algorithm.RouteFinder;
import com.indoornav.engine.model.RoutePreference;
import com.indoornav.engine.model.RouteRequest;
import com.indoornav.engine.result.RouteResult;
import com.indoornav.engine.result.RouteStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Computes a route response from a snapshot: engine call, warnings, and no-route diagnosis
 * (ARCHITECTURE.md 6.3 and 6.4). Pure: no database access and no state between requests.
 */
@Component
public class RoutePlanner {

    /** Detours shorter than this are reported as having no extra distance. */
    private static final double MIN_REPORTED_EXTRA_METRES = 0.05;

    private final RouteFinder routeFinder;
    private final InstructionBuilder instructionBuilder;

    public RoutePlanner(RouteFinder routeFinder, InstructionBuilder instructionBuilder) {
        this.routeFinder = routeFinder;
        this.instructionBuilder = instructionBuilder;
    }

    public RouteResponse plan(RoutingSnapshot snapshot, long startNodeId, long destinationNodeId, RoutePreference preference) {
        RouteResult route = find(snapshot, startNodeId, destinationNodeId, preference, true);
        if (route.status() == RouteStatus.FOUND) {
            return found(snapshot, startNodeId, destinationNodeId, preference, route);
        }
        return RouteResponse.noRoute(preference, diagnose(snapshot, startNodeId, destinationNodeId, preference));
    }

    private RouteResponse found(
            RoutingSnapshot snapshot, long startNodeId, long destinationNodeId, RoutePreference preference, RouteResult route) {
        List<RouteResponse.Warning> warnings = new ArrayList<>();
        if (!snapshot.activeObstacles().isEmpty()) {
            RouteResult unobstructed = find(snapshot, startNodeId, destinationNodeId, preference, false);
            addDetourWarning(snapshot, route, unobstructed, warnings);
        }
        // AVOID_STAIRS only penalises stairs (PRD D-4), so a result that still uses them must say so.
        if (preference == RoutePreference.AVOID_STAIRS && route.usesStairs()) {
            warnings.add(new RouteResponse.Warning(
                    WarningCode.STAIRS_REQUIRED, "There is no step-free route, so this route uses stairs.", List.of(), null));
        }
        return RouteResponse.found(
                preference,
                route.nodeIds().stream().map(nodeId -> toPathNode(snapshot, nodeId)).toList(),
                RouteFormatting.oneDecimal(route.totalDistanceMetres()),
                RouteFormatting.wholeSeconds(route.totalEstimatedTimeSeconds()),
                route.floorTransitions().stream().map(RoutePlanner::toFloorTransition).toList(),
                instructionBuilder.build(snapshot, route),
                warnings);
    }

    /**
     * Compares with the route the same preference would take with no obstacles, so a detour is
     * reported without the server remembering earlier requests (PRD FR-33).
     */
    private static void addDetourWarning(
            RoutingSnapshot snapshot, RouteResult route, RouteResult unobstructed, List<RouteResponse.Warning> warnings) {
        if (unobstructed.status() != RouteStatus.FOUND || unobstructed.edgeIds().equals(route.edgeIds())) {
            return;
        }
        List<ActiveObstacle> involved = obstaclesOn(snapshot, unobstructed);
        if (involved.isEmpty()) {
            return;
        }
        double extra = route.totalDistanceMetres() - unobstructed.totalDistanceMetres();
        Double extraMetres = extra >= MIN_REPORTED_EXTRA_METRES ? RouteFormatting.oneDecimal(extra) : null;
        String message = "Part of the usual route is closed. This route avoids it"
                + (extraMetres == null ? "." : " and adds about " + RouteFormatting.wholeMetres(extra) + ".");
        warnings.add(new RouteResponse.Warning(
                WarningCode.DETOUR, message, involved.stream().map(ActiveObstacle::id).toList(), extraMetres));
    }

    /** Re-runs without obstacles, then without any filtering, to explain the failure (ARCHITECTURE.md 6.4). */
    private RouteResponse.NoRoute diagnose(
            RoutingSnapshot snapshot, long startNodeId, long destinationNodeId, RoutePreference preference) {
        if (!snapshot.activeObstacles().isEmpty()) {
            RouteResult unobstructed = find(snapshot, startNodeId, destinationNodeId, preference, false);
            if (unobstructed.status() == RouteStatus.FOUND) {
                List<RouteResponse.ObstacleSummary> obstacles = obstaclesOn(snapshot, unobstructed).stream()
                        .map(obstacle -> new RouteResponse.ObstacleSummary(
                                obstacle.id(), obstacle.type(), obstacle.description()))
                        .toList();
                return new RouteResponse.NoRoute(
                        NoRouteReason.OBSTACLE, "Part of the way there is closed and there is no other route.", obstacles);
            }
        }
        RouteResult unrestricted = find(snapshot, startNodeId, destinationNodeId, RoutePreference.SHORTEST, false);
        if (unrestricted.status() == RouteStatus.FOUND) {
            return new RouteResponse.NoRoute(
                    NoRouteReason.ACCESSIBILITY_CONSTRAINT, "There is no step-free route to this destination.", List.of());
        }
        return new RouteResponse.NoRoute(
                NoRouteReason.DISCONNECTED, "This destination cannot be reached from the chosen start.", List.of());
    }

    private RouteResult find(
            RoutingSnapshot snapshot,
            long startNodeId,
            long destinationNodeId,
            RoutePreference preference,
            boolean withObstacles) {
        RouteRequest request = new RouteRequest(
                startNodeId,
                destinationNodeId,
                preference,
                withObstacles ? snapshot.blockedNodeIds() : Set.of(),
                withObstacles ? snapshot.blockedEdgeIds() : Set.of());
        return routeFinder.findRoute(snapshot.graph(), request, snapshot.costs());
    }

    /** The active obstacles whose edge or node lies on the given route. */
    private static List<ActiveObstacle> obstaclesOn(RoutingSnapshot snapshot, RouteResult route) {
        Set<Long> edgeIds = new HashSet<>(route.edgeIds());
        Set<Long> nodeIds = new HashSet<>(route.nodeIds());
        return snapshot.activeObstacles().stream()
                .filter(obstacle -> edgeIds.contains(obstacle.edgeId()) || nodeIds.contains(obstacle.nodeId()))
                .toList();
    }

    private static RouteResponse.PathNode toPathNode(RoutingSnapshot snapshot, long nodeId) {
        RoutingSnapshot.RoutingNode node = snapshot.node(nodeId);
        return new RouteResponse.PathNode(node.id(), node.floorId(), node.x(), node.y(), node.type(), node.displayName());
    }

    private static RouteResponse.FloorTransition toFloorTransition(com.indoornav.engine.result.FloorTransition transition) {
        return new RouteResponse.FloorTransition(
                transition.fromFloorId(),
                transition.toFloorId(),
                transition.connectionType().code(),
                transition.pathPosition());
    }
}
