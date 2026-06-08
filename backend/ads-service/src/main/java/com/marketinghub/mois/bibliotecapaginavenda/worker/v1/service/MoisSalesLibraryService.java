package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
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
            throw new IllegalArgumentException("Operational analysis execution not found: " + jobId);
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
                ORDER BY last_analyzed_at DESC, updated_at DESC, id DESC LIMIT ? OFFSET ?
                """, this::mapSalesPageResponse, workspaceId, normalizedPageSize, offset);
        return new MoisSalesLibraryDtos.SalesLibraryPageListResponse(normalizedPage, normalizedPageSize, total == null ? 0 : total, items);
    }

    /**
     * Calcula os contadores globais da biblioteca usando html_bytes > 0 como critério canônico de página capturada.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageSummaryResponse summarizePages(String workspaceId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total,
                       SUM(current_status = 'PENDING') AS pending,
                       SUM(current_status IN ('FETCHING', 'CAPTURING')) AS capturing,
                       SUM(COALESCE(html_bytes, 0) > 0) AS captured,
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

    private record IngestCounters(int persisted, int inserted, int updated, int jobsCreated, int skippedWithoutUrl) {
    }

    private record CaptureExecution(long executionId, long salesPageId) {
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
