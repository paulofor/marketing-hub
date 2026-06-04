package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageDualWriteGateway;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordena a ingestão, consulta e processamento das páginas de vendas da biblioteca MOIS.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoisSalesLibraryService {

    private static final String JOB_STATUS_PENDING = "PENDING";
    private static final String JOB_STATUS_FETCHING = "FETCHING";
    private static final String ANALYSIS_STATUS_CANCELED = "ANULADO";

    private final JdbcTemplate jdbcTemplate;
    private final MoisSalesPageDualWriteGateway dualWriteGateway;

    /**
     * Reserva o próximo job pendente da biblioteca para processamento pelo worker.
     */
    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryClaimResponse claimJob(MoisSalesLibraryDtos.SalesLibraryClaimRequest request) {
        String normalizedSource = request.source().trim().toUpperCase(Locale.ROOT);
        String claimedBy = UUID.randomUUID().toString();
        List<MoisSalesLibraryDtos.SalesLibraryClaimedJob> rows = jdbcTemplate.query("""
                SELECT e.id AS job_id, sp.id AS page_id, sp.url_canonical, sp.title
                FROM mois_sales_page_job_execution e
                JOIN mois_sales_page sp ON sp.id = e.sales_page_id
                WHERE e.status = 'PENDING'
                  AND e.stage = 'ANALYSIS'
                  AND e.job_type IN ('PAGE_ANALYSIS', 'PROCESSING_JOB')
                  AND sp.workspace_id = ?
                  AND sp.source = ?
                ORDER BY e.created_at ASC, e.id ASC
                LIMIT 1
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryClaimedJob(
                rs.getLong("job_id"), rs.getLong("page_id"), rs.getString("url_canonical"), rs.getString("title")),
                request.workspaceId(), normalizedSource);
        if (rows.isEmpty()) {
            return new MoisSalesLibraryDtos.SalesLibraryClaimResponse(false, null);
        }
        MoisSalesLibraryDtos.SalesLibraryClaimedJob job = rows.get(0);
        int claimed = jdbcTemplate.update("""
                UPDATE mois_sales_page_job_execution
                SET status = 'FETCHING', claimed_by = ?, started_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                WHERE id = ? AND status = 'PENDING'
                """, claimedBy, job.jobId());
        if (claimed == 0) {
            return new MoisSalesLibraryDtos.SalesLibraryClaimResponse(false, null);
        }
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                SET current_stage = 'ANALYSIS', current_status = 'FETCHING', analysis_status = 'FETCHING',
                    last_error_category = NULL, last_error_message = NULL, last_job_execution_id = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, job.jobId(), job.pageId());
        mirrorClaimToLegacyAudit(job.pageId());
        log.info("MOIS sales-library claim usando modelo operacional novo. modulo=MOIS, operacao=claimJob, workspaceId={}, source={}, pageId={}, executionId={}",
                request.workspaceId(), normalizedSource, job.pageId(), job.jobId());
        return new MoisSalesLibraryDtos.SalesLibraryClaimResponse(true, job);
    }

    /**
     * Registra a análise concluída de uma página e finaliza o job correspondente.
     */
    @Transactional
    public void completeJob(long jobId, MoisSalesLibraryDtos.SalesLibraryCompleteRequest request) {
        Long salesPageId = findOperationalSalesPageIdForExecution(jobId);
        if (salesPageId == null) {
            completeLegacyJob(jobId, request);
            return;
        }
        Instant analyzedAt = request.analyzedAt() == null ? Instant.now() : request.analyzedAt();
        jdbcTemplate.update("""
                UPDATE mois_sales_page_job_execution
                SET job_type = 'PAGE_ANALYSIS', stage = 'ANALYSIS', status = 'DONE', score_total = ?,
                    sections_json = ?, copy_json = ?, visual_json = ?, image_json = ?,
                    request_payload_json = ?, response_payload_json = ?, error_category = NULL, error_message = ?,
                    finished_at = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, request.scoreTotal(), request.sectionsJson(), request.copyJson(), request.visualJson(), request.imageJson(),
                request.requestPayloadJson(), request.analysisNotes(), null, Timestamp.from(analyzedAt), jobId);
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                SET current_stage = 'ANALYSIS', current_status = 'DONE', analysis_status = 'DONE', score_total = ?,
                    last_error_category = NULL, last_error_message = NULL, last_job_execution_id = ?,
                    last_analyzed_at = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, request.scoreTotal(), jobId, Timestamp.from(analyzedAt), salesPageId);
        mirrorAnalysisCompletionToLegacyAudit(salesPageId, jobId, request, analyzedAt);
        log.info("MOIS sales-library análise concluída no modelo operacional novo. modulo=MOIS, operacao=completeJob, pageId={}, executionId={}, scoreTotal={}",
                salesPageId, jobId, request.scoreTotal());
    }

    /**
     * Marca um job como falho preservando a categoria e a mensagem de erro operacional.
     */
    @Transactional
    public void failJob(long jobId, MoisSalesLibraryDtos.SalesLibraryFailRequest request) {
        Long salesPageId = findOperationalSalesPageIdForExecution(jobId);
        if (salesPageId == null) {
            failLegacyJob(jobId, request);
            return;
        }
        jdbcTemplate.update("""
                UPDATE mois_sales_page_job_execution
                SET status = 'FAILED', error_category = ?, error_message = ?, finished_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, request.errorCategory(), truncate(request.errorMessage(), 1000), jobId);
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                SET current_stage = 'ANALYSIS', current_status = 'FAILED', analysis_status = 'FAILED',
                    last_error_category = ?, last_error_message = ?, last_job_execution_id = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, request.errorCategory(), truncate(request.errorMessage(), 1000), jobId, salesPageId);
        mirrorAnalysisFailureToLegacyAudit(salesPageId, jobId, request);
        log.info("MOIS sales-library análise falhou no modelo operacional novo. modulo=MOIS, operacao=failJob, pageId={}, executionId={}, errorCategory={}",
                salesPageId, jobId, request.errorCategory());
    }

    /**
     * Ingere URLs informadas explicitamente e cria jobs apenas para páginas novas.
     */
    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryIngestResponse ingestUrls(MoisSalesLibraryDtos.SalesLibraryIngestRequest request) {
        log.info("Biblioteca de páginas de vendas recebeu payload bruto de ingestão explícita. request={}", request);
        IngestCounters counters = ingestUrlItems(request.workspaceId(), request.source(), request.urls());

        return new MoisSalesLibraryDtos.SalesLibraryIngestResponse(
                request.workspaceId(),
                request.source().trim().toUpperCase(Locale.ROOT),
                request.urls().size(),
                counters.persisted()
        );
    }

    /**
     * Ingere na biblioteca as URLs dos produtos Hotmart já coletados, priorizando o lote de 400 produtos mais recente.
     */
    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestResponse ingestHotmartCollectedProducts(
            MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestRequest request
    ) {
        log.info("Biblioteca de páginas de vendas recebeu payload bruto para ingestir produtos Hotmart coletados. request={}", request);
        int normalizedLimit = Math.max(1, Math.min(request.limit() == null ? 400 : request.limit(), 400));
        String effectiveJobId = resolveHotmartJobId(request.workspaceId(), request.jobId());
        if (effectiveJobId == null) {
            return new MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestResponse(
                    request.workspaceId(), null, 0, 0, 0, 0, 0, 0
            );
        }

        List<CollectedReferenceForIngest> references = findHotmartCollectedReferences(request.workspaceId(), effectiveJobId, normalizedLimit);
        List<MoisSalesLibraryDtos.SalesLibraryUrlItem> urls = new ArrayList<>();
        int skippedWithoutUrl = 0;
        for (CollectedReferenceForIngest reference : references) {
            String url = coalesceNotBlank(reference.salesPageUrl(), reference.productUrl(), reference.url());
            if (url == null || url.isBlank()) {
                skippedWithoutUrl++;
                continue;
            }
            String title = coalesceNotBlank(reference.productName(), reference.title(), reference.referenceId());
            urls.add(new MoisSalesLibraryDtos.SalesLibraryUrlItem(url, title, reference.collectedAt()));
        }

        IngestCounters counters = ingestUrlItems(request.workspaceId(), "HOTMART", urls);
        return new MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestResponse(
                request.workspaceId(),
                effectiveJobId,
                references.size(),
                urls.size(),
                counters.inserted(),
                counters.updated(),
                counters.jobsCreated(),
                skippedWithoutUrl + counters.skippedWithoutUrl()
        );
    }

    /**
     * Reserva uma referência coletada para a primeira etapa do pipeline de captura de HTML bruto.
     */
    @Transactional
    public MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse claimCollectedReferenceHtml(
            MoisSalesLibraryDtos.CollectedReferenceHtmlClaimRequest request
    ) {
        String normalizedSource = request.source().trim().toUpperCase(Locale.ROOT);
        String claimedBy = UUID.randomUUID().toString();
        try {
            int inserted = jdbcTemplate.update(
                    """
                            INSERT INTO mois_collected_reference_html_capture
                              (collected_reference_id, workspace_id, source, collection_job_id, reference_id, title,
                               url_source, url_original, status, claimed_by, claimed_at, created_at, updated_at)
                            SELECT r.id, r.workspace_id, r.source, r.job_id, r.reference_id,
                                   COALESCE(NULLIF(r.product_name, ''), NULLIF(r.title, ''), r.reference_id),
                                   CASE
                                     WHEN r.sales_page_url IS NOT NULL AND r.sales_page_url <> '' THEN 'SALES_PAGE_URL'
                                     WHEN r.product_url IS NOT NULL AND r.product_url <> '' THEN 'PRODUCT_URL'
                                     ELSE 'URL'
                                   END,
                                   COALESCE(NULLIF(r.sales_page_url, ''), NULLIF(r.product_url, ''), NULLIF(r.url, '')),
                                   'CLAIMED', ?, UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP()
                            FROM mois_collected_reference r
                            WHERE r.workspace_id = ?
                              AND r.source = ?
                              AND COALESCE(NULLIF(r.sales_page_url, ''), NULLIF(r.product_url, ''), NULLIF(r.url, '')) IS NOT NULL
                              AND NOT EXISTS (
                                SELECT 1 FROM mois_collected_reference_html_capture c
                                WHERE c.collected_reference_id = r.id
                              )
                            ORDER BY r.collected_at ASC, r.id ASC
                            LIMIT 1
                            """,
                    claimedBy,
                    request.workspaceId(),
                    normalizedSource);
            if (inserted == 0) {
                return new MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse(false, null);
            }
            return findClaimedCollectedReferenceHtml(claimedBy)
                    .map(job -> new MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse(true, job))
                    .orElseGet(() -> new MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse(false, null));
        } catch (DataAccessException ex) {
            log.warn("Falha ao reservar HTML bruto de referência coletada. operacao=claimCollectedReferenceHtml, workspaceId={}, source={}, claimedBy={}, erroClasse={}, erro={}",
                    request.workspaceId(), normalizedSource, claimedBy, ex.getClass().getName(), ex.getMessage(), ex);
            return new MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse(false, null);
        }
    }

    /**
     * Persiste o HTML bruto capturado pelo worker MOIS para uma referência coletada.
     */
    @Transactional
    public MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse completeCollectedReferenceHtml(
            long captureId,
            MoisSalesLibraryDtos.CollectedReferenceHtmlCompleteRequest request
    ) {
        String rawHtml = request.rawHtml() == null ? "" : request.rawHtml();
        byte[] rawHtmlBytes = rawHtml.getBytes(StandardCharsets.UTF_8);
        jdbcTemplate.update(
                """
                        UPDATE mois_collected_reference_html_capture
                        SET status = 'CAPTURED',
                            url_final = ?,
                            http_status = ?,
                            content_type = ?,
                            raw_html = ?,
                            raw_html_sha256 = ?,
                            raw_html_bytes = ?,
                            error_message = NULL,
                            fetched_at = ?,
                            updated_at = UTC_TIMESTAMP()
                        WHERE id = ?
                        """,
                truncate(request.finalUrl(), 1024),
                request.httpStatus(),
                truncate(request.contentType(), 255),
                rawHtml,
                sha256(rawHtml),
                rawHtmlBytes.length,
                Timestamp.from(request.fetchedAt() == null ? Instant.now() : request.fetchedAt()),
                captureId);
        dualWriteGateway.syncCollectedReferenceHtmlCapture(captureId);
        return new MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse(captureId, "CAPTURED");
    }

    /**
     * Registra falha terminal da captura de HTML bruto de uma referência coletada.
     */
    @Transactional
    public MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse failCollectedReferenceHtml(
            long captureId,
            MoisSalesLibraryDtos.CollectedReferenceHtmlFailRequest request
    ) {
        String message = coalesceNotBlank(request.errorMessage(), request.errorCategory(), "Falha sem detalhe");
        jdbcTemplate.update(
                """
                        UPDATE mois_collected_reference_html_capture
                        SET status = 'FAILED',
                            error_message = ?,
                            fetched_at = UTC_TIMESTAMP(),
                            updated_at = UTC_TIMESTAMP()
                        WHERE id = ?
                        """,
                truncate(message, 1000),
                captureId);
        dualWriteGateway.syncCollectedReferenceHtmlCapture(captureId);
        return new MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse(captureId, "FAILED");
    }

    /**
     * Busca os metadados operacionais de um job da biblioteca.
     */
    public MoisSalesLibraryDtos.SalesLibraryJobResponse getJob(long jobId) {
        List<MoisSalesLibraryDtos.SalesLibraryJobResponse> rows = jdbcTemplate.query("""
                        SELECT id, url_ingest_id, status, attempts, error_category, error_message,
                               next_retry_at, created_at, updated_at, started_at, finished_at
                        FROM mois_sales_library_processing_job
                        WHERE id = ?
                        LIMIT 1
                        """, (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryJobResponse(
                        rs.getLong("id"), rs.getLong("url_ingest_id"), rs.getString("status"),
                        rs.getInt("attempts"), rs.getString("error_category"), rs.getString("error_message"),
                        toInstant(rs.getTimestamp("next_retry_at")), toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at")), toInstant(rs.getTimestamp("started_at")),
                        toInstant(rs.getTimestamp("finished_at"))), jobId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        return rows.get(0);
    }

    /**
     * Lista jobs da biblioteca por workspace, com filtro opcional por status.
     */
    public MoisSalesLibraryDtos.SalesLibraryJobPageResponse listJobs(String workspaceId, String status, int page, int pageSize) {
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (normalizedPage - 1) * normalizedPageSize;
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        String statusClause = normalizedStatus == null ? "" : " AND j.status = ?";
        Long total = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM mois_sales_library_processing_job j
                        JOIN mois_sales_library_url_ingest i ON i.id = j.url_ingest_id
                        WHERE i.workspace_id = ?
                        """ + statusClause,
                Long.class, normalizedStatus == null ? new Object[]{workspaceId} : new Object[]{workspaceId, normalizedStatus});
        List<MoisSalesLibraryDtos.SalesLibraryJobResponse> items = jdbcTemplate.query("""
                        SELECT j.id, j.url_ingest_id, j.status, j.attempts, j.error_category, j.error_message,
                               j.next_retry_at, j.created_at, j.updated_at, j.started_at, j.finished_at
                        FROM mois_sales_library_processing_job j
                        JOIN mois_sales_library_url_ingest i ON i.id = j.url_ingest_id
                        WHERE i.workspace_id = ?
                        """ + statusClause + " ORDER BY j.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryJobResponse(rs.getLong("id"), rs.getLong("url_ingest_id"),
                        rs.getString("status"), rs.getInt("attempts"), rs.getString("error_category"), rs.getString("error_message"),
                        toInstant(rs.getTimestamp("next_retry_at")), toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at")), toInstant(rs.getTimestamp("started_at")),
                        toInstant(rs.getTimestamp("finished_at"))),
                normalizedStatus == null ? new Object[]{workspaceId, normalizedPageSize, offset} : new Object[]{workspaceId, normalizedStatus, normalizedPageSize, offset});
        return new MoisSalesLibraryDtos.SalesLibraryJobPageResponse(normalizedPage, normalizedPageSize, total == null ? 0 : total, items);
    }

    /**
     * Lista entradas de URL ingeridas na biblioteca para auditoria operacional.
     */
    public MoisSalesLibraryDtos.SalesLibraryEntryPageResponse listEntries(String workspaceId, int page, int pageSize) {
        int normalizedPage = Math.max(1, page); int normalizedPageSize = Math.max(1, Math.min(pageSize, 100)); int offset = (normalizedPage - 1) * normalizedPageSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_url_ingest WHERE workspace_id = ?", Long.class, workspaceId);
        List<MoisSalesLibraryDtos.SalesLibraryEntryResponse> items = jdbcTemplate.query("""
                        SELECT id, workspace_id, source, url_original, url_canonical, title, ingest_count,
                               first_captured_at, last_captured_at, updated_at
                        FROM mois_sales_library_url_ingest
                        WHERE workspace_id = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?
                        """, (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryEntryResponse(
                        rs.getLong("id"), rs.getString("workspace_id"), rs.getString("source"), rs.getString("url_original"),
                        rs.getString("url_canonical"), rs.getString("title"), rs.getInt("ingest_count"),
                        toInstant(rs.getTimestamp("first_captured_at")), toInstant(rs.getTimestamp("last_captured_at")),
                        toInstant(rs.getTimestamp("updated_at"))), workspaceId, normalizedPageSize, offset);
        return new MoisSalesLibraryDtos.SalesLibraryEntryPageResponse(normalizedPage, normalizedPageSize, total == null ? 0 : total, items);
    }

    /**
     * Lista páginas canônicas a partir do estado consolidado do modelo novo da Fase 4.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageListResponse listPages(String workspaceId, int page, int pageSize) {
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (normalizedPage - 1) * normalizedPageSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_page WHERE workspace_id = ?", Long.class, workspaceId);
        List<MoisSalesLibraryDtos.SalesLibraryPageResponse> items = jdbcTemplate.query("""
                SELECT id, workspace_id, source, url_canonical, title, current_stage, current_status, capture_status,
                       COALESCE(analysis_status, current_status) AS analysis_status, url_final, http_status, html_sha256,
                       html_bytes, score_total, offer_summary, mechanism_summary, promise_summary, proof_summary,
                       last_error_category, last_error_message, last_job_execution_id, last_captured_at, last_analyzed_at, updated_at
                FROM mois_sales_page
                WHERE workspace_id = ?
                ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?
                """, this::mapSalesPageResponse, workspaceId, normalizedPageSize, offset);
        return new MoisSalesLibraryDtos.SalesLibraryPageListResponse(normalizedPage, normalizedPageSize, total == null ? 0 : total, items);
    }

    /**
     * Calcula os contadores globais da biblioteca diretamente em mois_sales_page para eliminar paginação ambígua.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageSummaryResponse summarizePages(String workspaceId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total,
                       SUM(current_status = 'PENDING') AS pending,
                       SUM(current_status IN ('FETCHING', 'CAPTURING')) AS capturing,
                       SUM(current_status IN ('CAPTURED', 'DUPLICATE')) AS captured,
                       SUM(current_status IN ('DONE', 'ANALYZED')) AS analyzed,
                       SUM(current_status = 'FAILED') AS failed,
                       SUM(current_status = 'BLOCKED_COOLDOWN') AS blocked_cooldown,
                       SUM(source = 'HOTMART') AS hotmart,
                       SUM(source = 'CLICKBANK') AS clickbank,
                       MAX(updated_at) AS updated_at
                FROM mois_sales_page
                WHERE workspace_id = ?
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryPageSummaryResponse(
                workspaceId, rs.getLong("total"), rs.getLong("pending"), rs.getLong("capturing"),
                rs.getLong("captured"), rs.getLong("analyzed"), rs.getLong("failed"),
                rs.getLong("blocked_cooldown"), rs.getLong("hotmart"), rs.getLong("clickbank"),
                toInstant(rs.getTimestamp("updated_at"))), workspaceId);
    }

    /**
     * Busca uma página canônica pelo identificador consolidado em mois_sales_page.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageResponse getPage(long pageId) {
        List<MoisSalesLibraryDtos.SalesLibraryPageResponse> rows = jdbcTemplate.query("""
                SELECT id, workspace_id, source, url_canonical, title, current_stage, current_status, capture_status,
                       COALESCE(analysis_status, current_status) AS analysis_status, url_final, http_status, html_sha256,
                       html_bytes, score_total, offer_summary, mechanism_summary, promise_summary, proof_summary,
                       last_error_category, last_error_message, last_job_execution_id, last_captured_at, last_analyzed_at, updated_at
                FROM mois_sales_page
                WHERE id = ? LIMIT 1
                """, this::mapSalesPageResponse, pageId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Page not found: " + pageId);
        return rows.get(0);
    }

    /**
     * Busca a análise mais recente registrada no histórico consolidado da página.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse getPageAnalysis(long pageId) {
        List<MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse> rows = jdbcTemplate.query("""
                SELECT e.id, e.sales_page_id, e.status, e.score_total, e.sections_json, e.copy_json, e.visual_json,
                       e.image_json, e.request_payload_json, e.error_message, e.finished_at, e.updated_at
                FROM mois_sales_page_job_execution e
                WHERE e.sales_page_id = ? AND e.job_type = 'PAGE_ANALYSIS'
                ORDER BY e.updated_at DESC, e.id DESC LIMIT 1
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse(
                rs.getLong("id"), rs.getLong("sales_page_id"), null, rs.getString("status"),
                rs.getBigDecimal("score_total"), null, null, null, rs.getString("sections_json"),
                rs.getString("copy_json"), rs.getString("visual_json"), rs.getString("image_json"),
                rs.getString("error_message"), rs.getString("request_payload_json"),
                toInstant(rs.getTimestamp("finished_at")), toInstant(rs.getTimestamp("updated_at"))), pageId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Analysis not found for page: " + pageId);
        return rows.get(0);
    }

    /**
     * Lista o histórico auditável de execuções da página a partir de mois_sales_page_job_execution.
     */
    public List<MoisSalesLibraryDtos.SalesLibraryPageExecutionResponse> listPageExecutions(long pageId) {
        return jdbcTemplate.query("""
                SELECT id, sales_page_id, job_type, stage, status, attempt, input_url, final_url, redirect_root_url,
                       http_status, content_type, raw_html_bytes, screenshot_bytes, score_total, error_category,
                       error_message, started_at, finished_at, created_at, updated_at
                FROM mois_sales_page_job_execution
                WHERE sales_page_id = ?
                ORDER BY updated_at DESC, id DESC
                LIMIT 50
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryPageExecutionResponse(
                rs.getLong("id"), rs.getLong("sales_page_id"), rs.getString("job_type"), rs.getString("stage"),
                rs.getString("status"), rs.getInt("attempt"), rs.getString("input_url"), rs.getString("final_url"),
                rs.getString("redirect_root_url"), (Integer) rs.getObject("http_status"), rs.getString("content_type"),
                rs.getLong("raw_html_bytes"), rs.getLong("screenshot_bytes"), rs.getBigDecimal("score_total"),
                rs.getString("error_category"), rs.getString("error_message"), toInstant(rs.getTimestamp("started_at")),
                toInstant(rs.getTimestamp("finished_at")), toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))), pageId);
    }


    /**
     * Busca a página operacional vinculada a uma execução nova de análise.
     */
    private Long findOperationalSalesPageIdForExecution(long executionId) {
        return jdbcTemplate.query("""
                SELECT sales_page_id
                FROM mois_sales_page_job_execution
                WHERE id = ? AND stage = 'ANALYSIS' AND status IN ('PENDING', 'FETCHING')
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("sales_page_id"), executionId).stream().findFirst().orElse(null);
    }

    /**
     * Conclui um job legado quando o worker ainda enviar identificador antigo durante a transição.
     */
    private void completeLegacyJob(long jobId, MoisSalesLibraryDtos.SalesLibraryCompleteRequest request) {
        Long pageId = jdbcTemplate.queryForObject("SELECT url_ingest_id FROM mois_sales_library_processing_job WHERE id = ? LIMIT 1", Long.class, jobId);
        if (pageId == null) throw new IllegalArgumentException("Job not found: " + jobId);
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_page_analysis
                (url_ingest_id, job_id, status, score_total, parser_version, prompt_version, model_name, sections_json, copy_json, visual_json, image_json, analysis_notes, request_payload_json, analyzed_at, created_at, updated_at)
                VALUES (?, ?, 'DONE', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """, pageId, jobId, request.scoreTotal(), request.parserVersion(), request.promptVersion(), request.modelName(), request.sectionsJson(), request.copyJson(), request.visualJson(), request.imageJson(), request.analysisNotes(), request.requestPayloadJson(), request.analyzedAt() == null ? Instant.now() : request.analyzedAt());
        jdbcTemplate.update("UPDATE mois_sales_library_processing_job SET status='DONE', finished_at=UTC_TIMESTAMP(), updated_at=UTC_TIMESTAMP() WHERE id=?", jobId);
        dualWriteGateway.syncProcessingJob(jobId);
        dualWriteGateway.syncLatestAnalysis(pageId);
    }

    /**
     * Marca um job legado como falho quando o worker ainda enviar identificador antigo durante a transição.
     */
    private void failLegacyJob(long jobId, MoisSalesLibraryDtos.SalesLibraryFailRequest request) {
        jdbcTemplate.update("UPDATE mois_sales_library_processing_job SET status='FAILED', error_category=?, error_message=?, finished_at=UTC_TIMESTAMP(), updated_at=UTC_TIMESTAMP() WHERE id=?", request.errorCategory(), request.errorMessage(), jobId);
        dualWriteGateway.syncProcessingJob(jobId);
    }

    /**
     * Espelha o claim operacional novo no job legado mais próximo apenas para auditoria de transição.
     */
    private void mirrorClaimToLegacyAudit(long salesPageId) {
        Long legacyJobId = findLatestLegacyJobForSalesPage(salesPageId, JOB_STATUS_PENDING);
        if (legacyJobId != null) {
            jdbcTemplate.update("UPDATE mois_sales_library_processing_job SET status='FETCHING', started_at=UTC_TIMESTAMP(), updated_at=UTC_TIMESTAMP() WHERE id=? AND status='PENDING'", legacyJobId);
        }
    }

    /**
     * Espelha a conclusão da análise nova em tabelas legadas somente para manter auditoria compatível.
     */
    private void mirrorAnalysisCompletionToLegacyAudit(long salesPageId, long executionId, MoisSalesLibraryDtos.SalesLibraryCompleteRequest request, Instant analyzedAt) {
        Long urlIngestId = findUrlIngestIdForSalesPage(salesPageId);
        if (urlIngestId == null) {
            return;
        }
        Long legacyJobId = findLatestLegacyJobForSalesPage(salesPageId, JOB_STATUS_FETCHING);
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_page_analysis
                (url_ingest_id, job_id, status, score_total, parser_version, prompt_version, model_name, sections_json, copy_json, visual_json, image_json, analysis_notes, request_payload_json, analyzed_at, created_at, updated_at)
                VALUES (?, ?, 'DONE', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """, urlIngestId, legacyJobId, request.scoreTotal(), request.parserVersion(), request.promptVersion(), request.modelName(), request.sectionsJson(), request.copyJson(), request.visualJson(), request.imageJson(), request.analysisNotes(), request.requestPayloadJson(), Timestamp.from(analyzedAt));
        if (legacyJobId != null) {
            jdbcTemplate.update("UPDATE mois_sales_library_processing_job SET status='DONE', finished_at=UTC_TIMESTAMP(), updated_at=UTC_TIMESTAMP() WHERE id=?", legacyJobId);
        }
        log.info("MOIS sales-library espelhou conclusão nova no legado para auditoria. modulo=MOIS, operacao=mirrorAnalysisCompletionToLegacyAudit, pageId={}, executionId={}, legacyUrlIngestId={}, legacyJobId={}",
                salesPageId, executionId, urlIngestId, legacyJobId);
    }

    /**
     * Espelha a falha da análise nova em tabelas legadas somente para manter auditoria compatível.
     */
    private void mirrorAnalysisFailureToLegacyAudit(long salesPageId, long executionId, MoisSalesLibraryDtos.SalesLibraryFailRequest request) {
        Long urlIngestId = findUrlIngestIdForSalesPage(salesPageId);
        if (urlIngestId == null) {
            return;
        }
        Long legacyJobId = findLatestLegacyJobForSalesPage(salesPageId, JOB_STATUS_FETCHING);
        if (legacyJobId != null) {
            jdbcTemplate.update("UPDATE mois_sales_library_processing_job SET status='FAILED', error_category=?, error_message=?, finished_at=UTC_TIMESTAMP(), updated_at=UTC_TIMESTAMP() WHERE id=?", request.errorCategory(), truncate(request.errorMessage(), 1000), legacyJobId);
        }
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_page_analysis
                (url_ingest_id, job_id, status, analysis_notes, created_at, updated_at)
                VALUES (?, ?, 'FAILED', ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """, urlIngestId, legacyJobId, truncate(request.errorMessage(), 1000));
        log.info("MOIS sales-library espelhou falha nova no legado para auditoria. modulo=MOIS, operacao=mirrorAnalysisFailureToLegacyAudit, pageId={}, executionId={}, legacyUrlIngestId={}, legacyJobId={}",
                salesPageId, executionId, urlIngestId, legacyJobId);
    }

    /**
     * Localiza a URL legada correspondente a uma página operacional, quando ainda existir para auditoria.
     */
    private Long findUrlIngestIdForSalesPage(long salesPageId) {
        return jdbcTemplate.query("""
                SELECT i.id
                FROM mois_sales_page sp
                JOIN mois_sales_library_url_ingest i ON i.workspace_id = sp.workspace_id AND i.url_canonical = sp.url_canonical
                WHERE sp.id = ?
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("id"), salesPageId).stream().findFirst().orElse(null);
    }

    /**
     * Localiza o job legado mais recente de uma página operacional pelo status desejado.
     */
    private Long findLatestLegacyJobForSalesPage(long salesPageId, String status) {
        return jdbcTemplate.query("""
                SELECT j.id
                FROM mois_sales_page sp
                JOIN mois_sales_library_url_ingest i ON i.workspace_id = sp.workspace_id AND i.url_canonical = sp.url_canonical
                JOIN mois_sales_library_processing_job j ON j.url_ingest_id = i.id
                WHERE sp.id = ? AND j.status = ?
                ORDER BY j.updated_at DESC, j.id DESC
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("id"), salesPageId, status).stream().findFirst().orElse(null);
    }

    /**
     * Cria uma execução operacional pendente de análise usando as novas tabelas como fonte principal.
     */
    private long createOperationalAnalysisExecution(long salesPageId, String reason) {
        MoisSalesLibraryDtos.SalesLibraryPageResponse page = getPage(salesPageId);
        jdbcTemplate.update("""
                INSERT INTO mois_sales_page_job_execution
                (sales_page_id, workspace_id, job_type, stage, status, attempt, input_url, request_payload_json, created_at, updated_at)
                VALUES (?, ?, 'PAGE_ANALYSIS', 'ANALYSIS', 'PENDING', 1, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """, salesPageId, page.workspaceId(), page.urlCanonical(), reason);
        Long executionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long safeExecutionId = executionId == null ? 0L : executionId;
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                SET current_stage = 'ANALYSIS', current_status = 'PENDING', analysis_status = 'PENDING',
                    last_error_category = NULL, last_error_message = NULL, last_job_execution_id = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, safeExecutionId, salesPageId);
        return safeExecutionId;
    }

    /**
     * Atualiza manualmente o status da análise de uma página da biblioteca.
     */
    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryStatusUpdateResponse updatePageStatus(
            long pageId,
            MoisSalesLibraryDtos.SalesLibraryStatusUpdateRequest request
    ) {
        String normalizedStatus = request.status().trim().toUpperCase(Locale.ROOT);
        if (!JOB_STATUS_PENDING.equals(normalizedStatus) && !ANALYSIS_STATUS_CANCELED.equals(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported status: " + request.status());
        }

        Long jobId = null;
        String notes = request.reason() == null || request.reason().isBlank() ? "Status atualizado manualmente via API" : request.reason().trim();
        if (JOB_STATUS_PENDING.equals(normalizedStatus)) {
            jobId = createOperationalAnalysisExecution(pageId, notes);
            Long urlIngestId = findUrlIngestIdForSalesPage(pageId);
            if (urlIngestId != null) {
                long legacyJobId = createPendingJob(urlIngestId);
                jdbcTemplate.update("""
                        INSERT INTO mois_sales_library_page_analysis
                        (url_ingest_id, job_id, status, analysis_notes, created_at, updated_at)
                        VALUES (?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                        """, urlIngestId, legacyJobId, normalizedStatus, notes);
            }
        } else {
            jdbcTemplate.update("""
                    INSERT INTO mois_sales_page_job_execution
                    (sales_page_id, workspace_id, job_type, stage, status, attempt, request_payload_json, created_at, updated_at)
                    SELECT id, workspace_id, 'STATUS_UPDATE', 'ANALYSIS', ?, 1, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP()
                    FROM mois_sales_page WHERE id = ?
                    """, normalizedStatus, notes, pageId);
            Long executionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            jobId = executionId == null || executionId == 0L ? null : executionId;
            jdbcTemplate.update("""
                    UPDATE mois_sales_page
                    SET current_stage = 'ANALYSIS', current_status = ?, analysis_status = ?, last_job_execution_id = ?, updated_at = UTC_TIMESTAMP()
                    WHERE id = ?
                    """, normalizedStatus, normalizedStatus, jobId, pageId);
            Long urlIngestId = findUrlIngestIdForSalesPage(pageId);
            if (urlIngestId != null) {
                jdbcTemplate.update("""
                        INSERT INTO mois_sales_library_page_analysis
                        (url_ingest_id, job_id, status, analysis_notes, created_at, updated_at)
                        VALUES (?, NULL, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                        """, urlIngestId, normalizedStatus, notes);
            }
        }

        log.info("MOIS sales-library status manual atualizado no modelo operacional novo. modulo=MOIS, operacao=updatePageStatus, pageId={}, executionId={}, status={}",
                pageId, jobId, normalizedStatus);
        return new MoisSalesLibraryDtos.SalesLibraryStatusUpdateResponse(pageId, jobId, normalizedStatus, notes, Instant.now());
    }

    /**
     * Cria um novo job pendente para reanalisar uma página existente.
     */
    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryReanalyzeResponse reanalyzePage(long pageId) {
        String notes = "Reanálise solicitada via API";
        long jobId = createOperationalAnalysisExecution(pageId, notes);
        Long urlIngestId = findUrlIngestIdForSalesPage(pageId);
        if (urlIngestId != null) {
            long legacyJobId = createPendingJob(urlIngestId);
            jdbcTemplate.update("""
                    INSERT INTO mois_sales_library_page_analysis
                    (url_ingest_id, job_id, status, analysis_notes, created_at, updated_at)
                    VALUES (?, ?, 'PENDING', ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                    """, urlIngestId, legacyJobId, notes);
        }
        log.info("MOIS sales-library reanálise criada no modelo operacional novo. modulo=MOIS, operacao=reanalyzePage, pageId={}, executionId={}", pageId, jobId);
        return new MoisSalesLibraryDtos.SalesLibraryReanalyzeResponse(pageId, jobId, JOB_STATUS_PENDING, Instant.now());
    }


    /**
     * Executa a gravação comum de itens de URL usando mois_sales_page como escrita principal.
     */
    private IngestCounters ingestUrlItems(String workspaceId, String source, List<MoisSalesLibraryDtos.SalesLibraryUrlItem> urls) {
        int persisted = 0;
        int inserted = 0;
        int updated = 0;
        int jobsCreated = 0;
        int skippedWithoutUrl = 0;
        String normalizedSource = source.trim().toUpperCase(Locale.ROOT);
        for (MoisSalesLibraryDtos.SalesLibraryUrlItem item : urls) {
            String canonical = canonicalize(item.url());
            if (canonical == null || canonical.isBlank()) {
                skippedWithoutUrl++;
                continue;
            }
            Instant capturedAt = item.capturedAt() == null ? Instant.now() : item.capturedAt();
            LocalDateTime capturedAtUtc = LocalDateTime.ofInstant(capturedAt, ZoneOffset.UTC);
            int pageUpsertResult = jdbcTemplate.update(
                    """
                            INSERT INTO mois_sales_page
                            (workspace_id, source, title, url_original, url_canonical, current_stage, current_status,
                             analysis_status, ingest_count, first_seen_at, last_collected_at, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, 'ANALYSIS', 'PENDING', 'PENDING', 1, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                            ON DUPLICATE KEY UPDATE
                                source = VALUES(source),
                                title = COALESCE(NULLIF(VALUES(title), ''), title),
                                url_original = VALUES(url_original),
                                current_stage = CASE
                                  WHEN current_status IN ('DONE', 'ANALYZED', 'CAPTURED', 'DUPLICATE', 'FETCHING', 'FAILED') THEN current_stage
                                  ELSE 'ANALYSIS'
                                END,
                                current_status = CASE
                                  WHEN current_status IN ('DONE', 'ANALYZED', 'CAPTURED', 'DUPLICATE', 'FETCHING', 'FAILED') THEN current_status
                                  ELSE 'PENDING'
                                END,
                                analysis_status = CASE
                                  WHEN analysis_status IN ('DONE', 'FETCHING', 'FAILED') THEN analysis_status
                                  ELSE 'PENDING'
                                END,
                                ingest_count = ingest_count + 1,
                                last_collected_at = GREATEST(COALESCE(last_collected_at, VALUES(last_collected_at)), VALUES(last_collected_at)),
                                updated_at = UTC_TIMESTAMP()
                            """,
                    workspaceId,
                    normalizedSource,
                    item.title(),
                    item.url(),
                    canonical,
                    capturedAtUtc,
                    capturedAtUtc
            );

            Long pageId = findSalesPageIdByCanonical(workspaceId, canonical);
            if (pageId == null) {
                log.warn("MOIS sales-library não localizou página após upsert principal. modulo=MOIS, operacao=ingestUrlItems, workspaceId={}, canonical={}",
                        workspaceId, canonical);
                skippedWithoutUrl++;
                continue;
            }

            Long executionId = null;
            if (pageUpsertResult == 1) {
                inserted++;
                executionId = createOperationalAnalysisExecution(pageId, "INGESTION");
                jobsCreated++;
            } else {
                updated++;
            }
            mirrorOperationalIngestToLegacyAudit(pageId, executionId, capturedAtUtc);
            persisted++;
        }
        return new IngestCounters(persisted, inserted, updated, jobsCreated, skippedWithoutUrl);
    }



    /**
     * Localiza a página operacional pelo workspace e URL canônica.
     */
    private Long findSalesPageIdByCanonical(String workspaceId, String canonical) {
        return jdbcTemplate.query(
                """
                        SELECT id
                        FROM mois_sales_page
                        WHERE workspace_id = ? AND url_canonical = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                workspaceId,
                canonical
        ).stream().findFirst().orElse(null);
    }

    /**
     * Espelha a ingestão principal nova em tabelas legadas apenas para auditoria de transição.
     */
    private void mirrorOperationalIngestToLegacyAudit(long pageId, Long executionId, LocalDateTime capturedAtUtc) {
        MoisSalesLibraryDtos.SalesLibraryPageResponse page = getPage(pageId);
        jdbcTemplate.update(
                """
                        INSERT INTO mois_sales_library_url_ingest
                        (workspace_id, source, url_original, url_canonical, title, first_captured_at, last_captured_at, ingest_count, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                        ON DUPLICATE KEY UPDATE
                            source = VALUES(source),
                            url_original = VALUES(url_original),
                            title = COALESCE(NULLIF(VALUES(title), ''), title),
                            last_captured_at = GREATEST(COALESCE(last_captured_at, VALUES(last_captured_at)), VALUES(last_captured_at)),
                            ingest_count = ingest_count + 1,
                            updated_at = UTC_TIMESTAMP()
                        """,
                page.workspaceId(), page.source(), page.urlCanonical(), page.urlCanonical(), page.title(), capturedAtUtc, capturedAtUtc);
        Long urlIngestId = findUrlIngestIdByCanonical(page.workspaceId(), page.urlCanonical());
        if (urlIngestId != null && executionId != null) {
            jdbcTemplate.update(
                    """
                            INSERT INTO mois_sales_library_processing_job
                            (url_ingest_id, status, attempts, created_at, updated_at)
                            VALUES (?, 'PENDING', 0, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                            """,
                    urlIngestId);
        }
        log.info("MOIS sales-library espelhou ingestão nova no legado para auditoria. modulo=MOIS, operacao=mirrorOperationalIngestToLegacyAudit, pageId={}, executionId={}, legacyUrlIngestId={}",
                pageId, executionId, urlIngestId);
    }

    /**
     * Localiza o item recém-reservado pelo token de claim do worker.
     */
    private java.util.Optional<MoisSalesLibraryDtos.CollectedReferenceHtmlCaptureJob> findClaimedCollectedReferenceHtml(String claimedBy) {
        return jdbcTemplate.query(
                """
                        SELECT id, collected_reference_id, collection_job_id, reference_id, source, title, url_original, url_source
                        FROM mois_collected_reference_html_capture
                        WHERE claimed_by = ?
                          AND status = 'CLAIMED'
                        ORDER BY claimed_at DESC, id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new MoisSalesLibraryDtos.CollectedReferenceHtmlCaptureJob(
                        rs.getLong("id"),
                        rs.getLong("collected_reference_id"),
                        rs.getString("collection_job_id"),
                        rs.getString("reference_id"),
                        rs.getString("source"),
                        rs.getString("title"),
                        rs.getString("url_original"),
                        rs.getString("url_source")),
                claimedBy)
                .stream()
                .findFirst();
    }

    /**
     * Calcula SHA-256 do HTML bruto para deduplicação e auditoria.
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    /**
     * Limita textos persistidos em colunas de tamanho fixo.
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * Resolve o job Hotmart solicitado ou, quando ausente, identifica o job mais recente do workspace.
     */
    private String resolveHotmartJobId(String workspaceId, String requestedJobId) {
        if (requestedJobId != null && !requestedJobId.isBlank()) {
            return requestedJobId.trim();
        }
        return jdbcTemplate.query(
                """
                        SELECT job_id
                        FROM mois_collected_reference
                        WHERE workspace_id = ?
                          AND source = 'HOTMART'
                        GROUP BY job_id
                        ORDER BY MAX(updated_at) DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getString("job_id"),
                workspaceId
        ).stream().findFirst().orElse(null);
    }

    /**
     * Busca produtos Hotmart coletados no lote escolhido para transformá-los em URLs da biblioteca.
     */
    private List<CollectedReferenceForIngest> findHotmartCollectedReferences(String workspaceId, String jobId, int limit) {
        return jdbcTemplate.query(
                """
                        SELECT reference_id, title, product_name, url, product_url, sales_page_url, collected_at
                        FROM mois_collected_reference
                        WHERE workspace_id = ?
                          AND source = 'HOTMART'
                          AND job_id = ?
                        ORDER BY collected_at ASC, id ASC
                        LIMIT ?
                        """,
                (rs, rowNum) -> mapCollectedReferenceForIngest(rs),
                workspaceId,
                jobId,
                limit
        );
    }

    /**
     * Mapeia uma linha de referência coletada para a estrutura interna de ingestão.
     */
    private CollectedReferenceForIngest mapCollectedReferenceForIngest(ResultSet rs) throws SQLException {
        Timestamp collectedAt = rs.getTimestamp("collected_at");
        return new CollectedReferenceForIngest(
                rs.getString("reference_id"),
                rs.getString("title"),
                rs.getString("product_name"),
                rs.getString("url"),
                rs.getString("product_url"),
                rs.getString("sales_page_url"),
                collectedAt == null ? null : collectedAt.toInstant()
        );
    }

    /**
     * Retorna o primeiro texto não vazio para aplicar fallbacks de URL e título.
     */
    private String coalesceNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Converte uma linha de mois_sales_page para o contrato de página consolidada usado pela UI.
     */
    private MoisSalesLibraryDtos.SalesLibraryPageResponse mapSalesPageResponse(ResultSet rs, int rowNum) throws SQLException {
        return new MoisSalesLibraryDtos.SalesLibraryPageResponse(
                rs.getLong("id"),
                rs.getString("workspace_id"),
                rs.getString("source"),
                rs.getString("url_canonical"),
                rs.getString("title"),
                rs.getString("current_stage"),
                rs.getString("current_status"),
                rs.getString("capture_status"),
                rs.getString("analysis_status"),
                rs.getString("url_final"),
                (Integer) rs.getObject("http_status"),
                rs.getString("html_sha256"),
                rs.getLong("html_bytes"),
                rs.getBigDecimal("score_total"),
                rs.getString("offer_summary"),
                rs.getString("mechanism_summary"),
                rs.getString("promise_summary"),
                rs.getString("proof_summary"),
                rs.getString("last_error_category"),
                rs.getString("last_error_message"),
                rs.getObject("last_job_execution_id", Long.class),
                toInstant(rs.getTimestamp("last_captured_at")),
                toInstant(rs.getTimestamp("last_analyzed_at")),
                toInstant(rs.getTimestamp("updated_at"))
        );
    }

    /**
     * Resolve o identificador legado de URL a partir do identificador operacional novo para ações ainda em escrita dupla.
     */
    private long findUrlIngestIdForOperationalPage(long pageId) {
        List<Long> rows = jdbcTemplate.query("""
                SELECT i.id
                FROM mois_sales_page sp
                JOIN mois_sales_library_url_ingest i
                  ON i.workspace_id = sp.workspace_id AND i.url_canonical = sp.url_canonical
                WHERE sp.id = ?
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("id"), pageId);
        if (!rows.isEmpty()) {
            return rows.get(0);
        }
        Long legacyExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_url_ingest WHERE id = ?", Long.class, pageId);
        if (legacyExists != null && legacyExists > 0) {
            return pageId;
        }
        throw new IllegalArgumentException("Page not found: " + pageId);
    }

    /**
     * Localiza uma URL ingerida pelo workspace e identificador canônico para espelhar atualizações no modelo novo.
     */
    private Long findUrlIngestIdByCanonical(String workspaceId, String canonical) {
        return jdbcTemplate.query(
                """
                        SELECT id
                        FROM mois_sales_library_url_ingest
                        WHERE workspace_id = ? AND url_canonical = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                workspaceId,
                canonical
        ).stream().findFirst().orElse(null);
    }

    /**
     * Cria um job pendente para uma URL ingerida e retorna o identificador criado.
     */
    private long createPendingJob(long urlIngestId) {
        jdbcTemplate.update(
                "INSERT INTO mois_sales_library_processing_job (url_ingest_id, status, attempts, created_at, updated_at) VALUES (?, ?, 0, UTC_TIMESTAMP(), UTC_TIMESTAMP())",
                urlIngestId,
                JOB_STATUS_PENDING
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id == null ? 0L : id;
    }

    /**
     * Normaliza uma URL para deduplicação por esquema, host e caminho.
     */
    private String canonicalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return rawUrl.trim();
            }
            String path = (uri.getPath() == null || uri.getPath().isBlank()) ? "/" : uri.getPath();
            return scheme + "://" + host.toLowerCase(Locale.ROOT) + path;
        } catch (Exception ex) {
            log.warn("Biblioteca de páginas de vendas não conseguiu canonicalizar URL. operacao=canonicalize, rawUrl={}", rawUrl, ex);
            return rawUrl.trim();
        }
    }

    /**
     * Converte timestamp JDBC em Instant preservando nulos.
     */
    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record IngestCounters(int persisted, int inserted, int updated, int jobsCreated, int skippedWithoutUrl) {
    }

    private record CollectedReferenceForIngest(
            String referenceId,
            String title,
            String productName,
            String url,
            String productUrl,
            String salesPageUrl,
            Instant collectedAt
    ) {
    }
}
