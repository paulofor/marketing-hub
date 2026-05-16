package com.marketinghub.mois.biblioteca.service;

import com.marketinghub.mois.biblioteca.dto.MoisSalesLibraryDtos;
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
                jdbcTemplate.update(
                        """
                                INSERT INTO mois_sales_library_processing_job
                                (url_ingest_id, status, attempts, created_at, updated_at)
                                VALUES (?, ?, 0, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                                """,
                        urlIngestId,
                        JOB_STATUS_PENDING
                );
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

    public MoisSalesLibraryDtos.SalesLibraryJobResponse getJob(long jobId) {
        List<MoisSalesLibraryDtos.SalesLibraryJobResponse> rows = jdbcTemplate.query(
                """
                        SELECT id, url_ingest_id, status, attempts, error_category, error_message,
                               next_retry_at, created_at, updated_at, started_at, finished_at
                        FROM mois_sales_library_processing_job
                        WHERE id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryJobResponse(
                        rs.getLong("id"),
                        rs.getLong("url_ingest_id"),
                        rs.getString("status"),
                        rs.getInt("attempts"),
                        rs.getString("error_category"),
                        rs.getString("error_message"),
                        toInstant(rs.getTimestamp("next_retry_at")),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at")),
                        toInstant(rs.getTimestamp("started_at")),
                        toInstant(rs.getTimestamp("finished_at"))
                ),
                jobId
        );
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

        Long total = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM mois_sales_library_processing_job j
                        JOIN mois_sales_library_url_ingest i ON i.id = j.url_ingest_id
                        WHERE i.workspace_id = ?
                        """ + statusClause,
                Long.class,
                normalizedStatus == null ? new Object[]{workspaceId} : new Object[]{workspaceId, normalizedStatus}
        );

        List<MoisSalesLibraryDtos.SalesLibraryJobResponse> items = jdbcTemplate.query(
                """
                        SELECT j.id, j.url_ingest_id, j.status, j.attempts, j.error_category, j.error_message,
                               j.next_retry_at, j.created_at, j.updated_at, j.started_at, j.finished_at
                        FROM mois_sales_library_processing_job j
                        JOIN mois_sales_library_url_ingest i ON i.id = j.url_ingest_id
                        WHERE i.workspace_id = ?
                        """ + statusClause + " ORDER BY j.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryJobResponse(
                        rs.getLong("id"),
                        rs.getLong("url_ingest_id"),
                        rs.getString("status"),
                        rs.getInt("attempts"),
                        rs.getString("error_category"),
                        rs.getString("error_message"),
                        toInstant(rs.getTimestamp("next_retry_at")),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at")),
                        toInstant(rs.getTimestamp("started_at")),
                        toInstant(rs.getTimestamp("finished_at"))
                ),
                normalizedStatus == null
                        ? new Object[]{workspaceId, normalizedPageSize, offset}
                        : new Object[]{workspaceId, normalizedStatus, normalizedPageSize, offset}
        );

        return new MoisSalesLibraryDtos.SalesLibraryJobPageResponse(
                normalizedPage,
                normalizedPageSize,
                total == null ? 0 : total,
                items
        );
    }

    public MoisSalesLibraryDtos.SalesLibraryEntryPageResponse listEntries(String workspaceId, int page, int pageSize) {
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (normalizedPage - 1) * normalizedPageSize;

        Long total = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM mois_sales_library_url_ingest
                        WHERE workspace_id = ?
                        """,
                Long.class,
                workspaceId
        );

        List<MoisSalesLibraryDtos.SalesLibraryEntryResponse> items = jdbcTemplate.query(
                """
                        SELECT id, workspace_id, source, url_original, url_canonical, title, ingest_count,
                               first_captured_at, last_captured_at, updated_at
                        FROM mois_sales_library_url_ingest
                        WHERE workspace_id = ?
                        ORDER BY updated_at DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryEntryResponse(
                        rs.getLong("id"),
                        rs.getString("workspace_id"),
                        rs.getString("source"),
                        rs.getString("url_original"),
                        rs.getString("url_canonical"),
                        rs.getString("title"),
                        rs.getInt("ingest_count"),
                        toInstant(rs.getTimestamp("first_captured_at")),
                        toInstant(rs.getTimestamp("last_captured_at")),
                        toInstant(rs.getTimestamp("updated_at"))
                ),
                workspaceId,
                normalizedPageSize,
                offset
        );

        return new MoisSalesLibraryDtos.SalesLibraryEntryPageResponse(
                normalizedPage,
                normalizedPageSize,
                total == null ? 0 : total,
                items
        );
    }

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
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            String path = (uri.getPath() == null || uri.getPath().isBlank()) ? "/" : uri.getPath();
            return scheme + "://" + normalizedHost + path;
        } catch (Exception ignored) {
            return rawUrl.trim();
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
