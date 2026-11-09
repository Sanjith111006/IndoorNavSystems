package com.indoornav.backend.exception;

/** Uses one message for every failure, so a response never reveals which credential was wrong. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super(ErrorCode.UNAUTHORIZED, "Invalid username or password.");
    }
}
