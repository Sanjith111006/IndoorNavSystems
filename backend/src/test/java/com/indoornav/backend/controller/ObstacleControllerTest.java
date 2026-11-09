package com.indoornav.backend.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indoornav.backend.dto.ObstacleCreateRequest;
import com.indoornav.backend.dto.ObstacleResponse;
import com.indoornav.backend.dto.ObstacleTargetType;
import com.indoornav.backend.dto.ObstacleUpdateRequest;
import com.indoornav.backend.entity.ObstacleStatus;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.backend.exception.ConflictException;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.security.TokenService;
import com.indoornav.backend.service.ObstacleService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ObstacleController.class)
@Import(WebTestConfig.class)
class ObstacleControllerTest {

    private static final String VALID_CREATE = """
            {"targetType":"EDGE","targetId":7,"obstacleType":"BLOCKED_CORRIDOR","description":"Closed for cleaning"}
            """;
    private static final String VALID_UPDATE = """
            {"obstacleType":"BLOCKED_CORRIDOR","description":"Still closed","status":"RESOLVED","version":2}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private ObstacleService obstacleService;

    @Test
    void everyEndpointRejectsAMissingToken() throws Exception {
        mockMvc.perform(get("/api/obstacles").param("buildingId", "1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/obstacles").contentType(MediaType.APPLICATION_JSON).content(VALID_CREATE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/obstacles/1").contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/obstacles/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(obstacleService);
    }

    @Test
    void unauthorizedResponseUsesTheStandardBodyAndNamesBearer() throws Exception {
        mockMvc.perform(get("/api/obstacles").param("buildingId", "1"))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/obstacles"));
    }

    @Test
    void invalidTokenLooksTheSameAsAMissingOne() throws Exception {
        mockMvc.perform(get("/api/obstacles").param("buildingId", "1").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void listWithTokenPassesTheOptionalStatusFilter() throws Exception {
        when(obstacleService.list(1L, ObstacleStatus.ACTIVE)).thenReturn(List.of(resource(4L, 2L)));

        mockMvc.perform(asAdmin(get("/api/obstacles").param("buildingId", "1").param("status", "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetType").value("EDGE"))
                .andExpect(jsonPath("$[0].nodeId").value(nullValue()))
                .andExpect(jsonPath("$[0].version").value(2));
    }

    @Test
    void listRejectsAnUnknownStatus() throws Exception {
        mockMvc.perform(asAdmin(get("/api/obstacles").param("buildingId", "1").param("status", "CLOSED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(obstacleService.create(any(ObstacleCreateRequest.class))).thenReturn(resource(4L, 0L));

        mockMvc.perform(asAdmin(post("/api/obstacles").contentType(MediaType.APPLICATION_JSON).content(VALID_CREATE)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/obstacles/4"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createTrimsTheDescription() throws Exception {
        when(obstacleService.create(any(ObstacleCreateRequest.class))).thenReturn(resource(4L, 0L));
        String body = """
                {"targetType":"NODE","targetId":7,"obstacleType":"CLOSED_ROOM","description":"  Closed  "}
                """;

        mockMvc.perform(asAdmin(post("/api/obstacles").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isCreated());

        verify(obstacleService).create(argThat(request -> request.description().equals("Closed")));
    }

    @Test
    void descriptionTooShortAfterTrimmingIsRejected() throws Exception {
        String body = """
                {"targetType":"EDGE","targetId":7,"obstacleType":"BLOCKED_CORRIDOR","description":"  ab  "}
                """;

        mockMvc.perform(asAdmin(post("/api/obstacles").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("description"));
    }

    @Test
    void unknownEnumValueIsMalformed() throws Exception {
        String body = """
                {"targetType":"EDGE","targetId":7,"obstacleType":"ALIEN_INVASION","description":"Closed"}
                """;

        mockMvc.perform(asAdmin(post("/api/obstacles").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void invalidJsonIsMalformed() throws Exception {
        mockMvc.perform(asAdmin(post("/api/obstacles").contentType(MediaType.APPLICATION_JSON).content("{not json")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void nonJsonBodyReturns415() throws Exception {
        mockMvc.perform(asAdmin(post("/api/obstacles").contentType(MediaType.TEXT_PLAIN).content("hello")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unknownRequestFieldsAreIgnored() throws Exception {
        when(obstacleService.create(any(ObstacleCreateRequest.class))).thenReturn(resource(4L, 0L));
        String body = """
                {"targetType":"EDGE","targetId":7,"obstacleType":"BLOCKED_CORRIDOR","description":"Closed",
                 "buildingId":999,"status":"RESOLVED"}
                """;

        mockMvc.perform(asAdmin(post("/api/obstacles").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void duplicateActiveObstacleReturns409() throws Exception {
        when(obstacleService.create(any(ObstacleCreateRequest.class)))
                .thenThrow(new ConflictException("An active obstacle already exists on this target."));

        mockMvc.perform(asAdmin(post("/api/obstacles").contentType(MediaType.APPLICATION_JSON).content(VALID_CREATE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void updateReturnsTheNewVersion() throws Exception {
        when(obstacleService.update(eq(4L), any(ObstacleUpdateRequest.class))).thenReturn(resource(4L, 3L));

        mockMvc.perform(asAdmin(put("/api/obstacles/4").contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void updateRequiresTheVersion() throws Exception {
        String body = """
                {"obstacleType":"BLOCKED_CORRIDOR","description":"Still closed","status":"RESOLVED"}
                """;

        mockMvc.perform(asAdmin(put("/api/obstacles/4").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("version"));
    }

    @Test
    void deleteReturns204WithNoBody() throws Exception {
        mockMvc.perform(asAdmin(delete("/api/obstacles/4")))
                .andExpect(status().isNoContent());

        verify(obstacleService).delete(4L);
    }

    @Test
    void deletingAnUnknownObstacleReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Obstacle")).when(obstacleService).delete(99L);

        mockMvc.perform(asAdmin(delete("/api/obstacles/99")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + tokenService.issueAdminToken("admin").value());
    }

    private static ObstacleResponse resource(long id, long version) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ObstacleResponse(
                id, 1L, ObstacleTargetType.EDGE, 7L, null, List.of(10L), "A to B",
                ObstacleType.BLOCKED_CORRIDOR, "Closed", ObstacleStatus.ACTIVE, now, now, version);
    }
}
