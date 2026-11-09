package com.indoornav.backend.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises Flyway, Hibernate validation, seed data and database constraints against PostgreSQL.
 *
 * <p>A clean container is used for the class, so application startup proves that the complete
 * migration chain applies and that Hibernate's {@code validate} mode agrees with the schema.
 */
@SpringBootTest(properties = {"app.seed-demo-data=true", "springdoc.api-docs.enabled=false"})
@Testcontainers
class DatabaseIntegrityTest {

    private static final long SAMPLE_MALL_ID = 1L;
    private static final long SAMPLE_LIBRARY_ID = 2L;
    private static final long LEAF_DESTINATION_NODE_ID = 1020L;
    private static final long GROUND_TO_LEVEL_ONE_ELEVATOR_ID = 2002L;
    private static final long LEVEL_ONE_TO_LEVEL_TWO_ELEVATOR_ID = 2004L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("indoornav")
            .withUsername("test")
            .withPassword("test-password");

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

    @Test
    void appliesEveryVersionedMigrationAndBothRepeatableSeeds() {
        List<String> migrations = jdbcTemplate.queryForList(
                "select description from flyway_schema_history where success = true order by installed_rank",
                String.class);

        assertThat(migrations)
                .contains(
                        "create building floor and connection type tables",
                        "create graph room and point of interest tables",
                        "create obstacles table",
                        "insert connection type reference data",
                        "grant runtime database privileges",
                        "tune connection type reference data",
                        "sample mall demo data",
                        "sample library demo data");
    }

    @Test
    void loadsBothSeededEnvironmentsWithExpectedGraphData() {
        assertThat(count("buildings")).isEqualTo(2);
        assertThat(count("floors")).isEqualTo(4);
        assertThat(count("nodes")).isEqualTo(68);
        assertThat(count("edges")).isEqualTo(90);
        assertThat(count("rooms")).isEqualTo(34);
        assertThat(count("points_of_interest")).isEqualTo(34);
        assertThat(count("obstacles")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from floors where building_id in (?, ?)",
                        Long.class,
                        SAMPLE_MALL_ID,
                        SAMPLE_LIBRARY_ID))
                .isEqualTo(4L);
    }

    @Test
    void seedDataMaintainsGraphAndFloorPlanInvariants() {
        assertThat(count("""
                nodes n
                join floors f on f.id = n.floor_id
                where n.x > f.plan_width or n.y > f.plan_height
                """))
                .isZero();
        assertThat(count("""
                rooms r
                join floors f on f.id = r.floor_id
                where r.bounds_x + r.bounds_width > f.plan_width
                   or r.bounds_y + r.bounds_height > f.plan_height
                """))
                .isZero();
        assertThat(count("""
                points_of_interest p
                join nodes n on n.id = p.node_id
                join rooms r on r.id = p.room_id
                where p.room_id is not null and n.floor_id <> r.floor_id
                """))
                .isZero();
        assertThat(count("""
                edges e
                join nodes source on source.id = e.from_node_id
                join nodes target on target.id = e.to_node_id
                join floors f on f.id = source.floor_id
                where e.distance_m < sqrt(power(source.x - target.x, 2) + power(source.y - target.y, 2))
                    * f.metres_per_unit
                """))
                .isZero();
        assertThat(count("""
                edges e
                join nodes source on source.id = e.from_node_id
                join nodes target on target.id = e.to_node_id
                where source.floor_id <> target.floor_id
                  and (e.connection_type_code not in ('STAIRS', 'ELEVATOR') or e.distance_m is null)
                """))
                .isZero();
    }

    @Test
    void seedGraphContainsTheStructuresForAllFiveDemonstrationScenarios() {
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from edges where from_node_id = ? or to_node_id = ?",
                        Long.class,
                        LEAF_DESTINATION_NODE_ID,
                        LEAF_DESTINATION_NODE_ID))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                        "select accessible from edges where id = ?", Boolean.class, 2001L))
                .isFalse();
        assertThat(jdbcTemplate.queryForObject(
                        "select accessible from edges where id = ?", Boolean.class, 2003L))
                .isFalse();
        assertThat(jdbcTemplate.queryForObject(
                        "select accessible from edges where id = ?",
                        Boolean.class,
                        GROUND_TO_LEVEL_ONE_ELEVATOR_ID))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                        "select accessible from edges where id = ?",
                        Boolean.class,
                        LEVEL_ONE_TO_LEVEL_TWO_ELEVATOR_ID))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from edges where id in (2121, 2122, 2123, 2125, 2126)",
                        Long.class))
                .isEqualTo(5L);
    }

    @Test
    void rejectsAnEdgeWhoseNodesDoNotBelongToItsBuilding() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        insert into edges (building_id, from_node_id, to_node_id, connection_type_code,
                                           distance_m, accessible, bidirectional)
                        values (?, ?, ?, 'CORRIDOR', 1.00, true, true)
                        """,
                        SAMPLE_LIBRARY_ID,
                        1001L,
                        1301L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAnObstacleWithoutExactlyOneTarget() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        insert into obstacles (building_id, obstacle_type, description)
                        values (?, 'CONSTRUCTION_AREA', 'Work zone')
                        """,
                        SAMPLE_MALL_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void rejectsTwoActiveObstaclesForTheSameConnection() {
        jdbcTemplate.update(
                """
                insert into obstacles (building_id, edge_id, obstacle_type, description)
                values (?, 2101, 'BLOCKED_CORRIDOR', 'Temporary closure')
                """,
                SAMPLE_MALL_ID);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        insert into obstacles (building_id, edge_id, obstacle_type, description)
                        values (?, 2101, 'CONSTRUCTION_AREA', 'Work zone')
                        """,
                        SAMPLE_MALL_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long count(String fromClause) {
        Long value = jdbcTemplate.queryForObject("select count(*) from " + fromClause, Long.class);
        return value == null ? 0L : value;
    }
}
