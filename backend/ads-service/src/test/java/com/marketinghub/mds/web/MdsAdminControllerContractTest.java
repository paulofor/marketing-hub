package com.marketinghub.mds.web;

import com.marketinghub.mds.MdsRequestStatus;
import com.marketinghub.mds.dto.*;
import com.marketinghub.mds.service.MdsAdminAuthorizationService;
import com.marketinghub.mds.service.MdsAdminService;
import com.marketinghub.mds.service.MdsArtifactService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MdsAdminController.class)
class MdsAdminControllerContractTest {

    @SpringBootApplication
    static class TestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MdsAdminService adminService;

    @MockBean
    private MdsArtifactService artifactService;

    @MockBean
    private MdsAdminAuthorizationService authorizationService;

    @Test
    void shouldListRequestsWithExpectedContract() throws Exception {
        doNothing().when(authorizationService).assertAllowed("ADMIN");
        when(adminService.listRequests(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new MdsAdminRequestListResponse(
                        List.of(new MdsAdminRequestListItemResponse(
                                12L,
                                "fitness",
                                "plateau",
                                "perder gordura",
                                MdsRequestStatus.IN_PROGRESS,
                                "pipeline",
                                1,
                                Instant.parse("2026-04-27T10:00:00Z"),
                                Instant.parse("2026-04-27T10:01:00Z"),
                                false,
                                "request is in progress and cannot be retried"
                        )),
                        0,
                        20,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/mds/requests")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].requestId").value(12))
                .andExpect(jsonPath("$.items[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.items[0].retryEligible").value(false));
    }

    @Test
    void shouldGetRequestDetail() throws Exception {
        doNothing().when(authorizationService).assertAllowed("ADMIN");
        when(adminService.getRequestDetail(9L)).thenReturn(new MdsAdminRequestDetailResponse(
                9L,
                MdsRequestStatus.FAILED,
                "fitness",
                "dor",
                "resultado",
                "ate 30 dias",
                "peer-reviewed",
                "tenant-a",
                "timeout upstream",
                Instant.parse("2026-04-27T10:00:00Z"),
                Instant.parse("2026-04-27T10:01:00Z"),
                Instant.parse("2026-04-27T10:02:00Z"),
                Map.of("language", "pt-BR"),
                List.of(new MdsAdminProcessingEventResponse(
                        71L,
                        "pipeline",
                        com.marketinghub.mds.MdsEventType.ERROR,
                        "falha",
                        Map.of("step", "search"),
                        Instant.parse("2026-04-27T10:02:00Z")
                )),
                "RECOVERABLE",
                "/api/mds/requests/9/artifacts",
                "/api/mds/reports/9",
                true,
                "READY"
        ));

        mockMvc.perform(get("/api/mds/requests/9")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureClassification").value("RECOVERABLE"))
                .andExpect(jsonPath("$.timeline[0].eventId").value(71))
                .andExpect(jsonPath("$.retryEligible").value(true));
    }

    @Test
    void shouldRetryRequest() throws Exception {
        doNothing().when(authorizationService).assertAllowed("ADMIN");
        when(adminService.retryRequest(22L)).thenReturn(new MdsAdminRetryResponse(
                22L,
                MdsRequestStatus.FAILED,
                MdsRequestStatus.PENDING,
                "retry accepted"
        ));

        mockMvc.perform(post("/api/mds/requests/22/retry")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousStatus").value("FAILED"))
                .andExpect(jsonPath("$.currentStatus").value("PENDING"));
    }


    @Test
    void shouldListArtifactsWithCanonicalEnvelopeAndLineage() throws Exception {
        doNothing().when(authorizationService).assertAllowed("ADMIN");
        when(adminService.listArtifactsWithLineage(12L)).thenReturn(new MdsAdminArtifactsResponse(
                12L,
                List.of(
                        new MdsAdminArtifactItemResponse(
                                401L,
                                "mechanismSpec",
                                "v1",
                                "v1",
                                "VALIDATED",
                                List.of(301L),
                                List.of(501L),
                                Map.of("problem", "plateau", "intervention", "micro-ciclo")
                        )
                ),
                List.of(new MdsAdminArtifactLineageEdgeResponse(9001L, 301L, 401L, "DERIVED_FROM"))
        ));

        mockMvc.perform(get("/api/mds/requests/12/artifacts").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(12))
                .andExpect(jsonPath("$.artifacts[0].artifactId").value(401))
                .andExpect(jsonPath("$.artifacts[0].parentArtifactIds[0]").value(301))
                .andExpect(jsonPath("$.artifacts[0].content.intervention").value("micro-ciclo"))
                .andExpect(jsonPath("$.lineage[0].relationType").value("DERIVED_FROM"));
    }

    @Test
    void shouldGetReport() throws Exception {
        doNothing().when(authorizationService).assertAllowed("ADMIN");
        when(artifactService.getReportByRequest(12L))
                .thenReturn(new MdsReportResponse(12L, 501L, "mechanismDiscoveryReport", "v1", "v1", "VALIDATED", Map.of("status", "SUCCESS")));

        mockMvc.perform(get("/api/mds/reports/12").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifactType").value("mechanismDiscoveryReport"));
    }
}
