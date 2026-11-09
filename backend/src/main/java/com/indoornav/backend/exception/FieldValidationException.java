package com.indoornav.backend.exception;

/** A field that passed DTO validation but failed a service rule. */
public class FieldValidationException extends DomainException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
