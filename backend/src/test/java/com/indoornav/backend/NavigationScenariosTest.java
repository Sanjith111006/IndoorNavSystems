package com.indoornav.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indoornav.backend.security.TokenService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The PRD 8.3 demonstration scenarios end to end: HTTP, PostgreSQL with the real migrations and
 * seed, the engine, and back. Obstacles are created and removed through the admin API, so these
 * tests also prove that a route reflects an obstacle change on the very next request.
 *
 * <p>IDs are the fixed seed IDs in {@code database/seed/R__sample_mall_demo_data.sql}.
 */
@SpringBootTest(properties = {"app.seed-demo-data=true", "springdoc.api-docs.enabled=false"})
@AutoConfigureMockMvc
@Testcontainers
class NavigationScenariosTest {

    private static final long WEST_ENTRANCE = 1001L;
    private static final long GROUND_STAIRS = 1005L;
    private static final long CENTRAL_MARKET = 1006L;
    private static final long NORTH_RESTROOMS = 1012L;
    private static final long QUIET_CORNER = 1020L;
    private static final long BOOKS_AND_MEDIA = 1102L;
    private static final long CINEMA_LOBBY = 1202L;
    private static final long LIBRARY_NODE = 1301L;
    private static final long CORRIDOR_1003_TO_1004 = 2103L;
    private static final long QUIET_CORNER_DOORWAY = 2120L;
    private static final long ELEVATOR_LEVEL_ONE_TO_TWO = 2004L;
    private static final long LEVEL_ONE_FLOOR = 101L;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("indoornav")
            .withUsername("test")
            .withPassword("test-password");

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    /** Keeps the tests independent: the seed itself has no obstacles. */
    @AfterEach
    void removeObstacles() {
        jdbcTemplate.update("delete from obstacles");
    }

    @Test
    void scenario1BlockedCorridorForcesALongerDetourAndUnblockingRestoresTheRoute() throws Exception {
        JsonNode original = route(WEST_ENTRANCE, CENTRAL_MARKET, "SHORTEST");
        assertThat(original.get("distanceMeters").asDouble()).isEqualTo(50.0);
        assertThat(original.get("warnings")).isEmpty();

        long obstacleId = createObstacle("EDGE", CORRIDOR_1003_TO_1004, "BLOCKED_CORRIDOR");
        JsonNode detour = route(WEST_ENTRANCE, CENTRAL_MARKET, "SHORTEST");

        assertThat(detour.get("status").asString()).isEqualTo("FOUND");
        assertThat(detour.get("distanceMeters").asDouble()).isGreaterThan(50.0);
        assertThat(consecutivePairs(detour)).doesNotContain(List.of(1003L, 1004L), List.of(1004L, 1003L));
        JsonNode warning = detour.get("warnings").get(0);
        assertThat(warning.get("code").asString()).isEqualTo("DETOUR");
        assertThat(warning.get("obstacleIds").get(0).asLong()).isEqualTo(obstacleId);
        assertThat(warning.get("extraDistanceMeters").asDouble())
                .isEqualTo(detour.get("distanceMeters").asDouble() - 50.0);

        mockMvc.perform(asAdmin(delete("/api/obstacles/" + obstacleId))).andExpect(status().isNoContent());
        JsonNode restored = route(WEST_ENTRANCE, CENTRAL_MARKET, "SHORTEST");
        assertThat(restored.get("distanceMeters").asDouble()).isEqualTo(50.0);
        assertThat(restored.get("warnings")).isEmpty();
    }

    @Test
    void scenario2ShortestTakesTheStairsAndFastestTakesTheElevator() throws Exception {
        JsonNode shortest = route(WEST_ENTRANCE, BOOKS_AND_MEDIA, "SHORTEST");
        JsonNode fastest = route(WEST_ENTRANCE, BOOKS_AND_MEDIA, "FASTEST");

        assertThat(transitionType(shortest)).isEqualTo("STAIRS");
        assertThat(transitionType(fastest)).isEqualTo("ELEVATOR");
        assertThat(shortest.get("distanceMeters").asDouble()).isLessThan(fastest.get("distanceMeters").asDouble());
        assertThat(fastest.get("estimatedDurationSeconds").asLong())
                .isLessThan(shortest.get("estimatedDurationSeconds").asLong());
    }

