package com.indoornav.engine.algorithm;

import com.indoornav.engine.cost.RoutingCostConfiguration;
import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.model.RouteRequest;
import com.indoornav.engine.result.RouteResult;

/**
 * Stable pure interface for routing an immutable graph.
 */
public interface RouteFinder {
    RouteResult findRoute(NavigationGraph graph, RouteRequest request, RoutingCostConfiguration costConfiguration);
}
