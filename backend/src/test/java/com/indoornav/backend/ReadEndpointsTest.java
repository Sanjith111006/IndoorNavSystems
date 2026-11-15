package com.indoornav.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The read endpoints' queries against real PostgreSQL and the seed: search matching, ordering
 * and wildcard escaping, labels, default floors and floor maps. Mocked unit tests cannot show
 * that the SQL itself behaves.
 *
 * <p>IDs and names come from {@code database/seed}; nothing here writes data.
 */
@SpringBootTest(properties = {"app.seed-demo-data=true", "springdoc.api-docs.enabled=false"})
@AutoConfigureMockMvc
@Testcontainers
class ReadEndpointsTest {

    private static final long MALL = 1L;
    private static final long LIBRARY = 2L;
    private static final long MALL_GROUND = 100L;
    private static final long MALL_LEVEL_ONE = 101L;

    @Container
    static final PostgreSQLContainer POSTGRES = TestDatabase.newContainer();

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        TestDatabase.register(registry, POSTGRES);
    }

    @Test
    void buildingsAreOrderedByNameWithTheirFloorCounts() throws Exception {
        JsonNode buildings = read("/api/buildings");

        assertThat(texts(buildings, "name")).containsExactly("Sample Library", "Sample Mall");
        assertThat(buildings.get(0).get("floorCount").asLong()).isEqualTo(1);
        assertThat(buildings.get(1).get("floorCount").asLong()).isEqualTo(3);
    }

    @Test
    void defaultFloorIsTheLowestFloorWithAnEntrance() throws Exception {
        assertThat(read("/api/buildings/" + MALL).get("defaultFloorId").asLong()).isEqualTo(MALL_GROUND);
        assertThat(read("/api/buildings/" + LIBRARY).get("defaultFloorId").asLong()).isEqualTo(200L);
    }

    @Test
    void floorsAreOrderedByLevel() throws Exception {
        assertThat(texts(read("/api/buildings/" + MALL + "/floors"), "displayName"))
                .containsExactly("Ground", "Level 1", "Level 2");
    }

    @Test
    void searchIsCaseInsensitiveAndPutsPrefixMatchesFirst() throws Exception {
        assertThat(searchNames("view")).containsExactly("Viewing lounge", "Family viewing");
        assertThat(searchNames("VIEW")).containsExactly("Viewing lounge", "Family viewing");
    }

    @Test
    void sameNameOnSeveralFloorsIsDistinguishedByFloor() throws Exception {
        JsonNode results = search("restrooms");

        assertThat(texts(results, "name")).containsOnly("North restrooms").hasSize(3);
        assertThat(texts(results, "floorDisplayName")).containsExactly("Ground", "Level 1", "Level 2");
    }

    @Test
    void wildcardCharactersInSearchTextAreLiteral() throws Exception {
        // Unescaped, "%" and "_" would match every name and "\" would break the query.
        assertThat(searchNames("%")).isEmpty();
        assertThat(searchNames("_")).isEmpty();
        assertThat(searchNames("\\")).isEmpty();
        assertThat(searchNames("west_cafe")).isEmpty();
        assertThat(searchNames("west cafe")).containsExactly("West cafe");
    }

    @Test
    void categoryFilterAndLimitAreApplied() throws Exception {
        assertThat(read("/api/points-of-interest?buildingId=" + MALL + "&category=restroom")).hasSize(3);
        assertThat(read("/api/points-of-interest?buildingId=" + MALL + "&query=north&category=food")).isEmpty();
        assertThat(read("/api/points-of-interest?buildingId=" + MALL + "&limit=2")).hasSize(2);
    }

    @Test
    void browsingWithoutTextListsEveryDestinationByName() throws Exception {
        List<String> names = texts(read("/api/points-of-interest?buildingId=" + MALL + "&limit=50"), "name");

        assertThat(names).hasSize(30).isSortedAccordingTo(String::compareTo);
    }

    @Test
    void searchResultsCarryTheNodeToRouteTo() throws Exception {
        JsonNode cafe = search("west cafe").get(0);

        assertThat(cafe.get("nodeId").asLong()).isEqualTo(1003L);
        assertThat(cafe.get("floorId").asLong()).isEqualTo(MALL_GROUND);
    }

    @Test
    void categoriesAreDistinctAndSorted() throws Exception {
        List<String> categories = new ArrayList<>();
        read("/api/points-of-interest/categories?buildingId=" + MALL).forEach(node -> categories.add(node.asString()));

        assertThat(categories).doesNotHaveDuplicates().isSortedAccordingTo(String::compareTo).contains("restroom", "food");
    }

    @Test
    void startNodesAreLabelledFromDisplayNamesOrTheirDestination() throws Exception {
        assertThat(texts(read("/api/buildings/" + MALL + "/nodes?query=entrance"), "label"))
                .containsExactly("East entrance", "West entrance");

        JsonNode quietCorner = read("/api/buildings/" + MALL + "/nodes?query=quiet").get(0);
        assertThat(quietCorner.get("id").asLong()).isEqualTo(1020L);
        assertThat(quietCorner.get("label").asString()).isEqualTo("Quiet corner");
    }

    @Test
    void startNodeListSkipsUnlabelledNodesAndIsSorted() throws Exception {
        List<String> labels = texts(read("/api/buildings/" + MALL + "/nodes?limit=100"), "label");

        // 30 POI nodes plus 8 named entrances, stairs and elevators; plain junctions are left out.
        assertThat(labels).hasSize(38).doesNotContainNull().isSortedAccordingTo(String::compareTo);
    }

    @Test
    void floorMapHasTheFloorsNodesAndTheVerticalConnectionsTouchingIt() throws Exception {
        JsonNode map = read("/api/floors/" + MALL_GROUND + "/map");

        assertThat(map.get("nodes")).hasSize(20);
        assertThat(map.get("rooms")).hasSize(10);
        assertThat(map.get("pointsOfInterest")).hasSize(10);
        assertThat(map.get("plan").get("url").asString()).isEqualTo("/assets/floorplans/sample/ground.svg");
        JsonNode stairs = edge(map, 2001L);
        assertThat(stairs.get("connectionType").asString()).isEqualTo("STAIRS");
        assertThat(stairs.get("otherFloorId").asLong()).isEqualTo(MALL_LEVEL_ONE);
        assertThat(edge(map, 2101L).get("otherFloorId").isNull()).isTrue();
    }

    @Test
    void middleFloorMapShowsConnectionsToBothNeighbouringFloors() throws Exception {
        JsonNode map = read("/api/floors/" + MALL_LEVEL_ONE + "/map");

        assertThat(edge(map, 2002L).get("otherFloorId").asLong()).isEqualTo(MALL_GROUND);
        assertThat(edge(map, 2004L).get("otherFloorId").asLong()).isEqualTo(102L);
    }

    @Test
    void packagedFloorPlanIsServed() throws Exception {
        String svg = mockMvc.perform(get("/assets/floorplans/sample/ground.svg"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(svg).contains("<svg");
    }

    @Test
    void unknownBuildingIsNotFoundButAnEmptySearchIsNot() throws Exception {
        mockMvc.perform(get("/api/points-of-interest?buildingId=999")).andExpect(status().isNotFound());
        assertThat(searchNames("no such destination")).isEmpty();
    }

    private List<String> searchNames(String query) throws Exception {
        return texts(search(query), "name");
    }

    /** Sends the text as a request parameter, so characters such as % reach the server unchanged. */
    private JsonNode search(String query) throws Exception {
        String body = mockMvc.perform(get("/api/points-of-interest")
                        .param("buildingId", String.valueOf(MALL))
                        .param("query", query))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private JsonNode read(String path) throws Exception {
        String body = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private static JsonNode edge(JsonNode map, long edgeId) {
        for (JsonNode edge : map.get("edges")) {
            if (edge.get("id").asLong() == edgeId) {
                return edge;
            }
        }
        throw new AssertionError("Edge " + edgeId + " is not on this floor map");
    }

    private static List<String> texts(JsonNode items, String field) {
        List<String> values = new ArrayList<>();
        items.forEach(item -> values.add(item.get(field).asString()));
        return values;
    }
}
