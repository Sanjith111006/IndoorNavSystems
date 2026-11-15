package com.indoornav.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Checks the generated OpenAPI document against the contract in docs/API.md (ARCHITECTURE.md
 * 16.1): every endpoint and method, which ones need a token, success statuses, field names,
 * required request fields and enumeration values.
 *
 * <p>It also compares the whole document with a committed snapshot, so any change to the
 * contract shows up in review. After an intended change, update API.md first, then copy
 * {@code backend/target/openapi-generated.json} over {@code src/test/resources/contract/openapi.json}.
 *
 * <p>No database is needed: the document is built from the code alone.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/unreachable",
    "spring.datasource.username=test",
    "spring.datasource.password=fake-test-password",
    "spring.datasource.hikari.initialization-fail-timeout=-1",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
    "springdoc.api-docs.enabled=true",
    "springdoc.writer-with-order-by-keys=true"
})
@AutoConfigureMockMvc
class OpenApiContractTest {

    private static final String SNAPSHOT = "/contract/openapi.json";
    private static final Path GENERATED_COPY = Path.of("target/openapi-generated.json");

    /** API.md 1.1, endpoints 1 to 14 and 16; endpoint 15 is reserved and not built. */
    private static final Set<String> DOCUMENTED_OPERATIONS = Set.of(
            "GET /api/health",
            "GET /api/buildings",
            "GET /api/buildings/{buildingId}",
            "GET /api/buildings/{buildingId}/floors",
            "GET /api/floors/{floorId}/map",
            "GET /api/points-of-interest",
            "GET /api/points-of-interest/categories",
            "GET /api/buildings/{buildingId}/nodes",
            "POST /api/navigation/route",
            "POST /api/auth/login",
            "GET /api/obstacles",
            "POST /api/obstacles",
            "PUT /api/obstacles/{obstacleId}",
            "DELETE /api/obstacles/{obstacleId}",
            "GET /assets/floorplans/{path}");

    /** Field names exactly as the API.md tables list them. */
    private static final Map<String, Set<String>> DOCUMENTED_FIELDS = Map.ofEntries(
            Map.entry("HealthResponse", Set.of("status", "database", "timestamp", "code", "message", "path", "requestId")),
            Map.entry("BuildingSummaryResponse", Set.of("id", "name", "description", "buildingType", "address", "floorCount")),
            Map.entry("BuildingDetailResponse",
                    Set.of("id", "name", "description", "buildingType", "address", "floorCount", "defaultFloorId")),
            Map.entry("FloorResponse", Set.of("id", "buildingId", "level", "displayName")),
            Map.entry("FloorMapResponse", Set.of("floor", "plan", "nodes", "edges", "rooms", "pointsOfInterest")),
            Map.entry("Plan", Set.of("url", "width", "height", "metresPerUnit")),
            Map.entry("MapNode", Set.of("id", "x", "y", "type", "name", "accessible", "blocked", "obstacle")),
            Map.entry("MapEdge", Set.of("id", "fromNodeId", "toNodeId", "connectionType", "accessible", "bidirectional",
                    "otherFloorId", "blocked", "obstacle")),
            Map.entry("ObstacleSummary", Set.of("id", "type", "description")),
            Map.entry("MapRoom", Set.of("id", "name", "category", "description", "bounds")),
            Map.entry("Bounds", Set.of("x", "y", "width", "height")),
            Map.entry("MapPointOfInterest", Set.of("id", "name", "category", "nodeId", "roomId")),
            Map.entry("PointOfInterestResponse", Set.of("id", "name", "category", "nodeId", "floorId", "floorDisplayName")),
            Map.entry("LabelledNodeResponse", Set.of("id", "label", "type", "floorId", "floorDisplayName", "x", "y")),
            Map.entry("RouteRequest", Set.of("startNodeId", "destinationNodeId", "preference")),
            Map.entry("RouteResponse", Set.of("status", "preference", "pathNodes", "distanceMeters",
                    "estimatedDurationSeconds", "floorTransitions", "instructions", "warnings", "noRoute")),
            Map.entry("PathNode", Set.of("nodeId", "floorId", "x", "y", "type", "name")),
            Map.entry("FloorTransition", Set.of("fromFloorId", "toFloorId", "connectionType", "pathIndex")),
            Map.entry("Instruction", Set.of("type", "text", "distanceMeters", "floorId", "toFloorId", "connectionType",
                    "turn", "startPathIndex", "endPathIndex")),
            Map.entry("Warning", Set.of("code", "message", "obstacleIds", "extraDistanceMeters")),
            Map.entry("NoRoute", Set.of("reasonCode", "message", "obstacles")),
            Map.entry("LoginRequest", Set.of("username", "password")),
            Map.entry("LoginResponse", Set.of("accessToken", "tokenType", "expiresInSeconds")),
            Map.entry("ObstacleResponse", Set.of("id", "buildingId", "targetType", "edgeId", "nodeId", "floorIds",
                    "targetSummary", "obstacleType", "description", "status", "createdAt", "updatedAt", "version")),
            Map.entry("ObstacleCreateRequest", Set.of("targetType", "targetId", "obstacleType", "description")),
            Map.entry("ObstacleUpdateRequest", Set.of("obstacleType", "description", "status", "version")));

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    private JsonNode document;

