package com.indoornav.backend.service;

import com.indoornav.backend.config.AppProperties;
import com.indoornav.backend.dto.LoginRequest;
import com.indoornav.backend.dto.LoginResponse;
import com.indoornav.backend.exception.InvalidCredentialsException;
import com.indoornav.backend.exception.LoginThrottledException;
import com.indoornav.backend.security.LoginThrottle;
import com.indoornav.backend.security.TokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Signs in the single administrator configured through the environment. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String TOKEN_TYPE = "Bearer";

    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginThrottle loginThrottle;
    private final String adminUsername;
    private final String adminPasswordHash;

    public AuthService(
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            LoginThrottle loginThrottle,
            AppProperties properties) {
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginThrottle = loginThrottle;
        this.adminUsername = properties.admin().username();
        this.adminPasswordHash = hashOrUnguessablePlaceholder(properties.admin().passwordHash());
    }

    public LoginResponse login(LoginRequest request, String clientAddress) {
        loginThrottle.retryAfter(clientAddress).ifPresent(wait -> {
            throw new LoginThrottledException(wait);
        });

        // Both checks always run, so timing does not show whether the username was right.
        boolean passwordMatches = passwordEncoder.matches(request.password(), adminPasswordHash);
        boolean usernameMatches = constantTimeEquals(request.username(), adminUsername);
        if (!(usernameMatches && passwordMatches)) {
            loginThrottle.recordFailure(clientAddress);
            log.warn("Administrator sign-in failed");
            throw new InvalidCredentialsException();
        }

        loginThrottle.clear(clientAddress);
        TokenService.IssuedToken token = tokenService.issueAdminToken(adminUsername);
        return new LoginResponse(token.value(), TOKEN_TYPE, token.expiresInSeconds());
    }

    /** With no hash configured, sign-in stays disabled but takes the same time to fail. */
    private String hashOrUnguessablePlaceholder(String configuredHash) {
        if (configuredHash != null && !configuredHash.isBlank()) {
            return configuredHash;
        }
        log.warn("ADMIN_PASSWORD_HASH is not set: administrator sign-in is disabled");
        return passwordEncoder.encode(UUID.randomUUID().toString());
    }

    private static boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = (right == null ? "" : right).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(leftBytes, rightBytes);
    }
}
