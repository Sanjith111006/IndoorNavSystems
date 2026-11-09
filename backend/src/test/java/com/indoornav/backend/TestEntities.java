package com.indoornav.backend;

import com.indoornav.backend.entity.Building;
import com.indoornav.backend.entity.Edge;
import com.indoornav.backend.entity.Floor;
import com.indoornav.backend.entity.Node;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.entity.PointOfInterest;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import org.springframework.test.util.ReflectionTestUtils;

/** Builds graph entities for tests. They have no public constructors because they are read-only. */
public final class TestEntities {

    public static final long BUILDING_ID = 1L;

    private TestEntities() {
    }

    public static Building building(long id, String name) {
        Building building = instantiate(Building.class);
        set(building, "id", id);
        set(building, "name", name);
        set(building, "buildingType", "Test environment");
        return building;
    }

    public static Floor floor(long id, int level) {
        Floor floor = instantiate(Floor.class);
        set(floor, "id", id);
        set(floor, "buildingId", BUILDING_ID);
        set(floor, "level", level);
        set(floor, "displayName", "Level " + level);
        set(floor, "planAssetPath", "test/level-" + level + ".svg");
        set(floor, "planWidth", new BigDecimal("1000.00"));
        set(floor, "planHeight", new BigDecimal("800.00"));
        set(floor, "metresPerUnit", new BigDecimal("0.100000"));
        return floor;
    }

    public static Node node(long id, Floor floor, NodeType type, String displayName) {
        Node node = instantiate(Node.class);
        set(node, "id", id);
        set(node, "buildingId", BUILDING_ID);
        set(node, "floor", floor);
        set(node, "x", new BigDecimal("10.00"));
        set(node, "y", new BigDecimal("20.00"));
        set(node, "type", type);
        set(node, "displayName", displayName);
        set(node, "accessible", true);
        return node;
    }

    public static Edge edge(long id, Node from, Node to, String connectionTypeCode) {
        Edge edge = instantiate(Edge.class);
        set(edge, "id", id);
        set(edge, "buildingId", BUILDING_ID);
        set(edge, "fromNode", from);
        set(edge, "toNode", to);
        set(edge, "connectionTypeCode", connectionTypeCode);
        set(edge, "distanceMeters", new BigDecimal("5.00"));
        set(edge, "accessible", true);
        set(edge, "bidirectional", true);
        return edge;
    }

    public static PointOfInterest pointOfInterest(long id, Node node, String name, String category) {
        PointOfInterest point = instantiate(PointOfInterest.class);
        set(point, "id", id);
        set(point, "node", node);
        set(point, "name", name);
        set(point, "category", category);
        return point;
    }

    public static void set(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
