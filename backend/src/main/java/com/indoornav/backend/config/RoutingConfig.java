package com.indoornav.backend.config;

import com.indoornav.engine.algorithm.AStarRouteFinder;
import com.indoornav.engine.algorithm.RouteFinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingConfig {

    /** A* with Dijkstra kept in the engine for verification (ARCHITECTURE.md AD-8). */
    @Bean
    RouteFinder routeFinder() {
        return new AStarRouteFinder();
    }
}
