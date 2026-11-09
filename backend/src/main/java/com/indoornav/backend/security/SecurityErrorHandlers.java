package com.indoornav.backend.security;

import com.indoornav.backend.exception.ErrorCode;
import com.indoornav.backend.exception.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;

/**
 * Writes the standard error body for failures raised inside the security filter chain.
 *
 * <p>Failures never reveal which check failed: missing, malformed and expired tokens all look the
 * same to the client (API.md Section 3.1).
 */
@Component
public class SecurityErrorHandlers
        implements AuthenticationEntryPoint, AccessDeniedHandler, RequestRejectedHandler {

    private static final String FLOOR_PLAN_PREFIX = "/assets/floorplans/";

    private final ErrorResponseWriter errorResponseWriter;

    public SecurityErrorHandlers(ErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        errorResponseWriter.write(ErrorCode.UNAUTHORIZED, request, response);
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        errorResponseWriter.write(ErrorCode.FORBIDDEN, request, response);
    }

    /** A suspicious floor-plan path must look like any other missing file (API.md 8.1). */
    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, RequestRejectedException exception)
            throws IOException {
        ErrorCode code = request.getRequestURI().startsWith(FLOOR_PLAN_PREFIX)
                ? ErrorCode.RESOURCE_NOT_FOUND
                : ErrorCode.MALFORMED_REQUEST;
        errorResponseWriter.write(code, request, response);
    }
}
