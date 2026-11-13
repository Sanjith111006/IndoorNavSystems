package com.indoornav.backend.controller;

import com.indoornav.backend.dto.RouteRequest;
import com.indoornav.backend.dto.RouteResponse;
import com.indoornav.backend.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST only because the request has a body; it changes no data (API.md 2.3). */
@RestController
@RequestMapping("/api/navigation")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    /** No route is a normal 200 outcome with {@code status} NO_ROUTE, never an error. */
    @PostMapping("/route")
    public RouteResponse route(@Valid @RequestBody RouteRequest request) {
        return routeService.route(request);
    }
}
