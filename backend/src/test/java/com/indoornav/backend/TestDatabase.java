package com.indoornav.backend;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** A throwaway PostgreSQL for tests that need the real migrations and seed. */
public final class TestDatabase {

    private static final String IMAGE = "postgres:17-alpine";

    private TestDatabase() {
    }

    public static PostgreSQLContainer newContainer() {
        return new PostgreSQLContainer(IMAGE)
                .withDatabaseName("indoornav")
                .withUsername("test")
                .withPassword("test-password");
    }

    /** Points both the application and Flyway at the container. */
    public static void register(DynamicPropertyRegistry registry, PostgreSQLContainer container) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.flyway.user", container::getUsername);
        registry.add("spring.flyway.password", container::getPassword);
    }
}
