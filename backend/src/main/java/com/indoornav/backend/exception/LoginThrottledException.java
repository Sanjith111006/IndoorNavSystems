package com.indoornav.backend.exception;

import java.time.Duration;

public class LoginThrottledException extends DomainException {

    private final Duration retryAfter;

    public LoginThrottledException(Duration retryAfter) {
        super(ErrorCode.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS.defaultMessage());
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
