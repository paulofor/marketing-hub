package com.marketinghub.mois.biblioteca.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class MoisSalesLibraryDtos {

    private MoisSalesLibraryDtos() {
    }

    public record SalesLibraryIngestRequest(
            @NotBlank String workspaceId,
            @NotBlank String source,
            @NotEmpty List<@Valid SalesLibraryUrlItem> urls
    ) {
    }

    public record SalesLibraryUrlItem(
            @NotBlank String url,
            String title,
            Instant capturedAt
    ) {
    }

    public record SalesLibraryIngestResponse(
            String workspaceId,
            String source,
            int received,
            int persisted
    ) {
    }

    public record SalesLibraryJobResponse(
            long id,
            long urlIngestId,
            String status,
            int attempts,
            String errorCategory,
            String errorMessage,
            Instant nextRetryAt,
            Instant createdAt,
            Instant updatedAt,
            Instant startedAt,
            Instant finishedAt
    ) {
    }

    public record SalesLibraryJobPageResponse(
            int page,
            int pageSize,
            long total,
            List<SalesLibraryJobResponse> items
    ) {
    }

    public record SalesLibraryEntryResponse(
            long id,
            String workspaceId,
            String source,
            String urlOriginal,
            String urlCanonical,
            String title,
            int ingestCount,
            Instant firstCapturedAt,
            Instant lastCapturedAt,
            Instant updatedAt
    ) {
    }

    public record SalesLibraryEntryPageResponse(
            int page,
            int pageSize,
            long total,
            List<SalesLibraryEntryResponse> items
    ) {
    }

    public record SalesLibraryPageResponse(
            long pageId,
            String workspaceId,
            String source,
            String urlCanonical,
            String title,
            String analysisStatus,
            BigDecimal scoreTotal,
            Instant analyzedAt,
            Instant updatedAt
    ) {
    }

    public record SalesLibraryPageListResponse(
            int page,
            int pageSize,
            long total,
            List<SalesLibraryPageResponse> items
    ) {
    }

    public record SalesLibraryPageAnalysisResponse(
            long analysisId,
            long pageId,
            Long jobId,
            String status,
            BigDecimal scoreTotal,
            String parserVersion,
            String promptVersion,
            String modelName,
            String sectionsJson,
            String copyJson,
            String visualJson,
            String imageJson,
            String analysisNotes,
            Instant analyzedAt,
            Instant updatedAt
    ) {
    }

    public record SalesLibraryReanalyzeResponse(
            long pageId,
            long jobId,
            String status,
            Instant createdAt
    ) {
    }
}
