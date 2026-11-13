package com.indoornav.backend.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indoornav.backend.dto.RouteRequest;
import com.indoornav.backend.dto.RouteResponse;
import com.indoornav.backend.entity.NodeType;
import com.indoornav.backend.entity.ObstacleType;
import com.indoornav.backend.exception.NodesNotInSameBuildingException;
import com.indoornav.backend.exception.ResourceNotFoundException;
import com.indoornav.backend.exception.RouteTimeoutException;
import com.indoornav.backend.service.RouteService;
import com.indoornav.engine.model.RoutePreference;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(RouteController.class)
@Import(WebTestConfig.class)
class RouteControllerTest {

    private static final String PATH = "/api/navigation/route";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouteService routeService;

    @Test
    void foundRouteHasTheDocumentedShapeAndOmitsInapplicableFields() throws Exception {
        when(routeService.route(any(RouteRequest.class))).thenReturn(foundRoute());

        send("{\"startNodeId\":1,\"destinationNodeId\":2,\"preference\":\"FASTEST\"}")
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("FOUND"))
                .andExpect(jsonPath("$.preference").value("FASTEST"))
                .andExpect(jsonPath("$.pathNodes[0].nodeId").value(1))
                .andExpect(jsonPath("$.pathNodes[0].name").value("Entrance"))
                .andExpect(jsonPath("$.pathNodes[1].name").value(nullValue()))
                .andExpect(jsonPath("$.distanceMeters").value(12.5))
                .andExpect(jsonPath("$.estimatedDurationSeconds").value(11))
                .andExpect(jsonPath("$.instructions[0].type").value("WALK"))
                .andExpect(jsonPath("$.instructions[0].toFloorId").doesNotExist())
                .andExpect(jsonPath("$.instructions[0].turn").doesNotExist())
                .andExpect(jsonPath("$.instructions[1].turn").value("LEFT"))
                .andExpect(jsonPath("$.warnings[0].code").value("DETOUR"))
                .andExpect(jsonPath("$.warnings[0].extraDistanceMeters").value(nullValue()))
                .andExpect(jsonPath("$.noRoute").doesNotExist());
    }

    @Test
    void noRouteIsA200WithAReasonAndNoPath() throws Exception {
        RouteResponse.NoRoute noRoute = new RouteResponse.NoRoute(
                RouteResponse.NoRouteReason.OBSTACLE,
                "Part of the way there is closed and there is no other route.",
                List.of(new RouteResponse.ObstacleSummary(5L, ObstacleType.BROKEN_ELEVATOR, "Out of service")));
        when(routeService.route(any(RouteRequest.class)))
                .thenReturn(RouteResponse.noRoute(RoutePreference.ACCESSIBLE, noRoute));

        send("{\"startNodeId\":1,\"destinationNodeId\":2,\"preference\":\"ACCESSIBLE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_ROUTE"))
                .andExpect(jsonPath("$.noRoute.reasonCode").value("OBSTACLE"))
                .andExpect(jsonPath("$.noRoute.obstacles[0].type").value("BROKEN_ELEVATOR"))
                .andExpect(jsonPath("$.pathNodes").doesNotExist())
                .andExpect(jsonPath("$.warnings").doesNotExist());
    }

    @Test
    void omittedPreferenceReachesTheServiceAsShortest() throws Exception {
        when(routeService.route(any(RouteRequest.class)))
                .thenReturn(RouteResponse.noRoute(RoutePreference.SHORTEST, null));

        send("{\"startNodeId\":1,\"destinationNodeId\":2}").andExpect(status().isOk());

        verify(routeService).route(argThat(request -> request.preference() == RoutePreference.SHORTEST));
    }

    @Test
    void routeIsPublic() throws Exception {
        when(routeService.route(any(RouteRequest.class)))
                .thenReturn(RouteResponse.noRoute(RoutePreference.SHORTEST, null));

        send("{\"startNodeId\":1,\"destinationNodeId\":2}").andExpect(status().isOk());
    }

    @Test
    void missingAndNonPositiveIdsAreValidationFailures() throws Exception {
        send("{\"destinationNodeId\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(2));
        verifyNoInteractions(routeService);
    }

    @Test
    void unknownPreferenceIsMalformed() throws Exception {
        send("{\"startNodeId\":1,\"destinationNodeId\":2,\"preference\":\"SCENIC\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void nodesInDifferentBuildingsHaveTheirOwnCode() throws Exception {
        when(routeService.route(any(RouteRequest.class))).thenThrow(new NodesNotInSameBuildingException());

        send("{\"startNodeId\":1,\"destinationNodeId\":2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NODES_NOT_IN_SAME_BUILDING"));
    }

    @Test
    void unknownNodeReturns404() throws Exception {
        when(routeService.route(any(RouteRequest.class))).thenThrow(new ResourceNotFoundException("Node"));

        send("{\"startNodeId\":1,\"destinationNodeId\":2}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void timedOutCalculationReturns503() throws Exception {
        when(routeService.route(any(RouteRequest.class))).thenThrow(new RouteTimeoutException());

        send("{\"startNodeId\":1,\"destinationNodeId\":2}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void getIsNotAllowed() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isMethodNotAllowed());
    }

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static RouteResponse foundRoute() {
        List<RouteResponse.PathNode> path = List.of(
                new RouteResponse.PathNode(1L, 10L, BigDecimal.ZERO, BigDecimal.ZERO, NodeType.BUILDING_ENTRANCE, "Entrance"),
                new RouteResponse.PathNode(2L, 10L, BigDecimal.TEN, BigDecimal.ZERO, NodeType.ROOM_ENTRANCE, null));
        List<RouteResponse.Instruction> steps = List.of(
                new RouteResponse.Instruction(
                        RouteResponse.InstructionType.WALK, "Walk 13 m.", 12.5, 10L, null, null, null, 0, 1),
                new RouteResponse.Instruction(
                        RouteResponse.InstructionType.WALK, "Turn left and walk 5 m.", 5.0, 10L, null, null,
                        RouteResponse.Turn.LEFT, 1, 1));
        List<RouteResponse.Warning> warnings = List.of(new RouteResponse.Warning(
                RouteResponse.WarningCode.DETOUR, "Part of the usual route is closed. This route avoids it.", List.of(5L), null));
        return RouteResponse.found(RoutePreference.FASTEST, path, 12.5, 11, List.of(), steps, warnings);
    }
}
