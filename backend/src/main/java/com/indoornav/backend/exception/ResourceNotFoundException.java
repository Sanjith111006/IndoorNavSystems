package com.indoornav.backend.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resourceName) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resourceName + " not found.");
    }
}
