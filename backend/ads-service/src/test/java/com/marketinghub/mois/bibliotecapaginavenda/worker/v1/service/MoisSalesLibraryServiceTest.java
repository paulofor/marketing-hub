package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
     * Garante que o claim de HTML bruto lê a tabela de referências coletadas e retorna a URL reservada.
     */
    @Test
    void shouldClaimCollectedReferenceHtmlFromCollectedReferenceTable() throws Exception {
        MoisSalesLibraryDtos.CollectedReferenceHtmlClaimRequest request =
                new MoisSalesLibraryDtos.CollectedReferenceHtmlClaimRequest("workspace-001", "hotmart");

        given(jdbcTemplate.query(contains("FROM mois_collected_reference r"), isA(RowMapper.class), eq("workspace-001"), eq("HOTMART")))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet row = htmlCaptureRow();
                    return List.of(mapper.mapRow(row, 0));
                });
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
     * Configura mocks comuns da ingestão operacional principal em mois_sales_page.
     */
    private void stubOperationalIngest(int... upsertResults) {
        lenient().when(jdbcTemplate.update(contains("INSERT INTO mois_sales_page"), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(upsertResults.length == 0 ? 1 : upsertResults[0], java.util.Arrays.stream(upsertResults).skip(1).boxed().toArray(Integer[]::new));
        lenient().when(jdbcTemplate.query(contains("WHERE workspace_id = ? AND url_canonical = ?"), isA(RowMapper.class), any(), any()))
                .thenReturn(List.of(99L));
        lenient().when(jdbcTemplate.query(contains("SELECT id, workspace_id, source"), isA(RowMapper.class), any()))
                .thenReturn(List.of(new MoisSalesLibraryDtos.SalesLibraryPageResponse(99L, "10", "HOTMART", "https://example.com/pagina", "Title",
                        "ANALYSIS", "PENDING", null, "PENDING", null, null, null, 0L, BigDecimal.ZERO, null, null, null, null, null, null, null, null, null, Instant.now())));
        lenient().when(jdbcTemplate.update(contains("INSERT INTO mois_sales_page_job_execution"), any(), any(), any())).thenReturn(1);
        lenient().when(jdbcTemplate.queryForObject(contains("SELECT LAST_INSERT_ID()"), eq(Long.class))).thenReturn(123L);
        lenient().when(jdbcTemplate.update(contains("UPDATE mois_sales_page"), any(), any())).thenReturn(1);
        lenient().when(jdbcTemplate.update(contains("INSERT INTO mois_sales_library_url_ingest"), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        lenient().when(jdbcTemplate.update(contains("INSERT INTO mois_sales_library_processing_job"), isA(Long.class))).thenReturn(1);
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
     * Monta uma linha simulada de captura de HTML bruto reservada.
     */
    private ResultSet htmlCaptureRow() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getLong("collected_reference_id")).willReturn(20L);
        given(resultSet.getString("collection_job_id")).willReturn("hotmart-job-400");
        given(resultSet.getString("reference_id")).willReturn("ref-1");
        given(resultSet.getString("source")).willReturn("HOTMART");
        given(resultSet.getString("title")).willReturn("Produto");
        given(resultSet.getString("url_original")).willReturn("https://go.hotmart.com/A1");
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
