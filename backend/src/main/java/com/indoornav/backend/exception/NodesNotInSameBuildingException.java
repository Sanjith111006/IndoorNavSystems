package com.indoornav.backend.exception;

public class NodesNotInSameBuildingException extends DomainException {

    public NodesNotInSameBuildingException() {
        super(ErrorCode.NODES_NOT_IN_SAME_BUILDING, ErrorCode.NODES_NOT_IN_SAME_BUILDING.defaultMessage());
    }
}
