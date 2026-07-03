package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibraryDtos;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibraryService;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibrarySnapshotService;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesPageMarketWarmupService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Valida os contratos HTTP expostos pela Biblioteca de Páginas de Vendas do MOIS.
 */
@WebMvcTest(MoisSalesLibraryController.class)
class MoisSalesLibraryControllerTest {

    @SpringBootApplication
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MoisSalesLibraryService service;

    @MockBean
    private MoisSalesLibrarySnapshotService snapshotService;

    @MockBean
    private MoisSalesPageMarketWarmupService marketWarmupService;

    @Test
    void shouldExposeSnapshotCaptureEndpoint() throws Exception {
        when(snapshotService.captureSnapshots(any(MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest.class)))
                .thenReturn(new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureResponse(
                        "workspace-001",
                        3,
                        false,
                        1,
                        1,
                        0,
                        List.of(new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                                10L,
                                20L,
                                "https://example.test/sales",
                                "https://example.test/sales",
                                "https://example.test",
                                "CAPTURED",
                                "abc123",
                                200,
                                512L,
                                1024L,
                                null
                        )),
                        Instant.parse("2026-05-18T22:00:00Z")
                ));

        mockMvc.perform(post("/api/mois/sales-library/snapshots:capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "workspace-001",
                                  "limit": 3,
                                  "force": false
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.workspaceId").value("workspace-001"))
                .andExpect(jsonPath("$.captured").value(1))
                .andExpect(jsonPath("$.items[0].status").value("CAPTURED"))
                .andExpect(jsonPath("$.items[0].redirectDestinationUrl").value("https://example.test/sales"))
                .andExpect(jsonPath("$.items[0].redirectRootUrl").value("https://example.test"))
                .andExpect(jsonPath("$.items[0].rawHtmlBytes").value(512))
                .andExpect(jsonPath("$.items[0].screenshotBytes").value(1024));
    }

    @Test
    void shouldExposePageSnapshotsEndpoint() throws Exception {
        when(snapshotService.listSnapshots(10L))
                .thenReturn(List.of(new MoisSalesLibraryDtos.SalesLibraryPageSnapshotResponse(
                        20L,
                        10L,
                        "abc123",
                        "CAPTURED",
                        200,
                        "text/html; charset=UTF-8",
                        "https://example.test/sales",
                        "https://example.test",
                        512L,
                        1024L,
                        Instant.parse("2026-05-18T22:00:00Z"),
                        Instant.parse("2026-05-18T22:00:01Z")
                )));

        mockMvc.perform(get("/api/mois/sales-library/pages/10/snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snapshotId").value(20))
                .andExpect(jsonPath("$[0].status").value("CAPTURED"))
                .andExpect(jsonPath("$[0].rawHtmlBytes").value(512))
                .andExpect(jsonPath("$[0].screenshotBytes").value(1024));
    }


    /**
     * Garante que a listagem encaminha filtro e ordenação de aquecimento para priorização comercial.
     */
    @Test
    void shouldExposeMarketWarmupPrioritizationParamsOnPagesEndpoint() throws Exception {
        when(service.listPages("workspace-001", 1, 20, "HOT_OR_PROMISING", "MARKET_WARMUP_SCORE"))
                .thenReturn(new MoisSalesLibraryDtos.SalesLibraryPageListResponse(1, 20, 0L, List.of()));

        mockMvc.perform(get("/api/mois/sales-library/pages")
                        .param("workspaceId", "workspace-001")
                        .param("marketWarmupFilter", "HOT_OR_PROMISING")
                        .param("sort", "MARKET_WARMUP_SCORE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    /**
     * Garante que o endpoint lista candidatos quentes ao dossiê de produto.
     */
    @Test
    void shouldExposeHotProductDossierCandidatesEndpoint() throws Exception {
        when(service.listHotProductDossierCandidates("workspace-001", BigDecimal.valueOf(80), 10))
                .thenReturn(new MoisSalesLibraryDtos.HotProductDossierCandidateResponse(
                        "workspace-001",
                        BigDecimal.valueOf(80),
                        10,
                        1,
                        List.of(new MoisSalesLibraryDtos.HotProductDossierCandidateItem(
                                401L,
                                "workspace-001",
                                "HOTMART",
                                "https://example.com/oferta",
                                "BLACK MAGRA",
                                "BLACK MAGRA",
                                BigDecimal.valueOf(127.07),
                                BigDecimal.valueOf(86),
                                null,
                                null,
                                null,
                                Instant.parse("2026-07-01T10:00:00Z")
                        ))
                ));

        mockMvc.perform(get("/api/mois/sales-library/hot-products/dossier-candidates")
                        .param("workspaceId", "workspace-001")
                        .param("minTemperature", "80")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReturned").value(1))
                .andExpect(jsonPath("$.items[0].pageId").value(401))
                .andExpect(jsonPath("$.items[0].hotmartTemperature").value(127.07));
    }

    /**
     * Garante que o endpoint aceita enfileirar candidatos quentes no intake do dossiê.
     */
    @Test
    void shouldExposeHotProductDossierEnqueueEndpoint() throws Exception {
        when(service.enqueueHotProductDossierCandidates(any(MoisSalesLibraryDtos.HotProductDossierEnqueueRequest.class)))
                .thenReturn(new MoisSalesLibraryDtos.HotProductDossierEnqueueResponse(
                        "workspace-001",
                        BigDecimal.valueOf(80),
                        5,
                        1,
                        1,
                        0,
                        List.of(new MoisSalesLibraryDtos.HotProductDossierEnqueueItem(401L, "job-1", "INICIADO", "intake"))
                ));

        mockMvc.perform(post("/api/mois/sales-library/hot-products/dossier-candidates:enqueue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "workspace-001",
                                  "minTemperature": 80,
                                  "limit": 5
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.enqueued").value(1))
                .andExpect(jsonPath("$.items[0].stageCode").value("intake"));
    }

    /**
     * Garante que o resumo global consolidado da Fase 4 seja exposto ao frontend.
     */
    @Test
    void shouldExposeConsolidatedPagesSummaryEndpoint() throws Exception {
        when(service.summarizePages("workspace-001"))
                .thenReturn(new MoisSalesLibraryDtos.SalesLibraryPageSummaryResponse(
                        "workspace-001",
                        145L,
                        4L,
                        0L,
                        20L,
                        105L,
                        12L,
                        2L,
                        16L,
                        16L,
                        0L,
                        126L,
                        19L,
                        105L,
                        6L,
                        2L,
                        70L,
                        4L,
                        16L,
                        22L,
                        9L,
                        5L,
                        18L,
                        3L,
                        BigDecimal.valueOf(2.50),
                        true,
                        Instant.parse("2026-06-04T15:50:09Z"),
                        5L,
                        125L,
                        BigDecimal.valueOf(4.5),
                        Instant.parse("2026-06-04T16:00:09Z")
                ));

        mockMvc.perform(get("/api/mois/sales-library/pages/summary")
                        .param("workspaceId", "workspace-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(145))
                .andExpect(jsonPath("$.pending").value(4))
                .andExpect(jsonPath("$.captured").value(20))
                .andExpect(jsonPath("$.automaticProcessingActive").value(true))
                .andExpect(jsonPath("$.capturedLastHour").value(5))
                .andExpect(jsonPath("$.remainingWithoutHtml").value(125))
                .andExpect(jsonPath("$.averageCapturesPerHour").value(4.5))
                .andExpect(jsonPath("$.analyzed").value(105))
                .andExpect(jsonPath("$.analysisPending").value(12))
                .andExpect(jsonPath("$.analysisRunning").value(2))
                .andExpect(jsonPath("$.analysisFailed").value(16))
                .andExpect(jsonPath("$.failed").value(16))
                .andExpect(jsonPath("$.blockedCooldown").value(0))
                .andExpect(jsonPath("$.marketWarmupEligible").value(105))
                .andExpect(jsonPath("$.marketWarmupPending").value(6))
                .andExpect(jsonPath("$.marketWarmupRunning").value(2))
                .andExpect(jsonPath("$.marketWarmupCompleted").value(70))
                .andExpect(jsonPath("$.marketWarmupFailed").value(4))
                .andExpect(jsonPath("$.marketWarmupHot").value(16))
                .andExpect(jsonPath("$.marketWarmupPromising").value(22))
                .andExpect(jsonPath("$.marketWarmupSaturated").value(18))
                .andExpect(jsonPath("$.marketWarmupStuck").value(3))
                .andExpect(jsonPath("$.totalModelCostUsd").value(2.5));
    }

    /**
     * Garante que o endpoint pending expõe páginas elegíveis para diagnóstico da etapa 2 sem reservar job.
     */
    @Test
    void shouldExposePendingAnalysisEndpoint() throws Exception {
        when(service.listPendingAnalysis("workspace-001", "HOTMART", 25))
                .thenReturn(new MoisSalesLibraryDtos.SalesLibraryPendingAnalysisResponse(
                        "workspace-001",
                        "HOTMART",
                        25,
                        1,
                        List.of(new MoisSalesLibraryDtos.SalesLibraryPendingAnalysisItem(
                                99L,
                                10L,
                                "workspace-001",
                                "HOTMART",
                                "https://example.test/sales",
                                "Página de vendas",
                                2048L,
                                "PENDING",
                                2,
                                Instant.parse("2026-06-17T06:12:01Z"),
                                true
                        ))
                ));

        mockMvc.perform(get("/api/mois/sales-library/pending")
                        .param("workspaceId", "workspace-001")
                        .param("source", "HOTMART")
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value("workspace-001"))
                .andExpect(jsonPath("$.source").value("HOTMART"))
                .andExpect(jsonPath("$.limit").value(25))
                .andExpect(jsonPath("$.totalReturned").value(1))
                .andExpect(jsonPath("$.items[0].jobId").value(99))
                .andExpect(jsonPath("$.items[0].pageId").value(10))
                .andExpect(jsonPath("$.items[0].htmlBytes").value(2048))
                .andExpect(jsonPath("$.items[0].analysisStatus").value("PENDING"))
                .andExpect(jsonPath("$.items[0].rawHtmlAvailable").value(true));
    }

    /**
     * Garante que o histórico consolidado da Fase 4 seja exposto ao frontend.
     */
    @Test
    void shouldExposeConsolidatedPageExecutionsEndpoint() throws Exception {
        when(service.listPageExecutions(10L))
                .thenReturn(List.of(new MoisSalesLibraryDtos.SalesLibraryPageExecutionResponse(
                        99L,
                        10L,
                        "PAGE_ANALYSIS",
                        "ANALYSIS",
                        "DONE",
                        1,
                        "https://example.test/sales",
                        "https://example.test/final",
                        "https://example.test",
                        200,
                        "text/html",
                        512L,
                        1024L,
                        java.math.BigDecimal.valueOf(88.5),
                        null,
                        null,
                        Instant.parse("2026-05-18T21:59:00Z"),
                        Instant.parse("2026-05-18T22:00:00Z"),
                        Instant.parse("2026-05-18T21:59:00Z"),
                        Instant.parse("2026-05-18T22:00:01Z")
                )));

        mockMvc.perform(get("/api/mois/sales-library/pages/10/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].executionId").value(99))
                .andExpect(jsonPath("$[0].jobType").value("PAGE_ANALYSIS"))
                .andExpect(jsonPath("$[0].stage").value("ANALYSIS"))
                .andExpect(jsonPath("$[0].status").value("DONE"))
                .andExpect(jsonPath("$[0].rawHtmlBytes").value(512));
    }

    /**
     * Garante que o controller expõe a solicitação pública da Etapa 3 para uma página.
     */
    @Test
    void shouldExposeMarketWarmupRequestEndpoint() throws Exception {
        when(marketWarmupService.requestResearch(10L))
                .thenReturn(new MoisSalesLibraryDtos.MarketWarmupRequestResponse(
                        10L,
                        99L,
                        MoisSalesLibraryDtos.MarketWarmupJobStatus.PENDING,
                        Instant.parse("2026-06-10T09:00:00Z")
                ));

        mockMvc.perform(post("/api/mois/sales-library/pages/10/market-warmup:request"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pageId").value(10))
                .andExpect(jsonPath("$.jobId").value(99))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Garante que o controller bloqueia dossiê solicitado antes da análise comercial.
     */
    @Test
    void shouldMapMarketWarmupRequestBeforeAnalysisToHttp409() throws Exception {
        doThrow(new IllegalStateException("Dossiê MOIS só pode ser iniciado depois da análise comercial da página."))
                .when(marketWarmupService).requestResearch(10L);

        mockMvc.perform(post("/api/mois/sales-library/pages/10/market-warmup:request"))
                .andExpect(status().isConflict());
    }

    /**
     * Garante que a consulta pública retorna o resumo comercial rastreável da Etapa 3.
     */
    @Test
    void shouldExposeMarketWarmupSummaryEndpoint() throws Exception {
        when(marketWarmupService.getSummary(10L))
                .thenReturn(new MoisSalesLibraryDtos.MarketWarmupSummaryResponse(
                        99L,
                        10L,
                        BigDecimal.valueOf(82),
                        MoisSalesLibraryDtos.MarketWarmupTemperature.HOT,
                        MoisSalesLibraryDtos.MarketWarmupEcosystemType.CREATORS_HEATED,
                        MoisSalesLibraryDtos.MarketWarmupRecommendation.PRIORITIZE,
                        List.of("dor explícita"),
                        List.of("preço"),
                        List.of("resultado rápido"),
                        List.of("YouTube"),
                        List.of("Concorrente A"),
                        "baixo",
                        "priorizar experimento",
                        "testar criativo com dor explícita",
                        MoisSalesLibraryDtos.MarketWarmupJobStatus.DONE,
                        null,
                        null,
                        Instant.parse("2026-06-10T09:00:00Z"),
                        Instant.parse("2026-06-10T10:00:00Z")
                ));

        mockMvc.perform(get("/api/mois/sales-library/pages/10/market-warmup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreTotal").value(82))
                .andExpect(jsonPath("$.marketTemperature").value("HOT"))
                .andExpect(jsonPath("$.ecosystemType").value("CREATORS_HEATED"))
                .andExpect(jsonPath("$.recommendation").value("PRIORITIZE"))
                .andExpect(jsonPath("$.mainPains[0]").value("dor explícita"))
                .andExpect(jsonPath("$.mainChannels[0]").value("YouTube"));
    }

    /**
     * Garante que a API lista fontes públicas sem expor dados internos sensíveis do worker.
     */
    @Test
    void shouldExposeMarketWarmupSourcesEndpoint() throws Exception {
        when(marketWarmupService.listSources(10L))
                .thenReturn(new MoisSalesLibraryDtos.MarketWarmupSourceListResponse(
                        10L,
                        99L,
                        List.of(new MoisSalesLibraryDtos.MarketWarmupSourceResponse(
                                501L,
                                99L,
                                10L,
                                MoisSalesLibraryDtos.MarketWarmupPlatform.YOUTUBE,
                                MoisSalesLibraryDtos.MarketWarmupSourceType.CREATOR_CONTENT,
                                "https://youtube.com/watch?v=abc",
                                "Vídeo sobre a dor",
                                "Creator",
                                Instant.parse("2026-06-01T00:00:00Z"),
                                Instant.parse("2026-06-09T00:00:00Z"),
                                10000L,
                                5000L,
                                300L,
                                80L,
                                BigDecimal.valueOf(9),
                                BigDecimal.valueOf(8),
                                "Comentários recentes mostram dor explícita",
                                Instant.parse("2026-06-10T09:00:00Z"),
                                Instant.parse("2026-06-10T10:00:00Z")
                        ))
                ));

        mockMvc.perform(get("/api/mois/sales-library/pages/10/market-warmup/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value(10))
                .andExpect(jsonPath("$.items[0].sourceId").value(501))
                .andExpect(jsonPath("$.items[0].platform").value("YOUTUBE"))
                .andExpect(jsonPath("$.items[0].sourceUrl").value("https://youtube.com/watch?v=abc"));
    }

    /**
     * Garante que a API lista sinais que explicam o score da pesquisa.
     */
    @Test
    void shouldExposeMarketWarmupSignalsEndpoint() throws Exception {
        when(marketWarmupService.listSignals(10L))
                .thenReturn(new MoisSalesLibraryDtos.MarketWarmupSignalListResponse(
                        10L,
                        99L,
                        List.of(new MoisSalesLibraryDtos.MarketWarmupSignalResponse(
                                701L,
                                99L,
                                501L,
                                10L,
                                MoisSalesLibraryDtos.MarketWarmupSignalType.PAIN_EXPLICIT,
                                BigDecimal.valueOf(9),
                                "comentários pedem solução rápida",
                                "dor explícita e recente",
                                Instant.parse("2026-06-10T10:00:00Z")
                        ))
                ));

        mockMvc.perform(get("/api/mois/sales-library/pages/10/market-warmup/signals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].signalId").value(701))
                .andExpect(jsonPath("$.items[0].signalType").value("PAIN_EXPLICIT"))
                .andExpect(jsonPath("$.items[0].businessInterpretation").value("dor explícita e recente"));
    }

    /**
     * Garante que o contrato interno de claim valida o payload obrigatório do worker.
     */
    @Test
    void shouldValidateMarketWarmupClaimPayload() throws Exception {
        mockMvc.perform(post("/api/mois/sales-library/market-warmup/jobs:claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "",
                                  "workerId": "worker-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    /**
     * Garante que o worker reserva job de aquecimento apenas pelo contrato interno do módulo MOIS.
     */
    @Test
    void shouldExposeMarketWarmupClaimEndpoint() throws Exception {
        when(marketWarmupService.claimJob(any(MoisSalesLibraryDtos.MarketWarmupClaimRequest.class)))
                .thenReturn(new MoisSalesLibraryDtos.MarketWarmupClaimResponse(
                        true,
                        new MoisSalesLibraryDtos.MarketWarmupClaimedJob(
                                99L,
                                10L,
                                "workspace-001",
                                "https://example.test/oferta",
                                "Oferta principal",
                                "Produtor Especialista",
                                "Oferta transforma dor em resultado",
                                "Mecanismo plausível",
                                "Promessa clara",
                                "Prova social"
                        )
                ));

        mockMvc.perform(post("/api/mois/sales-library/market-warmup/jobs:claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "workspace-001",
                                  "workerId": "worker-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").value(true))
                .andExpect(jsonPath("$.job.jobId").value(99))
                .andExpect(jsonPath("$.job.workspaceId").value("workspace-001"))
                .andExpect(jsonPath("$.job.offerSummary").value("Oferta transforma dor em resultado"));
    }

    /**
     * Garante que o controller recebe o dossiê final do worker e responde sem conteúdo.
     */
    @Test
    void shouldExposeMarketWarmupCompleteEndpoint() throws Exception {
        mockMvc.perform(post("/api/mois/sales-library/market-warmup/jobs/99:complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(marketWarmupCompletePayload()))
                .andExpect(status().isNoContent());

        verify(marketWarmupService).completeJob(anyLong(), any(MoisSalesLibraryDtos.MarketWarmupCompleteRequest.class));
    }

    /**
     * Garante que conflito operacional do worker vira resposta HTTP 409.
     */
    @Test
    void shouldMapMarketWarmupCompleteConflictToHttp409() throws Exception {
        doThrow(new IllegalStateException("Job de aquecimento já concluído: 99"))
                .when(marketWarmupService).completeJob(anyLong(), any(MoisSalesLibraryDtos.MarketWarmupCompleteRequest.class));

        mockMvc.perform(post("/api/mois/sales-library/market-warmup/jobs/99:complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(marketWarmupCompletePayload()))
                .andExpect(status().isConflict());
    }


    /**
     * Garante que o operador consegue refileirar jobs presos sem acessar o banco.
     */
    @Test
    void shouldExposeMarketWarmupReprocessStaleEndpoint() throws Exception {
        when(marketWarmupService.reprocessStaleJobs(any(MoisSalesLibraryDtos.MarketWarmupReprocessStaleRequest.class)))
                .thenReturn(new MoisSalesLibraryDtos.MarketWarmupReprocessStaleResponse(
                        "workspace-001",
                        120,
                        2L,
                        Instant.parse("2026-06-10T14:00:00Z")
                ));

        mockMvc.perform(post("/api/mois/sales-library/market-warmup/jobs:reprocess-stale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "workspace-001",
                                  "staleMinutes": 120
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value("workspace-001"))
                .andExpect(jsonPath("$.staleMinutes").value(120))
                .andExpect(jsonPath("$.requeuedJobs").value(2));
    }

    /**
     * Garante que o controller recebe falha operacional do worker e responde sem conteúdo.
     */
    @Test
    void shouldExposeMarketWarmupFailEndpoint() throws Exception {
        mockMvc.perform(post("/api/mois/sales-library/market-warmup/jobs/99:fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorCategory": "WEB_SEARCH",
                                  "errorMessage": "Busca pública indisponível"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(marketWarmupService).failJob(anyLong(), any(MoisSalesLibraryDtos.MarketWarmupFailRequest.class));
    }

    /**
     * Monta o payload final mínimo válido enviado pelo worker de aquecimento.
     */
    private String marketWarmupCompletePayload() {
        return """
                {
                  "sources": [
                    {
                      "platform": "YOUTUBE",
                      "sourceType": "CREATOR_CONTENT",
                      "sourceUrl": "https://youtube.com/watch?v=abc",
                      "sourceTitle": "Vídeo sobre a dor",
                      "authorName": "Creator",
                      "publishedAt": "2026-06-01T00:00:00Z",
                      "lastActivityAt": "2026-06-09T00:00:00Z",
                      "followersOrSubscribers": 10000,
                      "viewsCount": 5000,
                      "likesCount": 300,
                      "commentsCount": 80,
                      "recencyScore": 9,
                      "engagementScore": 8,
                      "evidenceSummary": "Comentários recentes mostram dor explícita"
                    }
                  ],
                  "signals": [
                    {
                      "sourceIndex": 0,
                      "signalType": "PAIN_EXPLICIT",
                      "signalStrength": 9,
                      "signalText": "comentários pedem solução rápida",
                      "businessInterpretation": "dor explícita e recente"
                    }
                  ],
                  "summary": {
                    "scoreTotal": 82,
                    "marketTemperature": "HOT",
                    "ecosystemType": "CREATORS_HEATED",
                    "recommendation": "PRIORITIZE",
                    "mainPains": ["dor explícita"],
                    "mainObjections": ["preço"],
                    "mainPromises": ["resultado rápido"],
                    "mainChannels": ["YouTube"],
                    "mainCompetitors": ["Concorrente A"],
                    "saturationRisk": "baixo",
                    "opportunityRecommendation": "priorizar experimento",
                    "nextExperimentSuggestion": "testar criativo com dor explícita"
                  },
                  "finishedAt": "2026-06-10T10:00:00Z"
                }
                """;
    }

}
