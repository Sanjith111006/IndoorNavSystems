package com.indoornav.backend.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.api.Location;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Adds the demo seed to Flyway's locations only when seeding is on (ARCHITECTURE.md 7.5). */
@Configuration
public class FlywayConfig {

    private static final String SEED_LOCATION_PREFIX = "classpath:";
    private static final String SEED_LOCATION_PATH = "db/seed";

    @Bean
    FlywayConfigurationCustomizer seedLocationCustomizer(AppProperties properties) {
        return configuration -> {
            if (!properties.seedDemoData()) {
                return;
            }
            List<Location> locations = new ArrayList<>(Arrays.asList(configuration.getLocations()));
            locations.add(Location.fromPath(SEED_LOCATION_PREFIX, SEED_LOCATION_PATH));
            configuration.locations(locations.toArray(Location[]::new));
        };
    }
}
