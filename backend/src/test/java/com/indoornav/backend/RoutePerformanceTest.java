package com.indoornav.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Checks PRD NFR-01 (routes under 1 s at p95 for about 2,000 nodes) and NFR-02 (other calls under
 * 500 ms at p95) through HTTP against PostgreSQL. Each route request reloads the graph from the
 * database, as it does in production (ARCHITECTURE.md AD-3).
 *
 * <p>The graph is synthetic: two floors of a 32 by 32 corridor grid, 1 m apart, joined by four
 * elevators. It lives in its own container, so other tests never see it. The limits are the
 * requirement's own; measured times are logged so regressions show before they fail.
 */
@SpringBootTest(properties = {"springdoc.api-docs.enabled=false", "app.seed-demo-data=false"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutePerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(RoutePerformanceTest.class);

    private static final long BUILDING = 900_000L;
    private static final long LOWER_FLOOR = 900_000L;
    private static final long UPPER_FLOOR = 900_001L;
    private static final int GRID = 32;
    private static final int SPACING = 10;
    private static final Duration ROUTE_LIMIT = Duration.ofSeconds(1);
    private static final Duration READ_LIMIT = Duration.ofMillis(500);
    private static final int WARM_UP_REQUESTS = 5;
    private static final int MEASURED_REQUESTS = 40;
    private static final String[] PREFERENCES = {"SHORTEST", "FASTEST", "ACCESSIBLE", "AVOID_STAIRS", "PREFER_ELEVATOR"};

    /**
     * Started here rather than by the JUnit extension: with one test instance per class, Spring
     * builds the context before the extension would start the container. Testcontainers removes
     * it when the JVM exits.
     */
    static final PostgreSQLContainer POSTGRES = TestDatabase.newContainer();

    static {
        POSTGRES.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        TestDatabase.register(registry, POSTGRES);
    }

    @BeforeAll
    void insertLargeBuilding() {
        jdbcTemplate.update("""
                insert into buildings (id, name, building_type) values (?, 'Performance building', 'Test')
                """, BUILDING);
        insertFloor(LOWER_FLOOR, 0);
        insertFloor(UPPER_FLOOR, 1);
        insertGridNodes(LOWER_FLOOR);
        insertGridNodes(UPPER_FLOOR);
        insertGridCorridors(LOWER_FLOOR);
        insertGridCorridors(UPPER_FLOOR);
        insertElevators();
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from nodes where building_id = ?", Long.class, BUILDING))
                .isEqualTo(2L * GRID * GRID);
    }

    @Test
    void crossFloorRoutesMeetTheOneSecondTarget() throws Exception {
        long start = nodeId(LOWER_FLOOR, 0, 0);
        long destination = nodeId(UPPER_FLOOR, GRID - 1, GRID - 1);

        Duration p95 = p95(attempt -> mockMvc.perform(post("/api/navigation/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startNodeId\":" + start + ",\"destinationNodeId\":" + destination
                                + ",\"preference\":\"" + PREFERENCES[attempt % PREFERENCES.length] + "\"}"))
                .andExpect(status().isOk()));

        log.info("Route p95 over {} requests on {} nodes: {} ms", MEASURED_REQUESTS, 2 * GRID * GRID, p95.toMillis());
        assertThat(p95).isLessThan(ROUTE_LIMIT);
    }

    @Test
    void noRouteDiagnosisAlsoMeetsTheTarget() throws Exception {
        // ACCESSIBLE to a non-accessible node fails, then is re-run twice to find the reason.
        long start = nodeId(LOWER_FLOOR, 0, 0);
        long blocked = nodeId(UPPER_FLOOR, GRID - 1, 0);
        jdbcTemplate.update("update nodes set accessible = false where id = ?", blocked);
        try {
            Duration p95 = p95(attempt -> mockMvc.perform(post("/api/navigation/route")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startNodeId\":" + start + ",\"destinationNodeId\":" + blocked
                                    + ",\"preference\":\"ACCESSIBLE\"}"))
                    .andExpect(status().isOk()));

            log.info("No-route p95 over {} requests: {} ms", MEASURED_REQUESTS, p95.toMillis());
            assertThat(p95).isLessThan(ROUTE_LIMIT);
        } finally {
            jdbcTemplate.update("update nodes set accessible = true where id = ?", blocked);
        }
    }

    @Test
    void largeFloorMapMeetsTheReadTarget() throws Exception {
        Duration p95 = p95(attempt -> mockMvc.perform(get("/api/floors/" + LOWER_FLOOR + "/map"))
                .andExpect(status().isOk()));

        log.info("Floor map p95 for {} nodes: {} ms", GRID * GRID, p95.toMillis());
        assertThat(p95).isLessThan(READ_LIMIT);
    }

    @FunctionalInterface
    private interface Request {
        void send(int attempt) throws Exception;
    }

    private static Duration p95(Request request) throws Exception {
        for (int attempt = 0; attempt < WARM_UP_REQUESTS; attempt++) {
            request.send(attempt);
        }
        List<Long> nanos = new ArrayList<>();
        for (int attempt = 0; attempt < MEASURED_REQUESTS; attempt++) {
            long startedAt = System.nanoTime();
            request.send(attempt);
            nanos.add(System.nanoTime() - startedAt);
        }
        Collections.sort(nanos);
        int index = (int) Math.ceil(0.95 * nanos.size()) - 1;
        return Duration.ofNanos(nanos.get(index));
    }

    private void insertFloor(long floorId, int level) {
        jdbcTemplate.update("""
                insert into floors (id, building_id, level, display_name, plan_asset_path, plan_width, plan_height, metres_per_unit)
                values (?, ?, ?, ?, 'perf/plan.svg', 400.00, 400.00, 0.100000)
                """, floorId, BUILDING, level, "Level " + level);
    }

    private void insertGridNodes(long floorId) {
        List<Object[]> rows = new ArrayList<>();
        for (int row = 0; row < GRID; row++) {
            for (int column = 0; column < GRID; column++) {
                rows.add(new Object[] {nodeId(floorId, row, column), BUILDING, floorId, column * SPACING, row * SPACING});
            }
        }
        jdbcTemplate.batchUpdate("""
                insert into nodes (id, building_id, floor_id, x, y, node_type, accessible)
                values (?, ?, ?, ?, ?, 'CORRIDOR_JUNCTION', true)
                """, rows);
    }

    private void insertGridCorridors(long floorId) {
        List<Object[]> rows = new ArrayList<>();
        for (int row = 0; row < GRID; row++) {
            for (int column = 0; column < GRID; column++) {
                long here = nodeId(floorId, row, column);
                if (column + 1 < GRID) {
                    rows.add(new Object[] {edgeId(here, 0), BUILDING, here, nodeId(floorId, row, column + 1)});
                }
                if (row + 1 < GRID) {
                    rows.add(new Object[] {edgeId(here, 1), BUILDING, here, nodeId(floorId, row + 1, column)});
                }
            }
        }
        jdbcTemplate.batchUpdate("""
                insert into edges (id, building_id, from_node_id, to_node_id, connection_type_code, distance_m, accessible, bidirectional)
                values (?, ?, ?, ?, 'CORRIDOR', 1.00, true, true)
                """, rows);
    }

    private void insertElevators() {
        int[][] corners = {{0, 0}, {0, GRID - 1}, {GRID - 1, 0}, {GRID - 1, GRID - 1}};
        List<Object[]> rows = new ArrayList<>();
        for (int[] corner : corners) {
            long lower = nodeId(LOWER_FLOOR, corner[0], corner[1]);
            rows.add(new Object[] {edgeId(lower, 2), BUILDING, lower, nodeId(UPPER_FLOOR, corner[0], corner[1])});
        }
        jdbcTemplate.batchUpdate("""
                insert into edges (id, building_id, from_node_id, to_node_id, connection_type_code, distance_m, accessible, bidirectional)
                values (?, ?, ?, ?, 'ELEVATOR', 5.00, true, true)
                """, rows);
    }

    /** Node IDs above the seed range (below 10,000) and far above the runtime sequences. */
    private static long nodeId(long floorId, int row, int column) {
        return floorId * 10_000L + (long) row * GRID + column;
    }

    private static long edgeId(long fromNodeId, int direction) {
        return fromNodeId * 3 + direction;
    }
}
