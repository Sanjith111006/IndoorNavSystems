package com.indoornav.engine.algorithm;

import com.indoornav.engine.cost.RoutingCostConfiguration;
import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.model.ConnectionType;
import com.indoornav.engine.model.NavigationEdge;
import com.indoornav.engine.model.NavigationNode;
import com.indoornav.engine.model.RoutePreference;
import com.indoornav.engine.model.RouteRequest;
import com.indoornav.engine.result.FloorTransition;
import com.indoornav.engine.result.RouteResult;
import com.indoornav.engine.result.RouteStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

abstract class AbstractRouteFinder implements RouteFinder {
    private static final double COST_TOLERANCE = 1.0e-9;

    protected void validateRequest(NavigationGraph graph, RouteRequest request, RoutingCostConfiguration configuration) {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(configuration, "costConfiguration must not be null");
        if (graph.node(request.startNodeId()) == null) {
            throw new InvalidRouteRequestException("Start node does not exist.");
        }
        if (graph.node(request.destinationNodeId()) == null) {
            throw new InvalidRouteRequestException("Destination node does not exist.");
        }
    }

    protected boolean isAvailable(NavigationGraph graph, RouteRequest request, long currentNodeId, NavigationGraph.Traversal traversal) {
        NavigationEdge edge = traversal.edge();
        if (request.blockedNodeIds().contains(currentNodeId)
                || request.blockedNodeIds().contains(traversal.destinationNodeId())
                || request.blockedEdgeIds().contains(edge.id())) {
            return false;
        }
        if (request.preference() != RoutePreference.ACCESSIBLE) {
            return true;
        }
        return edge.accessible()
                && graph.node(currentNodeId).accessible()
                && graph.node(traversal.destinationNodeId()).accessible();
    }

    protected double traversalCost(NavigationEdge edge, RoutePreference preference, RoutingCostConfiguration configuration) {
        if (preference == RoutePreference.SHORTEST) {
            return edge.distanceMetres();
        }
        double baseTime = configuration.travelTimeSeconds(edge.distanceMetres(), edge.connectionType());
        if (!edge.connectionType().isStairs()) {
            return baseTime;
        }
        return switch (preference) {
            case AVOID_STAIRS -> baseTime * configuration.avoidStairsMultiplier();
            case PREFER_ELEVATOR -> baseTime * configuration.preferElevatorStairsMultiplier();
            default -> baseTime;
        };
    }

    protected RouteResult createResult(
            NavigationGraph graph,
            RouteRequest request,
            RoutingCostConfiguration configuration,
            Map<Long, PreviousStep> previousSteps) {
        if (!previousSteps.containsKey(request.destinationNodeId()) && request.startNodeId() != request.destinationNodeId()) {
            return RouteResult.noRoute();
        }
        List<Long> nodeIds = new ArrayList<>();
        List<Long> edgeIds = new ArrayList<>();
        long nodeId = request.destinationNodeId();
        nodeIds.add(nodeId);
        while (nodeId != request.startNodeId()) {
            PreviousStep step = previousSteps.get(nodeId);
            if (step == null) {
                return RouteResult.noRoute();
            }
            edgeIds.add(step.edge().id());
            nodeId = step.previousNodeId();
            nodeIds.add(nodeId);
        }
        Collections.reverse(nodeIds);
        Collections.reverse(edgeIds);
        return buildFoundResult(graph, request, configuration, nodeIds, edgeIds);
    }

    private RouteResult buildFoundResult(
            NavigationGraph graph,
            RouteRequest request,
            RoutingCostConfiguration configuration,
            List<Long> nodeIds,
            List<Long> edgeIds) {
        double totalDistance = 0.0;
        double totalTime = 0.0;
        boolean usesStairs = false;
        List<FloorTransition> transitions = new ArrayList<>();
        for (int edgeIndex = 0; edgeIndex < edgeIds.size(); edgeIndex++) {
            NavigationEdge edge = graph.edge(edgeIds.get(edgeIndex));
            totalDistance += edge.distanceMetres();
            totalTime += configuration.travelTimeSeconds(edge.distanceMetres(), edge.connectionType());
            usesStairs |= edge.connectionType().isStairs();
            NavigationNode from = graph.node(nodeIds.get(edgeIndex));
            NavigationNode to = graph.node(nodeIds.get(edgeIndex + 1));
            if (from.floorId() != to.floorId()) {
                transitions.add(new FloorTransition(from.floorId(), to.floorId(), edge.connectionType(), edgeIndex));
            }
        }
        return new RouteResult(RouteStatus.FOUND, nodeIds, edgeIds, totalDistance, totalTime, transitions, usesStairs);
    }

    protected boolean isBetter(double candidateCost, double knownCost) {
        return candidateCost + COST_TOLERANCE < knownCost;
    }

    protected record PreviousStep(long previousNodeId, NavigationEdge edge) {
    }
}
