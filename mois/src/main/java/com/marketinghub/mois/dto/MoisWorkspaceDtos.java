package com.marketinghub.mois.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.List;

public final class MoisWorkspaceDtos {

    private MoisWorkspaceDtos() {
    }

    public record WorkspaceDashboardResponse(
            String workspaceId,
            WorkspaceKpisResponse kpis,
            String currentStage,
            List<RecentAnalysisResponse> recentAnalyses
    ) {
    }

    public record WorkspaceKpisResponse(int collections, int extractions, int applications, int tests) {
    }

    public record RecentAnalysisResponse(
            String analysisId,
            String niche,
            String status,
            Instant updatedAt
    ) {
    }

    public record CreateReferenceRequest(
            @NotBlank String workspaceId,
            @NotBlank String niche,
            @NotBlank
            @Pattern(regexp = "https?://.+", message = "sourceUrl must start with http:// or https://")
            String sourceUrl,
            @NotBlank String assetType,
            @NotBlank String primaryPromise,
            @NotBlank String awarenessStage,
            @Size(max = 120) String priceRange,
            @Size(max = 80) String formatType,
            @Size(max = 1000) String notes
    ) {
    }

    public record ReferenceResponse(
            String referenceId,
            String workspaceId,
            String niche,
            String sourceUrl,
            String assetType,
            String primaryPromise,
            String awarenessStage,
            String priceRange,
            String formatType,
            String notes,
            Instant createdAt
    ) {
    }

    public record ReferenceListResponse(List<ReferenceResponse> items) {
    }

    public record UpsertExtractionDraftRequest(
            String pain,
            String result,
            String mechanism,
            String proof,
            String offer,
            List<String> evidenceItems
    ) {
    }

    public record ExtractionDraftResponse(
            String extractionId,
            String referenceId,
            String status,
            Instant updatedAt
    ) {
    }

    public record LibraryBlockResponse(
            String blockId,
            String workspaceId,
            String type,
            String summary,
            List<String> tags,
            double score,
            String origin,
            boolean favorite,
            Instant updatedAt
    ) {
    }

    public record LibraryBlockListResponse(List<LibraryBlockResponse> items) {
    }

    public record LibraryBlockActionResponse(
            String blockId,
            String action,
            String status,
            Instant updatedAt
    ) {
    }

    public record CreateComparisonRequest(
            @NotBlank String workspaceId,
            @NotBlank String referenceBaseId,
            @NotBlank String currentOfferId
    ) {
    }

    public record ComparisonDimensionResponse(
            String dimension,
            String market,
            String current,
            String highlight
    ) {
    }

    public record ComparisonScorecardResponse(
            String metric,
            int value,
            String explanation
    ) {
    }

    public record ComparisonImprovementResponse(
            String improvementId,
            String priority,
            String description
    ) {
    }

    public record ComparisonResponse(
            String comparisonId,
            String workspaceId,
            List<ComparisonDimensionResponse> dimensions,
            List<ComparisonScorecardResponse> scorecards,
            List<ComparisonImprovementResponse> improvements
    ) {
    }

    public record BuildOfferRequest(
            @NotBlank String workspaceId,
            @NotBlank String currentOfferId,
            List<String> selectedBlockIds,
            @NotBlank String currentVersion
    ) {
    }

    public record BuildOfferResponse(
            String offerId,
            String workspaceId,
            String status,
            String proposedVersion,
            Map<String, Boolean> checklist,
            Instant updatedAt
    ) {
    }

    public record CreateCollectionJobRequest(
            @NotBlank String workspaceId,
            @NotBlank String niche,
            @Size(max = 160) String marketTheme,
            @NotEmpty List<@NotBlank String> sources,
            @NotBlank
            @Pattern(regexp = "LAST_7_DAYS|LAST_30_DAYS", message = "timeWindow must be LAST_7_DAYS or LAST_30_DAYS")
            String timeWindow,
            @Min(1) @Max(200) Integer limitPerSource,
            @Size(max = 16) String locale,
            @Size(max = 8) String country,
            @Min(0) @Max(100) Integer minSuccessScore
    ) {
    }

    public record CollectionJobResponse(
            String jobId,
            String workspaceId,
            String niche,
            String marketTheme,
            String status,
            String timeWindow,
            int limitPerSource,
            int minSuccessScore,
            List<String> sources,
            Instant createdAt
    ) {
    }

    public record CollectionJobListResponse(List<CollectionJobResponse> items) {
    }

    public record CollectedReferenceResponse(
            String referenceId,
            String jobId,
            String source,
            String title,
            String url,
            String niche,
            String status,
            boolean favorite,
            String importedReferenceId,
            int successScore,
            String successSignal,
            String confidenceLevel,
            int rankingPosition,
            double engagementRelative,
            double recurrenceScore,
            double evidenceScore,
            Instant collectedAt,
            Map<String, String> rawMetadata
    ) {
    }

    public record CollectedReferenceListResponse(
            String jobId,
            List<CollectedReferenceResponse> items
    ) {
    }

    public record CollectedReferenceActionResponse(
            String jobId,
            String referenceId,
            String action,
            String status,
            String importedReferenceId,
            String extractionId,
            List<String> generatedLibraryBlockIds,
            Instant updatedAt
    ) {
    }

    public record CollectedReferenceLineageResponse(
            String jobId,
            String referenceId,
            String sourceUrl,
            String importedReferenceId,
            String extractionId,
            List<String> generatedLibraryBlockIds,
            Instant updatedAt
    ) {
    }

    public record CollectionSourceOpsSummaryResponse(
            String source,
            int attempts,
            int successes,
            int failures,
            int retries,
            int rateLimitedEvents,
            long averageLatencyMs,
            String lastError,
            Instant lastAttemptAt
    ) {
    }

    public record CollectionOpsSummaryResponse(
            String workspaceId,
            boolean rolloutEnabled,
            int totalJobs,
            int queuedJobs,
            int runningJobs,
            int completedJobs,
            int failedJobs,
            int totalCollectedReferences,
            long averageJobLatencyMs,
            int totalRetries,
            List<CollectionSourceOpsSummaryResponse> sourceBreakdown,
            Instant generatedAt
    ) {
    }
}
