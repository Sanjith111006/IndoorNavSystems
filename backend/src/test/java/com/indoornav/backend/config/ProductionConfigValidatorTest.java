package com.indoornav.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigValidatorTest {

    private static final String STRONG_SECRET = "fake-test-signing-key-that-is-long-enough-1234";

    @Test
    void completeConfigurationHasNoProblems() {
        assertThat(ProductionConfigValidator.findProblems(environment(STRONG_SECRET))).isEmpty();
    }

    @Test
    void reportsEverySetMissingSecretByNameOnly() {
        List<String> problems = ProductionConfigValidator.findProblems(new MockEnvironment());

        assertThat(problems).containsExactlyInAnyOrder(
                "DB_PASSWORD is required", "ADMIN_PASSWORD_HASH is required", "JWT_SECRET is required");
    }

    @Test
    void rejectsAShortSecretWithoutRepeatingIt() {
        List<String> problems = ProductionConfigValidator.findProblems(environment("short-fake-key"));

        assertThat(problems).containsExactly("JWT_SECRET must be at least 32 bytes");
        assertThat(problems.get(0)).doesNotContain("short-fake-key");
    }

    @Test
    void rejectsThePublishedPlaceholder() {
        assertThat(ProductionConfigValidator.findProblems(
                        environment("replace-with-a-random-secret-of-at-least-32-bytes")))
                .containsExactly("JWT_SECRET must not be a known development value");
    }

    private static MockEnvironment environment(String jwtSecret) {
        return new MockEnvironment()
                .withProperty("spring.datasource.password", "fake-db-password")
                .withProperty("app.admin.password-hash", "fake-hash")
                .withProperty("app.jwt.secret", jwtSecret);
    }
}
