package com.indoornav.backend.exception;

/** The route calculation ran past its time limit (API.md APA-4). */
public class RouteTimeoutException extends DomainException {

    public RouteTimeoutException() {
        super(ErrorCode.SERVICE_UNAVAILABLE, "The route calculation took too long. Try again.");
    }
}
