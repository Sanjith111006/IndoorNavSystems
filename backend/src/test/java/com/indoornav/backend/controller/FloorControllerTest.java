package com.indoornav.backend.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indoornav.backend.dto.FloorMapResponse;
import com.indoornav.backend.dto.FloorResponse;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.service.FloorMapService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FloorController.class)
@Import(WebTestConfig.class)
class FloorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FloorMapService floorMapService;

    @Test
    void obstacleIsOmittedWhenOpenButNullableFloorIsPresent() throws Exception {
        FloorMapResponse.MapEdge openEdge =
                new FloorMapResponse.MapEdge(7L, 1L, 2L, "CORRIDOR", true, true, null, false, null);
        when(floorMapService.getFloorMap(10L)).thenReturn(mapWith(openEdge));

        mockMvc.perform(get("/api/floors/10/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.url").value("/assets/floorplans/a.svg"))
                .andExpect(jsonPath("$.edges[0].blocked").value(false))
                .andExpect(jsonPath("$.edges[0].otherFloorId").value(nullValue()))
                .andExpect(jsonPath("$.edges[0].obstacle").doesNotExist());
    }

    @Test
    void blockedEdgeExposesOnlyTheVisitorFacingObstacleFields() throws Exception {
        FloorMapResponse.ObstacleSummary obstacle = new FloorMapResponse.ObstacleSummary(
                3L, ObstacleType.BLOCKED_CORRIDOR, "Closed for cleaning");
        FloorMapResponse.MapEdge blockedEdge =
                new FloorMapResponse.MapEdge(7L, 1L, 2L, "CORRIDOR", true, true, null, true, obstacle);
        when(floorMapService.getFloorMap(10L)).thenReturn(mapWith(blockedEdge));

        mockMvc.perform(get("/api/floors/10/map"))
                .andExpect(jsonPath("$.edges[0].blocked").value(true))
                .andExpect(jsonPath("$.edges[0].obstacle.type").value("BLOCKED_CORRIDOR"))
                .andExpect(jsonPath("$.edges[0].obstacle.description").value("Closed for cleaning"))
                .andExpect(jsonPath("$.edges[0].obstacle.version").doesNotExist())
                .andExpect(jsonPath("$.edges[0].obstacle.createdAt").doesNotExist());
    }

    @Test
    void unknownFloorReturns404() throws Exception {
        when(floorMapService.getFloorMap(99L)).thenThrow(new ResourceNotFoundException("Floor"));

        mockMvc.perform(get("/api/floors/99/map"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private static FloorMapResponse mapWith(FloorMapResponse.MapEdge edge) {
        FloorMapResponse.Plan plan = new FloorMapResponse.Plan(
                "/assets/floorplans/a.svg", new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("0.1"));
        return new FloorMapResponse(
                new FloorResponse(10L, 1L, 0, "G"), plan, List.of(), List.of(edge), List.of(), List.of());
    }
}
