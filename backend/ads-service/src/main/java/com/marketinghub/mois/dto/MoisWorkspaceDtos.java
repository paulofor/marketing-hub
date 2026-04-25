package com.marketinghub.mois.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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
}
