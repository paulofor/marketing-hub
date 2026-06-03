package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Agrupa contratos HTTP da Biblioteca de Páginas de Vendas do MOIS.
 */
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


    /**
     * Impede instanciação do agrupador de contratos.
     */
    private MoisSalesLibraryDtos() {
    }


    public record CollectedReferenceHtmlClaimRequest(
            @NotBlank String workspaceId,
            @NotBlank String source
    ) {
    }

    public record CollectedReferenceHtmlCaptureJob(
            long captureId,
            long collectedReferenceId,
            String collectionJobId,
            String referenceId,
            String source,
            String title,
            String url,
            String urlSource
    ) {
    }

    public record CollectedReferenceHtmlClaimResponse(
            boolean claimed,
            CollectedReferenceHtmlCaptureJob job
    ) {
    }

    public record CollectedReferenceHtmlCompleteRequest(
            @NotBlank String rawHtml,
            String finalUrl,
            Integer httpStatus,
            String contentType,
            Instant fetchedAt
    ) {
    }

    public record CollectedReferenceHtmlFailRequest(
            String errorCategory,
            String errorMessage
    ) {
    }

    public record CollectedReferenceHtmlPersistResponse(
            long captureId,
            String status
    ) {
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

    public record SalesLibraryHotmartCollectedIngestRequest(
            @NotBlank String workspaceId,
            String jobId,
            Integer limit
    ) {
    }

    public record SalesLibraryHotmartCollectedIngestResponse(
            String workspaceId,
            String jobId,
            int collectedReferencesRead,
            int eligibleUrls,
            int insertedUrls,
            int updatedUrls,
            int jobsCreated,
            int skippedWithoutUrl
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
            String redirectDestinationUrl,
            String redirectRootUrl,
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
            String redirectDestinationUrl,
            String redirectRootUrl,
            long rawHtmlBytes,
            long screenshotBytes,
            Instant capturedAt,
            Instant updatedAt
    ) {
    }

    public record HtmlCaptureClaimRequest(
            @NotBlank String workspaceId,
            Integer limit,
            Boolean force
    ) {
    }

    public record HtmlCaptureJobResponse(
            long snapshotId,
            long pageId,
            String urlCanonical,
            String title
    ) {
    }

    public record HtmlCaptureClaimResponse(
            boolean claimed,
            HtmlCaptureJobResponse job
    ) {
    }

    public record HtmlCaptureCompleteRequest(
            @NotBlank String rawHtml,
            String finalUrl,
            String redirectDestinationUrl,
            String redirectRootUrl,
            Integer httpStatus,
            String contentType,
            String sha256,
            Long sizeBytes,
            Instant capturedAt
    ) {
    }

    public record HtmlCaptureFailRequest(
            String errorCategory,
            String errorMessage,
            String redirectDestinationUrl,
            String redirectRootUrl,
            Integer httpStatus
    ) {
    }

    public record HtmlCapturePersistResponse(
            long snapshotId,
            String status
    ) {
    }

}
