package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibraryDtos;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final String ANALYSIS_STATUS_CANCELED = "ANULADO";
    private static final int COLLECTED_REFERENCE_HTML_CANDIDATE_SCAN_LIMIT = 2000;

    private final JdbcTemplate jdbcTemplate;
    private final MoisSalesLibraryPricingService pricingService;

    /**
     * Reserva o próximo job pendente da biblioteca para processamento pelo worker.
     */
    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryClaimResponse claimJob(MoisSalesLibraryDtos.SalesLibraryClaimRequest request) {
        String normalizedSource = request.source().trim().toUpperCase(Locale.ROOT);
        MoisSalesLibraryDtos.SalesLibraryClaimResponse existingClaim = claimPendingAnalysisJob(request.workspaceId(), normalizedSource);
        if (existingClaim.claimed()) {
            return existingClaim;
        }
        Long createdExecutionId = createNextCapturedPageAnalysisExecution(request.workspaceId(), normalizedSource);
        if (createdExecutionId == null) {
            return new MoisSalesLibraryDtos.SalesLibraryClaimResponse(false, null);
        }
        log.info("MOIS sales-library etapa 2 criou análise pendente para página capturada. modulo=MOIS, operacao=claimJob, workspaceId={}, source={}, executionId={}",
                request.workspaceId(), normalizedSource, createdExecutionId);
        return claimPendingAnalysisJob(request.workspaceId(), normalizedSource);
    }

    /**
     * Lista páginas com HTML útil que podem ser consumidas pela etapa 2 sem alterar status ou reservar execução.
     */
    public MoisSalesLibraryDtos.SalesLibraryPendingAnalysisResponse listPendingAnalysis(
            String workspaceId,
            String source,
            int limit
    ) {
        String normalizedSource = source == null || source.isBlank() ? "HOTMART" : source.trim().toUpperCase(Locale.ROOT);
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        List<MoisSalesLibraryDtos.SalesLibraryPendingAnalysisItem> items = jdbcTemplate.query("""
                SELECT sp.id AS page_id,
                       sp.workspace_id,
                       sp.source,
                       sp.url_canonical,
                       sp.title,
                       COALESCE(sp.html_bytes, 0) AS html_bytes,
                       COALESCE(sp.analysis_status, sp.current_status) AS analysis_status,
                       sp.last_captured_at,
                       pending_analysis.id AS job_id,
                       COALESCE(MAX(all_analysis.attempt), 0) + 1 AS next_attempt,
                       COUNT(DISTINCT cap.id) > 0 AS raw_html_available,
                       COUNT(DISTINCT active_analysis.id) AS active_count,
                       COUNT(DISTINCT CASE WHEN all_analysis.status IN ('DONE', 'ANALYZED') THEN all_analysis.id END) AS done_count,
                       COUNT(DISTINCT CASE WHEN all_analysis.status = 'FAILED' THEN all_analysis.id END) AS failed_count
                FROM mois_sales_page sp
                LEFT JOIN mois_sales_page_job_execution pending_analysis
                  ON pending_analysis.sales_page_id = sp.id
                 AND pending_analysis.stage = 'ANALYSIS'
                 AND pending_analysis.status = 'PENDING'
                LEFT JOIN mois_sales_page_job_execution active_analysis
                  ON active_analysis.sales_page_id = sp.id
                 AND active_analysis.stage = 'ANALYSIS'
                 AND active_analysis.status IN ('PENDING', 'FETCHING')
                LEFT JOIN mois_sales_page_job_execution all_analysis
                  ON all_analysis.sales_page_id = sp.id
                 AND all_analysis.stage = 'ANALYSIS'
                LEFT JOIN mois_sales_page_job_execution cap
                  ON cap.sales_page_id = sp.id
                 AND cap.stage = 'CAPTURE'
                 AND cap.status IN ('CAPTURED', 'DUPLICATE')
                 AND COALESCE(cap.raw_html_bytes, 0) > 0
                WHERE sp.workspace_id = ?
                  AND sp.source = ?
                  AND COALESCE(sp.html_bytes, 0) > 0
                  AND COALESCE(sp.analysis_status, sp.current_status) NOT IN ('DONE', 'ANALYZED', 'ANULADO', 'FETCHING')
                GROUP BY sp.id, sp.workspace_id, sp.source, sp.url_canonical, sp.title, sp.html_bytes,
                         COALESCE(sp.analysis_status, sp.current_status), sp.last_captured_at, pending_analysis.id
                HAVING active_count = COUNT(DISTINCT pending_analysis.id)
                   AND done_count = 0
                   AND failed_count < 3
                ORDER BY sp.last_captured_at ASC, sp.updated_at ASC, sp.id ASC
                LIMIT ?
                """, (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryPendingAnalysisItem(
                nullableLong(rs, "job_id"),
                rs.getLong("page_id"),
                rs.getString("workspace_id"),
                rs.getString("source"),
                rs.getString("url_canonical"),
                rs.getString("title"),
                rs.getLong("html_bytes"),
                rs.getString("analysis_status"),
                rs.getInt("next_attempt"),
                toInstant(rs.getTimestamp("last_captured_at")),
                rs.getBoolean("raw_html_available")
        ), workspaceId, normalizedSource, normalizedLimit);
        return new MoisSalesLibraryDtos.SalesLibraryPendingAnalysisResponse(
                workspaceId,
                normalizedSource,
                normalizedLimit,
                items.size(),
                items
        );
    }

    /**
     * Reserva um job de análise pendente que já possui HTML útil capturado para servir como entrada da etapa 2.
     */
    private MoisSalesLibraryDtos.SalesLibraryClaimResponse claimPendingAnalysisJob(String workspaceId, String normalizedSource) {
        String claimedBy = UUID.randomUUID().toString();
        List<MoisSalesLibraryDtos.SalesLibraryClaimedJob> rows = jdbcTemplate.query("""
                SELECT e.id AS job_id, sp.id AS page_id, sp.url_canonical, sp.title, cap.raw_html AS raw_html
                FROM mois_sales_page_job_execution e
                JOIN mois_sales_page sp ON sp.id = e.sales_page_id
                LEFT JOIN mois_sales_page_job_execution cap ON cap.id = (
                    SELECT cap2.id
                    FROM mois_sales_page_job_execution cap2
                    WHERE cap2.sales_page_id = sp.id
                      AND cap2.stage = 'CAPTURE'
                      AND cap2.status IN ('CAPTURED', 'DUPLICATE')
                      AND COALESCE(cap2.raw_html_bytes, 0) > 0
                    ORDER BY cap2.finished_at DESC, cap2.id DESC
                    LIMIT 1
                )
                WHERE e.status = 'PENDING'
                  AND e.stage = 'ANALYSIS'
                  AND e.job_type IN ('PAGE_ANALYSIS', 'PROCESSING_JOB')
                  AND COALESCE(sp.html_bytes, 0) > 0
                  AND sp.workspace_id = ?
                  AND sp.source = ?
                ORDER BY e.created_at ASC, e.id ASC
                LIMIT 1
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryClaimedJob(
                rs.getLong("job_id"), rs.getLong("page_id"), rs.getString("url_canonical"), rs.getString("title"), rs.getString("raw_html")),
                workspaceId, normalizedSource);
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
        log.info("MOIS sales-library claim usando modelo operacional novo. modulo=MOIS, operacao=claimJob, workspaceId={}, source={}, pageId={}, executionId={}",
                workspaceId, normalizedSource, job.pageId(), job.jobId());
        return new MoisSalesLibraryDtos.SalesLibraryClaimResponse(true, job);
    }

    /**
     * Registra a análise concluída de uma página e finaliza o job correspondente.
     */
    @Transactional
    public void completeJob(long jobId, MoisSalesLibraryDtos.SalesLibraryCompleteRequest request) {
        Long salesPageId = findOperationalSalesPageIdForExecution(jobId);
        if (salesPageId == null) {
            throw new IllegalArgumentException("Operational analysis execution not found: " + jobId);
        }
        Instant analyzedAt = request.analyzedAt() == null ? Instant.now() : request.analyzedAt();
        BigDecimal modelCostUsd = resolveModelCostUsd(request);
        jdbcTemplate.update("""
                UPDATE mois_sales_page_job_execution
                SET job_type = 'PAGE_ANALYSIS', stage = 'ANALYSIS', status = 'DONE', score_total = ?,
                    sections_json = ?, copy_json = ?, visual_json = ?, image_json = ?,
                    request_payload_json = ?, response_payload_json = ?, parser_version = ?, prompt_version = ?,
                    model_name = ?, input_tokens = ?, output_tokens = ?, model_cost_usd = ?,
                    error_category = NULL, error_message = ?, finished_at = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, request.scoreTotal(), request.sectionsJson(), request.copyJson(), request.visualJson(), request.imageJson(),
                request.requestPayloadJson(), request.responsePayloadJson(), request.parserVersion(), request.promptVersion(),
                request.modelName(), request.inputTokens(), request.outputTokens(), modelCostUsd, null, Timestamp.from(analyzedAt), jobId);
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                SET current_stage = 'ANALYSIS', current_status = 'DONE', analysis_status = 'DONE', score_total = ?,
                    model_name = ?, input_tokens = ?, output_tokens = ?, model_cost_usd = ?,
                    last_error_category = NULL, last_error_message = NULL, last_job_execution_id = ?,
                    last_analyzed_at = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, request.scoreTotal(), request.modelName(), request.inputTokens(), request.outputTokens(), modelCostUsd,
                jobId, Timestamp.from(analyzedAt), salesPageId);
        log.info(
                "MOIS sales-library análise concluída no modelo operacional novo. modulo=MOIS, operacao=completeJob, "
                        + "pageId={}, executionId={}, scoreTotal={}, modelName={}, inputTokens={}, outputTokens={}, modelCostUsd={}",
                salesPageId,
                jobId,
                request.scoreTotal(),
                request.modelName(),
                request.inputTokens(),
                request.outputTokens(),
                modelCostUsd);
    }

    /**
     * Calcula o custo batch do modelo a partir dos tokens retornados pela OpenAI, usando fallback do payload quando necessário.
     */
    private BigDecimal resolveModelCostUsd(MoisSalesLibraryDtos.SalesLibraryCompleteRequest request) {
        if (request.modelCostUsd() != null) {
            return request.modelCostUsd();
        }
        if ((request.inputTokens() == null && request.outputTokens() == null) || request.modelName() == null || request.modelName().isBlank()) {
            return null;
        }
        try {
            return pricingService.estimateBatchCost(request.modelName(), request.inputTokens(), request.outputTokens());
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao calcular custo OpenAI da análise MOIS. modulo=MOIS, operacao=resolveModelCostUsd, "
                            + "modelName={}, inputTokens={}, outputTokens={}",
                    request.modelName(),
                    request.inputTokens(),
                    request.outputTokens(),
                    ex);
            return null;
        }
    }

    /**
     * Marca um job como falho preservando a categoria e a mensagem de erro operacional.
     */
    @Transactional
    public void failJob(long jobId, MoisSalesLibraryDtos.SalesLibraryFailRequest request) {
        Long salesPageId = findOperationalSalesPageIdForExecution(jobId);
        if (salesPageId == null) {
            throw new IllegalArgumentException("Operational analysis execution not found: " + jobId);
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
     * Reserva uma referência coletada criando página e execução de captura no modelo operacional principal.
     */
    @Transactional
    public MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse claimCollectedReferenceHtml(
            MoisSalesLibraryDtos.CollectedReferenceHtmlClaimRequest request
    ) {
        String normalizedSource = request.source().trim().toUpperCase(Locale.ROOT);
        String claimedBy = UUID.randomUUID().toString();
        log.info("MOIS sales-library claim de referência coletada recebido. modulo=MOIS, operacao=claimCollectedReferenceHtml, workspaceId={}, source={}",
                request.workspaceId(), normalizedSource);
        return findNextCollectedReferenceHtmlCandidate(request.workspaceId(), normalizedSource)
                .map(candidate -> claimCollectedReferenceCandidate(candidate, claimedBy))
                .orElseGet(() -> {
                    log.info("MOIS sales-library claim de referência coletada sem candidato elegível. modulo=MOIS, operacao=claimCollectedReferenceHtml, workspaceId={}, source={}",
                            request.workspaceId(), normalizedSource);
                    return new MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse(false, null);
                });
    }

    /**
     * Persiste o HTML bruto capturado pelo worker MOIS diretamente na execução operacional.
     */
    @Transactional
    public MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse completeCollectedReferenceHtml(
            long captureId,
            MoisSalesLibraryDtos.CollectedReferenceHtmlCompleteRequest request
    ) {
        CaptureExecution execution = findCollectedReferenceCaptureExecution(captureId);
        String rawHtml = request.rawHtml() == null ? "" : request.rawHtml();
        byte[] rawHtmlBytes = rawHtml.getBytes(StandardCharsets.UTF_8);
        String hash = sha256(rawHtml);
        Instant fetchedAt = request.fetchedAt() == null ? Instant.now() : request.fetchedAt();
        jdbcTemplate.update(
                """
                        UPDATE mois_sales_page_job_execution
                        SET status = 'CAPTURED', final_url = ?, http_status = ?, content_type = ?, raw_html = ?,
                            raw_html_sha256 = ?, raw_html_bytes = ?, error_category = NULL, error_message = NULL,
                            finished_at = ?, updated_at = UTC_TIMESTAMP()
                        WHERE id = ?
                        """,
                truncate(request.finalUrl(), 1024),
                request.httpStatus(),
                truncate(request.contentType(), 255),
                rawHtml,
                hash,
                rawHtmlBytes.length,
                Timestamp.from(fetchedAt),
                captureId);
        updateSalesPageAfterCollectedReferenceCapture(execution.salesPageId(), captureId, "CAPTURED", request.httpStatus(),
                request.contentType(), request.finalUrl(), hash, rawHtmlBytes.length, null, null, fetchedAt);
        log.info("MOIS sales-library captura de referência coletada concluída no modelo operacional novo. modulo=MOIS, operacao=completeCollectedReferenceHtml, pageId={}, executionId={}, bytes={}",
                execution.salesPageId(), captureId, rawHtmlBytes.length);
        return new MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse(captureId, "CAPTURED");
    }

    /**
     * Registra falha terminal da captura de HTML bruto de uma referência coletada no histórico operacional.
     */
    @Transactional
    public MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse failCollectedReferenceHtml(
            long captureId,
            MoisSalesLibraryDtos.CollectedReferenceHtmlFailRequest request
    ) {
        CaptureExecution execution = findCollectedReferenceCaptureExecution(captureId);
        String message = coalesceNotBlank(request.errorMessage(), request.errorCategory(), "Falha sem detalhe");
        jdbcTemplate.update(
                """
                        UPDATE mois_sales_page_job_execution
                        SET status = 'FAILED', error_category = ?, error_message = ?, finished_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                        WHERE id = ?
                        """,
                truncate(request.errorCategory(), 120),
                truncate(message, 1000),
                captureId);
        updateSalesPageAfterCollectedReferenceCapture(execution.salesPageId(), captureId, "FAILED", null, null, null, null, 0L,
                request.errorCategory(), message, Instant.now());
        log.info("MOIS sales-library captura de referência coletada falhou no modelo operacional novo. modulo=MOIS, operacao=failCollectedReferenceHtml, pageId={}, executionId={}, errorCategory={}",
                execution.salesPageId(), captureId, request.errorCategory());
        return new MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse(captureId, "FAILED");
    }

    /**
     * Busca os metadados operacionais de uma execução da biblioteca no histórico consolidado.
     */
    public MoisSalesLibraryDtos.SalesLibraryJobResponse getJob(long jobId) {
        List<MoisSalesLibraryDtos.SalesLibraryJobResponse> rows = jdbcTemplate.query("""
                        SELECT id, sales_page_id, status, attempt, error_category, error_message,
                               created_at, updated_at, started_at, finished_at
                        FROM mois_sales_page_job_execution
                        WHERE id = ?
                        LIMIT 1
                        """, (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryJobResponse(
                        rs.getLong("id"), rs.getLong("sales_page_id"), rs.getString("status"),
                        rs.getInt("attempt"), rs.getString("error_category"), rs.getString("error_message"),
                        null, toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at")), toInstant(rs.getTimestamp("started_at")),
                        toInstant(rs.getTimestamp("finished_at"))), jobId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Job execution not found: " + jobId);
        }
        return rows.get(0);
    }

    /**
     * Lista execuções da biblioteca por workspace, com filtro opcional por status, usando as tabelas operacionais novas.
     */
    public MoisSalesLibraryDtos.SalesLibraryJobPageResponse listJobs(String workspaceId, String status, int page, int pageSize) {
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (normalizedPage - 1) * normalizedPageSize;
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        String statusClause = normalizedStatus == null ? "" : " AND status = ?";
        Long total = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM mois_sales_page_job_execution
                        WHERE workspace_id = ?
                        """ + statusClause,
                Long.class, normalizedStatus == null ? new Object[]{workspaceId} : new Object[]{workspaceId, normalizedStatus});
        List<MoisSalesLibraryDtos.SalesLibraryJobResponse> items = jdbcTemplate.query("""
                        SELECT id, sales_page_id, status, attempt, error_category, error_message,
                               created_at, updated_at, started_at, finished_at
                        FROM mois_sales_page_job_execution
                        WHERE workspace_id = ?
                        """ + statusClause + " ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryJobResponse(rs.getLong("id"), rs.getLong("sales_page_id"),
                        rs.getString("status"), rs.getInt("attempt"), rs.getString("error_category"), rs.getString("error_message"),
                        null, toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at")), toInstant(rs.getTimestamp("started_at")),
                        toInstant(rs.getTimestamp("finished_at"))),
                normalizedStatus == null ? new Object[]{workspaceId, normalizedPageSize, offset} : new Object[]{workspaceId, normalizedStatus, normalizedPageSize, offset});
        return new MoisSalesLibraryDtos.SalesLibraryJobPageResponse(normalizedPage, normalizedPageSize, total == null ? 0 : total, items);
    }

    /**
     * Lista entradas de URL ingeridas usando mois_sales_page como fonte operacional principal.
     */
    public MoisSalesLibraryDtos.SalesLibraryEntryPageResponse listEntries(String workspaceId, int page, int pageSize) {
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (normalizedPage - 1) * normalizedPageSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_page WHERE workspace_id = ?", Long.class, workspaceId);
        List<MoisSalesLibraryDtos.SalesLibraryEntryResponse> items = jdbcTemplate.query("""
                        SELECT id, workspace_id, source, url_original, url_canonical, title, ingest_count,
                               first_seen_at, last_captured_at, updated_at
                        FROM mois_sales_page
                        WHERE workspace_id = ? ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?
                        """, (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryEntryResponse(
                        rs.getLong("id"), rs.getString("workspace_id"), rs.getString("source"), rs.getString("url_original"),
                        rs.getString("url_canonical"), rs.getString("title"), rs.getInt("ingest_count"),
                        toInstant(rs.getTimestamp("first_seen_at")), toInstant(rs.getTimestamp("last_captured_at")),
                        toInstant(rs.getTimestamp("updated_at"))), workspaceId, normalizedPageSize, offset);
        return new MoisSalesLibraryDtos.SalesLibraryEntryPageResponse(normalizedPage, normalizedPageSize, total == null ? 0 : total, items);
    }

    /**
     * Resume URLs únicas de páginas de venda vindas da origem bruta mois_collected_reference.
     */
    public MoisSalesLibraryDtos.CollectedReferenceUrlSummaryResponse summarizeCollectedReferenceUrls(String workspaceId) {
        List<CollectedReferenceUrlCandidate> collectedUrls = jdbcTemplate.query("""
                        SELECT DISTINCT source, url_source, effective_url
                        FROM (
                          SELECT source,
                                 CASE
                                   WHEN sales_page_url IS NOT NULL AND TRIM(sales_page_url) <> '' THEN 'SALES_PAGE_URL'
                                   WHEN product_url IS NOT NULL AND TRIM(product_url) <> '' THEN 'PRODUCT_URL'
                                   WHEN url IS NOT NULL AND TRIM(url) <> '' THEN 'URL'
                                   ELSE 'NONE'
                                 END AS url_source,
                                 COALESCE(NULLIF(TRIM(sales_page_url), ''), NULLIF(TRIM(product_url), ''), NULLIF(TRIM(url), '')) AS effective_url
                          FROM mois_collected_reference
                          WHERE workspace_id = ?
                        ) collected
                        WHERE effective_url IS NOT NULL
                        """,
                (rs, rowNum) -> new CollectedReferenceUrlCandidate(
                        normalizeSource(rs.getString("source")),
                        rs.getString("url_source"),
                        canonicalize(rs.getString("effective_url"))),
                workspaceId);

        Set<String> operationalUrls = new HashSet<>(jdbcTemplate.query(
                "SELECT url_canonical FROM mois_sales_page WHERE workspace_id = ?",
                (rs, rowNum) -> canonicalize(rs.getString("url_canonical")),
                workspaceId));
        operationalUrls.remove(null);

        Map<String, Set<String>> urlsBySource = new LinkedHashMap<>();
        Map<String, Set<String>> urlsByType = new LinkedHashMap<>();
        Set<String> allCollectedUrls = new HashSet<>();
        for (CollectedReferenceUrlCandidate candidate : collectedUrls) {
            if (candidate.canonicalUrl() == null || candidate.canonicalUrl().isBlank()) {
                continue;
            }
            allCollectedUrls.add(candidate.canonicalUrl());
            urlsBySource.computeIfAbsent(candidate.source(), ignored -> new HashSet<>()).add(candidate.canonicalUrl());
            urlsByType.computeIfAbsent(candidate.urlSource(), ignored -> new HashSet<>()).add(candidate.canonicalUrl());
        }

        List<MoisSalesLibraryDtos.CollectedReferenceUrlSourceBreakdown> bySource = urlsBySource.entrySet().stream()
                .map(entry -> buildCollectedReferenceSourceBreakdown(entry.getKey(), entry.getValue(), operationalUrls))
                .sorted(Comparator.comparing(MoisSalesLibraryDtos.CollectedReferenceUrlSourceBreakdown::uniqueEffectiveUrls).reversed())
                .toList();
        List<MoisSalesLibraryDtos.CollectedReferenceUrlTypeBreakdown> byUrlType = urlsByType.entrySet().stream()
                .map(entry -> new MoisSalesLibraryDtos.CollectedReferenceUrlTypeBreakdown(entry.getKey(), entry.getValue().size()))
                .sorted(Comparator.comparing(MoisSalesLibraryDtos.CollectedReferenceUrlTypeBreakdown::uniqueUrls).reversed())
                .toList();

        long operationalOverlap = allCollectedUrls.stream().filter(operationalUrls::contains).count();
        return new MoisSalesLibraryDtos.CollectedReferenceUrlSummaryResponse(
                workspaceId,
                allCollectedUrls.size(),
                sizeOf(urlsByType, "SALES_PAGE_URL"),
                sizeOf(urlsByType, "PRODUCT_URL"),
                operationalOverlap,
                allCollectedUrls.size() - operationalOverlap,
                bySource,
                byUrlType);
    }

    /**
     * Monta o desdobramento de cobertura operacional para uma origem de marketplace.
     */
    private MoisSalesLibraryDtos.CollectedReferenceUrlSourceBreakdown buildCollectedReferenceSourceBreakdown(
            String source,
            Set<String> sourceUrls,
            Set<String> operationalUrls
    ) {
        long operationalOverlap = sourceUrls.stream().filter(operationalUrls::contains).count();
        return new MoisSalesLibraryDtos.CollectedReferenceUrlSourceBreakdown(
                source, sourceUrls.size(), operationalOverlap, sourceUrls.size() - operationalOverlap);
    }

    /**
     * Retorna a quantidade de URLs únicas de um agrupamento de tipo, preservando zero para ausentes.
     */
    private long sizeOf(Map<String, Set<String>> groupedUrls, String key) {
        Set<String> values = groupedUrls.get(key);
        return values == null ? 0 : values.size();
    }

    /**
     * Lista páginas canônicas consolidadas priorizando as análises mais recentes para a UI operacional.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageListResponse listPages(String workspaceId, int page, int pageSize) {
        return listPages(workspaceId, page, pageSize, null, "RECENT_ANALYSIS");
    }

    /**
     * Lista páginas canônicas com filtro e ordenação de aquecimento para priorização comercial da Etapa 3.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageListResponse listPages(
            String workspaceId,
            int page,
            int pageSize,
            String marketWarmupFilter,
            String sort
    ) {
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (normalizedPage - 1) * normalizedPageSize;
        String warmupCondition = resolveMarketWarmupFilterCondition(marketWarmupFilter);
        String orderBy = resolveSalesPageOrderBy(sort);
        String joins = """
                FROM mois_sales_page p
                LEFT JOIN (
                    SELECT sales_page_id, MAX(id) AS latest_warmup_job_id
                    FROM mois_sales_page_market_warmup_job
                    GROUP BY sales_page_id
                ) mwj_latest ON mwj_latest.sales_page_id = p.id
                LEFT JOIN mois_sales_page_market_warmup_job mwj ON mwj.id = mwj_latest.latest_warmup_job_id
                LEFT JOIN mois_sales_page_market_warmup_summary mws ON mws.job_id = mwj.id
                LEFT JOIN mois_collected_reference cr_direct ON cr_direct.id = p.collected_reference_id
                LEFT JOIN (
                    SELECT workspace_id, COALESCE(sales_page_url, product_url) AS reference_url, MAX(id) AS latest_reference_id
                    FROM mois_collected_reference
                    WHERE source = 'HOTMART' AND COALESCE(sales_page_url, product_url) IS NOT NULL
                    GROUP BY workspace_id, COALESCE(sales_page_url, product_url)
                ) cr_latest ON cr_latest.workspace_id = p.workspace_id AND cr_latest.reference_url = p.url_canonical
                LEFT JOIN mois_collected_reference cr_url ON cr_url.id = cr_latest.latest_reference_id
                WHERE p.workspace_id = ?
                """ + warmupCondition;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + joins, Long.class, workspaceId);
        List<MoisSalesLibraryDtos.SalesLibraryPageResponse> items = jdbcTemplate.query("""
                SELECT p.id, p.workspace_id, p.source, p.url_canonical, p.title, p.current_stage, p.current_status, p.capture_status,
                       COALESCE(p.analysis_status, p.current_status) AS analysis_status, p.url_final, p.http_status, p.html_sha256,
                       p.html_bytes, p.score_total, p.product_name, COALESCE(cr_direct.producer_name, cr_url.producer_name) AS producer_name, COALESCE(cr_direct.hotmart_price, cr_url.hotmart_price) AS hotmart_price, COALESCE(cr_direct.hotmart_temperature, cr_url.hotmart_temperature) AS hotmart_temperature, COALESCE(cr_direct.hotmart_producer, cr_url.hotmart_producer) AS hotmart_producer,
                       p.offer_summary, p.mechanism_summary, p.promise_summary, p.proof_summary,
                       p.model_name, p.input_tokens, p.output_tokens, p.model_cost_usd,
                       p.last_error_category, p.last_error_message, p.last_job_execution_id, p.last_captured_at, p.last_analyzed_at, p.updated_at,
                       mws.score_total AS market_warmup_score_total, mws.market_temperature AS market_warmup_temperature,
                       mws.ecosystem_type AS market_warmup_ecosystem_type, mwj.recommendation AS market_warmup_recommendation,
                       mwj.status AS market_warmup_status, COALESCE(mws.updated_at, mwj.updated_at) AS market_warmup_updated_at
                """ + joins + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
                this::mapSalesPageResponse, workspaceId, normalizedPageSize, offset);
        return new MoisSalesLibraryDtos.SalesLibraryPageListResponse(normalizedPage, normalizedPageSize, total == null ? 0 : total, items);
    }

    /**
     * Converte o filtro público de aquecimento em condição SQL fixa para evitar concatenação de entrada do usuário.
     */
    private String resolveMarketWarmupFilterCondition(String marketWarmupFilter) {
        if (marketWarmupFilter == null || marketWarmupFilter.isBlank()) {
            return "";
        }
        return switch (marketWarmupFilter) {
            case "WITH_DOSSIER" -> " AND mwj.status = 'DONE' AND mws.job_id IS NOT NULL\n";
            case "PENDING_OR_RUNNING" -> " AND mwj.status IN ('PENDING', 'FETCHING')\n";
            case "FAILED" -> " AND mwj.status = 'FAILED'\n";
            case "HOT_OR_PROMISING" -> " AND mws.market_temperature IN ('HOT', 'PROMISING')\n";
            case "WITHOUT_DOSSIER" -> " AND mwj.id IS NULL\n";
            default -> "";
        };
    }

    /**
     * Converte a ordenação pública em cláusula SQL fixa para a listagem operacional da biblioteca.
     */
    private String resolveSalesPageOrderBy(String sort) {
        if ("MARKET_WARMUP_SCORE".equals(sort)) {
            return "mws.score_total IS NULL ASC, mws.score_total DESC, COALESCE(mws.updated_at, mwj.updated_at) DESC, p.last_analyzed_at DESC, p.id DESC";
        }
        return "p.last_analyzed_at DESC, p.updated_at DESC, p.id DESC";
    }

    /**
     * Lista oportunidades com decisão comercial combinando score da página, aquecimento, saturação e recência da evidência.
     */
    public MoisSalesLibraryDtos.MarketWarmupOpportunityRankingResponse rankMarketWarmupOpportunities(String workspaceId, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 50));
        List<MoisSalesLibraryDtos.MarketWarmupOpportunityRankingItem> items = jdbcTemplate.query("""
                SELECT ranked.page_id, ranked.title, ranked.url_canonical, ranked.source, ranked.page_score_total,
                       ranked.warmup_score_total, ranked.combined_commercial_score, ranked.market_temperature,
                       ranked.ecosystem_type, ranked.recommendation, ranked.saturation_risk, ranked.evidence_updated_at,
                       ranked.opportunity_recommendation, ranked.next_experiment_suggestion
                FROM (
                    SELECT p.id AS page_id, p.title, p.url_canonical, p.source,
                           COALESCE(p.score_total, 0) AS page_score_total,
                           COALESCE(mws.score_total, 0) AS warmup_score_total,
                           mws.market_temperature, mws.ecosystem_type, mwj.recommendation, mws.saturation_risk,
                           COALESCE(src.evidence_updated_at, mws.updated_at, mwj.updated_at) AS evidence_updated_at,
                           mws.opportunity_recommendation, mws.next_experiment_suggestion,
                           ROUND(
                               (COALESCE(p.score_total, 0) * 0.45) +
                               (COALESCE(mws.score_total, 0) * 0.35) +
                               (GREATEST(0, 100 - LEAST(100, TIMESTAMPDIFF(DAY, COALESCE(src.evidence_updated_at, mws.updated_at, mwj.updated_at), UTC_TIMESTAMP()) * 5)) * 0.20) -
                               (CASE
                                   WHEN mws.market_temperature = 'SATURATED' OR mwj.recommendation = 'SATURATED_REQUIRES_ANGLE'
                                        OR COALESCE(mws.saturation_risk, '') <> '' THEN 20
                                   ELSE 0
                                END)
                           , 2) AS combined_commercial_score
                    FROM mois_sales_page p
                    JOIN (
                        SELECT sales_page_id, MAX(id) AS latest_warmup_job_id
                        FROM mois_sales_page_market_warmup_job
                        WHERE status = 'DONE'
                        GROUP BY sales_page_id
                    ) mwj_latest ON mwj_latest.sales_page_id = p.id
                    JOIN mois_sales_page_market_warmup_job mwj ON mwj.id = mwj_latest.latest_warmup_job_id
                    JOIN mois_sales_page_market_warmup_summary mws ON mws.job_id = mwj.id
                    LEFT JOIN (
                        SELECT job_id, MAX(COALESCE(last_activity_at, published_at, updated_at, created_at)) AS evidence_updated_at
                        FROM mois_sales_page_market_warmup_source
                        GROUP BY job_id
                    ) src ON src.job_id = mwj.id
                    WHERE p.workspace_id = ?
                ) ranked
                ORDER BY ranked.combined_commercial_score DESC, ranked.warmup_score_total DESC, ranked.evidence_updated_at DESC, ranked.page_id DESC
                LIMIT ?
                """, this::mapMarketWarmupOpportunityRankingItem, workspaceId, normalizedLimit);
        return new MoisSalesLibraryDtos.MarketWarmupOpportunityRankingResponse(workspaceId, normalizedLimit, items);
    }

    /**
     * Calcula os contadores globais da biblioteca usando html_bytes > 0 como critério canônico de página capturada.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageSummaryResponse summarizePages(String workspaceId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total,
                       SUM(p.current_status = 'PENDING') AS pending,
                       SUM(p.current_status IN ('FETCHING', 'CAPTURING')) AS capturing,
                       SUM(COALESCE(p.html_bytes, 0) > 0) AS captured,
                       SUM(p.current_status IN ('DONE', 'ANALYZED')) AS analyzed,
                       SUM(COALESCE(p.html_bytes, 0) > 0 AND COALESCE(p.analysis_status, p.current_status) = 'PENDING') AS analysis_pending,
                       SUM(COALESCE(p.html_bytes, 0) > 0 AND COALESCE(p.analysis_status, p.current_status) = 'FETCHING') AS analysis_running,
                       SUM(COALESCE(p.html_bytes, 0) > 0 AND COALESCE(p.analysis_status, p.current_status) = 'FAILED') AS analysis_failed,
                       SUM(p.current_status = 'FAILED') AS failed,
                       SUM(p.current_status = 'BLOCKED_COOLDOWN') AS blocked_cooldown,
                       SUM(p.source = 'HOTMART') AS hotmart,
                       SUM(p.source = 'CLICKBANK') AS clickbank,
                       SUM(p.current_status IN ('DONE', 'ANALYZED')) AS market_warmup_eligible,
                       SUM(mwj.status = 'PENDING') AS market_warmup_pending,
                       SUM(mwj.status = 'FETCHING') AS market_warmup_running,
                       SUM(mwj.status = 'DONE' AND mws.job_id IS NOT NULL) AS market_warmup_completed,
                       SUM(mwj.status = 'FAILED') AS market_warmup_failed,
                       SUM(mws.market_temperature = 'HOT') AS market_warmup_hot,
                       SUM(mws.market_temperature = 'PROMISING') AS market_warmup_promising,
                       SUM(mws.market_temperature = 'WARM') AS market_warmup_warm,
                       SUM(mws.market_temperature = 'COLD') AS market_warmup_cold,
                       SUM(mws.market_temperature = 'SATURATED') AS market_warmup_saturated,
                       SUM(mwj.status = 'FETCHING'
                           AND COALESCE(mwj.started_at, mwj.updated_at, mwj.created_at) < DATE_SUB(UTC_TIMESTAMP(), INTERVAL 120 MINUTE)) AS market_warmup_stuck,
                       SUM(COALESCE(p.html_bytes, 0) = 0) AS remaining_without_html,
                       MAX(CASE WHEN COALESCE(p.html_bytes, 0) > 0 THEN p.last_captured_at END) AS last_captured_at,
                       (
                           SELECT COUNT(DISTINCT e.sales_page_id)
                           FROM mois_sales_page_job_execution e
                           JOIN mois_sales_page ep ON ep.id = e.sales_page_id
                           WHERE ep.workspace_id = ?
                             AND e.stage = 'CAPTURE'
                             AND e.status = 'CAPTURED'
                             AND e.finished_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 HOUR)
                       ) AS captured_last_hour,
                       (
                           SELECT COUNT(DISTINCT e.sales_page_id)
                           FROM mois_sales_page_job_execution e
                           JOIN mois_sales_page ep ON ep.id = e.sales_page_id
                           WHERE ep.workspace_id = ?
                             AND e.stage = 'CAPTURE'
                             AND e.status = 'CAPTURED'
                             AND e.finished_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 6 HOUR)
                       ) / 6.0 AS average_captures_per_hour,
                       MAX(p.updated_at) AS updated_at
                FROM mois_sales_page p
                LEFT JOIN (
                    SELECT sales_page_id, MAX(id) AS latest_warmup_job_id
                    FROM mois_sales_page_market_warmup_job
                    GROUP BY sales_page_id
                ) mwj_latest ON mwj_latest.sales_page_id = p.id
                LEFT JOIN mois_sales_page_market_warmup_job mwj ON mwj.id = mwj_latest.latest_warmup_job_id
                LEFT JOIN mois_sales_page_market_warmup_summary mws ON mws.job_id = mwj.id
                WHERE p.workspace_id = ?
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryPageSummaryResponse(
                workspaceId, rs.getLong("total"), rs.getLong("pending"), rs.getLong("capturing"),
                rs.getLong("captured"), rs.getLong("analyzed"), rs.getLong("analysis_pending"),
                rs.getLong("analysis_running"), rs.getLong("analysis_failed"), rs.getLong("failed"),
                rs.getLong("blocked_cooldown"), rs.getLong("hotmart"), rs.getLong("clickbank"),
                rs.getLong("market_warmup_eligible"), rs.getLong("market_warmup_pending"),
                rs.getLong("market_warmup_running"), rs.getLong("market_warmup_completed"),
                rs.getLong("market_warmup_failed"), rs.getLong("market_warmup_hot"),
                rs.getLong("market_warmup_promising"), rs.getLong("market_warmup_warm"),
                rs.getLong("market_warmup_cold"), rs.getLong("market_warmup_saturated"),
                rs.getLong("market_warmup_stuck"),
                rs.getLong("capturing") > 0 || rs.getLong("captured_last_hour") > 0,
                toInstant(rs.getTimestamp("last_captured_at")), rs.getLong("captured_last_hour"),
                rs.getLong("remaining_without_html"), rs.getBigDecimal("average_captures_per_hour"),
                toInstant(rs.getTimestamp("updated_at"))), workspaceId, workspaceId, workspaceId);
    }

    /**
     * Busca uma página canônica pelo identificador consolidado em mois_sales_page.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageResponse getPage(long pageId) {
        List<MoisSalesLibraryDtos.SalesLibraryPageResponse> rows = jdbcTemplate.query("""
                SELECT p.id, p.workspace_id, p.source, p.url_canonical, p.title, p.current_stage, p.current_status, p.capture_status,
                       COALESCE(p.analysis_status, p.current_status) AS analysis_status, p.url_final, p.http_status, p.html_sha256,
                       p.html_bytes, p.score_total, p.product_name, COALESCE(cr_direct.producer_name, cr_url.producer_name) AS producer_name, COALESCE(cr_direct.hotmart_price, cr_url.hotmart_price) AS hotmart_price, COALESCE(cr_direct.hotmart_temperature, cr_url.hotmart_temperature) AS hotmart_temperature, COALESCE(cr_direct.hotmart_producer, cr_url.hotmart_producer) AS hotmart_producer,
                       p.offer_summary, p.mechanism_summary, p.promise_summary, p.proof_summary,
                       p.model_name, p.input_tokens, p.output_tokens, p.model_cost_usd,
                       p.last_error_category, p.last_error_message, p.last_job_execution_id, p.last_captured_at, p.last_analyzed_at, p.updated_at,
                       mws.score_total AS market_warmup_score_total, mws.market_temperature AS market_warmup_temperature,
                       mws.ecosystem_type AS market_warmup_ecosystem_type, mwj.recommendation AS market_warmup_recommendation,
                       mwj.status AS market_warmup_status, COALESCE(mws.updated_at, mwj.updated_at) AS market_warmup_updated_at
                FROM mois_sales_page p
                LEFT JOIN (
                    SELECT sales_page_id, MAX(id) AS latest_warmup_job_id
                    FROM mois_sales_page_market_warmup_job
                    GROUP BY sales_page_id
                ) mwj_latest ON mwj_latest.sales_page_id = p.id
                LEFT JOIN mois_sales_page_market_warmup_job mwj ON mwj.id = mwj_latest.latest_warmup_job_id
                LEFT JOIN mois_sales_page_market_warmup_summary mws ON mws.job_id = mwj.id
                LEFT JOIN mois_collected_reference cr_direct ON cr_direct.id = p.collected_reference_id
                LEFT JOIN (
                    SELECT workspace_id, COALESCE(sales_page_url, product_url) AS reference_url, MAX(id) AS latest_reference_id
                    FROM mois_collected_reference
                    WHERE source = 'HOTMART' AND COALESCE(sales_page_url, product_url) IS NOT NULL
                    GROUP BY workspace_id, COALESCE(sales_page_url, product_url)
                ) cr_latest ON cr_latest.workspace_id = p.workspace_id AND cr_latest.reference_url = p.url_canonical
                LEFT JOIN mois_collected_reference cr_url ON cr_url.id = cr_latest.latest_reference_id
                WHERE p.id = ? LIMIT 1
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
                       e.image_json, e.request_payload_json, e.response_payload_json, e.error_message, e.parser_version, e.prompt_version,
                       e.model_name, e.finished_at, e.updated_at
                FROM mois_sales_page_job_execution e
                WHERE e.sales_page_id = ? AND e.job_type = 'PAGE_ANALYSIS'
                ORDER BY e.updated_at DESC, e.id DESC LIMIT 1
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse(
                rs.getLong("id"), rs.getLong("sales_page_id"), null, rs.getString("status"),
                rs.getBigDecimal("score_total"), rs.getString("parser_version"), rs.getString("prompt_version"), rs.getString("model_name"), rs.getString("sections_json"),
                rs.getString("copy_json"), rs.getString("visual_json"), rs.getString("image_json"),
                rs.getString("error_message"), rs.getString("request_payload_json"), rs.getString("response_payload_json"),
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
     * Cria uma execução pendente para a próxima página com HTML capturado e sem análise ativa ou concluída.
     */
    private Long createNextCapturedPageAnalysisExecution(String workspaceId, String normalizedSource) {
        List<CapturedPageAnalysisCandidate> candidates = jdbcTemplate.query("""
                SELECT sp.id AS page_id,
                       sp.url_canonical,
                       COALESCE(MAX(all_analysis.attempt), 0) + 1 AS next_attempt,
                       SUM(CASE WHEN active_analysis.id IS NULL THEN 0 ELSE 1 END) AS active_count,
                       SUM(CASE WHEN all_analysis.status IN ('DONE', 'ANALYZED') THEN 1 ELSE 0 END) AS done_count,
                       SUM(CASE WHEN all_analysis.status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count
                FROM mois_sales_page sp
                LEFT JOIN mois_sales_page_job_execution active_analysis
                  ON active_analysis.sales_page_id = sp.id
                 AND active_analysis.stage = 'ANALYSIS'
                 AND active_analysis.status IN ('PENDING', 'FETCHING')
                LEFT JOIN mois_sales_page_job_execution all_analysis
                  ON all_analysis.sales_page_id = sp.id
                 AND all_analysis.stage = 'ANALYSIS'
                WHERE sp.workspace_id = ?
                  AND sp.source = ?
                  AND COALESCE(sp.html_bytes, 0) > 0
                  AND COALESCE(sp.analysis_status, sp.current_status) NOT IN ('DONE', 'ANALYZED', 'ANULADO', 'FETCHING')
                GROUP BY sp.id, sp.url_canonical
                HAVING active_count = 0
                   AND done_count = 0
                   AND failed_count < 3
                ORDER BY sp.last_captured_at ASC, sp.updated_at ASC, sp.id ASC
                LIMIT 1
                """, (rs, rowNum) -> new CapturedPageAnalysisCandidate(
                rs.getLong("page_id"),
                rs.getString("url_canonical"),
                rs.getInt("next_attempt")
        ), workspaceId, normalizedSource);
        if (candidates.isEmpty()) {
            return null;
        }
        CapturedPageAnalysisCandidate candidate = candidates.get(0);
        jdbcTemplate.update("""
                INSERT INTO mois_sales_page_job_execution
                (sales_page_id, workspace_id, job_type, stage, status, attempt, input_url, request_payload_json, created_at, updated_at)
                VALUES (?, ?, 'PAGE_ANALYSIS', 'ANALYSIS', 'PENDING', ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """, candidate.pageId(), workspaceId, candidate.nextAttempt(), candidate.urlCanonical(), "AUTO_STAGE_2_CAPTURED_HTML");
        Long executionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long safeExecutionId = executionId == null ? 0L : executionId;
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                SET current_stage = 'ANALYSIS', current_status = 'PENDING', analysis_status = 'PENDING',
                    last_error_category = NULL, last_error_message = NULL, last_job_execution_id = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, safeExecutionId, candidate.pageId());
        return safeExecutionId;
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
            Timestamp capturedAtUtc = Timestamp.from(capturedAt);
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
     * Localiza a próxima referência bruta ainda elegível para captura operacional, pulando URLs já consolidadas.
     */
    private java.util.Optional<MoisSalesLibraryDtos.CollectedReferenceHtmlCaptureJob> findNextCollectedReferenceHtmlCandidate(String workspaceId, String source) {
        List<MoisSalesLibraryDtos.CollectedReferenceHtmlCaptureJob> candidates = jdbcTemplate.query(
                """
                        SELECT r.id AS collected_reference_id, r.job_id AS collection_job_id, r.reference_id, r.source,
                               COALESCE(NULLIF(r.product_name, ''), NULLIF(r.title, ''), r.reference_id) AS title,
                               candidate.effective_url AS url_original,
                               CASE candidate.url_source_priority
                                 WHEN 1 THEN 'SALES_PAGE_URL'
                                 WHEN 2 THEN 'PRODUCT_URL'
                                 ELSE 'URL'
                               END AS url_source
                        FROM mois_collected_reference r
                        JOIN (
                            SELECT MIN(id) AS collected_reference_id,
                                   effective_url,
                                   MIN(url_source_priority) AS url_source_priority,
                                   MIN(collected_at) AS first_collected_at
                            FROM (
                                SELECT id,
                                       collected_at,
                                       COALESCE(NULLIF(TRIM(sales_page_url), ''), NULLIF(TRIM(product_url), ''), NULLIF(TRIM(url), '')) AS effective_url,
                                       CASE
                                         WHEN sales_page_url IS NOT NULL AND TRIM(sales_page_url) <> '' THEN 1
                                         WHEN product_url IS NOT NULL AND TRIM(product_url) <> '' THEN 2
                                         ELSE 3
                                       END AS url_source_priority
                                FROM mois_collected_reference
                                WHERE workspace_id = ?
                                  AND source = ?
                                  AND COALESCE(NULLIF(TRIM(sales_page_url), ''), NULLIF(TRIM(product_url), ''), NULLIF(TRIM(url), '')) IS NOT NULL
                            ) raw_urls
                            GROUP BY effective_url
                        ) candidate ON candidate.collected_reference_id = r.id
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM mois_sales_page sp
                            JOIN mois_sales_page_job_execution e ON e.sales_page_id = sp.id
                            WHERE sp.collected_reference_id = r.id
                              AND e.stage = 'CAPTURE'
                              AND e.status IN ('FETCHING', 'CAPTURED')
                          )
                        ORDER BY candidate.first_collected_at ASC, r.id ASC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new MoisSalesLibraryDtos.CollectedReferenceHtmlCaptureJob(
                        0L,
                        rs.getLong("collected_reference_id"),
                        rs.getString("collection_job_id"),
                        rs.getString("reference_id"),
                        rs.getString("source"),
                        rs.getString("title"),
                        rs.getString("url_original"),
                        rs.getString("url_source")),
                workspaceId,
                source,
                COLLECTED_REFERENCE_HTML_CANDIDATE_SCAN_LIMIT);
        Set<String> operationalUrls = findOperationalCanonicalUrls(workspaceId);
        int skippedAlreadyConsolidated = 0;
        int skippedWithoutCanonical = 0;
        for (MoisSalesLibraryDtos.CollectedReferenceHtmlCaptureJob candidate : candidates) {
            String canonical = canonicalize(candidate.url());
            if (canonical == null || canonical.isBlank()) {
                skippedWithoutCanonical++;
                continue;
            }
            if (operationalUrls.contains(canonical)) {
                skippedAlreadyConsolidated++;
                continue;
            }
            log.info("MOIS sales-library referência coletada elegível localizada. modulo=MOIS, operacao=findNextCollectedReferenceHtmlCandidate, workspaceId={}, source={}, collectedReferenceId={}, urlSource={}, canonicalUrl={}, scanned={}, skippedAlreadyConsolidated={}, skippedWithoutCanonical={}",
                    workspaceId, source, candidate.collectedReferenceId(), candidate.urlSource(), canonical, candidates.size(), skippedAlreadyConsolidated,
                    skippedWithoutCanonical);
            return java.util.Optional.of(candidate);
        }
        log.info("MOIS sales-library não encontrou referência bruta faltante para consolidar. modulo=MOIS, operacao=findNextCollectedReferenceHtmlCandidate, workspaceId={}, source={}, scanned={}, skippedAlreadyConsolidated={}, skippedWithoutCanonical={}",
                workspaceId, source, candidates.size(), skippedAlreadyConsolidated, skippedWithoutCanonical);
        return java.util.Optional.empty();
    }

    /**
     * Carrega as URLs canônicas já consolidadas para evitar reprocessar duplicatas da origem bruta.
     */
    private Set<String> findOperationalCanonicalUrls(String workspaceId) {
        Set<String> urls = new HashSet<>(jdbcTemplate.query(
                "SELECT url_canonical FROM mois_sales_page WHERE workspace_id = ? AND url_canonical IS NOT NULL",
                (rs, rowNum) -> canonicalize(rs.getString("url_canonical")),
                workspaceId));
        urls.remove(null);
        return urls;
    }

    /**
     * Converte uma referência bruta em página consolidada e cria a execução de captura reservada.
     */
    private MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse claimCollectedReferenceCandidate(
            MoisSalesLibraryDtos.CollectedReferenceHtmlCaptureJob candidate,
            String claimedBy
    ) {
        String canonical = canonicalize(candidate.url());
        jdbcTemplate.update(
                """
                        INSERT INTO mois_sales_page
                        (workspace_id, source, source_job_id, source_reference_id, collected_reference_id, title, url_original, url_canonical,
                         current_stage, current_status, capture_status, ingest_count, first_seen_at, last_collected_at, created_at, updated_at)
                        SELECT r.workspace_id, r.source, r.job_id, r.reference_id, r.id,
                               COALESCE(NULLIF(r.product_name, ''), NULLIF(r.title, ''), r.reference_id),
                               ?, ?, 'CAPTURE', 'FETCHING', 'FETCHING', 1, UTC_TIMESTAMP(), r.collected_at, UTC_TIMESTAMP(), UTC_TIMESTAMP()
                        FROM mois_collected_reference r
                        WHERE r.id = ?
                        ON DUPLICATE KEY UPDATE
                            collected_reference_id = COALESCE(mois_sales_page.collected_reference_id, VALUES(collected_reference_id)),
                            source_job_id = COALESCE(mois_sales_page.source_job_id, VALUES(source_job_id)),
                            source_reference_id = COALESCE(mois_sales_page.source_reference_id, VALUES(source_reference_id)),
                            title = COALESCE(NULLIF(VALUES(title), ''), mois_sales_page.title),
                            current_stage = 'CAPTURE',
                            current_status = 'FETCHING',
                            capture_status = 'FETCHING',
                            last_error_category = NULL,
                            last_error_message = NULL,
                            updated_at = UTC_TIMESTAMP()
                        """,
                candidate.url(),
                canonical,
                candidate.collectedReferenceId());
        Long pageId = findSalesPageIdByCanonical(getWorkspaceForCollectedReference(candidate.collectedReferenceId()), canonical);
        if (pageId == null) {
            throw new IllegalStateException("Sales page not found after collected reference claim: " + candidate.collectedReferenceId());
        }
        jdbcTemplate.update(
                """
                        INSERT INTO mois_sales_page_job_execution
                        (sales_page_id, workspace_id, job_type, stage, status, attempt, claimed_by, input_url, started_at, created_at, updated_at)
                        SELECT id, workspace_id, 'COLLECTED_REFERENCE_HTML', 'CAPTURE', 'FETCHING', 1, ?, url_canonical, UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP()
                        FROM mois_sales_page
                        WHERE id = ?
                        """,
                claimedBy,
                pageId);
        Long executionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long captureId = executionId == null ? 0L : executionId;
        jdbcTemplate.update("UPDATE mois_sales_page SET last_job_execution_id = ?, updated_at = UTC_TIMESTAMP() WHERE id = ?", captureId, pageId);
        log.info("MOIS sales-library referência coletada reservada no modelo operacional novo. modulo=MOIS, operacao=claimCollectedReferenceHtml, pageId={}, executionId={}, collectedReferenceId={}, canonicalUrl={}, urlSource={}",
                pageId, captureId, candidate.collectedReferenceId(), canonical, candidate.urlSource());
        return new MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse(
                true,
                new MoisSalesLibraryDtos.CollectedReferenceHtmlCaptureJob(captureId, candidate.collectedReferenceId(), candidate.collectionJobId(),
                        candidate.referenceId(), candidate.source(), candidate.title(), candidate.url(), candidate.urlSource()));
    }

    /**
     * Localiza o workspace da referência bruta para buscar a página consolidada recém-criada.
     */
    private String getWorkspaceForCollectedReference(long collectedReferenceId) {
        return jdbcTemplate.query(
                "SELECT workspace_id FROM mois_collected_reference WHERE id = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("workspace_id"),
                collectedReferenceId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Collected reference not found: " + collectedReferenceId));
    }

    /**
     * Localiza uma execução de captura de referência que ainda pode receber conclusão ou falha.
     */
    private CaptureExecution findCollectedReferenceCaptureExecution(long executionId) {
        List<CaptureExecution> rows = jdbcTemplate.query(
                """
                        SELECT id, sales_page_id
                        FROM mois_sales_page_job_execution
                        WHERE id = ? AND stage = 'CAPTURE' AND status IN ('FETCHING', 'PENDING')
                        LIMIT 1
                        """,
                (rs, rowNum) -> new CaptureExecution(rs.getLong("id"), rs.getLong("sales_page_id")),
                executionId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Collected reference capture execution not found: " + executionId);
        }
        return rows.get(0);
    }

    /**
     * Atualiza o estado consolidado da página após a conclusão ou falha da captura de referência.
     */
    private void updateSalesPageAfterCollectedReferenceCapture(long pageId, long executionId, String status, Integer httpStatus, String contentType,
                                                              String finalUrl, String hash, long rawHtmlBytes, String errorCategory, String errorMessage, Instant fetchedAt) {
        jdbcTemplate.update(
                """
                        UPDATE mois_sales_page
                        SET current_stage = 'CAPTURE', current_status = ?, capture_status = ?, http_status = ?, content_type = ?, url_final = ?,
                            html_sha256 = COALESCE(?, html_sha256), html_bytes = ?, last_error_category = ?, last_error_message = ?,
                            last_job_execution_id = ?, last_captured_at = ?, updated_at = UTC_TIMESTAMP()
                        WHERE id = ?
                        """,
                status, status, httpStatus, truncate(contentType, 255), truncate(finalUrl, 1024), hash, rawHtmlBytes,
                truncate(errorCategory, 120), truncate(errorMessage, 1000), executionId, Timestamp.from(fetchedAt), pageId);
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
     * Converte uma linha ranqueada em oportunidade comercial objetiva para a biblioteca MOIS.
     */
    private MoisSalesLibraryDtos.MarketWarmupOpportunityRankingItem mapMarketWarmupOpportunityRankingItem(ResultSet rs, int rowNum) throws SQLException {
        MoisSalesLibraryDtos.MarketWarmupTemperature temperature = mapEnum(MoisSalesLibraryDtos.MarketWarmupTemperature.class, rs.getString("market_temperature"));
        MoisSalesLibraryDtos.MarketWarmupRecommendation recommendation = mapEnum(MoisSalesLibraryDtos.MarketWarmupRecommendation.class, rs.getString("recommendation"));
        String nextSuggestion = coalesceNotBlank(rs.getString("next_experiment_suggestion"), rs.getString("opportunity_recommendation"));
        return new MoisSalesLibraryDtos.MarketWarmupOpportunityRankingItem(
                rs.getLong("page_id"),
                rs.getString("title"),
                rs.getString("url_canonical"),
                rs.getString("source"),
                rs.getBigDecimal("page_score_total"),
                rs.getBigDecimal("warmup_score_total"),
                rs.getBigDecimal("combined_commercial_score"),
                temperature,
                mapEnum(MoisSalesLibraryDtos.MarketWarmupEcosystemType.class, rs.getString("ecosystem_type")),
                recommendation,
                rs.getString("saturation_risk"),
                toInstant(rs.getTimestamp("evidence_updated_at")),
                resolveSuggestedNextAction(temperature, recommendation, nextSuggestion),
                buildRankingEvidenceSummary(rs.getBigDecimal("page_score_total"), rs.getBigDecimal("warmup_score_total"), rs.getString("saturation_risk"), toInstant(rs.getTimestamp("evidence_updated_at")))
        );
    }

    /**
     * Define a próxima ação comercial a partir da recomendação de aquecimento e da sugestão persistida.
     */
    private String resolveSuggestedNextAction(
            MoisSalesLibraryDtos.MarketWarmupTemperature temperature,
            MoisSalesLibraryDtos.MarketWarmupRecommendation recommendation,
            String persistedSuggestion
    ) {
        if (recommendation == MoisSalesLibraryDtos.MarketWarmupRecommendation.SATURATED_REQUIRES_ANGLE
                || temperature == MoisSalesLibraryDtos.MarketWarmupTemperature.SATURATED) {
            return "Pesquisar ângulo diferenciado com OPRM/MDS antes de criar experimento.";
        }
        if (recommendation == MoisSalesLibraryDtos.MarketWarmupRecommendation.PRIORITIZE) {
            return persistedSuggestion == null ? "Criar próximo experimento comercial para este mercado." : persistedSuggestion;
        }
        if (recommendation == MoisSalesLibraryDtos.MarketWarmupRecommendation.OBSERVE) {
            return persistedSuggestion == null ? "Refinar promessa e validar mais evidências antes do experimento." : persistedSuggestion;
        }
        if (recommendation == MoisSalesLibraryDtos.MarketWarmupRecommendation.RESEARCH_MORE) {
            return "Solicitar próxima pesquisa OPRM/MDS para aprofundar dor, rotina e mecanismo.";
        }
        return "Não priorizar agora; manter apenas como referência de mercado.";
    }

    /**
     * Monta explicação curta e rastreável do score combinado exibido no ranking.
     */
    private String buildRankingEvidenceSummary(BigDecimal pageScore, BigDecimal warmupScore, String saturationRisk, Instant evidenceUpdatedAt) {
        String saturation = saturationRisk == null || saturationRisk.isBlank() ? "sem penalidade explícita de saturação" : "com risco de saturação descontado";
        String evidenceDate = evidenceUpdatedAt == null ? "sem data de evidência" : "evidência até " + evidenceUpdatedAt;
        return "Combina score da página " + pageScore + ", aquecimento " + warmupScore + ", " + saturation + " e " + evidenceDate + ".";
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
                rs.getString("product_name"),
                rs.getString("producer_name"),
                rs.getString("hotmart_price"),
                rs.getBigDecimal("hotmart_temperature"),
                rs.getString("hotmart_producer"),
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
                rs.getString("model_name"),
                (Integer) rs.getObject("input_tokens"),
                (Integer) rs.getObject("output_tokens"),
                rs.getBigDecimal("model_cost_usd"),
                rs.getString("last_error_category"),
                rs.getString("last_error_message"),
                rs.getObject("last_job_execution_id", Long.class),
                toInstant(rs.getTimestamp("last_captured_at")),
                toInstant(rs.getTimestamp("last_analyzed_at")),
                toInstant(rs.getTimestamp("updated_at")),
                rs.getBigDecimal("market_warmup_score_total"),
                mapEnum(MoisSalesLibraryDtos.MarketWarmupTemperature.class, rs.getString("market_warmup_temperature")),
                mapEnum(MoisSalesLibraryDtos.MarketWarmupEcosystemType.class, rs.getString("market_warmup_ecosystem_type")),
                mapEnum(MoisSalesLibraryDtos.MarketWarmupRecommendation.class, rs.getString("market_warmup_recommendation")),
                mapEnum(MoisSalesLibraryDtos.MarketWarmupJobStatus.class, rs.getString("market_warmup_status")),
                toInstant(rs.getTimestamp("market_warmup_updated_at"))
        );
    }


    /**
     * Converte texto persistido em enum sem quebrar a UI quando o campo ainda não existe.
     */
    private <E extends Enum<E>> E mapEnum(Class<E> enumType, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Enum.valueOf(enumType, value);
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
     * Normaliza a origem coletada para manter agrupamentos estáveis na tela.
     */
    private String normalizeSource(String source) {
        return source == null || source.isBlank() ? "UNKNOWN" : source.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Converte timestamp JDBC em Instant preservando nulos.
     */
    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    /**
     * Lê identificadores opcionais do ResultSet preservando nulo quando a coluna SQL está vazia.
     */
    private Long nullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private record IngestCounters(int persisted, int inserted, int updated, int jobsCreated, int skippedWithoutUrl) {
    }

    private record CaptureExecution(long executionId, long salesPageId) {
    }

    private record CapturedPageAnalysisCandidate(long pageId, String urlCanonical, int nextAttempt) {
    }

    private record CollectedReferenceUrlCandidate(String source, String urlSource, String canonicalUrl) {
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
