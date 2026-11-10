package com.indoornav.engine.algorithm;

/**
 * Raised when a route request names a node absent from its graph.
 */
public final class InvalidRouteRequestException extends RuntimeException {
    public InvalidRouteRequestException(String message) {
        super(message);
    }
}
