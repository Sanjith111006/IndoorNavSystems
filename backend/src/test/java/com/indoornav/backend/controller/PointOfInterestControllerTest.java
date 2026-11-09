package com.indoornav.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indoornav.backend.dto.PointOfInterestResponse;
import com.indoornav.backend.service.DestinationSearchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PointOfInterestController.class)
@Import(WebTestConfig.class)
class PointOfInterestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DestinationSearchService destinationSearchService;

    @Test
    void searchReturnsTheNodeIdNeededForRouting() throws Exception {
        when(destinationSearchService.search(1L, "desk", null, 30)).thenReturn(List.of(
                new PointOfInterestResponse(5L, "Help desk", "information", 42L, 10L, "G")));

        mockMvc.perform(get("/api/points-of-interest").param("buildingId", "1").param("query", "desk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nodeId").value(42))
                .andExpect(jsonPath("$[0].floorDisplayName").value("G"));
    }

    @Test
    void defaultLimitIsThirty() throws Exception {
        mockMvc.perform(get("/api/points-of-interest").param("buildingId", "1")).andExpect(status().isOk());

        verify(destinationSearchService).search(eq(1L), isNull(), isNull(), eq(30));
    }

    @Test
    void missingBuildingIdIsRejected() throws Exception {
        mockMvc.perform(get("/api/points-of-interest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("buildingId"));
    }

    @Test
    void limitAboveFiftyIsRejected() throws Exception {
        mockMvc.perform(get("/api/points-of-interest").param("buildingId", "1").param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("limit"));
    }

    @Test
    void categoryMustBeASlug() throws Exception {
        mockMvc.perform(get("/api/points-of-interest").param("buildingId", "1").param("category", "Not A Slug"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("category"));
    }

    @Test
    void categoriesAreAPlainList() throws Exception {
        when(destinationSearchService.listCategories(1L)).thenReturn(List.of("information", "restroom"));

        mockMvc.perform(get("/api/points-of-interest/categories").param("buildingId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("information"))
                .andExpect(jsonPath("$[1]").value("restroom"));
    }
}
