package com.marketinghub.mois.web;

import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import com.marketinghub.mois.dto.MoisCollectionPersistenceDtos;
import com.marketinghub.mois.service.MoisCollectionPersistenceService;
import com.marketinghub.mois.service.MoisHotmartProductService;
import com.marketinghub.mois.service.MoisModuleGateway;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MoisController.class)
class MoisControllerContractTest {

    @SpringBootApplication
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MoisModuleGateway gateway;

    @MockBean
    private MoisCollectionPersistenceService collectionPersistenceService;

    @MockBean
    private MoisHotmartProductService moisHotmartProductService;

    @Test
    void shouldAcceptDiscoveryRequest() throws Exception {
        when(gateway.createDiscoveryRequest(any(MoisDiscoveryDtos.CreateDiscoveryRequest.class)))
                .thenReturn(new MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse("mois-req-123", "ACCEPTED"));

        mockMvc.perform(post("/api/v1/mois/discovery-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nicheName": "personal trainer",
                                  "marketTheme": "retencao de alunos"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value("mois-req-123"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void shouldReturnCollectionOpsSummaryContract() throws Exception {
        when(gateway.getCollectionOpsSummary("workspace-001"))
                .thenReturn(new MoisWorkspaceDtos.CollectionOpsSummaryResponse(
                        "workspace-001",
                        true,
                        2,
                        0,
                        0,
                        2,
                        0,
                        4,
                        280,
                        1,
                        List.of(new MoisWorkspaceDtos.CollectionSourceOpsSummaryResponse(
                                "CLICKBANK",
                                2,
                                2,
                                0,
                                0,
                                0,
                                110,
                                null,
                                Instant.parse("2026-04-26T12:00:00Z")
                        )),
                        Instant.parse("2026-04-26T12:00:30Z")
                ));

        mockMvc.perform(get("/api/v1/mois/workspaces/workspace-001/collection-ops/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value("workspace-001"))
                .andExpect(jsonPath("$.totalJobs").value(2))
                .andExpect(jsonPath("$.sourceBreakdown[0].source").value("CLICKBANK"));
    }

    @Test
    void shouldReturnValidationErrorWhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/mois/discovery-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnDiscoveryRequestDetail() throws Exception {
        when(gateway.getDiscoveryRequest(eq("mois-req-001")))
                .thenReturn(Optional.of(new MoisDiscoveryDtos.DiscoveryRequestDetailResponse(
                        "mois-req-001",
                        "personal trainer",
                        "retencao de alunos",
                        "agenda previsivel sem desconto",
                        "DRAFT",
                        Instant.parse("2026-04-22T00:00:00Z"),
                        List.of(new MoisDiscoveryDtos.ArtifactRefResponse("mois-art-001", "mois.marketOfferDiscoveryRequest.v1", "v1"))
                )));

        mockMvc.perform(get("/api/v1/mois/discovery-requests/mois-req-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("mois-req-001"))
                .andExpect(jsonPath("$.artifacts[0].artifactId").value("mois-art-001"));
    }

    @Test
    void shouldReturn404WhenOfferDoesNotExist() throws Exception {
        when(gateway.getOffer(eq("unknown-offer"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/mois/offers/unknown-offer"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnOfferListContract() throws Exception {
        when(gateway.listOffers(any(), any(), any()))
                .thenReturn(new MoisOfferDtos.OfferCardListResponse(List.of(
                        new MoisOfferDtos.OfferCardSummaryResponse(
                                "mois-offer-001",
                                "mois-req-001",
                                "personal trainer",
                                "Agenda Cheia Sem Desconto",
                                "Studio Exemplo",
                                "Agenda previsivel com onboarding estruturado",
                                "mentoria",
                                "R$ 1.497",
                                0.79
                        )
                )));

        mockMvc.perform(get("/api/v1/mois/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].offerId").value("mois-offer-001"))
                .andExpect(jsonPath("$.items[0].confidence").value(0.79));
    }

    @Test
    void shouldPropagateCategoryFilterToInsightReportsEndpoint() throws Exception {
        when(gateway.listInsightReports(eq("mois-req-001"), eq("nutricao"), eq("DIGITAL_PRODUCT")))
                .thenReturn(new MoisInsightDtos.InsightReportListResponse(List.of()));

        mockMvc.perform(get("/api/v1/mois/insight-reports")
                        .param("requestId", "mois-req-001")
                        .param("nicheName", "nutricao")
                        .param("category", "DIGITAL_PRODUCT"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposeExecutiveSummaryEndpointContract() throws Exception {
        when(gateway.getInsightExecutiveSummary("mois-report-001"))
                .thenReturn(Optional.of(new MoisInsightDtos.InsightExecutiveSummaryResponse(
                        "mois-report-001",
                        "mois-req-001",
                        "nutricao",
                        "perda de peso",
                        new MoisInsightDtos.FrameworkRecommendationResponse(
                                "dor dominante",
                                "resultado",
                                "mecanismo",
                                "prova",
                                List.of("angulo")),
                        List.of(),
                        List.of(),
                        List.of("acao"))));

        mockMvc.perform(get("/api/v1/mois/insight-reports/mois-report-001/executive-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value("mois-report-001"));
    }

    @Test
    void shouldReturnWorkspaceDashboardContract() throws Exception {
        when(gateway.getDashboard(eq("workspace-001")))
                .thenReturn(new MoisWorkspaceDtos.WorkspaceDashboardResponse(
                        "workspace-001",
                        new MoisWorkspaceDtos.WorkspaceKpisResponse(12, 4, 2, 1),
                        "COLETA",
                        List.of(new MoisWorkspaceDtos.RecentAnalysisResponse(
                                "ref-1",
                                "nutricao",
                                "COLETA_CONCLUIDA",
                                Instant.parse("2026-04-25T12:00:00Z")) )
                ));

        mockMvc.perform(get("/api/v1/mois/workspaces/workspace-001/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.collections").value(12))
                .andExpect(jsonPath("$.recentAnalyses[0].analysisId").value("ref-1"));
    }

    @Test
    void shouldCreateReferenceForSprintOneContract() throws Exception {
        when(gateway.createReference(any(MoisWorkspaceDtos.CreateReferenceRequest.class)))
                .thenReturn(new MoisWorkspaceDtos.ReferenceResponse(
                        "ref-123",
                        "workspace-001",
                        "nutricao-esportiva",
                        "https://exemplo.com/oferta",
                        "LANDING_PAGE",
                        "Secar 5kg em 8 semanas",
                        "PROBLEM_AWARE",
                        "97-297",
                        "CURSO",
                        "Oferta com prova social",
                        Instant.parse("2026-04-25T12:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/mois/references")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "workspace-001",
                                  "niche": "nutricao-esportiva",
                                  "sourceUrl": "https://exemplo.com/oferta",
                                  "assetType": "LANDING_PAGE",
                                  "primaryPromise": "Secar 5kg em 8 semanas",
                                  "awarenessStage": "PROBLEM_AWARE",
                                  "priceRange": "97-297",
                                  "formatType": "CURSO",
                                  "notes": "Oferta com prova social"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceId").value("ref-123"))
                .andExpect(jsonPath("$.workspaceId").value("workspace-001"));
    }

    @Test
    void shouldValidateReferencePayload() throws Exception {
        mockMvc.perform(post("/api/v1/mois/references")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListReferencesByWorkspace() throws Exception {
        when(gateway.listReferences(eq("workspace-001")))
                .thenReturn(new MoisWorkspaceDtos.ReferenceListResponse(List.of(
                        new MoisWorkspaceDtos.ReferenceResponse(
                                "ref-123",
                                "workspace-001",
                                "nutricao-esportiva",
                                "https://exemplo.com/oferta",
                                "LANDING_PAGE",
                                "Secar 5kg em 8 semanas",
                                "PROBLEM_AWARE",
                                "97-297",
                                "CURSO",
                                null,
                                Instant.parse("2026-04-25T12:00:00Z")
                        )
                )));

        mockMvc.perform(get("/api/v1/mois/references").param("workspaceId", "workspace-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].referenceId").value("ref-123"));
    }


    @Test
    void shouldUpsertExtractionDraft() throws Exception {
        when(gateway.upsertExtractionDraft(eq("ref-123"), any(MoisWorkspaceDtos.UpsertExtractionDraftRequest.class)))
                .thenReturn(new MoisWorkspaceDtos.ExtractionDraftResponse(
                        "ext-123",
                        "ref-123",
                        "DRAFT",
                        Instant.parse("2026-04-25T12:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/mois/references/ref-123/extractions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pain": "Falta de tempo",
                                  "result": "Emagrecer com rotina curta",
                                  "mechanism": "Treino de 15 minutos"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extractionId").value("ext-123"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldReturnLibraryBlocksContract() throws Exception {
        when(gateway.listLibraryBlocks(eq("workspace-001"), eq("nutricao"), eq("CURSO")))
                .thenReturn(new MoisWorkspaceDtos.LibraryBlockListResponse(List.of(
                        new MoisWorkspaceDtos.LibraryBlockResponse(
                                "block-1",
                                "workspace-001",
                                "PROMISE",
                                "Headline objetiva",
                                List.of("nutricao", "CURSO"),
                                0.9,
                                "MARKET_REFERENCE",
                                false,
                                Instant.parse("2026-04-25T12:00:00Z")
                        ))));

        mockMvc.perform(get("/api/v1/mois/library/blocks")
                        .param("workspaceId", "workspace-001")
                        .param("niche", "nutricao")
                        .param("formatType", "CURSO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].blockId").value("block-1"));
    }

    @Test
    void shouldCreateComparisonContract() throws Exception {
        when(gateway.createComparison(any(MoisWorkspaceDtos.CreateComparisonRequest.class)))
                .thenReturn(new MoisWorkspaceDtos.ComparisonResponse(
                        "comparison-1",
                        "workspace-001",
                        List.of(new MoisWorkspaceDtos.ComparisonDimensionResponse(
                                "PROMESSA",
                                "Mercado",
                                "Atual",
                                "Ajustar headline")),
                        List.of(new MoisWorkspaceDtos.ComparisonScorecardResponse(
                                "clareza",
                                70,
                                "Boa direção com oportunidade de refinamento.")),
                        List.of(new MoisWorkspaceDtos.ComparisonImprovementResponse(
                                "imp-1",
                                "HIGH",
                                "Incluir prova mensurável"))
                ));

        mockMvc.perform(post("/api/v1/mois/comparisons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "workspace-001",
                                  "referenceBaseId": "ref-123",
                                  "currentOfferId": "offer-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparisonId").value("comparison-1"))
                .andExpect(jsonPath("$.scorecards[0].metric").value("clareza"));
    }

    @Test
    void shouldBuildOfferContract() throws Exception {
        when(gateway.buildOffer(any(MoisWorkspaceDtos.BuildOfferRequest.class)))
                .thenReturn(new MoisWorkspaceDtos.BuildOfferResponse(
                        "offer-1",
                        "workspace-001",
                        "READY_TO_EXPORT",
                        "Conteúdo proposto",
                        java.util.Map.of("dor", true),
                        Instant.parse("2026-04-25T12:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/mois/offers/build")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "workspace-001",
                                  "currentOfferId": "offer-123",
                                  "selectedBlockIds": ["block-1"],
                                  "currentVersion": "dor resultado mecanismo prova oferta"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offerId").value("offer-1"))
                .andExpect(jsonPath("$.status").value("READY_TO_EXPORT"));
    }

    @Test
    void shouldCreateCollectionJobContract() throws Exception {
        when(gateway.createCollectionJob(any(MoisWorkspaceDtos.CreateCollectionJobRequest.class)))
                .thenReturn(new MoisWorkspaceDtos.CollectionJobResponse(
                        "mois-collect-001",
                        "workspace-001",
                        "nutricao",
                        "perda de gordura",
                        "QUEUED",
                        "LAST_7_DAYS",
                        50,
                        60,
                        List.of("META_AD_LIBRARY", "CLICKBANK"),
                        Instant.parse("2026-04-25T12:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/mois/collection-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "workspace-001",
                                  "niche": "nutricao",
                                  "marketTheme": "perda de gordura",
                                  "sources": ["META_AD_LIBRARY", "CLICKBANK"],
                                  "timeWindow": "LAST_7_DAYS",
                                  "limitPerSource": 50,
                                  "minSuccessScore": 60
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value("mois-collect-001"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void shouldValidateCollectionJobPayload() throws Exception {
        mockMvc.perform(post("/api/v1/mois/collection-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "",
                                  "niche": "",
                                  "sources": [],
                                  "timeWindow": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListCollectionJobsContract() throws Exception {
        when(collectionPersistenceService.listJobStates(eq("workspace-001"), eq("QUEUED")))
                .thenReturn(new MoisCollectionPersistenceDtos.CollectionJobStateListResponse(List.of(
                        new MoisCollectionPersistenceDtos.CollectionJobStateResponse(
                                new MoisWorkspaceDtos.CollectionJobResponse(
                                        "mois-collect-001",
                                        "workspace-001",
                                        "nutricao",
                                        "perda de gordura",
                                        "QUEUED",
                                        "LAST_7_DAYS",
                                        50,
                                        60,
                                        List.of("META_AD_LIBRARY"),
                                        Instant.parse("2026-04-25T12:00:00Z")
                                ),
                                List.of(),
                                java.util.Map.of(),
                                null,
                                List.of()
                        )
                )));

        mockMvc.perform(get("/api/v1/mois/collection-jobs")
                        .param("workspaceId", "workspace-001")
                        .param("status", "QUEUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].jobId").value("mois-collect-001"))
                .andExpect(jsonPath("$.items[0].timeWindow").value("LAST_7_DAYS"));
    }

    @Test
    void shouldListCollectedReferencesByJobContract() throws Exception {
        when(gateway.listCollectedReferencesByJob(eq("mois-collect-001"), eq("CLICKBANK"), eq("nutricao"), eq(70), eq("HIGH")))
                .thenReturn(Optional.of(new MoisWorkspaceDtos.CollectedReferenceListResponse(
                        "mois-collect-001",
                        List.of(new MoisWorkspaceDtos.CollectedReferenceResponse(
                                "ref-auto-001",
                                "mois-collect-001",
                                "CLICKBANK",
                                "Oferta com alta recorrência",
                                "https://example.com/offer",
                                "nutricao",
                                "ACTIVE",
                                false,
                                null,
                                78,
                                "HIGH",
                                "HIGH",
                                1,
                                81.0,
                                76.0,
                                72.0,
                                Instant.parse("2026-04-25T12:00:00Z"),
                                java.util.Map.of("status", "ACTIVE")
                        ))
                )));

        mockMvc.perform(get("/api/v1/mois/collection-jobs/mois-collect-001/references")
                        .param("source", "CLICKBANK")
                        .param("niche", "nutricao")
                        .param("minSuccessScore", "70")
                        .param("confidenceLevel", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("mois-collect-001"))
                .andExpect(jsonPath("$.items[0].source").value("CLICKBANK"))
                .andExpect(jsonPath("$.items[0].successScore").value(78))
                .andExpect(jsonPath("$.items[0].confidenceLevel").value("HIGH"));
    }

    @Test
    void shouldReturn404WhenCollectionJobReferencesNotFound() throws Exception {
        when(gateway.listCollectedReferencesByJob(eq("mois-collect-missing"), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/mois/collection-jobs/mois-collect-missing/references"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFavoriteCollectedReference() throws Exception {
        when(gateway.favoriteCollectedReference("mois-collect-001", "ref-auto-001"))
                .thenReturn(Optional.of(new MoisWorkspaceDtos.CollectedReferenceActionResponse(
                        "mois-collect-001",
                        "ref-auto-001",
                        "FAVORITE",
                        "ACTIVE",
                        null,
                        null,
                        List.of(),
                        Instant.parse("2026-04-26T12:00:00Z")
                )));

        mockMvc.perform(post("/api/v1/mois/collection-jobs/mois-collect-001/references/ref-auto-001/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("FAVORITE"))
                .andExpect(jsonPath("$.referenceId").value("ref-auto-001"));
    }

    @Test
    void shouldImportCollectedReference() throws Exception {
        when(gateway.importCollectedReference("mois-collect-001", "ref-auto-001"))
                .thenReturn(Optional.of(new MoisWorkspaceDtos.CollectedReferenceActionResponse(
                        "mois-collect-001",
                        "ref-auto-001",
                        "IMPORT",
                        "IMPORTED",
                        "ref-imported-001",
                        null,
                        List.of("block-1", "block-2"),
                        Instant.parse("2026-04-26T12:00:00Z")
                )));

        mockMvc.perform(post("/api/v1/mois/collection-jobs/mois-collect-001/references/ref-auto-001/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IMPORTED"))
                .andExpect(jsonPath("$.importedReferenceId").value("ref-imported-001"));
    }

    @Test
    void shouldImportAndStartExtractionCollectedReference() throws Exception {
        when(gateway.importAndStartExtraction("mois-collect-001", "ref-auto-001"))
                .thenReturn(Optional.of(new MoisWorkspaceDtos.CollectedReferenceActionResponse(
                        "mois-collect-001",
                        "ref-auto-001",
                        "IMPORT_AND_START_EXTRACTION",
                        "IMPORTED",
                        "ref-imported-001",
                        "ext-001",
                        List.of("block-1", "block-2"),
                        Instant.parse("2026-04-26T12:10:00Z")
                )));

        mockMvc.perform(post("/api/v1/mois/collection-jobs/mois-collect-001/references/ref-auto-001/import-and-start-extraction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("IMPORT_AND_START_EXTRACTION"))
                .andExpect(jsonPath("$.extractionId").value("ext-001"));
    }

    @Test
    void shouldReturnCollectedReferenceLineage() throws Exception {
        when(gateway.getCollectedReferenceLineage("mois-collect-001", "ref-auto-001"))
                .thenReturn(Optional.of(new MoisWorkspaceDtos.CollectedReferenceLineageResponse(
                        "mois-collect-001",
                        "ref-auto-001",
                        "https://example.com/offer",
                        "ref-imported-001",
                        "ext-001",
                        List.of("block-1", "block-2"),
                        Instant.parse("2026-04-26T12:12:00Z")
                )));

        mockMvc.perform(get("/api/v1/mois/collection-jobs/mois-collect-001/references/ref-auto-001/lineage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedReferenceId").value("ref-imported-001"))
                .andExpect(jsonPath("$.generatedLibraryBlockIds[0]").value("block-1"));
    }

}
