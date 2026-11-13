package com.indoornav.backend.service;

import com.indoornav.backend.config.RoutingProperties;
import com.indoornav.backend.dto.RouteRequest;
import com.indoornav.backend.dto.RouteResponse;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.exception.NodesNotInSameBuildingException;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.exception.RouteTimeoutException;
import com.indoornav.backend.repository.NodeRepository;
import com.indoornav.backend.routing.RoutingGraphLoader;
import com.indoornav.backend.routing.RoutingSnapshot;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Route orchestration (ARCHITECTURE.md 6.3): validate, load a fresh snapshot, then plan within
 * the time limit. Read-only and safe to repeat; no result is ever cached.
 */
@Service
public class RouteService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final NodeRepository nodeRepository;
    private final RoutingGraphLoader graphLoader;
    private final RoutePlanner routePlanner;
    private final Duration timeLimit;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    public RouteService(
            NodeRepository nodeRepository,
            RoutingGraphLoader graphLoader,
            RoutePlanner routePlanner,
            RoutingProperties routingProperties) {
        this(nodeRepository, graphLoader, routePlanner, Duration.ofSeconds(routingProperties.timeoutSeconds()));
    }

    RouteService(
            NodeRepository nodeRepository, RoutingGraphLoader graphLoader, RoutePlanner routePlanner, Duration timeLimit) {
        this.nodeRepository = nodeRepository;
        this.graphLoader = graphLoader;
        this.routePlanner = routePlanner;
        this.timeLimit = timeLimit;
    }

    public RouteResponse route(RouteRequest request) {
        Node start = findNode(request.startNodeId());
        Node destination = findNode(request.destinationNodeId());
        if (!start.getBuildingId().equals(destination.getBuildingId())) {
            throw new NodesNotInSameBuildingException();
        }
        RoutingSnapshot snapshot = graphLoader.load(start.getBuildingId());
        return planWithinTimeLimit(
                () -> routePlanner.plan(snapshot, start.getId(), destination.getId(), request.preference()));
    }

    /**
     * The engine does not check for interruption, so a timed-out task may finish in the
     * background. That is harmless: it is pure computation and its result is discarded.
     */
    private RouteResponse planWithinTimeLimit(Callable<RouteResponse> plan) {
        Future<RouteResponse> result = executor.submit(plan);
        try {
            return result.get(timeLimit.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            result.cancel(true);
            log.warn("Route calculation exceeded {} and was abandoned", timeLimit);
            throw new RouteTimeoutException();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw new IllegalStateException("Route calculation failed", exception.getCause());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a route", exception);
        }
    }

    private Node findNode(long nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new ResourceNotFoundException("Node"));
    }

    @Override
    public void destroy() {
        executor.close();
    }
}
