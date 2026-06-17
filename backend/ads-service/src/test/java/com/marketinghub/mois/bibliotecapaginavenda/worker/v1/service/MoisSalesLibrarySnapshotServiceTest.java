package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibraryDtos;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Valida a captura de snapshots HTML da biblioteca de páginas de venda MOIS. */
public class MoisSalesLibrarySnapshotServiceTest {

    private JdbcTemplate jdbcTemplate;
    private HttpServer server;
    private MoisSalesLibrarySnapshotService service;

    /** Prepara banco H2 e servidor HTTP local para cada cenário de captura. */
    @BeforeEach
    void setUp() throws IOException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:mois_snapshot;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE ALIAS IF NOT EXISTS UTC_TIMESTAMP FOR \"com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibrarySnapshotServiceTest.utcTimestamp\"");
        createSchema();
        service = new MoisSalesLibrarySnapshotService(jdbcTemplate);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/sales", exchange -> {
            byte[] body = """
                    <html>
                      <head><title>Oferta Teste</title></head>
                      <body><h1>Oferta Teste</h1><p>Promessa clara com prova e CTA.</p></body>
                    </html>
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/missing", exchange -> {
            byte[] body = "produto removido".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/redirect-to-missing", exchange -> {
            exchange.getResponseHeaders().add("Location", "/missing");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/", exchange -> {
            byte[] body = """
                    <html>
                      <head><title>Oferta Raiz</title></head>
                      <body><h1>Oferta Raiz</h1><p>Fallback comercial disponível na raiz.</p></body>
                    </html>
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    /** Encerra o servidor HTTP local usado pelos testes. */
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Garante que HTML válido gera snapshot CAPTURED com artefatos RAW_HTML e SCREENSHOT_PNG. */
    @Test
    void shouldCaptureRawHtmlAndScreenshotArtifacts() {
        String url = "http://localhost:" + server.getAddress().getPort() + "/sales";
        insertPage(1L, url);

        var response = service.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, false));

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.captured()).isEqualTo(1);
        assertThat(response.failed()).isZero();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().status()).isEqualTo("CAPTURED");
        assertThat(response.items().getFirst().rawHtmlBytes()).isPositive();
        assertThat(response.items().getFirst().screenshotBytes()).isPositive();

        Integer executionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mois_sales_page_job_execution WHERE sales_page_id = 1 AND stage = 'CAPTURE' AND status = 'CAPTURED'",
                Integer.class);
        Integer htmlCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM mois_sales_page_job_execution
                        WHERE sales_page_id = 1 AND raw_html LIKE '%Oferta Teste%'
                        """,
                Integer.class);
        Integer screenshotCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM mois_sales_page_job_execution
                        WHERE sales_page_id = 1 AND screenshot_blob IS NOT NULL
                        """,
                Integer.class);
        Integer pageCaptured = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mois_sales_page WHERE id = 1 AND current_stage = 'CAPTURE' AND current_status = 'CAPTURED'",
                Integer.class);

        assertThat(executionCount).isEqualTo(1);
        assertThat(htmlCount).isEqualTo(1);
        assertThat(screenshotCount).isEqualTo(1);
        assertThat(pageCaptured).isEqualTo(1);
        assertThat(service.listSnapshots(1)).hasSize(1);
    }

    /** Garante que falhas HTTP sejam categorizadas e persistam status HTTP para auditoria. */
    @Test
    void shouldCategorizeHttpFailureAndPersistHttpStatus() {
        String url = "http://localhost:" + server.getAddress().getPort() + "/missing";
        insertPage(2L, url);

        var response = service.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, false));

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.captured()).isZero();
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.items().getFirst().status()).isEqualTo("FAILED");
        assertThat(response.items().getFirst().httpStatus()).isEqualTo(404);
        assertThat(response.items().getFirst().errorMessage()).startsWith("HTTP_404");
        String persistedError = jdbcTemplate.queryForObject(
                "SELECT error_message FROM mois_sales_page_job_execution WHERE sales_page_id = 2 AND stage = 'CAPTURE'",
                String.class);
        Integer persistedHttpStatus = jdbcTemplate.queryForObject(
                "SELECT http_status FROM mois_sales_page_job_execution WHERE sales_page_id = 2 AND stage = 'CAPTURE'",
                Integer.class);
        assertThat(persistedError).startsWith("HTTP_404");
        assertThat(persistedHttpStatus).isEqualTo(404);
    }

    /** Garante que falhas recentes não sejam reprocessadas sem acionamento forçado. */
    @Test
    void shouldSkipRecentFailedSnapshotsWithoutForce() {
        String failedUrl = "http://localhost:" + server.getAddress().getPort() + "/missing";
        String eligibleUrl = "http://localhost:" + server.getAddress().getPort() + "/sales";
        insertPage(3L, failedUrl);
        insertPage(4L, eligibleUrl);
        insertFailedSnapshot(3L, Timestamp.from(Instant.now()));

        var response = service.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, false));

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.items().getFirst().pageId()).isEqualTo(4L);
        assertThat(response.items().getFirst().status()).isEqualTo("CAPTURED");
    }

    /** Garante que URLs com falhas recorrentes deixem de concorrer com URLs úteis. */
    @Test
    void shouldSkipRepeatedFailedSnapshotsWithoutForce() {
        String failedUrl = "http://localhost:" + server.getAddress().getPort() + "/missing";
        String eligibleUrl = "http://localhost:" + server.getAddress().getPort() + "/sales";
        insertPage(5L, failedUrl);
        insertPage(6L, eligibleUrl);
        insertFailedSnapshot(5L, Timestamp.from(Instant.parse("2026-05-01T10:00:00Z")));
        insertFailedSnapshot(5L, Timestamp.from(Instant.parse("2026-05-02T10:00:00Z")));
        insertFailedSnapshot(5L, Timestamp.from(Instant.parse("2026-05-03T10:00:00Z")));

        var response = service.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, false));

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.items().getFirst().pageId()).isEqualTo(6L);
        assertThat(response.items().getFirst().status()).isEqualTo("CAPTURED");
    }

    /** Garante que o modo forçado permita recapturar uma URL mesmo durante o cooldown de falha. */
    @Test
    void shouldAllowForceToBypassFailedCooldown() {
        String url = "http://localhost:" + server.getAddress().getPort() + "/sales";
        insertPage(7L, url);
        insertFailedSnapshot(7L, Timestamp.from(Instant.now()));

        var response = service.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, true));

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.items().getFirst().pageId()).isEqualTo(7L);
        assertThat(response.items().getFirst().status()).isEqualTo("CAPTURED");
    }


    /** Garante que o pipeline registre o destino redirecionado e capture a raiz quando o caminho final falha. */
    @Test
    void shouldTryRedirectRootWhenRedirectDestinationIsNotCapturable() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        String url = baseUrl + "/redirect-to-missing";
        insertPage(8L, url);

        var response = service.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, false));

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.captured()).isEqualTo(1);
        assertThat(response.failed()).isZero();
        assertThat(response.items().getFirst().status()).isEqualTo("CAPTURED");
        assertThat(response.items().getFirst().redirectDestinationUrl()).isEqualTo(baseUrl + "/missing");
        assertThat(response.items().getFirst().redirectRootUrl()).isEqualTo(baseUrl);
        String capturedHtml = jdbcTemplate.queryForObject(
                "SELECT raw_html FROM mois_sales_page_job_execution WHERE sales_page_id = 8 AND stage = 'CAPTURE'",
                String.class);
        assertThat(capturedHtml).contains("Oferta Raiz");
        var storedUrls = jdbcTemplate.queryForMap(
                "SELECT final_url, redirect_root_url FROM mois_sales_page_job_execution WHERE sales_page_id = 8 AND stage = 'CAPTURE'");
        assertThat(storedUrls.get("final_url")).isEqualTo(baseUrl + "/missing");
        assertThat(storedUrls.get("redirect_root_url")).isEqualTo(baseUrl);
    }

    /** Garante que páginas com html_bytes > 0 não sejam recapturadas nem em acionamento forçado da etapa 1. */
    @Test
    void shouldSkipPagesWithUsefulHtmlBytesEvenWhenForceIsEnabled() {
        String url = "http://localhost:" + server.getAddress().getPort() + "/sales";
        insertPageWithState(9L, url, "CAPTURE", "CAPTURED", 128L);

        var response = service.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, true));

        assertThat(response.processed()).isZero();
        Integer executions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mois_sales_page_job_execution WHERE sales_page_id = 9 AND stage = 'CAPTURE'",
                Integer.class);
        assertThat(executions).isZero();
    }

    /** Garante que a etapa 1 reprocesse páginas sem HTML útil mesmo quando o status anterior dizia CAPTURED. */
    @Test
    void shouldProcessCapturedStatusWhenHtmlBytesIsZero() {
        String url = "http://localhost:" + server.getAddress().getPort() + "/sales";
        insertPageWithState(10L, url, "CAPTURE", "CAPTURED", 0L);

        var response = service.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, false));

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.items().getFirst().pageId()).isEqualTo(10L);
        assertThat(response.items().getFirst().rawHtmlBytes()).isPositive();
        Long persistedHtmlBytes = jdbcTemplate.queryForObject(
                "SELECT html_bytes FROM mois_sales_page WHERE id = 10",
                Long.class);
        assertThat(persistedHtmlBytes).isPositive();
    }

    /** Fornece timestamp UTC compatível com a função usada no SQL MySQL dos testes. */
    public static Timestamp utcTimestamp() {
        return Timestamp.from(Instant.now());
    }

    /** Insere uma página operacional para os testes de seleção e captura. */
    private void insertPage(long pageId, String url) {
        insertPageWithState(pageId, url, "ANALYSIS", "PENDING", 0L);
    }

    /** Insere uma página operacional com estado e bytes de HTML controlados pelo cenário de teste. */
    private void insertPageWithState(long pageId, String url, String currentStage, String currentStatus, long htmlBytes) {
        jdbcTemplate.update("""
                INSERT INTO mois_sales_page
                (id, workspace_id, source, url_original, url_canonical, title, current_stage, current_status, capture_status, html_bytes,
                 ingest_count, created_at, updated_at)
                VALUES (?, 'workspace-001', 'TEST', ?, ?, 'Oferta Teste', ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, pageId, url, url, currentStage, currentStatus, currentStatus, htmlBytes);
    }

    /** Insere uma falha prévia para validar cooldown e limite de tentativas. */
    private void insertFailedSnapshot(long pageId, Timestamp capturedAt) {
        jdbcTemplate.update("""
                INSERT INTO mois_sales_page_job_execution
                (sales_page_id, workspace_id, job_type, stage, status, attempt, input_url, error_category, error_message, finished_at, created_at, updated_at)
                SELECT id, workspace_id, 'HTML_CAPTURE', 'CAPTURE', 'FAILED', 1, url_canonical, 'CAPTURE_FAILED',
                       'HTTP_404: página final não encontrada', ?, ?, ?
                FROM mois_sales_page
                WHERE id = ?
                """, capturedAt, capturedAt, capturedAt, pageId);
    }

    /** Cria o schema mínimo usado pelos testes de capturas no modelo consolidado. */
    private void createSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_sales_page_job_execution");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_sales_page");
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_page (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  workspace_id VARCHAR(120) NOT NULL,
                  source VARCHAR(40) NOT NULL,
                  title VARCHAR(512),
                  url_original VARCHAR(1024) NOT NULL,
                  url_canonical VARCHAR(1024) NOT NULL,
                  url_final VARCHAR(1024),
                  redirect_root_url VARCHAR(1024),
                  current_stage VARCHAR(40) NOT NULL,
                  current_status VARCHAR(40) NOT NULL,
                  capture_status VARCHAR(40),
                  http_status INT,
                  content_type VARCHAR(255),
                  html_sha256 VARCHAR(64),
                  html_bytes BIGINT NOT NULL DEFAULT 0,
                  last_error_category VARCHAR(120),
                  last_error_message VARCHAR(1000),
                  last_job_execution_id BIGINT,
                  ingest_count INT NOT NULL DEFAULT 1,
                  last_captured_at TIMESTAMP,
                  created_at TIMESTAMP NOT NULL,
                  updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_page_job_execution (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  sales_page_id BIGINT NOT NULL,
                  workspace_id VARCHAR(120) NOT NULL,
                  job_type VARCHAR(40) NOT NULL,
                  stage VARCHAR(40) NOT NULL,
                  status VARCHAR(40) NOT NULL,
                  attempt INT NOT NULL DEFAULT 1,
                  claimed_by VARCHAR(120),
                  input_url VARCHAR(1024),
                  final_url VARCHAR(1024),
                  redirect_root_url VARCHAR(1024),
                  http_status INT,
                  content_type VARCHAR(255),
                  raw_html LONGTEXT,
                  raw_html_sha256 VARCHAR(64),
                  raw_html_bytes BIGINT NOT NULL DEFAULT 0,
                  screenshot_blob BLOB,
                  screenshot_bytes BIGINT NOT NULL DEFAULT 0,
                  score_total DECIMAL(6,2),
                  error_category VARCHAR(120),
                  error_message VARCHAR(1000),
                  started_at TIMESTAMP,
                  finished_at TIMESTAMP,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

}
