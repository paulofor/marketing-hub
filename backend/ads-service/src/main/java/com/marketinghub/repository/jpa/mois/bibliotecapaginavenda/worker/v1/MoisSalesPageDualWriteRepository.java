package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Executa as escritas espelhadas do pipeline legado da biblioteca MOIS nas tabelas consolidadas de páginas de venda.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MoisSalesPageDualWriteRepository implements MoisSalesPageDualWriteGateway {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Sincroniza a URL legada e o job de análise inicial no modelo consolidado.
     */
    @Override
    public void syncUrlIngest(long urlIngestId, Long processingJobId) {
        try {
            syncSalesPageFromLegacyUrlIngest(urlIngestId);
            if (processingJobId != null) {
                insertProcessingJobExecution(processingJobId);
            }
            updateLastJobExecutionPointer(urlIngestId);
            logDivergenceForUrlIngest(urlIngestId);
        } catch (RuntimeException ex) {
            log.error("Falha na escrita dupla de ingestão MOIS. modulo=MOIS, operacao=syncUrlIngest, urlIngestId={}, processingJobId={}, erroClasse={}, erro={}",
                    urlIngestId, processingJobId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Sincroniza o job legado atualizado no modelo consolidado.
     */
    @Override
    public void syncProcessingJob(long processingJobId) {
        try {
            Long urlIngestId = findUrlIngestIdByProcessingJob(processingJobId);
            if (urlIngestId == null) {
                log.warn("Divergência MOIS sales page: job legado sem URL para escrita dupla. modulo=MOIS, operacao=syncProcessingJob, processingJobId={}", processingJobId);
                return;
            }
            syncSalesPageFromLegacyUrlIngest(urlIngestId);
            insertProcessingJobExecution(processingJobId);
            updateLastJobExecutionPointer(urlIngestId);
            logDivergenceForUrlIngest(urlIngestId);
        } catch (RuntimeException ex) {
            log.error("Falha na escrita dupla de job MOIS. modulo=MOIS, operacao=syncProcessingJob, processingJobId={}, erroClasse={}, erro={}",
                    processingJobId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Sincroniza a análise legada mais recente no modelo consolidado.
     */
    @Override
    public void syncLatestAnalysis(long urlIngestId) {
        try {
            syncSalesPageFromLegacyUrlIngest(urlIngestId);
            insertLatestAnalysisExecution(urlIngestId);
            updateLastJobExecutionPointer(urlIngestId);
            logDivergenceForUrlIngest(urlIngestId);
        } catch (RuntimeException ex) {
            log.error("Falha na escrita dupla de análise MOIS. modulo=MOIS, operacao=syncLatestAnalysis, urlIngestId={}, erroClasse={}, erro={}",
                    urlIngestId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Sincroniza o snapshot legado no histórico consolidado e atualiza o estado atual da página.
     */
    @Override
    public void syncSnapshot(long snapshotId) {
        try {
            Long urlIngestId = findUrlIngestIdBySnapshot(snapshotId);
            if (urlIngestId == null) {
                log.warn("Divergência MOIS sales page: snapshot legado sem URL para escrita dupla. modulo=MOIS, operacao=syncSnapshot, snapshotId={}", snapshotId);
                return;
            }
            syncSalesPageFromLegacyUrlIngest(urlIngestId);
            insertSnapshotExecution(snapshotId);
            updateLastJobExecutionPointer(urlIngestId);
            logDivergenceForUrlIngest(urlIngestId);
        } catch (RuntimeException ex) {
            log.error("Falha na escrita dupla de snapshot MOIS. modulo=MOIS, operacao=syncSnapshot, snapshotId={}, erroClasse={}, erro={}",
                    snapshotId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Sincroniza a captura bruta de referência coletada no histórico consolidado quando houver página vinculada.
     */
    @Override
    public void syncCollectedReferenceHtmlCapture(long captureId) {
        try {
            List<Long> urlIngestIds = findUrlIngestIdsByCollectedCapture(captureId);
            for (Long urlIngestId : urlIngestIds) {
                syncSalesPageFromLegacyUrlIngest(urlIngestId);
            }
            insertCollectedReferenceHtmlExecution(captureId);
            for (Long urlIngestId : urlIngestIds) {
                updateLastJobExecutionPointer(urlIngestId);
                logDivergenceForUrlIngest(urlIngestId);
            }
        } catch (RuntimeException ex) {
            log.error("Falha na escrita dupla de captura bruta MOIS. modulo=MOIS, operacao=syncCollectedReferenceHtmlCapture, captureId={}, erroClasse={}, erro={}",
                    captureId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Recalcula o estado consolidado de uma página a partir das tabelas legadas ainda oficiais nesta fase.
     */
    private void syncSalesPageFromLegacyUrlIngest(long urlIngestId) {
        jdbcTemplate.update("""
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
                WHERE i.id = ?
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
                """, urlIngestId);
    }

    /**
     * Insere o estado atual do job legado como evento auditável do novo histórico.
     */
    private void insertProcessingJobExecution(long processingJobId) {
        jdbcTemplate.update("""
                INSERT INTO mois_sales_page_job_execution
                (sales_page_id, workspace_id, job_type, stage, status, attempt, input_url, error_category, error_message,
                 started_at, finished_at, created_at, updated_at)
                SELECT sp.id, i.workspace_id, 'PROCESSING_JOB', 'ANALYSIS', j.status,
                       GREATEST(COALESCE(j.attempts, 0), 1), i.url_canonical, j.error_category, j.error_message,
                       j.started_at, j.finished_at, j.created_at, j.updated_at
                FROM mois_sales_library_processing_job j
                JOIN mois_sales_library_url_ingest i ON i.id = j.url_ingest_id
                JOIN mois_sales_page sp ON sp.workspace_id = i.workspace_id AND sp.url_canonical = i.url_canonical
                WHERE j.id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution e
                    WHERE e.sales_page_id = sp.id
                      AND e.job_type = 'PROCESSING_JOB'
                      AND e.stage = 'ANALYSIS'
                      AND e.status = j.status
                      AND e.created_at = j.created_at
                      AND e.updated_at = j.updated_at
                  )
                """, processingJobId);
    }

    /**
     * Insere a análise legada mais recente da página como evento auditável do novo histórico.
     */
    private void insertLatestAnalysisExecution(long urlIngestId) {
        jdbcTemplate.update("""
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
                WHERE i.id = ?
                  AND a.id = (
                    SELECT a2.id FROM mois_sales_library_page_analysis a2
                    WHERE a2.url_ingest_id = i.id
                    ORDER BY a2.updated_at DESC, a2.id DESC LIMIT 1
                  )
                  AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution e
                    WHERE e.sales_page_id = sp.id
                      AND e.job_type = 'PAGE_ANALYSIS'
                      AND e.stage = 'ANALYSIS'
                      AND e.status = a.status
                      AND e.created_at = a.created_at
                      AND e.updated_at = a.updated_at
                  )
                """, urlIngestId);
    }

    /**
     * Insere o snapshot legado como evento auditável do novo histórico de captura.
     */
    private void insertSnapshotExecution(long snapshotId) {
        jdbcTemplate.update("""
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
                WHERE s.id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution e
                    WHERE e.sales_page_id = sp.id
                      AND e.job_type = 'PAGE_SNAPSHOT'
                      AND e.stage = 'CAPTURE'
                      AND e.status = s.status
                      AND e.created_at = s.created_at
                      AND e.updated_at = s.updated_at
                  )
                """, snapshotId);
    }

    /**
     * Insere a captura bruta de referência coletada como evento auditável quando já houver página consolidada relacionada.
     */
    private void insertCollectedReferenceHtmlExecution(long captureId) {
        jdbcTemplate.update("""
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
                WHERE c.id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution e
                    WHERE e.sales_page_id = sp.id
                      AND e.job_type = 'COLLECTED_REFERENCE_HTML'
                      AND e.stage = 'CAPTURE'
                      AND e.status = c.status
                      AND e.created_at = c.created_at
                      AND e.updated_at = c.updated_at
                  )
                """, captureId);
    }

    /**
     * Atualiza a página consolidada com a execução mais recente vinculada à URL legada.
     */
    private void updateLastJobExecutionPointer(long urlIngestId) {
        jdbcTemplate.update("""
                UPDATE mois_sales_page sp
                SET sp.last_job_execution_id = (
                  SELECT e.id FROM mois_sales_page_job_execution e
                  WHERE e.sales_page_id = sp.id
                  ORDER BY e.updated_at DESC, e.id DESC LIMIT 1
                ),
                sp.updated_at = UTC_TIMESTAMP()
                WHERE EXISTS (
                  SELECT 1 FROM mois_sales_library_url_ingest i
                  WHERE i.id = ?
                    AND i.workspace_id = sp.workspace_id
                    AND i.url_canonical = sp.url_canonical
                )
                  AND EXISTS (SELECT 1 FROM mois_sales_page_job_execution e2 WHERE e2.sales_page_id = sp.id)
                """, urlIngestId);
    }

    /**
     * Compara o estado atual legado com o consolidado e registra divergências para diagnóstico da transição.
     */
    private void logDivergenceForUrlIngest(long urlIngestId) {
        List<StateComparison> rows = jdbcTemplate.query("""
                SELECT i.id AS url_ingest_id,
                       CASE
                         WHEN a.id IS NOT NULL THEN 'ANALYSIS'
                         WHEN s.id IS NOT NULL OR c.id IS NOT NULL THEN 'CAPTURE'
                         WHEN j.id IS NOT NULL THEN 'ANALYSIS'
                         ELSE 'INGEST'
                       END AS legacy_stage,
                       COALESCE(a.status, s.status, c.status, j.status, 'INGESTED') AS legacy_status,
                       sp.current_stage AS new_stage,
                       sp.current_status AS new_status
                FROM mois_sales_library_url_ingest i
                LEFT JOIN mois_sales_page sp ON sp.workspace_id = i.workspace_id AND sp.url_canonical = i.url_canonical
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
                LEFT JOIN mois_collected_reference_html_capture c ON c.id = (
                  SELECT c2.id FROM mois_collected_reference_html_capture c2
                  WHERE c2.workspace_id = i.workspace_id
                    AND c2.source = i.source
                    AND c2.url_original IN (i.url_original, i.url_canonical)
                  ORDER BY c2.updated_at DESC, c2.id DESC LIMIT 1
                )
                WHERE i.id = ?
                LIMIT 1
                """, (rs, rowNum) -> new StateComparison(
                rs.getLong("url_ingest_id"),
                rs.getString("legacy_stage"),
                rs.getString("legacy_status"),
                rs.getString("new_stage"),
                rs.getString("new_status")), urlIngestId);
        if (rows.isEmpty()) {
            log.warn("Divergência MOIS sales page: URL legada ausente após escrita dupla. modulo=MOIS, operacao=logDivergenceForUrlIngest, urlIngestId={}", urlIngestId);
            return;
        }
        StateComparison comparison = rows.get(0);
        if (!comparison.legacyStage().equals(comparison.newStage()) || !comparison.legacyStatus().equals(comparison.newStatus())) {
            log.warn("Divergência MOIS sales page: estado legado não bate com modelo novo. modulo=MOIS, operacao=logDivergenceForUrlIngest, urlIngestId={}, legacyStage={}, legacyStatus={}, newStage={}, newStatus={}",
                    comparison.urlIngestId(), comparison.legacyStage(), comparison.legacyStatus(), comparison.newStage(), comparison.newStatus());
        }
    }

    /**
     * Localiza a URL legada de um job de processamento.
     */
    private Long findUrlIngestIdByProcessingJob(long processingJobId) {
        List<Long> rows = jdbcTemplate.query("SELECT url_ingest_id FROM mois_sales_library_processing_job WHERE id = ? LIMIT 1",
                (rs, rowNum) -> rs.getLong("url_ingest_id"), processingJobId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Localiza a URL legada de um snapshot.
     */
    private Long findUrlIngestIdBySnapshot(long snapshotId) {
        List<Long> rows = jdbcTemplate.query("SELECT url_ingest_id FROM mois_sales_library_page_snapshot WHERE id = ? LIMIT 1",
                (rs, rowNum) -> rs.getLong("url_ingest_id"), snapshotId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Localiza URLs legadas que podem estar relacionadas a uma captura bruta de referência coletada.
     */
    private List<Long> findUrlIngestIdsByCollectedCapture(long captureId) {
        return jdbcTemplate.query("""
                SELECT i.id
                FROM mois_collected_reference_html_capture c
                JOIN mois_sales_library_url_ingest i
                  ON i.workspace_id = c.workspace_id
                 AND i.source = c.source
                 AND i.url_canonical = c.url_original
                WHERE c.id = ?
                UNION
                SELECT i.id
                FROM mois_collected_reference_html_capture c
                JOIN mois_collected_reference r ON r.id = c.collected_reference_id
                JOIN mois_sales_library_url_ingest i
                  ON i.workspace_id = r.workspace_id
                 AND i.source = r.source
                 AND (i.url_original IN (r.sales_page_url, r.product_url, r.url)
                   OR i.url_canonical IN (r.sales_page_url, r.product_url, r.url))
                WHERE c.id = ?
                """, (rs, rowNum) -> rs.getLong("id"), captureId, captureId);
    }

    /**
     * Guarda os dados comparados entre legado e modelo novo.
     */
    private record StateComparison(long urlIngestId, String legacyStage, String legacyStatus, String newStage, String newStatus) {
    }
}
