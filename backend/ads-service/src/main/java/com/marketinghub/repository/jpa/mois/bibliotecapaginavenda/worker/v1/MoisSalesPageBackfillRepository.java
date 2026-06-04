package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Executa a persistência JDBC do backfill inicial do modelo consolidado de páginas de venda MOIS.
 */
@Repository
@RequiredArgsConstructor
public class MoisSalesPageBackfillRepository implements MoisSalesPageBackfillGateway {

    private final JdbcTemplate jdbcTemplate;


    /**
     * Verifica se as tabelas necessárias ao backfill existem antes de executar consultas de migração.
     */
    @Override
    public boolean hasBackfillTables() {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            return tableExists(metadata, "mois_sales_library_url_ingest")
                    && tableExists(metadata, "mois_sales_library_processing_job")
                    && tableExists(metadata, "mois_sales_library_page_analysis")
                    && tableExists(metadata, "mois_sales_library_page_snapshot")
                    && tableExists(metadata, "mois_sales_page")
                    && tableExists(metadata, "mois_sales_page_job_execution");
        }));
    }

    /**
     * Consulta o metadado JDBC considerando diferenças de capitalização entre MySQL e bancos de teste.
     */
    private boolean tableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
        return tableExistsWithName(metadata, tableName) || tableExistsWithName(metadata, tableName.toUpperCase());
    }

    /**
     * Consulta uma variação de nome de tabela no metadado JDBC.
     */
    private boolean tableExistsWithName(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, tableName, new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    /**
     * Conta páginas consolidadas no modelo novo para medir o progresso do backfill.
     */
    @Override
    public long countSalesPages() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_page", Long.class);
        return count == null ? 0 : count;
    }

    /**
     * Conta execuções no histórico novo para medir a cobertura de auditoria migrada.
     */
    @Override
    public long countJobExecutions() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_page_job_execution", Long.class);
        return count == null ? 0 : count;
    }

    /**
     * Conta URLs consolidadas no modelo legado que devem existir no novo estado operacional.
     */
    @Override
    public long countLegacyUrlIngests() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_url_ingest", Long.class);
        return count == null ? 0 : count;
    }

    /**
     * Insere ou atualiza o estado atual consolidado em mois_sales_page a partir das tabelas legadas.
     */
    @Override
    public int backfillSalesPages() {
        return jdbcTemplate.update("""
                INSERT INTO mois_sales_page
                (workspace_id, source, source_job_id, source_reference_id, collected_reference_id, product_name, title,
                 url_original, url_canonical, sales_page_url, product_url, url_final, redirect_root_url,
                 current_stage, current_status, capture_status, analysis_status, http_status, content_type,
                 html_sha256, html_bytes, score_total, last_error_category, last_error_message, last_job_execution_id,
                 ingest_count, first_seen_at, last_collected_at, last_captured_at, last_analyzed_at, created_at, updated_at)
                SELECT
                  i.workspace_id,
                  i.source,
                  COALESCE(r.job_id, c.collection_job_id) AS source_job_id,
                  COALESCE(r.reference_id, c.reference_id) AS source_reference_id,
                  r.id AS collected_reference_id,
                  r.product_name,
                  COALESCE(NULLIF(r.product_name, ''), NULLIF(i.title, ''), NULLIF(r.title, ''), i.url_canonical) AS title,
                  i.url_original,
                  i.url_canonical,
                  r.sales_page_url,
                  r.product_url,
                  COALESCE(s.redirect_destination_url, c.url_final, i.url_canonical) AS url_final,
                  s.redirect_root_url,
                  CASE
                    WHEN a.id IS NOT NULL THEN 'ANALYSIS'
                    WHEN s.id IS NOT NULL OR c.id IS NOT NULL THEN 'CAPTURE'
                    WHEN j.id IS NOT NULL THEN 'ANALYSIS'
                    ELSE 'INGEST'
                  END AS current_stage,
                  COALESCE(a.status, s.status, c.status, j.status, 'INGESTED') AS current_status,
                  COALESCE(s.status, c.status) AS capture_status,
                  a.status AS analysis_status,
                  COALESCE(s.http_status, c.http_status) AS http_status,
                  COALESCE(s.content_type, c.content_type) AS content_type,
                  COALESCE(s.snapshot_hash, c.raw_html_sha256) AS html_sha256,
                  COALESCE(raw.size_bytes, c.raw_html_bytes, 0) AS html_bytes,
                  a.score_total,
                  COALESCE(j.error_category,
                           CASE WHEN s.status = 'FAILED' THEN 'CAPTURE_FAILED' END,
                           CASE WHEN c.status = 'FAILED' THEN 'CAPTURE_FAILED' END) AS last_error_category,
                  COALESCE(j.error_message, s.error_message, c.error_message,
                           CASE WHEN a.status NOT IN ('DONE', 'PENDING') THEN a.analysis_notes END) AS last_error_message,
                  NULL AS last_job_execution_id,
                  i.ingest_count,
                  COALESCE(i.first_captured_at, i.created_at) AS first_seen_at,
                  COALESCE(r.collected_at, i.last_captured_at) AS last_collected_at,
                  COALESCE(s.captured_at, c.fetched_at) AS last_captured_at,
                  a.analyzed_at AS last_analyzed_at,
                  i.created_at,
                  UTC_TIMESTAMP() AS updated_at
                FROM mois_sales_library_url_ingest i
                LEFT JOIN mois_sales_library_processing_job j ON j.id = (
                  SELECT j2.id FROM mois_sales_library_processing_job j2
                  WHERE j2.url_ingest_id = i.id
                  ORDER BY j2.updated_at DESC, j2.id DESC LIMIT 1
                )
                LEFT JOIN mois_sales_library_page_analysis a ON a.id = (
                  SELECT a2.id FROM mois_sales_library_page_analysis a2
                  WHERE a2.url_ingest_id = i.id
                  ORDER BY a2.updated_at DESC, a2.id DESC LIMIT 1
                )
                LEFT JOIN mois_sales_library_page_snapshot s ON s.id = (
                  SELECT s2.id FROM mois_sales_library_page_snapshot s2
                  WHERE s2.url_ingest_id = i.id
                  ORDER BY s2.captured_at DESC, s2.id DESC LIMIT 1
                )
                LEFT JOIN mois_sales_library_snapshot_artifact raw
                  ON raw.snapshot_id = s.id AND raw.artifact_type = 'RAW_HTML'
                LEFT JOIN mois_collected_reference r ON r.id = (
                  SELECT r2.id FROM mois_collected_reference r2
                  WHERE r2.workspace_id = i.workspace_id
                    AND r2.source = i.source
                    AND (r2.sales_page_url IN (i.url_original, i.url_canonical)
                      OR r2.product_url IN (i.url_original, i.url_canonical)
                      OR r2.url IN (i.url_original, i.url_canonical))
                  ORDER BY r2.collected_at DESC, r2.id DESC LIMIT 1
                )
                LEFT JOIN mois_collected_reference_html_capture c ON c.id = (
                  SELECT c2.id FROM mois_collected_reference_html_capture c2
                  WHERE (r.id IS NOT NULL AND c2.collected_reference_id = r.id)
                     OR (r.id IS NULL
                       AND c2.workspace_id = i.workspace_id
                       AND c2.source = i.source
                       AND c2.url_original IN (i.url_original, i.url_canonical))
                  ORDER BY c2.updated_at DESC, c2.id DESC LIMIT 1
                )
                ON DUPLICATE KEY UPDATE
                  source = VALUES(source),
                  source_job_id = VALUES(source_job_id),
                  source_reference_id = VALUES(source_reference_id),
                  collected_reference_id = VALUES(collected_reference_id),
                  product_name = VALUES(product_name),
                  title = VALUES(title),
                  url_original = VALUES(url_original),
                  sales_page_url = VALUES(sales_page_url),
                  product_url = VALUES(product_url),
                  url_final = VALUES(url_final),
                  redirect_root_url = VALUES(redirect_root_url),
                  current_stage = VALUES(current_stage),
                  current_status = VALUES(current_status),
                  capture_status = VALUES(capture_status),
                  analysis_status = VALUES(analysis_status),
                  http_status = VALUES(http_status),
                  content_type = VALUES(content_type),
                  html_sha256 = VALUES(html_sha256),
                  html_bytes = VALUES(html_bytes),
                  score_total = VALUES(score_total),
                  last_error_category = VALUES(last_error_category),
                  last_error_message = VALUES(last_error_message),
                  ingest_count = VALUES(ingest_count),
                  first_seen_at = VALUES(first_seen_at),
                  last_collected_at = VALUES(last_collected_at),
                  last_captured_at = VALUES(last_captured_at),
                  last_analyzed_at = VALUES(last_analyzed_at),
                  updated_at = UTC_TIMESTAMP()
                """);
    }

    /**
     * Migra o último job legado de processamento de cada página como execução de análise.
     */
    @Override
    public int backfillLatestProcessingJobs() {
        return jdbcTemplate.update("""
                INSERT INTO mois_sales_page_job_execution
                (sales_page_id, workspace_id, job_type, stage, status, attempt, input_url, error_category, error_message,
                 started_at, finished_at, created_at, updated_at)
                SELECT sp.id, i.workspace_id, 'PROCESSING_JOB', 'ANALYSIS', j.status,
                       GREATEST(COALESCE(j.attempts, 0), 1), i.url_canonical, j.error_category, j.error_message,
                       j.started_at, j.finished_at, j.created_at, j.updated_at
                FROM mois_sales_library_processing_job j
                JOIN mois_sales_library_url_ingest i ON i.id = j.url_ingest_id
                JOIN mois_sales_page sp ON sp.workspace_id = i.workspace_id AND sp.url_canonical = i.url_canonical
                WHERE j.id = (
                  SELECT j2.id FROM mois_sales_library_processing_job j2
                  WHERE j2.url_ingest_id = i.id
                  ORDER BY j2.updated_at DESC, j2.id DESC LIMIT 1
                )
                  AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution e
                    WHERE e.sales_page_id = sp.id
                      AND e.job_type = 'PROCESSING_JOB'
                      AND e.stage = 'ANALYSIS'
                      AND e.created_at = j.created_at
                      AND e.updated_at = j.updated_at
                  )
                """);
    }

    /**
     * Migra a última análise de cada página como execução de auditoria de análise.
     */
    @Override
    public int backfillLatestAnalyses() {
        return jdbcTemplate.update("""
                INSERT INTO mois_sales_page_job_execution
                (sales_page_id, workspace_id, job_type, stage, status, attempt, input_url, score_total,
                 sections_json, copy_json, visual_json, image_json, request_payload_json, error_message,
                 finished_at, created_at, updated_at)
                SELECT sp.id, i.workspace_id, 'PAGE_ANALYSIS', 'ANALYSIS', a.status, 1, i.url_canonical, a.score_total,
                       a.sections_json, a.copy_json, a.visual_json, a.image_json, a.request_payload_json,
                       CASE WHEN a.status NOT IN ('DONE', 'PENDING') THEN a.analysis_notes END,
                       a.analyzed_at, a.created_at, a.updated_at
                FROM mois_sales_library_page_analysis a
                JOIN mois_sales_library_url_ingest i ON i.id = a.url_ingest_id
                JOIN mois_sales_page sp ON sp.workspace_id = i.workspace_id AND sp.url_canonical = i.url_canonical
                WHERE a.id = (
                  SELECT a2.id FROM mois_sales_library_page_analysis a2
                  WHERE a2.url_ingest_id = i.id
                  ORDER BY a2.updated_at DESC, a2.id DESC LIMIT 1
                )
                  AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution e
                    WHERE e.sales_page_id = sp.id
                      AND e.job_type = 'PAGE_ANALYSIS'
                      AND e.stage = 'ANALYSIS'
                      AND e.created_at = a.created_at
                      AND e.updated_at = a.updated_at
                  )
                """);
    }

    /**
     * Migra o último snapshot de cada página como execução de auditoria da captura.
     */
    @Override
    public int backfillLatestSnapshots() {
        return jdbcTemplate.update("""
                INSERT INTO mois_sales_page_job_execution
                (sales_page_id, workspace_id, job_type, stage, status, attempt, input_url, final_url, redirect_root_url,
                 http_status, content_type, raw_html, raw_html_sha256, raw_html_bytes, screenshot_blob, screenshot_bytes,
                 error_category, error_message, started_at, finished_at, created_at, updated_at)
                SELECT sp.id, i.workspace_id, 'PAGE_SNAPSHOT', 'CAPTURE', s.status, 1, i.url_canonical,
                       s.redirect_destination_url, s.redirect_root_url, s.http_status, s.content_type,
                       raw.content_text, s.snapshot_hash, COALESCE(raw.size_bytes, 0), png.content_blob, COALESCE(png.size_bytes, 0),
                       CASE WHEN s.status = 'FAILED' THEN 'CAPTURE_FAILED' END, s.error_message,
                       s.created_at, s.captured_at, s.created_at, s.updated_at
                FROM mois_sales_library_page_snapshot s
                JOIN mois_sales_library_url_ingest i ON i.id = s.url_ingest_id
                JOIN mois_sales_page sp ON sp.workspace_id = i.workspace_id AND sp.url_canonical = i.url_canonical
                LEFT JOIN mois_sales_library_snapshot_artifact raw
                  ON raw.snapshot_id = s.id AND raw.artifact_type = 'RAW_HTML'
                LEFT JOIN mois_sales_library_snapshot_artifact png
                  ON png.snapshot_id = s.id AND png.artifact_type = 'SCREENSHOT_PNG'
                WHERE s.id = (
                  SELECT s2.id FROM mois_sales_library_page_snapshot s2
                  WHERE s2.url_ingest_id = i.id
                  ORDER BY s2.captured_at DESC, s2.id DESC LIMIT 1
                )
                  AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution e
                    WHERE e.sales_page_id = sp.id
                      AND e.job_type = 'PAGE_SNAPSHOT'
                      AND e.stage = 'CAPTURE'
                      AND e.created_at = s.created_at
                      AND e.updated_at = s.updated_at
                  )
                """);
    }

    /**
     * Migra a última captura bruta ligada a uma referência coletada quando há vínculo com a página consolidada.
     */
    @Override
    public int backfillLatestCollectedReferenceHtmlCaptures() {
        return jdbcTemplate.update("""
                INSERT INTO mois_sales_page_job_execution
                (sales_page_id, workspace_id, job_type, stage, status, attempt, claimed_by, input_url, final_url,
                 http_status, content_type, raw_html, raw_html_sha256, raw_html_bytes, error_category, error_message,
                 started_at, finished_at, created_at, updated_at)
                SELECT sp.id, c.workspace_id, 'COLLECTED_REFERENCE_HTML', 'CAPTURE', c.status, 1, c.claimed_by,
                       c.url_original, c.url_final, c.http_status, c.content_type, c.raw_html, c.raw_html_sha256,
                       COALESCE(c.raw_html_bytes, 0), CASE WHEN c.status = 'FAILED' THEN 'CAPTURE_FAILED' END,
                       c.error_message, c.claimed_at, c.fetched_at, c.created_at, c.updated_at
                FROM mois_collected_reference_html_capture c
                JOIN mois_sales_page sp ON sp.collected_reference_id = c.collected_reference_id
                WHERE c.id = (
                  SELECT c2.id FROM mois_collected_reference_html_capture c2
                  WHERE c2.collected_reference_id = c.collected_reference_id
                  ORDER BY c2.updated_at DESC, c2.id DESC LIMIT 1
                )
                  AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution e
                    WHERE e.sales_page_id = sp.id
                      AND e.job_type = 'COLLECTED_REFERENCE_HTML'
                      AND e.stage = 'CAPTURE'
                      AND e.created_at = c.created_at
                      AND e.updated_at = c.updated_at
                  )
                """);
    }

    /**
     * Atualiza cada página consolidada com o ponteiro para a execução mais recente migrada.
     */
    @Override
    public int updateLastJobExecutionPointers() {
        return jdbcTemplate.update("""
                UPDATE mois_sales_page sp
                SET sp.last_job_execution_id = (
                  SELECT e.id FROM mois_sales_page_job_execution e
                  WHERE e.sales_page_id = sp.id
                  ORDER BY e.updated_at DESC, e.id DESC LIMIT 1
                ),
                sp.updated_at = UTC_TIMESTAMP()
                WHERE EXISTS (
                  SELECT 1 FROM mois_sales_page_job_execution e2 WHERE e2.sales_page_id = sp.id
                )
                """);
    }
}
