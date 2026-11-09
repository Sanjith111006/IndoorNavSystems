package com.indoornav.backend.exception;

import com.indoornav.backend.dto.ErrorResponse;
import com.indoornav.backend.security.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;

/** Builds error bodies, so the handler and the servlet filters produce the same shape. */
@Component
public class ErrorResponseFactory {

    private final Clock clock;

    public ErrorResponseFactory(Clock clock) {
        this.clock = clock;
    }

    public ErrorResponse create(ErrorCode code, HttpServletRequest request) {
        return create(code, code.defaultMessage(), request, null);
    }

    public ErrorResponse create(
            ErrorCode code,
            String message,
            HttpServletRequest request,
            List<ErrorResponse.FieldProblem> details) {
        return new ErrorResponse(
                clock.instant(),
                code.status().value(),
                code.name(),
                message,
                request.getRequestURI(),
                RequestIdFilter.currentRequestId(),
                details);
    }
}
