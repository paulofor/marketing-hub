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
    private static final String ANALYSIS_STATUS_CANCELED = "ANULADO";

    private final JdbcTemplate jdbcTemplate;
    private final MoisSalesPageDualWriteGateway dualWriteGateway;

    /**
     * Reserva o próximo job pendente da biblioteca para processamento pelo worker.
     */
    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryClaimResponse claimJob(MoisSalesLibraryDtos.SalesLibraryClaimRequest request) {
        List<MoisSalesLibraryDtos.SalesLibraryClaimedJob> rows = jdbcTemplate.query("""
                SELECT j.id AS job_id, i.id AS page_id, i.url_canonical, i.title
                FROM mois_sales_library_processing_job j
                JOIN mois_sales_library_url_ingest i ON i.id = j.url_ingest_id
                WHERE j.status = 'PENDING' AND i.workspace_id = ? AND i.source = ?
                ORDER BY j.created_at ASC
                LIMIT 1
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryClaimedJob(
                rs.getLong("job_id"), rs.getLong("page_id"), rs.getString("url_canonical"), rs.getString("title")),
                request.workspaceId(), request.source().trim().toUpperCase(Locale.ROOT));
        if (rows.isEmpty()) {
            return new MoisSalesLibraryDtos.SalesLibraryClaimResponse(false, null);
        }
        MoisSalesLibraryDtos.SalesLibraryClaimedJob job = rows.get(0);
        jdbcTemplate.update("UPDATE mois_sales_library_processing_job SET status='FETCHING', started_at=UTC_TIMESTAMP(), updated_at=UTC_TIMESTAMP() WHERE id=? AND status='PENDING'", job.jobId());
        dualWriteGateway.syncProcessingJob(job.jobId());
        return new MoisSalesLibraryDtos.SalesLibraryClaimResponse(true, job);
    }

    /**
     * Registra a análise concluída de uma página e finaliza o job correspondente.
     */
    @Transactional
    public void completeJob(long jobId, MoisSalesLibraryDtos.SalesLibraryCompleteRequest request) {
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
     * Marca um job como falho preservando a categoria e a mensagem de erro operacional.
     */
    @Transactional
    public void failJob(long jobId, MoisSalesLibraryDtos.SalesLibraryFailRequest request) {
        jdbcTemplate.update("UPDATE mois_sales_library_processing_job SET status='FAILED', error_category=?, error_message=?, finished_at=UTC_TIMESTAMP(), updated_at=UTC_TIMESTAMP() WHERE id=?", request.errorCategory(), request.errorMessage(), jobId);
        dualWriteGateway.syncProcessingJob(jobId);
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
     * Lista páginas canônicas da biblioteca junto com a análise mais recente.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageListResponse listPages(String workspaceId, int page, int pageSize) {
        int normalizedPage = Math.max(1, page); int normalizedPageSize = Math.max(1, Math.min(pageSize, 100)); int offset = (normalizedPage - 1) * normalizedPageSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_url_ingest WHERE workspace_id = ?", Long.class, workspaceId);
        List<MoisSalesLibraryDtos.SalesLibraryPageResponse> items = jdbcTemplate.query("""
                SELECT i.id, i.workspace_id, i.source, i.url_canonical, i.title, i.updated_at,
                       a.status AS analysis_status, a.score_total, a.analyzed_at
                FROM mois_sales_library_url_ingest i
                LEFT JOIN mois_sales_library_page_analysis a ON a.id = (
                    SELECT a2.id FROM mois_sales_library_page_analysis a2
                    WHERE a2.url_ingest_id = i.id ORDER BY a2.updated_at DESC LIMIT 1
                )
                WHERE i.workspace_id = ?
                ORDER BY i.updated_at DESC LIMIT ? OFFSET ?
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryPageResponse(
                rs.getLong("id"), rs.getString("workspace_id"), rs.getString("source"), rs.getString("url_canonical"),
                rs.getString("title"), rs.getString("analysis_status"), rs.getBigDecimal("score_total"),
                toInstant(rs.getTimestamp("analyzed_at")), toInstant(rs.getTimestamp("updated_at"))), workspaceId, normalizedPageSize, offset);
        return new MoisSalesLibraryDtos.SalesLibraryPageListResponse(normalizedPage, normalizedPageSize, total == null ? 0 : total, items);
    }

    /**
     * Busca uma página canônica da biblioteca pelo identificador interno.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageResponse getPage(long pageId) {
        List<MoisSalesLibraryDtos.SalesLibraryPageResponse> rows = jdbcTemplate.query("""
                SELECT i.id, i.workspace_id, i.source, i.url_canonical, i.title, i.updated_at,
                       a.status AS analysis_status, a.score_total, a.analyzed_at
                FROM mois_sales_library_url_ingest i
                LEFT JOIN mois_sales_library_page_analysis a ON a.id = (
                    SELECT a2.id FROM mois_sales_library_page_analysis a2
                    WHERE a2.url_ingest_id = i.id ORDER BY a2.updated_at DESC LIMIT 1
                )
                WHERE i.id = ? LIMIT 1
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryPageResponse(
                rs.getLong("id"), rs.getString("workspace_id"), rs.getString("source"), rs.getString("url_canonical"),
                rs.getString("title"), rs.getString("analysis_status"), rs.getBigDecimal("score_total"),
                toInstant(rs.getTimestamp("analyzed_at")), toInstant(rs.getTimestamp("updated_at"))), pageId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Page not found: " + pageId);
        return rows.get(0);
    }

    /**
     * Busca a análise mais recente de uma página da biblioteca.
     */
    public MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse getPageAnalysis(long pageId) {
        List<MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse> rows = jdbcTemplate.query("""
                SELECT a.id, a.url_ingest_id, a.job_id, a.status, a.score_total, a.parser_version,
                       a.prompt_version, a.model_name, a.sections_json, a.copy_json, a.visual_json,
                       a.image_json, a.analysis_notes, a.request_payload_json, a.analyzed_at, a.updated_at
                FROM mois_sales_library_page_analysis a
                WHERE a.url_ingest_id = ? ORDER BY a.updated_at DESC LIMIT 1
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse(
                rs.getLong("id"), rs.getLong("url_ingest_id"), rs.getObject("job_id", Long.class),
                rs.getString("status"), rs.getBigDecimal("score_total"), rs.getString("parser_version"),
                rs.getString("prompt_version"), rs.getString("model_name"), rs.getString("sections_json"),
                rs.getString("copy_json"), rs.getString("visual_json"), rs.getString("image_json"),
                rs.getString("analysis_notes"), rs.getString("request_payload_json"), toInstant(rs.getTimestamp("analyzed_at")), toInstant(rs.getTimestamp("updated_at"))), pageId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Analysis not found for page: " + pageId);
        return rows.get(0);
    }

    /**
     * Atualiza manualmente o status da análise de uma página da biblioteca.
     */
    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryStatusUpdateResponse updatePageStatus(
            long pageId,
            MoisSalesLibraryDtos.SalesLibraryStatusUpdateRequest request
    ) {
        Long exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_url_ingest WHERE id = ?", Long.class, pageId);
        if (exists == null || exists == 0) throw new IllegalArgumentException("Page not found: " + pageId);

        String normalizedStatus = request.status().trim().toUpperCase(Locale.ROOT);
        if (!JOB_STATUS_PENDING.equals(normalizedStatus) && !ANALYSIS_STATUS_CANCELED.equals(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported status: " + request.status());
        }

        Long jobId = null;
        String notes = request.reason() == null || request.reason().isBlank() ? "Status atualizado manualmente via API" : request.reason().trim();
        if (JOB_STATUS_PENDING.equals(normalizedStatus)) {
            jobId = createPendingJob(pageId);
        }

        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_page_analysis
                (url_ingest_id, job_id, status, analysis_notes, created_at, updated_at)
                VALUES (?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """, pageId, jobId, normalizedStatus, notes);
        if (jobId != null) {
            dualWriteGateway.syncProcessingJob(jobId);
        }
        dualWriteGateway.syncLatestAnalysis(pageId);

        return new MoisSalesLibraryDtos.SalesLibraryStatusUpdateResponse(pageId, jobId, normalizedStatus, notes, Instant.now());
    }

    /**
     * Cria um novo job pendente para reanalisar uma página existente.
     */
    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryReanalyzeResponse reanalyzePage(long pageId) {
        Long exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mois_sales_library_url_ingest WHERE id = ?", Long.class, pageId);
        if (exists == null || exists == 0) throw new IllegalArgumentException("Page not found: " + pageId);
        long jobId = createPendingJob(pageId);
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_page_analysis
                (url_ingest_id, job_id, status, analysis_notes, created_at, updated_at)
                VALUES (?, ?, 'PENDING', 'Reanálise solicitada via API', UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """, pageId, jobId);
        dualWriteGateway.syncProcessingJob(jobId);
        dualWriteGateway.syncLatestAnalysis(pageId);
        return new MoisSalesLibraryDtos.SalesLibraryReanalyzeResponse(pageId, jobId, JOB_STATUS_PENDING, Instant.now());
    }


    /**
     * Executa a gravação comum de itens de URL e consolida contadores de inserção, atualização e jobs.
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
            int ingestUpsertResult = jdbcTemplate.update(
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
                    workspaceId,
                    normalizedSource,
                    item.url(),
                    canonical,
                    item.title(),
                    capturedAtUtc,
                    capturedAtUtc
            );

            if (ingestUpsertResult == 1) {
                inserted++;
                Long urlIngestId = jdbcTemplate.queryForObject(
                        """
                                SELECT id
                                FROM mois_sales_library_url_ingest
                                WHERE workspace_id = ? AND url_canonical = ?
                                LIMIT 1
                                """,
                        Long.class,
                        workspaceId,
                        canonical
                );
                if (urlIngestId != null) {
                    long jobId = createPendingJob(urlIngestId);
                    dualWriteGateway.syncUrlIngest(urlIngestId, jobId);
                    jobsCreated++;
                }
            } else {
                updated++;
                Long urlIngestId = findUrlIngestIdByCanonical(workspaceId, canonical);
                if (urlIngestId != null) {
                    dualWriteGateway.syncUrlIngest(urlIngestId, null);
                }
            }
            persisted++;
        }
        return new IngestCounters(persisted, inserted, updated, jobsCreated, skippedWithoutUrl);
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
