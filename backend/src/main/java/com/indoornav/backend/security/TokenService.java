package com.indoornav.backend.security;

import com.indoornav.backend.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues administrator tokens: subject, role, issue time and expiry, and nothing else. */
@Service
public class TokenService {

    public record IssuedToken(String value, long expiresInSeconds) {
    }

    static final String ADMIN_ROLE = "ADMIN";

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration tokenLifetime;

    public TokenService(JwtEncoder jwtEncoder, Clock clock, AppProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.tokenLifetime = Duration.ofMinutes(properties.jwt().ttlMinutes());
    }

    public IssuedToken issueAdminToken(String username) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(username)
                .claim("role", ADMIN_ROLE)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(tokenLifetime))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, tokenLifetime.toSeconds());
    }
}
