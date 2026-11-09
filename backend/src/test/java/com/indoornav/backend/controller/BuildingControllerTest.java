package com.indoornav.backend.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indoornav.backend.dto.BuildingDetailResponse;
import com.indoornav.backend.dto.BuildingSummaryResponse;
import com.indoornav.backend.dto.FloorResponse;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.security.RequestIdFilter;
import com.indoornav.backend.service.BuildingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BuildingController.class)
@Import(WebTestConfig.class)
class BuildingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuildingService buildingService;

    @Test
    void listIsPublicAndCarriesTheStandardHeaders() throws Exception {
        when(buildingService.listBuildings()).thenReturn(List.of(
                new BuildingSummaryResponse(1L, "Sample", null, "Test environment", null, 3)));

        mockMvc.perform(get("/api/buildings"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists(RequestIdFilter.HEADER))
                .andExpect(jsonPath("$[0].name").value("Sample"))
                .andExpect(jsonPath("$[0].floorCount").value(3));
    }

    @Test
    void nullableFieldsArePresentAsNull() throws Exception {
        when(buildingService.listBuildings()).thenReturn(List.of(
                new BuildingSummaryResponse(1L, "Sample", null, "Test environment", null, 3)));

        mockMvc.perform(get("/api/buildings"))
                .andExpect(jsonPath("$[0].description").value(nullValue()))
                .andExpect(jsonPath("$[0].address").hasJsonPath());
    }

    @Test
    void clientSuppliedRequestIdIsIgnored() throws Exception {
        when(buildingService.listBuildings()).thenReturn(List.of());

        mockMvc.perform(get("/api/buildings").header(RequestIdFilter.HEADER, "client-chosen"))
                .andExpect(header().string(RequestIdFilter.HEADER, not("client-chosen")));
    }

    @Test
    void detailIncludesDefaultFloor() throws Exception {
        when(buildingService.getBuilding(1L)).thenReturn(
                new BuildingDetailResponse(1L, "Sample", null, "Test environment", null, 3, 11L));

        mockMvc.perform(get("/api/buildings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultFloorId").value(11));
    }

    @Test
    void unknownBuildingReturnsTheStandardErrorBody() throws Exception {
        when(buildingService.getBuilding(99L)).thenThrow(new ResourceNotFoundException("Building"));

        mockMvc.perform(get("/api/buildings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Building not found."))
                .andExpect(jsonPath("$.path").value("/api/buildings/99"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void nonPositiveIdIsAValidationFailure() throws Exception {
        mockMvc.perform(get("/api/buildings/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("buildingId"));
        verifyNoInteractions(buildingService);
    }

    @Test
    void nonNumericIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/buildings/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void wrongMethodOnAPublicPathReturns405() throws Exception {
        mockMvc.perform(post("/api/buildings"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void floorsAreReturnedForABuilding() throws Exception {
        when(buildingService.listFloors(1L)).thenReturn(List.of(
                new FloorResponse(10L, 1L, 0, "G"), new FloorResponse(11L, 1L, 1, "Floor 1")));

        mockMvc.perform(get("/api/buildings/1/floors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].displayName", containsInAnyOrder("G", "Floor 1")));
    }

    @Test
    void nodesUseDefaultLimitOfFifty() throws Exception {
        when(buildingService.listLabelledNodes(anyLong(), isNull(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/buildings/1/nodes")).andExpect(status().isOk());

        verify(buildingService).listLabelledNodes(eq(1L), isNull(), eq(50));
    }

    @Test
    void nodesRejectLimitAboveOneHundred() throws Exception {
        mockMvc.perform(get("/api/buildings/1/nodes").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("limit"));
    }

    @Test
    void unlistedPathIsDenied() throws Exception {
        mockMvc.perform(get("/api/not-a-documented-endpoint"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
