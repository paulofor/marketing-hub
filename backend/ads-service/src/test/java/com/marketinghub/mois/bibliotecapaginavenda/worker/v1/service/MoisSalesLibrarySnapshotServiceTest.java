package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
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

public class MoisSalesLibrarySnapshotServiceTest {

    private JdbcTemplate jdbcTemplate;
    private HttpServer server;
    private MoisSalesLibrarySnapshotService service;

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
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldCaptureRawHtmlAndScreenshotArtifacts() {
        String url = "http://localhost:" + server.getAddress().getPort() + "/sales";
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_url_ingest
                (id, workspace_id, source, url_original, url_canonical, title, ingest_count, created_at, updated_at)
                VALUES (1, 'workspace-001', 'TEST', ?, ?, 'Oferta Teste', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, url, url);

        var response = service.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, false));

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.captured()).isEqualTo(1);
        assertThat(response.failed()).isZero();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().status()).isEqualTo("CAPTURED");
        assertThat(response.items().getFirst().rawHtmlBytes()).isPositive();
        assertThat(response.items().getFirst().screenshotBytes()).isPositive();

        Integer snapshotCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_page_snapshot", Integer.class);
        Integer artifactCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_snapshot_artifact", Integer.class);
        Integer htmlCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_snapshot_artifact WHERE artifact_type = 'RAW_HTML' AND content_text LIKE '%Oferta Teste%'", Integer.class);
        Integer screenshotCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_snapshot_artifact WHERE artifact_type = 'SCREENSHOT_PNG' AND content_blob IS NOT NULL", Integer.class);

        assertThat(snapshotCount).isEqualTo(1);
        assertThat(artifactCount).isEqualTo(2);
        assertThat(htmlCount).isEqualTo(1);
        assertThat(screenshotCount).isEqualTo(1);
        assertThat(service.listSnapshots(1)).hasSize(1);
    }

    public static Timestamp utcTimestamp() {
        return Timestamp.from(Instant.now());
    }

    private void createSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_library_url_ingest (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  workspace_id VARCHAR(120) NOT NULL,
                  source VARCHAR(40) NOT NULL,
                  url_original VARCHAR(1024) NOT NULL,
                  url_canonical VARCHAR(1024) NOT NULL,
                  title VARCHAR(512),
                  first_captured_at TIMESTAMP,
                  last_captured_at TIMESTAMP,
                  ingest_count INT NOT NULL DEFAULT 1,
                  created_at TIMESTAMP NOT NULL,
                  updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_library_page_snapshot (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  url_ingest_id BIGINT NOT NULL,
                  snapshot_hash VARCHAR(128),
                  status VARCHAR(32) NOT NULL,
                  http_status INT,
                  content_type VARCHAR(255),
                  error_message VARCHAR(1000),
                  captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_library_snapshot_artifact (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  snapshot_id BIGINT NOT NULL,
                  artifact_type VARCHAR(40) NOT NULL,
                  content_type VARCHAR(255) NOT NULL,
                  storage_kind VARCHAR(40) NOT NULL,
                  content_text LONGTEXT,
                  content_blob LONGBLOB,
                  size_bytes BIGINT NOT NULL DEFAULT 0,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }
}
