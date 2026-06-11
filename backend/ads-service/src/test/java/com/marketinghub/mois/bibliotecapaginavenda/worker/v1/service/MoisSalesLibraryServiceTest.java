package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Valida regras de ingestão e criação de jobs da Biblioteca de Páginas de Vendas.
 */
@ExtendWith(MockitoExtension.class)
class MoisSalesLibraryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MoisSalesLibraryService service;

    /**
     * Garante criação de job quando a URL ingerida ainda não existe.
     */
    @Test
    void shouldCreatePendingJobWhenUrlIsNew() {
        MoisSalesLibraryDtos.SalesLibraryIngestRequest request = new MoisSalesLibraryDtos.SalesLibraryIngestRequest(
                "10",
                "hotmart",
                List.of(new MoisSalesLibraryDtos.SalesLibraryUrlItem("https://example.com/pagina", "Title", Instant.parse("2026-05-19T00:00:00Z")))
        );

        stubOperationalIngest(1);

        service.ingestUrls(request);

        verify(jdbcTemplate).update(contains("INSERT INTO mois_sales_page_job_execution"), eq(99L), any(), any(), any());
    }

    /**
     * Garante que URLs já existentes não geram jobs duplicados.
     */
    @Test
    void shouldNotCreatePendingJobWhenUrlAlreadyExists() {
        MoisSalesLibraryDtos.SalesLibraryIngestRequest request = new MoisSalesLibraryDtos.SalesLibraryIngestRequest(
                "10",
                "hotmart",
                List.of(new MoisSalesLibraryDtos.SalesLibraryUrlItem("https://example.com/pagina", "Title", Instant.parse("2026-05-19T00:00:00Z")))
        );

        stubOperationalIngest(2);

        service.ingestUrls(request);

        verify(jdbcTemplate, never()).update(contains("INSERT INTO mois_sales_page_job_execution"), any(), any(), any(), any());
    }

    /**
     * Garante que o bootstrap Hotmart usa o job mais recente e limita o lote inicial a 400 produtos.
     */
    @Test
    void shouldIngestLatestHotmartCollectedProductsWithLimitOf400() throws Exception {
        MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestRequest request =
                new MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestRequest("workspace-001", null, null);

        given(jdbcTemplate.query(contains("GROUP BY job_id"), isA(RowMapper.class), eq("workspace-001")))
                .willReturn(List.of("hotmart-job-400"));
        given(jdbcTemplate.query(contains("SELECT reference_id"), isA(RowMapper.class), eq("workspace-001"), eq("hotmart-job-400"), eq(400)))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet firstRow = collectedReferenceRow(
                            "ref-1", "Produto com sales page", "https://go.hotmart.com/A1", null, null);
                    ResultSet secondRow = collectedReferenceRow(
                            "ref-2", "Produto com fallback", null, "https://produto.example/pagina", null);
                    return List.of(mapper.mapRow(firstRow, 0), mapper.mapRow(secondRow, 1));
                });
        stubOperationalIngest(1, 2);

        MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestResponse response =
                service.ingestHotmartCollectedProducts(request);

        verify(jdbcTemplate).update(contains("INSERT INTO mois_sales_page_job_execution"), eq(99L), any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(response.jobId()).isEqualTo("hotmart-job-400");
        org.assertj.core.api.Assertions.assertThat(response.collectedReferencesRead()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(response.eligibleUrls()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(response.insertedUrls()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(response.updatedUrls()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(response.jobsCreated()).isEqualTo(1);
    }

    /**
     * Garante que o resumo operacional considera captura correta somente quando html_bytes é maior que zero.
     */
    @Test
    void shouldSummarizeCapturedPagesByUsefulHtmlBytes() {
        given(jdbcTemplate.queryForObject(
                contains("SUM(COALESCE(p.html_bytes, 0) > 0) AS captured"),
                isA(RowMapper.class),
                eq("workspace-001")))
                .willReturn(new MoisSalesLibraryDtos.SalesLibraryPageSummaryResponse(
                        "workspace-001",
                        421L,
                        0L,
                        0L,
                        327L,
                        106L,
                        190L,
                        4L,
                        27L,
                        91L,
                        0L,
                        402L,
                        19L,
                        106L,
                        8L,
                        3L,
                        70L,
                        2L,
                        18L,
                        28L,
                        12L,
                        5L,
                        7L,
                        2L,
                        Instant.parse("2026-06-07T05:38:13Z")
                ));

        MoisSalesLibraryDtos.SalesLibraryPageSummaryResponse response = service.summarizePages("workspace-001");

        org.assertj.core.api.Assertions.assertThat(response.captured()).isEqualTo(327L);
        org.assertj.core.api.Assertions.assertThat(response.analysisPending()).isEqualTo(190L);
        org.assertj.core.api.Assertions.assertThat(response.marketWarmupEligible()).isEqualTo(106L);
        org.assertj.core.api.Assertions.assertThat(response.marketWarmupCompleted()).isEqualTo(70L);
        org.assertj.core.api.Assertions.assertThat(response.marketWarmupPromising()).isEqualTo(28L);
        org.assertj.core.api.Assertions.assertThat(response.marketWarmupStuck()).isEqualTo(2L);
    }

    /**
     * Garante que a listagem de páginas prioriza as análises mais recentes.
     */
    @Test
    void shouldListPagesOrderedByMostRecentAnalysisDate() {
        given(jdbcTemplate.queryForObject(
                contains("SELECT COUNT(*)"),
                eq(Long.class),
                eq("workspace-001")))
                .willReturn(42L);
        given(jdbcTemplate.query(any(String.class), isA(RowMapper.class), eq("workspace-001"), eq(20), eq(0)))
                .willReturn(List.of());

        MoisSalesLibraryDtos.SalesLibraryPageListResponse response = service.listPages("workspace-001", 1, 20);

        org.assertj.core.api.Assertions.assertThat(response.pageSize()).isEqualTo(20);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), isA(RowMapper.class), eq("workspace-001"), eq(20), eq(0));
        org.assertj.core.api.Assertions.assertThat(sqlCaptor.getValue())
                .contains("LEFT JOIN mois_sales_page_market_warmup_summary mws")
                .contains("ORDER BY p.last_analyzed_at DESC, p.updated_at DESC, p.id DESC LIMIT ? OFFSET ?");
    }

    /**
     * Garante que a listagem de páginas permite priorização global pelo score de aquecimento.
     */
    @Test
    void shouldListPagesOrderedByMarketWarmupScoreWhenRequested() {
        given(jdbcTemplate.queryForObject(
                contains("mws.market_temperature IN ('HOT', 'PROMISING')"),
                eq(Long.class),
                eq("workspace-001")))
                .willReturn(8L);
        given(jdbcTemplate.query(any(String.class), isA(RowMapper.class), eq("workspace-001"), eq(20), eq(0)))
                .willReturn(List.of());

        MoisSalesLibraryDtos.SalesLibraryPageListResponse response =
                service.listPages("workspace-001", 1, 20, "HOT_OR_PROMISING", "MARKET_WARMUP_SCORE");

        org.assertj.core.api.Assertions.assertThat(response.total()).isEqualTo(8L);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), isA(RowMapper.class), eq("workspace-001"), eq(20), eq(0));
        org.assertj.core.api.Assertions.assertThat(sqlCaptor.getValue())
                .contains("mws.market_temperature IN ('HOT', 'PROMISING')")
                .contains("ORDER BY mws.score_total IS NULL ASC, mws.score_total DESC");
    }

    /**
     * Garante que o ranking de oportunidades combina página, aquecimento, saturação e recência no backend.
     */
    @Test
    void shouldRankMarketWarmupOpportunitiesByCombinedCommercialScore() throws Exception {
        given(jdbcTemplate.query(contains("combined_commercial_score"), isA(RowMapper.class), eq("workspace-001"), eq(5)))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet row = mock(ResultSet.class);
                    given(row.getLong("page_id")).willReturn(77L);
                    given(row.getString("title")).willReturn("Produto promissor");
                    given(row.getString("url_canonical")).willReturn("https://example.com/oferta");
                    given(row.getString("source")).willReturn("HOTMART");
                    given(row.getBigDecimal("page_score_total")).willReturn(BigDecimal.valueOf(82));
                    given(row.getBigDecimal("warmup_score_total")).willReturn(BigDecimal.valueOf(76));
                    given(row.getBigDecimal("combined_commercial_score")).willReturn(BigDecimal.valueOf(79.5));
                    given(row.getString("market_temperature")).willReturn("PROMISING");
                    given(row.getString("ecosystem_type")).willReturn("CREATORS_HEATED");
                    given(row.getString("recommendation")).willReturn("PRIORITIZE");
                    given(row.getString("saturation_risk")).willReturn(null);
                    given(row.getTimestamp("evidence_updated_at")).willReturn(Timestamp.from(Instant.parse("2026-06-09T12:00:00Z")));
                    given(row.getString("next_experiment_suggestion")).willReturn("Criar experimento com promessa direta.");
                    given(row.getString("opportunity_recommendation")).willReturn("Priorizar mercado.");
                    return List.of(mapper.mapRow(row, 0));
                });

        MoisSalesLibraryDtos.MarketWarmupOpportunityRankingResponse response =
                service.rankMarketWarmupOpportunities("workspace-001", 5);

        org.assertj.core.api.Assertions.assertThat(response.items()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(response.items().get(0).combinedCommercialScore()).isEqualByComparingTo("79.5");
        org.assertj.core.api.Assertions.assertThat(response.items().get(0).suggestedNextAction()).isEqualTo("Criar experimento com promessa direta.");
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), isA(RowMapper.class), eq("workspace-001"), eq(5));
        org.assertj.core.api.Assertions.assertThat(sqlCaptor.getValue())
                .contains("COALESCE(p.score_total, 0) * 0.45")
                .contains("mws.market_temperature = 'SATURATED'")
                .contains("ORDER BY ranked.combined_commercial_score DESC");
    }

    /**
     * Garante que a etapa 2 entrega ao worker o HTML bruto capturado na etapa 1.
     */
    @Test
    void shouldClaimAnalysisJobWithCapturedRawHtml() throws Exception {
        MoisSalesLibraryDtos.SalesLibraryClaimRequest request =
                new MoisSalesLibraryDtos.SalesLibraryClaimRequest("workspace-001", "hotmart");
        given(jdbcTemplate.query(contains("SELECT e.id AS job_id"), isA(RowMapper.class), eq("workspace-001"), eq("HOTMART")))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(analysisClaimRow(77L, 99L, "<html><body>Oferta capturada</body></html>"), 0));
                });
        given(jdbcTemplate.update(contains("SET status = 'FETCHING'"), any(), eq(77L))).willReturn(1);
        given(jdbcTemplate.update(contains("SET current_stage = 'ANALYSIS'"), eq(77L), eq(99L))).willReturn(1);

        MoisSalesLibraryDtos.SalesLibraryClaimResponse response = service.claimJob(request);

        org.assertj.core.api.Assertions.assertThat(response.claimed()).isTrue();
        org.assertj.core.api.Assertions.assertThat(response.job().rawHtml()).contains("Oferta capturada");
    }

    /**
     * Garante que a etapa 2 cria fila real para páginas capturadas que ainda não possuem análise ativa.
     */
    @Test
    void shouldCreatePendingAnalysisForCapturedPageWhenClaimQueueIsEmpty() throws Exception {
        MoisSalesLibraryDtos.SalesLibraryClaimRequest request =
                new MoisSalesLibraryDtos.SalesLibraryClaimRequest("workspace-001", "hotmart");
        given(jdbcTemplate.query(contains("SELECT e.id AS job_id"), isA(RowMapper.class), eq("workspace-001"), eq("HOTMART")))
                .willReturn(List.of())
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(analysisClaimRow(88L, 100L, "<html><body>Oferta refileirada</body></html>"), 0));
                });
        given(jdbcTemplate.query(contains("COALESCE(MAX(all_analysis.attempt), 0) + 1"), isA(RowMapper.class), eq("workspace-001"), eq("HOTMART")))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(capturedAnalysisCandidateRow(), 0));
                });
        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_page_job_execution"), eq(100L), eq("workspace-001"), eq(2), eq("https://example.com/oferta"), eq("AUTO_STAGE_2_CAPTURED_HTML")))
                .willReturn(1);
        given(jdbcTemplate.queryForObject(contains("SELECT LAST_INSERT_ID()"), eq(Long.class))).willReturn(88L);
        given(jdbcTemplate.update(contains("SET current_stage = 'ANALYSIS'"), eq(88L), eq(100L))).willReturn(1);
        given(jdbcTemplate.update(contains("SET status = 'FETCHING'"), any(), eq(88L))).willReturn(1);

        MoisSalesLibraryDtos.SalesLibraryClaimResponse response = service.claimJob(request);

        org.assertj.core.api.Assertions.assertThat(response.claimed()).isTrue();
        org.assertj.core.api.Assertions.assertThat(response.job().jobId()).isEqualTo(88L);
        org.assertj.core.api.Assertions.assertThat(response.job().rawHtml()).contains("Oferta refileirada");
    }

    /**
     * Garante que o claim de HTML bruto lê a tabela de referências coletadas e retorna a URL reservada.
     */
    @Test
    void shouldClaimCollectedReferenceHtmlFromCollectedReferenceTable() throws Exception {
        MoisSalesLibraryDtos.CollectedReferenceHtmlClaimRequest request =
                new MoisSalesLibraryDtos.CollectedReferenceHtmlClaimRequest("workspace-001", "hotmart");

        given(jdbcTemplate.query(contains("FROM mois_collected_reference r"), isA(RowMapper.class), eq("workspace-001"), eq("HOTMART"), eq(2000)))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet row = htmlCaptureRow();
                    return List.of(mapper.mapRow(row, 0));
                });
        given(jdbcTemplate.query(contains("SELECT url_canonical FROM mois_sales_page WHERE workspace_id = ? AND url_canonical IS NOT NULL"),
                isA(RowMapper.class), eq("workspace-001"))).willReturn(List.of());
        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_page"), any(), any(), eq(20L))).willReturn(1);
        given(jdbcTemplate.query(contains("SELECT workspace_id FROM mois_collected_reference"), isA(RowMapper.class), eq(20L)))
                .willReturn(List.of("workspace-001"));
        given(jdbcTemplate.query(contains("WHERE workspace_id = ? AND url_canonical = ?"), isA(RowMapper.class), eq("workspace-001"), eq("https://go.hotmart.com/A1")))
                .willReturn(List.of(99L));
        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_page_job_execution"), any(), eq(99L))).willReturn(1);
        given(jdbcTemplate.queryForObject(contains("SELECT LAST_INSERT_ID()"), eq(Long.class))).willReturn(10L);
        given(jdbcTemplate.update(contains("UPDATE mois_sales_page SET last_job_execution_id"), eq(10L), eq(99L))).willReturn(1);

        MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse response = service.claimCollectedReferenceHtml(request);

        org.assertj.core.api.Assertions.assertThat(response.claimed()).isTrue();
        org.assertj.core.api.Assertions.assertThat(response.job().url()).isEqualTo("https://go.hotmart.com/A1");
        org.assertj.core.api.Assertions.assertThat(response.job().urlSource()).isEqualTo("SALES_PAGE_URL");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), isA(RowMapper.class), eq("workspace-001"), eq("HOTMART"), eq(2000));
        org.assertj.core.api.Assertions.assertThat(sqlCaptor.getValue())
                .contains("GROUP BY effective_url")
                .contains("TRIM(sales_page_url)");

        ArgumentCaptor<String> upsertSqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(upsertSqlCaptor.capture(), any(), any(), eq(20L));
        org.assertj.core.api.Assertions.assertThat(upsertSqlCaptor.getValue())
                .contains("mois_sales_page.title")
                .contains("mois_sales_page.collected_reference_id")
                .doesNotContain("NULLIF(VALUES(title), ''), title");
    }

    /**
     * Garante que o claim pula referências brutas cuja URL canônica já está consolidada na biblioteca.
     */
    @Test
    void shouldSkipAlreadyConsolidatedCollectedReferenceHtmlCandidate() throws Exception {
        MoisSalesLibraryDtos.CollectedReferenceHtmlClaimRequest request =
                new MoisSalesLibraryDtos.CollectedReferenceHtmlClaimRequest("workspace-001", "hotmart");

        given(jdbcTemplate.query(contains("FROM mois_collected_reference r"), isA(RowMapper.class), eq("workspace-001"), eq("HOTMART"), eq(2000)))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet alreadyConsolidated = htmlCaptureRow(20L, "ref-1", "https://go.hotmart.com/A1");
                    ResultSet missing = htmlCaptureRow(21L, "ref-2", "https://go.hotmart.com/B2");
                    return List.of(mapper.mapRow(alreadyConsolidated, 0), mapper.mapRow(missing, 1));
                });
        given(jdbcTemplate.query(contains("SELECT url_canonical FROM mois_sales_page WHERE workspace_id = ? AND url_canonical IS NOT NULL"),
                isA(RowMapper.class), eq("workspace-001")))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(operationalUrlRow("https://go.hotmart.com/A1"), 0));
                });
        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_page"), any(), any(), eq(21L))).willReturn(1);
        given(jdbcTemplate.query(contains("SELECT workspace_id FROM mois_collected_reference"), isA(RowMapper.class), eq(21L)))
                .willReturn(List.of("workspace-001"));
        given(jdbcTemplate.query(contains("WHERE workspace_id = ? AND url_canonical = ?"), isA(RowMapper.class), eq("workspace-001"), eq("https://go.hotmart.com/B2")))
                .willReturn(List.of(100L));
        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_page_job_execution"), any(), eq(100L))).willReturn(1);
        given(jdbcTemplate.queryForObject(contains("SELECT LAST_INSERT_ID()"), eq(Long.class))).willReturn(11L);
        given(jdbcTemplate.update(contains("UPDATE mois_sales_page SET last_job_execution_id"), eq(11L), eq(100L))).willReturn(1);

        MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse response = service.claimCollectedReferenceHtml(request);

        org.assertj.core.api.Assertions.assertThat(response.claimed()).isTrue();
        org.assertj.core.api.Assertions.assertThat(response.job().collectedReferenceId()).isEqualTo(21L);
        org.assertj.core.api.Assertions.assertThat(response.job().url()).isEqualTo("https://go.hotmart.com/B2");
    }

    /**
     * Garante persistência de HTML bruto capturado com status final CAPTURED.
     */
    @Test
    void shouldCompleteCollectedReferenceHtmlCapture() {
        MoisSalesLibraryDtos.CollectedReferenceHtmlCompleteRequest request =
                new MoisSalesLibraryDtos.CollectedReferenceHtmlCompleteRequest(
                        "<html><body>Oferta</body></html>",
                        "https://final.example/oferta",
                        200,
                        "text/html",
                        Instant.parse("2026-06-03T10:00:00Z"));

        given(jdbcTemplate.query(contains("FROM mois_sales_page_job_execution"), isA(RowMapper.class), eq(10L)))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet row = captureExecutionRow();
                    return List.of(mapper.mapRow(row, 0));
                });

        MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse response = service.completeCollectedReferenceHtml(10L, request);

        verify(jdbcTemplate).update(contains("UPDATE mois_sales_page_job_execution"),
                eq("https://final.example/oferta"), eq(200), eq("text/html"), eq("<html><body>Oferta</body></html>"),
                any(), eq(32), any(), eq(10L));
        org.assertj.core.api.Assertions.assertThat(response.status()).isEqualTo("CAPTURED");
    }



    /**
     * Garante que o resumo de referências coletadas expõe apenas URLs únicas relevantes.
     */
    @Test
    void shouldSummarizeUniqueCollectedReferenceUrlsWithoutRawLineCounts() throws Exception {
        given(jdbcTemplate.query(contains("SELECT DISTINCT source, url_source, effective_url"), isA(RowMapper.class), eq("workspace-001")))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(
                            mapper.mapRow(collectedReferenceUrlSummaryRow("HOTMART", "SALES_PAGE_URL", "https://offer.example/a?utm=1"), 0),
                            mapper.mapRow(collectedReferenceUrlSummaryRow("HOTMART", "PRODUCT_URL", "https://product.example/p?ref=1"), 1),
                            mapper.mapRow(collectedReferenceUrlSummaryRow("CLICKBANK", "SALES_PAGE_URL", "https://hop.clickbank.net/?affiliate=abc&vendor=x"), 2)
                    );
                });
        given(jdbcTemplate.query(contains("SELECT url_canonical FROM mois_sales_page"), isA(RowMapper.class), eq("workspace-001")))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(operationalUrlRow("https://offer.example/a"), 0));
                });

        MoisSalesLibraryDtos.CollectedReferenceUrlSummaryResponse response = service.summarizeCollectedReferenceUrls("workspace-001");

        org.assertj.core.api.Assertions.assertThat(response.uniqueEffectiveUrls()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(response.explicitSalesPageUrls()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(response.fallbackProductUrls()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(response.operationalLibraryUrls()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(response.missingFromOperationalLibrary()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(response.bySource())
                .extracting(MoisSalesLibraryDtos.CollectedReferenceUrlSourceBreakdown::source)
                .containsExactly("HOTMART", "CLICKBANK");
    }

    /**
     * Garante que a listagem de entradas usa a tabela operacional nova, sem ler a tabela legada de URL.
     */
    @Test
    void shouldListEntriesFromOperationalSalesPageTable() throws Exception {
        given(jdbcTemplate.queryForObject(contains("SELECT COUNT(*) FROM mois_sales_page"), eq(Long.class), eq("workspace-001")))
                .willReturn(1L);
        given(jdbcTemplate.query(contains("FROM mois_sales_page"), isA(RowMapper.class), eq("workspace-001"), eq(20), eq(0)))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet row = salesPageEntryRow();
                    return List.of(mapper.mapRow(row, 0));
                });

        MoisSalesLibraryDtos.SalesLibraryEntryPageResponse response = service.listEntries("workspace-001", 1, 20);

        org.assertj.core.api.Assertions.assertThat(response.total()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(response.items()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(response.items().get(0).urlCanonical()).isEqualTo("https://example.com/pagina");
        verify(jdbcTemplate, never()).queryForObject(contains("mois_sales_library_url_ingest"), eq(Long.class), any());
        verify(jdbcTemplate, never()).query(contains("FROM mois_sales_library_url_ingest"), isA(RowMapper.class), any(), any(), any());
    }

    /**
     * Configura mocks comuns da ingestão operacional principal em mois_sales_page.
     */
    private void stubOperationalIngest(int... upsertResults) {
        lenient().when(jdbcTemplate.update(contains("INSERT INTO mois_sales_page"), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(upsertResults.length == 0 ? 1 : upsertResults[0], java.util.Arrays.stream(upsertResults).skip(1).boxed().toArray(Integer[]::new));
        lenient().when(jdbcTemplate.query(contains("WHERE workspace_id = ? AND url_canonical = ?"), isA(RowMapper.class), any(), any()))
                .thenReturn(List.of(99L));
        lenient().when(jdbcTemplate.query(contains("SELECT p.id, p.workspace_id, p.source"), isA(RowMapper.class), any()))
                .thenReturn(List.of(new MoisSalesLibraryDtos.SalesLibraryPageResponse(99L, "10", "HOTMART", "https://example.com/pagina", "Title",
                        "ANALYSIS", "PENDING", null, "PENDING", null, null, null, 0L, BigDecimal.ZERO, null, null, null, null, null, null, null, null, null, null, null, Instant.now(), null, null, null, null, null, null, null, null)));
        lenient().when(jdbcTemplate.update(contains("INSERT INTO mois_sales_page_job_execution"), any(), any(), any())).thenReturn(1);
        lenient().when(jdbcTemplate.queryForObject(contains("SELECT LAST_INSERT_ID()"), eq(Long.class))).thenReturn(123L);
        lenient().when(jdbcTemplate.update(contains("UPDATE mois_sales_page"), any(), any())).thenReturn(1);
    }


    /**
     * Monta uma linha simulada de URL efetiva única da origem bruta coletada.
     */
    private ResultSet collectedReferenceUrlSummaryRow(String source, String urlSource, String effectiveUrl) throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getString("source")).willReturn(source);
        given(resultSet.getString("url_source")).willReturn(urlSource);
        given(resultSet.getString("effective_url")).willReturn(effectiveUrl);
        return resultSet;
    }

    /**
     * Monta uma linha simulada de URL já consolidada na biblioteca operacional.
     */
    private ResultSet operationalUrlRow(String urlCanonical) throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getString("url_canonical")).willReturn(urlCanonical);
        return resultSet;
    }
    /**
     * Monta uma linha simulada de entrada operacional de página consolidada.
     */
    private ResultSet salesPageEntryRow() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getLong("id")).willReturn(99L);
        given(resultSet.getString("workspace_id")).willReturn("workspace-001");
        given(resultSet.getString("source")).willReturn("HOTMART");
        given(resultSet.getString("url_original")).willReturn("https://example.com/pagina?utm=1");
        given(resultSet.getString("url_canonical")).willReturn("https://example.com/pagina");
        given(resultSet.getString("title")).willReturn("Página de oferta");
        given(resultSet.getInt("ingest_count")).willReturn(2);
        given(resultSet.getTimestamp("first_seen_at")).willReturn(Timestamp.from(Instant.parse("2026-06-01T10:00:00Z")));
        given(resultSet.getTimestamp("last_captured_at")).willReturn(Timestamp.from(Instant.parse("2026-06-02T10:00:00Z")));
        given(resultSet.getTimestamp("updated_at")).willReturn(Timestamp.from(Instant.parse("2026-06-03T10:00:00Z")));
        return resultSet;
    }

    /**
     * Monta uma linha simulada de execução operacional de captura reservada.
     */
    private ResultSet captureExecutionRow() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getLong("id")).willReturn(10L);
        given(resultSet.getLong("sales_page_id")).willReturn(99L);
        return resultSet;
    }

    /**
     * Monta uma linha simulada de job de análise reservado com HTML bruto capturado.
     */
    private ResultSet analysisClaimRow(long jobId, long pageId, String rawHtml) throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getLong("job_id")).willReturn(jobId);
        given(resultSet.getLong("page_id")).willReturn(pageId);
        given(resultSet.getString("url_canonical")).willReturn("https://example.com/oferta");
        given(resultSet.getString("title")).willReturn("Oferta");
        given(resultSet.getString("raw_html")).willReturn(rawHtml);
        return resultSet;
    }

    /**
     * Monta uma linha simulada de página capturada elegível para criação automática de análise.
     */
    private ResultSet capturedAnalysisCandidateRow() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getLong("page_id")).willReturn(100L);
        given(resultSet.getString("url_canonical")).willReturn("https://example.com/oferta");
        given(resultSet.getInt("next_attempt")).willReturn(2);
        return resultSet;
    }

    /**
     * Monta uma linha simulada de captura de HTML bruto reservada.
     */
    private ResultSet htmlCaptureRow() throws Exception {
        return htmlCaptureRow(20L, "ref-1", "https://go.hotmart.com/A1");
    }

    /**
     * Monta uma linha simulada de captura de HTML bruto reservada com URL variável.
     */
    private ResultSet htmlCaptureRow(long collectedReferenceId, String referenceId, String url) throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getLong("collected_reference_id")).willReturn(collectedReferenceId);
        given(resultSet.getString("collection_job_id")).willReturn("hotmart-job-400");
        given(resultSet.getString("reference_id")).willReturn(referenceId);
        given(resultSet.getString("source")).willReturn("HOTMART");
        given(resultSet.getString("title")).willReturn("Produto");
        given(resultSet.getString("url_original")).willReturn(url);
        given(resultSet.getString("url_source")).willReturn("SALES_PAGE_URL");
        return resultSet;
    }

    /**
     * Monta uma linha simulada de produto Hotmart coletado para o mapper JDBC.
     */
    private ResultSet collectedReferenceRow(
            String referenceId,
            String productName,
            String salesPageUrl,
            String productUrl,
            String url
    ) throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getString("reference_id")).willReturn(referenceId);
        given(resultSet.getString("title")).willReturn(productName);
        given(resultSet.getString("product_name")).willReturn(productName);
        given(resultSet.getString("url")).willReturn(url);
        given(resultSet.getString("product_url")).willReturn(productUrl);
        given(resultSet.getString("sales_page_url")).willReturn(salesPageUrl);
        given(resultSet.getTimestamp("collected_at")).willReturn(Timestamp.from(Instant.parse("2026-06-01T21:00:24Z")));
        return resultSet;
    }

}
