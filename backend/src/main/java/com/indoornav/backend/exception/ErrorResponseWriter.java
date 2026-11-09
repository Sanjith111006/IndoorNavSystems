package com.indoornav.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Writes error bodies from servlet filters, which run outside the MVC exception handler. */
@Component
public class ErrorResponseWriter {

    private final ErrorResponseFactory errorResponseFactory;
    private final JsonMapper jsonMapper;

    public ErrorResponseWriter(ErrorResponseFactory errorResponseFactory, JsonMapper jsonMapper) {
        this.errorResponseFactory = errorResponseFactory;
        this.jsonMapper = jsonMapper;
    }

    public void write(ErrorCode code, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                jsonMapper.writeValueAsString(errorResponseFactory.create(code, request)));
    }
}
