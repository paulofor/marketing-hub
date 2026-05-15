package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisSalesLibraryDtos;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
}
