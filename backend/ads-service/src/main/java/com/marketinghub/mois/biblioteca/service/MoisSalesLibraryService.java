package com.marketinghub.mois.biblioteca.service;

import com.marketinghub.mois.biblioteca.dto.MoisSalesLibraryDtos;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoisSalesLibraryService {

    private static final String JOB_STATUS_PENDING = "PENDING";

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public MoisSalesLibraryDtos.SalesLibraryIngestResponse ingestUrls(MoisSalesLibraryDtos.SalesLibraryIngestRequest request) {
        int persisted = 0;
        String normalizedSource = request.source().trim().toUpperCase(Locale.ROOT);
        for (MoisSalesLibraryDtos.SalesLibraryUrlItem item : request.urls()) {
            String canonical = canonicalize(item.url());
            if (canonical == null || canonical.isBlank()) {
                continue;
            }
            Instant capturedAt = item.capturedAt() == null ? Instant.now() : item.capturedAt();
            LocalDateTime capturedAtUtc = LocalDateTime.ofInstant(capturedAt, ZoneOffset.UTC);
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
                    request.workspaceId(),
                    normalizedSource,
                    item.url(),
                    canonical,
                    item.title(),
                    capturedAtUtc,
                    capturedAtUtc
            );

            Long urlIngestId = jdbcTemplate.queryForObject(
                    """
                            SELECT id
                            FROM mois_sales_library_url_ingest
                            WHERE url_canonical = ?
                            LIMIT 1
                            """,
                    Long.class,
                    canonical
            );
            if (urlIngestId != null) {
                createPendingJob(urlIngestId);
            }
            persisted++;
        }

        return new MoisSalesLibraryDtos.SalesLibraryIngestResponse(
                request.workspaceId(),
                normalizedSource,
                request.urls().size(),
                persisted
        );
    }

    public MoisSalesLibraryDtos.SalesLibraryJobResponse getJob(long jobId) { /* unchanged simplified */
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

    public MoisSalesLibraryDtos.SalesLibraryEntryPageResponse listEntries(String workspaceId, int page, int pageSize) { /* keep */
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

    public MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse getPageAnalysis(long pageId) {
        List<MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse> rows = jdbcTemplate.query("""
                SELECT a.id, a.url_ingest_id, a.job_id, a.status, a.score_total, a.parser_version,
                       a.prompt_version, a.model_name, a.sections_json, a.copy_json, a.visual_json,
                       a.image_json, a.analysis_notes, a.analyzed_at, a.updated_at
                FROM mois_sales_library_page_analysis a
                WHERE a.url_ingest_id = ? ORDER BY a.updated_at DESC LIMIT 1
                """, (rs, rn) -> new MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse(
                rs.getLong("id"), rs.getLong("url_ingest_id"), rs.getObject("job_id", Long.class),
                rs.getString("status"), rs.getBigDecimal("score_total"), rs.getString("parser_version"),
                rs.getString("prompt_version"), rs.getString("model_name"), rs.getString("sections_json"),
                rs.getString("copy_json"), rs.getString("visual_json"), rs.getString("image_json"),
                rs.getString("analysis_notes"), toInstant(rs.getTimestamp("analyzed_at")), toInstant(rs.getTimestamp("updated_at"))), pageId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Analysis not found for page: " + pageId);
        return rows.get(0);
    }

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
        return new MoisSalesLibraryDtos.SalesLibraryReanalyzeResponse(pageId, jobId, JOB_STATUS_PENDING, Instant.now());
    }

    private long createPendingJob(long urlIngestId) {
        jdbcTemplate.update("INSERT INTO mois_sales_library_processing_job (url_ingest_id, status, attempts, created_at, updated_at) VALUES (?, ?, 0, UTC_TIMESTAMP(), UTC_TIMESTAMP())", urlIngestId, JOB_STATUS_PENDING);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id == null ? 0L : id;
    }

    private String canonicalize(String rawUrl) { if (rawUrl == null || rawUrl.isBlank()) return null; try { URI uri = URI.create(rawUrl.trim()); String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT); String host = uri.getHost(); if (host == null || host.isBlank()) return rawUrl.trim(); String path = (uri.getPath() == null || uri.getPath().isBlank()) ? "/" : uri.getPath(); return scheme + "://" + host.toLowerCase(Locale.ROOT) + path; } catch (Exception ignored) { return rawUrl.trim(); } }
    private Instant toInstant(Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant(); }
}
