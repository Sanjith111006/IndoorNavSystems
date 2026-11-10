package com.indoornav.engine.model;

/**
 * The roles a navigation node can have in an indoor graph.
 */
public enum NodeType {
    CORRIDOR_JUNCTION,
    ROOM_ENTRANCE,
    STAIR_LANDING,
    ELEVATOR_ENTRANCE,
    BUILDING_ENTRANCE,
    FLOOR_TRANSITION,
    POINT_OF_INTEREST
}