    @Test
    void scenario3AccessibleAvoidsTheStairsAndDiffersFromShortest() throws Exception {
        JsonNode accessible = route(WEST_ENTRANCE, BOOKS_AND_MEDIA, "ACCESSIBLE");

        assertThat(transitionType(accessible)).isEqualTo("ELEVATOR");
        assertThat(pathNodeIds(accessible)).doesNotContain(GROUND_STAIRS, 1105L);
        assertThat(pathNodeIds(accessible)).isNotEqualTo(pathNodeIds(route(WEST_ENTRANCE, BOOKS_AND_MEDIA, "SHORTEST")));
    }

    @Test
    void scenario4BrokenElevatorLeavesNoAccessibleRouteButShortestStillUsesTheStairs() throws Exception {
        long obstacleId = createObstacle("EDGE", ELEVATOR_LEVEL_ONE_TO_TWO, "BROKEN_ELEVATOR");

        JsonNode accessible = route(WEST_ENTRANCE, CINEMA_LOBBY, "ACCESSIBLE");
        assertThat(accessible.get("status").asString()).isEqualTo("NO_ROUTE");
        assertThat(accessible.get("noRoute").get("reasonCode").asString()).isEqualTo("OBSTACLE");
        assertThat(accessible.get("noRoute").get("obstacles").get(0).get("id").asLong()).isEqualTo(obstacleId);

        JsonNode shortest = route(WEST_ENTRANCE, CINEMA_LOBBY, "SHORTEST");
        assertThat(shortest.get("status").asString()).isEqualTo("FOUND");
        assertThat(shortest.get("floorTransitions")).allSatisfy(
                transition -> assertThat(transition.get("connectionType").asString()).isEqualTo("STAIRS"));
    }

    @Test
    void avoidStairsWithTheElevatorBrokenWarnsThatStairsAreRequired() throws Exception {
        createObstacle("EDGE", ELEVATOR_LEVEL_ONE_TO_TWO, "BROKEN_ELEVATOR");

        JsonNode route = route(WEST_ENTRANCE, CINEMA_LOBBY, "AVOID_STAIRS");

        List<String> codes = new ArrayList<>();
        route.get("warnings").forEach(warning -> codes.add(warning.get("code").asString()));
        assertThat(codes).contains("STAIRS_REQUIRED");
    }

    @Test
    void scenario5BlockingTheOnlyDoorwayMakesTheDestinationUnreachable() throws Exception {
        assertThat(route(WEST_ENTRANCE, QUIET_CORNER, "SHORTEST").get("status").asString()).isEqualTo("FOUND");

        createObstacle("EDGE", QUIET_CORNER_DOORWAY, "BLOCKED_CORRIDOR");
        JsonNode blocked = route(WEST_ENTRANCE, QUIET_CORNER, "SHORTEST");

        assertThat(blocked.get("status").asString()).isEqualTo("NO_ROUTE");
        assertThat(blocked.get("noRoute").get("reasonCode").asString()).isEqualTo("OBSTACLE");
    }

    @Test
    void stepFreeRouteFromAStairLandingIsAnAccessibilityConstraint() throws Exception {
        JsonNode route = route(GROUND_STAIRS, CENTRAL_MARKET, "ACCESSIBLE");

        assertThat(route.get("noRoute").get("reasonCode").asString()).isEqualTo("ACCESSIBILITY_CONSTRAINT");
    }

