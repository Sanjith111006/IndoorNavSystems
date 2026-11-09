package com.indoornav.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.indoornav.backend.config.AppProperties;
import com.indoornav.backend.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FloorPlanServiceTest {

    @TempDir
    private Path root;

    private Path plans;
    private FloorPlanService service;

    @BeforeEach
    void setUp() throws IOException {
        plans = Files.createDirectories(root.resolve("plans/sample"));
        Files.writeString(plans.resolve("level-0.svg"), "<svg/>");
        Files.writeString(root.resolve("secret.svg"), "<svg>outside</svg>");
        service = new FloorPlanService(properties(root.resolve("plans").toString()));
    }

    @Test
    void servesAPlanWithAStableEntityTag() {
        FloorPlanService.FloorPlan first = service.load("sample/level-0.svg");
        FloorPlanService.FloorPlan second = service.load("sample/level-0.svg");

        assertThat(new String(first.content())).isEqualTo("<svg/>");
        assertThat(first.entityTag()).startsWith("\"").endsWith("\"").isEqualTo(second.entityTag());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../secret.svg",
        "sample/../../secret.svg",
        "/etc/passwd.svg",
        "sample/level-0.txt",
        "sample/LEVEL-0.svg",
        "sample//level-0.svg",
        "sample/level-0.svg/",
        "",
        ".svg"
    })
    void rejectsPathsOutsideTheStoredPattern(String path) {
        assertThatThrownBy(() -> service.load(path)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void missingFileIsNotFound() {
        assertThatThrownBy(() -> service.load("sample/none.svg")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void symbolicLinkLeadingOutsideTheDirectoryIsNotFound() throws IOException {
        Files.createSymbolicLink(plans.resolve("escape.svg"), root.resolve("secret.svg"));

        assertThatThrownBy(() -> service.load("sample/escape.svg")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void blankDirectoryUsesPackagedPlansAndReportsMissingOnes() {
        FloorPlanService packaged = new FloorPlanService(properties(""));

        assertThatThrownBy(() -> packaged.load("not/packaged.svg")).isInstanceOf(ResourceNotFoundException.class);
    }

    private static AppProperties properties(String directory) {
        return new AppProperties(
                false,
                new AppProperties.Admin("admin", ""),
                new AppProperties.Jwt("", 60),
                new AppProperties.Login(5, 15, false),
                new AppProperties.FloorPlans(directory));
    }
}
