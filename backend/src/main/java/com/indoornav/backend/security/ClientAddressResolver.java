package com.indoornav.backend.security;

import com.indoornav.backend.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Finds the client address used by the login throttle. */
@Component
public class ClientAddressResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final boolean trustForwardedHeaders;

    public ClientAddressResolver(AppProperties properties) {
        this.trustForwardedHeaders = properties.login().trustForwardedHeaders();
    }

    /**
     * Uses the last X-Forwarded-For entry, which is the one the trusted proxy appended. Earlier
     * entries come from the client and can be forged to dodge the throttle.
     */
    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (!trustForwardedHeaders || forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwardedFor.substring(forwardedFor.lastIndexOf(',') + 1).strip();
    }
}
