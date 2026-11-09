package com.indoornav.backend.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Stops a prod start with missing or development secrets (API.md Section 12.1).
 *
 * <p>Runs as a bean factory post-processor so it fails before the database pool or Flyway start.
 * Error messages name the setting only, never its value.
 */
@Component
@Profile("prod")
public class ProductionConfigValidator implements BeanFactoryPostProcessor, EnvironmentAware {

    static final int MIN_JWT_SECRET_BYTES = 32;

    /** The placeholder published in .env.example, which is public and must never sign tokens. */
    private static final Set<String> KNOWN_DEVELOPMENT_JWT_SECRETS =
            Set.of("replace-with-a-random-secret-of-at-least-32-bytes");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        List<String> problems = findProblems(environment);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid production configuration: " + String.join("; ", problems));
        }
    }

    static List<String> findProblems(Environment environment) {
        List<String> problems = new ArrayList<>();
        requirePresent(environment, "spring.datasource.password", "DB_PASSWORD", problems);
        requirePresent(environment, "app.admin.password-hash", "ADMIN_PASSWORD_HASH", problems);

        String secret = environment.getProperty("app.jwt.secret", "");
        if (secret.isBlank()) {
            problems.add("JWT_SECRET is required");
        } else if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            problems.add("JWT_SECRET must be at least " + MIN_JWT_SECRET_BYTES + " bytes");
        } else if (KNOWN_DEVELOPMENT_JWT_SECRETS.contains(secret)) {
            problems.add("JWT_SECRET must not be a known development value");
        }
        return problems;
    }

    private static void requirePresent(
            Environment environment, String property, String variable, List<String> problems) {
        if (environment.getProperty(property, "").isBlank()) {
            problems.add(variable + " is required");
        }
    }
}
