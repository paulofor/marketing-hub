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

@Service
@RequiredArgsConstructor
public class MoisSalesLibraryService {

    private final JdbcTemplate jdbcTemplate;

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
            persisted++;
        }

        return new MoisSalesLibraryDtos.SalesLibraryIngestResponse(
                request.workspaceId(),
                normalizedSource,
                request.urls().size(),
                persisted
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

