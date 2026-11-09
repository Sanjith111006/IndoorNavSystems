package com.indoornav.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

class FlywayConfigTest {

    private static final String SEED = "classpath:db/seed";

    @Test
    void addsTheSeedLocationWhenSeedingIsOn() {
        assertThat(locationsWithSeeding(true)).contains(SEED, "classpath:db/migration");
    }

    @Test
    void leavesLocationsAloneWhenSeedingIsOff() {
        assertThat(locationsWithSeeding(false)).containsExactly("classpath:db/migration");
    }

    private static List<String> locationsWithSeeding(boolean seedDemoData) {
        AppProperties properties = new AppProperties(
                seedDemoData,
                new AppProperties.Admin("admin", ""),
                new AppProperties.Jwt("", 60),
                new AppProperties.Login(5, 15, false),
                new AppProperties.FloorPlans(""));
        FluentConfiguration configuration = new FluentConfiguration().locations("classpath:db/migration");
        new FlywayConfig().seedLocationCustomizer(properties).customize(configuration);
        return Arrays.stream(configuration.getLocations()).map(Object::toString).toList();
    }
}