    @Test
    void multiFloorRouteNamesEachFloorChangeInItsInstructions() throws Exception {
        JsonNode route = route(WEST_ENTRANCE, CINEMA_LOBBY, "ACCESSIBLE");

        List<String> texts = new ArrayList<>();
        route.get("instructions").forEach(step -> texts.add(step.get("text").asString()));
        assertThat(texts.getFirst()).isEqualTo("Start at West entrance.");
        assertThat(texts).contains("Take the elevator to Level 2.");
        assertThat(texts.getLast()).isEqualTo("Arrive at Cinema lobby.");
        assertThat(route.get("floorTransitions")).hasSize(2);
    }

    @Test
    void turnWordingFollowsTheFloorPlanGeometry() throws Exception {
        // East along the ground corridor to 1004 (400,350), then up the plan to 1012 (500,150).
        JsonNode route = route(WEST_ENTRANCE, NORTH_RESTROOMS, "SHORTEST");

        List<String> texts = new ArrayList<>();
        route.get("instructions").forEach(step -> texts.add(step.get("text").asString()));
        assertThat(texts).containsExactly(
                "Start at West entrance.",
                "Walk 30 m.",
                "Turn left and walk 23 m to North restrooms.",
                "Arrive at North restrooms.");
        assertThat(route.get("instructions").get(2).get("turn").asString()).isEqualTo("LEFT");
    }

    @Test
    void blockedConnectionShowsOnTheFloorMap() throws Exception {
        long obstacleId = createObstacle("EDGE", ELEVATOR_LEVEL_ONE_TO_TWO, "BROKEN_ELEVATOR");

        String body = mockMvc.perform(get("/api/floors/" + LEVEL_ONE_FLOOR + "/map"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode elevator = null;
        for (JsonNode edge : json.readTree(body).get("edges")) {
            if (edge.get("id").asLong() == ELEVATOR_LEVEL_ONE_TO_TWO) {
                elevator = edge;
            }
        }
        assertThat(elevator).isNotNull();
        assertThat(elevator.get("blocked").asBoolean()).isTrue();
        assertThat(elevator.get("obstacle").get("id").asLong()).isEqualTo(obstacleId);
    }

    @Test
    void startAndDestinationInDifferentBuildingsAreRejected() throws Exception {
        mockMvc.perform(routeRequest(WEST_ENTRANCE, LIBRARY_NODE, "SHORTEST"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownNodeIsNotFound() throws Exception {
        mockMvc.perform(routeRequest(WEST_ENTRANCE, 999_999L, "SHORTEST")).andExpect(status().isNotFound());
    }

    private JsonNode route(long start, long destination, String preference) throws Exception {
        String body = mockMvc.perform(routeRequest(start, destination, preference))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private MockHttpServletRequestBuilder routeRequest(
            long start, long destination, String preference) {
        String body = "{\"startNodeId\":" + start + ",\"destinationNodeId\":" + destination
                + ",\"preference\":\"" + preference + "\"}";
        return post("/api/navigation/route").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private long createObstacle(String targetType, long targetId, String obstacleType) throws Exception {
        String body = "{\"targetType\":\"" + targetType + "\",\"targetId\":" + targetId
                + ",\"obstacleType\":\"" + obstacleType + "\",\"description\":\"Closed for the test\"}";
        String response = mockMvc.perform(asAdmin(post("/api/obstacles"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    private MockHttpServletRequestBuilder asAdmin(
            MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + tokenService.issueAdminToken("admin").value());
    }

    private static String transitionType(JsonNode route) {
        return route.get("floorTransitions").get(0).get("connectionType").asString();
    }

    private static List<Long> pathNodeIds(JsonNode route) {
        List<Long> ids = new ArrayList<>();
        route.get("pathNodes").forEach(node -> ids.add(node.get("nodeId").asLong()));
        return ids;
    }

    private static List<List<Long>> consecutivePairs(JsonNode route) {
        List<Long> ids = pathNodeIds(route);
        List<List<Long>> pairs = new ArrayList<>();
        for (int index = 0; index + 1 < ids.size(); index++) {
            pairs.add(List.of(ids.get(index), ids.get(index + 1)));
        }
        return pairs;
    }
}
