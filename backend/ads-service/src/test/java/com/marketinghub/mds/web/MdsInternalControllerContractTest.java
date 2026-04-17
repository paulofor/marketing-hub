package com.marketinghub.mds.web;

import com.marketinghub.mds.MdsRequestStatus;
import com.marketinghub.mds.dto.MdsArtifactPublishBatchResponse;
import com.marketinghub.mds.dto.MdsLineageCreateRequest;
import com.marketinghub.mds.dto.MdsLineageResponse;
import com.marketinghub.mds.dto.MdsRequestStatusResponse;
import com.marketinghub.mds.dto.MdsSourceAccessPublishBatchResponse;
import com.marketinghub.mds.service.MdsArtifactService;
import com.marketinghub.mds.service.MdsRequestService;
import com.marketinghub.mds.service.MdsSourceAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MdsInternalController.class)
class MdsInternalControllerContractTest {

    @SpringBootApplication
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MdsRequestService requestService;

    @MockBean
    private MdsArtifactService artifactService;

    @MockBean
    private MdsSourceAccessService sourceAccessService;

    @Test
    void shouldClaimRequestWithExpectedContract() throws Exception {
        when(requestService.claim(eq(7L), any())).thenReturn(response(MdsRequestStatus.IN_PROGRESS));

        mockMvc.perform(post("/api/internal/mds/requests/7/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":"mds-worker-contract"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldRejectLineageWhenPathIdDoesNotMatchBody() throws Exception {
        mockMvc.perform(post("/api/internal/mds/artifacts/31/lineage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentArtifactId": 10,
                                  "childArtifactId": 30,
                                  "relationType": "derived_from"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldCreateLineageWhenPathIdMatchesBody() throws Exception {
        when(artifactService.createLineage(any(MdsLineageCreateRequest.class)))
                .thenReturn(new MdsLineageResponse(99L, 10L, 30L, "derived_from"));

        mockMvc.perform(post("/api/internal/mds/artifacts/30/lineage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentArtifactId": 10,
                                  "childArtifactId": 30,
                                  "relationType": "derived_from"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.parentArtifactId").value(10))
                .andExpect(jsonPath("$.childArtifactId").value(30));
    }

    @Test
    void shouldPublishArtifactsWithExpectedContract() throws Exception {
        when(artifactService.publishBatch(any()))
                .thenReturn(new MdsArtifactPublishBatchResponse(12L, 1, java.util.List.of(101L)));

        mockMvc.perform(post("/api/internal/mds/artifacts/publish-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": 12,
                                  "artifacts": [
                                    {
                                      "artifactType": "mechanismSpec",
                                      "schemaVersion": "v1",
                                      "version": "1",
                                      "status": "DRAFT",
                                      "producerModule": "mds",
                                      "ownerModule": "mds",
                                      "content": {"title": "stub"}
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(12))
                .andExpect(jsonPath("$.publishedCount").value(1));
    }

    @Test
    void shouldPublishSourceAccessRecordsWithExpectedContract() throws Exception {
        when(sourceAccessService.publishBatch(any()))
                .thenReturn(new MdsSourceAccessPublishBatchResponse(1, java.util.List.of(301L)));

        mockMvc.perform(post("/api/internal/mds/source-access/publish-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "records": [
                                    {
                                      "sourceDocumentId": "pubmed:123",
                                      "accessClass": "open_access",
                                      "permissionState": "can_download",
                                      "licenseText": "CC-BY",
                                      "accessUrl": "https://pubmed.ncbi.nlm.nih.gov/123/"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1));
    }

    @Test
    void shouldGetRecommendedMechanismByRequest() throws Exception {
        when(artifactService.getRecommendedMechanismByRequest(12L))
                .thenReturn(new com.marketinghub.mds.dto.MdsRecommendedMechanismResponse(
                        12L,
                        401L,
                        "mechanismSpec",
                        "v1",
                        "v1",
                        "DRAFT",
                        java.util.Map.of(
                                "recommendedMechanismCandidateKey", "mc-1",
                                "confidenceLevel", "moderada"
                        )
                ));

        mockMvc.perform(get("/api/internal/mds/requests/12/recommended-mechanism"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(12))
                .andExpect(jsonPath("$.artifactId").value(401))
                .andExpect(jsonPath("$.artifactType").value("mechanismSpec"))
                .andExpect(jsonPath("$.content.recommendedMechanismCandidateKey").value("mc-1"));
    }

    @Test
    void shouldGetDiscoveryReportByRequest() throws Exception {
        when(artifactService.getReportByRequest(12L))
                .thenReturn(new com.marketinghub.mds.dto.MdsReportResponse(
                        12L,
                        501L,
                        "mechanismDiscoveryReport",
                        "v1",
                        "v1",
                        "DRAFT",
                        java.util.Map.of("status", "SUCCESS")
                ));

        mockMvc.perform(get("/api/internal/mds/reports/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(12))
                .andExpect(jsonPath("$.artifactId").value(501))
                .andExpect(jsonPath("$.artifactType").value("mechanismDiscoveryReport"))
                .andExpect(jsonPath("$.content.status").value("SUCCESS"));
    }

    @Test
    void shouldListArtifactsByRequest() throws Exception {
        when(artifactService.listArtifactsByRequest(12L))
                .thenReturn(java.util.List.of(
                        new com.marketinghub.mds.dto.MdsArtifactSummaryResponse(401L, "mechanismSpec", "v1", "v1", "DRAFT"),
                        new com.marketinghub.mds.dto.MdsArtifactSummaryResponse(402L, "practicalKnowledgePack", "v1", "v1", "DRAFT")
                ));

        mockMvc.perform(get("/api/internal/mds/requests/12/artifacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].artifactId").value(401))
                .andExpect(jsonPath("$[1].artifactType").value("practicalKnowledgePack"));
    }

    private MdsRequestStatusResponse response(MdsRequestStatus status) {
        return new MdsRequestStatusResponse(
                7L,
                status,
                "weight-loss",
                "plateau",
                "consistent fat loss",
                "corr-7",
                null,
                Instant.parse("2026-04-17T00:00:00Z"),
                Instant.parse("2026-04-17T00:01:00Z"),
                null,
                Instant.parse("2026-04-17T00:01:00Z")
        );
    }
}