    @BeforeEach
    void loadGeneratedDocument() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        document = json.readTree(body);
    }

    @Test
    void exposesExactlyTheDocumentedOperations() {
        assertThat(operations()).containsExactlyInAnyOrderElementsOf(DOCUMENTED_OPERATIONS);
    }

    @Test
    void onlyObstacleOperationsRequireABearerToken() {
        for (String operation : operations()) {
            boolean isAdmin = operation.contains("/api/obstacles");
            assertThat(operation(operation).has("security")).as(operation).isEqualTo(isAdmin);
        }
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asString()).isEqualTo("bearer");
    }

    @Test
    void successStatusesMatchTheContract() {
        assertThat(successStatus("POST /api/obstacles")).isEqualTo("201");
        assertThat(successStatus("DELETE /api/obstacles/{obstacleId}")).isEqualTo("204");
        for (String operation : operations()) {
            if (!operation.equals("POST /api/obstacles") && !operation.equals("DELETE /api/obstacles/{obstacleId}")) {
                assertThat(successStatus(operation)).as(operation).isEqualTo("200");
            }
        }
    }

    @Test
    void schemasUseTheDocumentedFieldNames() {
        DOCUMENTED_FIELDS.forEach((schema, fields) ->
                assertThat(propertyNames(schema)).as(schema).containsExactlyInAnyOrderElementsOf(fields));
    }

    @Test
    void requiredRequestFieldsMatchTheContract() {
        assertThat(required("RouteRequest")).containsExactlyInAnyOrder("startNodeId", "destinationNodeId");
        assertThat(required("LoginRequest")).containsExactlyInAnyOrder("username", "password");
        assertThat(required("ObstacleCreateRequest"))
                .containsExactlyInAnyOrder("targetType", "targetId", "obstacleType", "description");
        assertThat(required("ObstacleUpdateRequest"))
                .containsExactlyInAnyOrder("obstacleType", "description", "status", "version");
    }

    @Test
    void enumerationsHaveTheDocumentedValues() {
        assertThat(enumValues("RouteRequest", "preference"))
                .containsExactly("SHORTEST", "FASTEST", "ACCESSIBLE", "AVOID_STAIRS", "PREFER_ELEVATOR");
        assertThat(enumValues("RouteResponse", "status")).containsExactly("FOUND", "NO_ROUTE");
        assertThat(enumValues("Instruction", "type")).containsExactly("START", "WALK", "FLOOR_CHANGE", "ARRIVE");
        assertThat(enumValues("Instruction", "turn")).containsExactly("LEFT", "RIGHT");
        assertThat(enumValues("Warning", "code")).containsExactly("DETOUR", "STAIRS_REQUIRED");
        assertThat(enumValues("NoRoute", "reasonCode"))
                .containsExactly("OBSTACLE", "ACCESSIBILITY_CONSTRAINT", "DISCONNECTED");
        assertThat(enumValues("ObstacleCreateRequest", "targetType")).containsExactly("EDGE", "NODE");
        assertThat(enumValues("ObstacleCreateRequest", "obstacleType")).containsExactly(
                "BLOCKED_CORRIDOR", "CLOSED_ROOM", "BROKEN_ELEVATOR", "CLOSED_STAIRCASE",
                "CONSTRUCTION_AREA", "RESTRICTED_CONNECTION");
        assertThat(enumValues("ObstacleUpdateRequest", "status")).containsExactly("ACTIVE", "RESOLVED");
    }

    @Test
    void documentMatchesTheCommittedSnapshot() throws IOException {
        JsonNode generated = withoutServers(document);
        Files.writeString(GENERATED_COPY, json.writerWithDefaultPrettyPrinter().writeValueAsString(generated) + "\n");

        assertThat(generated)
                .as("The API contract changed. If intended, update docs/API.md, then copy %s over "
                        + "backend/src/test/resources%s", GENERATED_COPY, SNAPSHOT)
                .isEqualTo(snapshot());
    }

    private List<String> operations() {
        List<String> operations = new ArrayList<>();
        document.get("paths").properties().forEach(path -> path.getValue().properties()
                .forEach(method -> operations.add(method.getKey().toUpperCase() + " " + path.getKey())));
        return operations;
    }

    private JsonNode operation(String operation) {
        String[] parts = operation.split(" ", 2);
        return document.get("paths").get(parts[1]).get(parts[0].toLowerCase());
    }

    private String successStatus(String operation) {
        return operation(operation).get("responses").propertyNames().stream()
                .filter(code -> code.startsWith("2"))
                .findFirst()
                .orElseThrow();
    }

    private JsonNode schema(String name) {
        JsonNode schema = document.at("/components/schemas/" + name);
        assertThat(schema.isMissingNode()).as("schema %s exists", name).isFalse();
        return schema;
    }

    private Set<String> propertyNames(String schema) {
        return new TreeSet<>(schema(schema).get("properties").propertyNames());
    }

    private List<String> required(String schema) {
        List<String> fields = new ArrayList<>();
        schema(schema).path("required").forEach(field -> fields.add(field.asString()));
        return fields;
    }

    private List<String> enumValues(String schema, String property) {
        JsonNode definition = schema(schema).get("properties").get(property);
        List<String> values = new ArrayList<>();
        definition.path("enum").forEach(value -> values.add(value.asString()));
        return values;
    }

    /** The server URL depends on how the document was requested, so it is not part of the contract. */
    private static JsonNode withoutServers(JsonNode document) {
        ObjectNode copy = (ObjectNode) document.deepCopy();
        copy.remove("servers");
        return copy;
    }

    private JsonNode snapshot() throws IOException {
        try (InputStream stream = OpenApiContractTest.class.getResourceAsStream(SNAPSHOT)) {
            assertThat(stream).as("snapshot %s exists", SNAPSHOT).isNotNull();
            return json.readTree(stream);
        }
    }
}
