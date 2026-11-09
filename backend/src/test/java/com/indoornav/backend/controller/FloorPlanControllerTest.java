package com.indoornav.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.service.FloorPlanService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FloorPlanController.class)
@Import(WebTestConfig.class)
class FloorPlanControllerTest {

    private static final String ENTITY_TAG = "\"abc123\"";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FloorPlanService floorPlanService;

    @Test
    void planIsServedAsSvgWithCachingAndNoScriptsPolicy() throws Exception {
        when(floorPlanService.load("sample/level-0.svg")).thenReturn(plan());

        mockMvc.perform(get("/assets/floorplans/sample/level-0.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(header().string("Cache-Control", "max-age=3600"))
                .andExpect(header().string("ETag", ENTITY_TAG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")));
    }

    @Test
    void matchingEntityTagReturns304() throws Exception {
        when(floorPlanService.load("sample/level-0.svg")).thenReturn(plan());

        mockMvc.perform(get("/assets/floorplans/sample/level-0.svg").header("If-None-Match", ENTITY_TAG))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }

    @Test
    void missingPlanReturns404() throws Exception {
        when(floorPlanService.load("sample/none.svg")).thenThrow(new ResourceNotFoundException("Floor plan"));

        mockMvc.perform(get("/assets/floorplans/sample/none.svg"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private static FloorPlanService.FloorPlan plan() {
        return new FloorPlanService.FloorPlan("<svg/>".getBytes(StandardCharsets.UTF_8), ENTITY_TAG);
    }
}
