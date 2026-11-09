package com.indoornav.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Typed application settings, bound from the environment variables in API.md Section 12. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        boolean seedDemoData,
        @NotNull @Valid Admin admin,
        @NotNull @Valid Jwt jwt,
        @NotNull @Valid Login login,
        @NotNull @Valid FloorPlans floorPlans) {

    public record Admin(String username, String passwordHash) {

        /** Hides the hash so it cannot reach a log through this record. */
        @Override
        public String toString() {
            return "Admin[username=" + username + "]";
        }
    }

    public record Jwt(String secret, @Min(1) int ttlMinutes) {

        /** Hides the signing key so it cannot reach a log through this record. */
        @Override
        public String toString() {
            return "Jwt[ttlMinutes=" + ttlMinutes + "]";
        }
    }

    public record Login(
            @Min(1) int maxFailedAttempts,
            @Min(1) int attemptWindowMinutes,
            boolean trustForwardedHeaders) {
    }

    /** {@code directory} is blank to serve the plans packaged with the backend. */
    public record FloorPlans(String directory) {
    }
}
