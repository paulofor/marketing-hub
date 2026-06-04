package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageDualWriteGateway;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
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

    @Mock
    private MoisSalesPageDualWriteGateway dualWriteGateway;

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

        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_library_url_ingest"), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(1);
        given(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any())).willReturn(99L);

        service.ingestUrls(request);

        verify(jdbcTemplate).update(contains("INSERT INTO mois_sales_library_processing_job"), eq(99L), eq("PENDING"));
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

        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_library_url_ingest"), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(2);

        service.ingestUrls(request);

        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class), any());
        verify(jdbcTemplate, never()).update(contains("INSERT INTO mois_sales_library_processing_job"), any(), any());
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
        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_library_url_ingest"), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(1, 2);
        given(jdbcTemplate.queryForObject(contains("SELECT id"), eq(Long.class), any(), any())).willReturn(77L);

        MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestResponse response =
                service.ingestHotmartCollectedProducts(request);

        verify(jdbcTemplate).update(contains("INSERT INTO mois_sales_library_processing_job"), eq(77L), eq("PENDING"));
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

        given(jdbcTemplate.update(contains("INSERT INTO mois_collected_reference_html_capture"), any(), eq("workspace-001"), eq("HOTMART")))
                .willReturn(1);
        given(jdbcTemplate.query(contains("FROM mois_collected_reference_html_capture"), isA(RowMapper.class), any()))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet row = htmlCaptureRow();
                    return List.of(mapper.mapRow(row, 0));
                });

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

        MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse response = service.completeCollectedReferenceHtml(10L, request);

        verify(jdbcTemplate).update(contains("UPDATE mois_collected_reference_html_capture"),
                eq("https://final.example/oferta"), eq(200), eq("text/html"), eq("<html><body>Oferta</body></html>"),
                any(), eq(32), any(), eq(10L));
        org.assertj.core.api.Assertions.assertThat(response.status()).isEqualTo("CAPTURED");
    }


    /**
     * Monta uma linha simulada de captura de HTML bruto reservada.
     */
    private ResultSet htmlCaptureRow() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        given(resultSet.getLong("id")).willReturn(10L);
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
