package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class MoisSalesLibraryDtos {


    public record SalesLibraryClaimRequest(
            @NotBlank String workspaceId,
            @NotBlank String source
    ) {
    }

    public record SalesLibraryClaimedJob(
            long jobId,
            long pageId,
            String urlCanonical,
            String title
    ) {
    }

    public record SalesLibraryClaimResponse(
            boolean claimed,
            SalesLibraryClaimedJob job
    ) {
    }

    public record SalesLibraryCompleteRequest(
            BigDecimal scoreTotal,
            String sectionsJson,
            String copyJson,
            String visualJson,
            String imageJson,
            String analysisNotes,
            String requestPayloadJson,
            String parserVersion,
            String promptVersion,
            String modelName,
            Instant analyzedAt
    ) {
    }

    public record SalesLibraryFailRequest(
            String errorCategory,
            String errorMessage
    ) {
    }


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
            String requestPayloadJson,
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


    public record SalesLibraryStatusUpdateRequest(
            @NotBlank String status,
            String reason
    ) {
    }

    public record SalesLibraryStatusUpdateResponse(
            long pageId,
            Long jobId,
            String status,
            String reason,
            Instant createdAt
    ) {
    }

    public record SalesLibrarySnapshotCaptureRequest(
            @NotBlank String workspaceId,
            Integer limit,
            Boolean force
    ) {
    }

    public record SalesLibrarySnapshotCaptureItem(
            long pageId,
            Long snapshotId,
            String urlCanonical,
            String status,
            String snapshotHash,
            Integer httpStatus,
            long rawHtmlBytes,
            long screenshotBytes,
            String errorMessage
    ) {
    }

    public record SalesLibrarySnapshotCaptureResponse(
            String workspaceId,
            int requestedLimit,
            boolean force,
            int processed,
            int captured,
            int failed,
            List<SalesLibrarySnapshotCaptureItem> items,
            Instant capturedAt
    ) {
    }

    public record SalesLibraryPageSnapshotResponse(
            long snapshotId,
            long pageId,
            String snapshotHash,
            String status,
            Integer httpStatus,
            String contentType,
            long rawHtmlBytes,
            long screenshotBytes,
            Instant capturedAt,
            Instant updatedAt
    ) {
    }

}
