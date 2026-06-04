package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Valida a escrita dupla temporária da biblioteca MOIS para o modelo consolidado de páginas de venda.
 */
public class MoisSalesPageDualWriteRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private MoisSalesPageDualWriteRepository repository;

    /**
     * Prepara schema mínimo em H2 compatível com MySQL para exercitar a sincronização consolidada.
     */
    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:mois_dual_write;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE ALIAS IF NOT EXISTS UTC_TIMESTAMP FOR \"com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageDualWriteRepositoryTest.utcTimestamp\"");
        createSchema();
        repository = new MoisSalesPageDualWriteRepository(jdbcTemplate);
    }

    /**
     * Garante que ingestão e job pendente sejam espelhados em página consolidada e execução nova.
     */
    @Test
    void shouldMirrorUrlIngestAndProcessingJob() {
        insertLegacyUrlAndJob("PENDING");

        repository.syncUrlIngest(1L, 10L);

        var page = jdbcTemplate.queryForMap("SELECT current_stage, current_status, last_job_execution_id FROM mois_sales_page WHERE url_canonical = 'https://offer.test/'");
        Integer executions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_page_job_execution WHERE job_type = 'PROCESSING_JOB' AND status = 'PENDING'", Integer.class);
        assertThat(page.get("current_stage")).isEqualTo("ANALYSIS");
        assertThat(page.get("current_status")).isEqualTo("PENDING");
        assertThat(page.get("last_job_execution_id")).isNotNull();
        assertThat(executions).isEqualTo(1);
    }

    /**
     * Garante que análise concluída atualize o estado consolidado e registre execução PAGE_ANALYSIS.
     */
    @Test
    void shouldMirrorLatestAnalysisState() {
        insertLegacyUrlAndJob("DONE");
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_page_analysis
                (id, url_ingest_id, job_id, status, score_total, sections_json, copy_json, visual_json, image_json, request_payload_json, analyzed_at, created_at, updated_at)
                VALUES (20, 1, 10, 'DONE', 88.50, '{}', '{}', '{}', '{}', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        repository.syncLatestAnalysis(1L);

        var page = jdbcTemplate.queryForMap("SELECT current_stage, current_status, analysis_status, score_total FROM mois_sales_page WHERE url_canonical = 'https://offer.test/'");
        Integer executions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_page_job_execution WHERE job_type = 'PAGE_ANALYSIS' AND status = 'DONE'", Integer.class);
        assertThat(page.get("current_stage")).isEqualTo("ANALYSIS");
        assertThat(page.get("current_status")).isEqualTo("DONE");
        assertThat(page.get("analysis_status")).isEqualTo("DONE");
        assertThat(page.get("score_total").toString()).isEqualTo("88.50");
        assertThat(executions).isEqualTo(1);
    }

    /**
     * Garante que snapshot capturado alimente status de captura e histórico consolidado com HTML bruto.
     */
    @Test
    void shouldMirrorCapturedSnapshot() {
        insertLegacyUrlAndJob("DONE");
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_page_snapshot
                (id, url_ingest_id, snapshot_hash, status, http_status, content_type, redirect_destination_url, redirect_root_url, captured_at, created_at, updated_at)
                VALUES (30, 1, 'abc123', 'CAPTURED', 200, 'text/html', 'https://offer.test/final', 'https://offer.test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_snapshot_artifact
                (id, snapshot_id, artifact_type, content_type, storage_kind, content_text, size_bytes, created_at)
                VALUES (40, 30, 'RAW_HTML', 'text/html', 'DATABASE_TEXT', '<html>Oferta</html>', 19, CURRENT_TIMESTAMP)
                """);

        repository.syncSnapshot(30L);

        var page = jdbcTemplate.queryForMap("SELECT current_stage, current_status, capture_status, html_sha256, html_bytes FROM mois_sales_page WHERE url_canonical = 'https://offer.test/'");
        String rawHtml = jdbcTemplate.queryForObject("SELECT raw_html FROM mois_sales_page_job_execution WHERE job_type = 'PAGE_SNAPSHOT'", String.class);
        assertThat(page.get("current_stage")).isEqualTo("CAPTURE");
        assertThat(page.get("current_status")).isEqualTo("CAPTURED");
        assertThat(page.get("capture_status")).isEqualTo("CAPTURED");
        assertThat(page.get("html_sha256")).isEqualTo("abc123");
        assertThat(page.get("html_bytes")).isEqualTo(19L);
        assertThat(rawHtml).contains("Oferta");
    }

    /**
     * Insere a URL e o job legados compartilhados pelos cenários de escrita dupla.
     */
    private void insertLegacyUrlAndJob(String jobStatus) {
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_url_ingest
                (id, workspace_id, source, url_original, url_canonical, title, ingest_count, first_captured_at, last_captured_at, created_at, updated_at)
                VALUES (1, 'workspace-001', 'HOTMART', 'https://offer.test/', 'https://offer.test/', 'Oferta', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_processing_job
                (id, url_ingest_id, status, attempts, created_at, updated_at, started_at, finished_at)
                VALUES (10, 1, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, jobStatus);
    }

    /**
     * Cria as tabelas mínimas acessadas pelos SQLs de escrita dupla.
     */
    private void createSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_sales_page_job_execution");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_sales_page");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_sales_library_snapshot_artifact");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_sales_library_page_snapshot");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_sales_library_page_analysis");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_sales_library_processing_job");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_sales_library_url_ingest");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_collected_reference_html_capture");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mois_collected_reference");
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_library_url_ingest (
                  id BIGINT PRIMARY KEY,
                  workspace_id VARCHAR(120), source VARCHAR(40), url_original VARCHAR(1024), url_canonical VARCHAR(1024), title VARCHAR(512),
                  first_captured_at TIMESTAMP, last_captured_at TIMESTAMP, ingest_count INT, created_at TIMESTAMP, updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_library_processing_job (
                  id BIGINT PRIMARY KEY, url_ingest_id BIGINT, status VARCHAR(40), attempts INT, error_category VARCHAR(120), error_message VARCHAR(1000),
                  next_retry_at TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP, started_at TIMESTAMP, finished_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_library_page_analysis (
                  id BIGINT PRIMARY KEY, url_ingest_id BIGINT, job_id BIGINT, status VARCHAR(40), score_total DECIMAL(6,2), parser_version VARCHAR(40),
                  prompt_version VARCHAR(40), model_name VARCHAR(120), sections_json LONGTEXT, copy_json LONGTEXT, visual_json LONGTEXT, image_json LONGTEXT,
                  analysis_notes VARCHAR(1000), request_payload_json LONGTEXT, analyzed_at TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_library_page_snapshot (
                  id BIGINT PRIMARY KEY, url_ingest_id BIGINT, snapshot_hash VARCHAR(64), status VARCHAR(40), http_status INT, content_type VARCHAR(255),
                  redirect_destination_url VARCHAR(1024), redirect_root_url VARCHAR(1024), error_message VARCHAR(1000), captured_at TIMESTAMP,
                  created_at TIMESTAMP, updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_library_snapshot_artifact (
                  id BIGINT PRIMARY KEY, snapshot_id BIGINT, artifact_type VARCHAR(40), content_type VARCHAR(255), storage_kind VARCHAR(40),
                  content_text LONGTEXT, content_blob LONGBLOB, size_bytes BIGINT, created_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_collected_reference (
                  id BIGINT PRIMARY KEY, workspace_id VARCHAR(120), source VARCHAR(40), job_id VARCHAR(120), reference_id VARCHAR(120), title VARCHAR(512),
                  product_name VARCHAR(512), url VARCHAR(1024), product_url VARCHAR(1024), sales_page_url VARCHAR(1024), collected_at TIMESTAMP, updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_collected_reference_html_capture (
                  id BIGINT PRIMARY KEY, collected_reference_id BIGINT, workspace_id VARCHAR(120), source VARCHAR(40), collection_job_id VARCHAR(120),
                  reference_id VARCHAR(120), title VARCHAR(512), url_source VARCHAR(40), url_original VARCHAR(1024), url_final VARCHAR(1024), status VARCHAR(40),
                  claimed_by VARCHAR(120), claimed_at TIMESTAMP, http_status INT, content_type VARCHAR(255), raw_html LONGTEXT, raw_html_sha256 VARCHAR(64),
                  raw_html_bytes BIGINT, error_message VARCHAR(1000), fetched_at TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_page (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, workspace_id VARCHAR(120), source VARCHAR(40), source_job_id VARCHAR(120), source_reference_id VARCHAR(120),
                  collected_reference_id BIGINT, product_name VARCHAR(512), title VARCHAR(512), url_original VARCHAR(1024), url_canonical VARCHAR(1024),
                  sales_page_url VARCHAR(1024), product_url VARCHAR(1024), url_final VARCHAR(1024), redirect_root_url VARCHAR(1024), current_stage VARCHAR(40),
                  current_status VARCHAR(40), capture_status VARCHAR(40), analysis_status VARCHAR(40), http_status INT, content_type VARCHAR(255),
                  html_sha256 VARCHAR(64), html_bytes BIGINT, score_total DECIMAL(6,2), offer_summary VARCHAR(1000), mechanism_summary VARCHAR(1000),
                  promise_summary VARCHAR(1000), proof_summary VARCHAR(1000), last_error_category VARCHAR(120), last_error_message VARCHAR(1000),
                  last_job_execution_id BIGINT, ingest_count INT, first_seen_at TIMESTAMP, last_collected_at TIMESTAMP, last_captured_at TIMESTAMP,
                  last_analyzed_at TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP, UNIQUE (workspace_id, url_canonical)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mois_sales_page_job_execution (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, sales_page_id BIGINT, workspace_id VARCHAR(120), job_type VARCHAR(40), stage VARCHAR(40), status VARCHAR(40),
                  attempt INT, claimed_by VARCHAR(120), input_url VARCHAR(1024), final_url VARCHAR(1024), redirect_root_url VARCHAR(1024), http_status INT,
                  content_type VARCHAR(255), raw_html LONGTEXT, raw_html_sha256 VARCHAR(64), raw_html_bytes BIGINT, screenshot_blob LONGBLOB, screenshot_bytes BIGINT,
                  score_total DECIMAL(6,2), sections_json LONGTEXT, copy_json LONGTEXT, visual_json LONGTEXT, image_json LONGTEXT, request_payload_json LONGTEXT,
                  response_payload_json LONGTEXT, error_category VARCHAR(120), error_message VARCHAR(1000), started_at TIMESTAMP, finished_at TIMESTAMP,
                  created_at TIMESTAMP, updated_at TIMESTAMP
                )
                """);
    }

    /**
     * Fornece timestamp UTC compatível com a função usada nos SQLs MySQL dos testes.
     */
    public static Timestamp utcTimestamp() {
        return Timestamp.from(Instant.now());
    }
}
