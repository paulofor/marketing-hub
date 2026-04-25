package com.marketinghub.mois.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MoisWorkspaceDtos {

    private MoisWorkspaceDtos() {
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
            int successScore,
            String successSignal,
            Instant collectedAt,
            Map<String, String> rawMetadata
    ) {
    }

    public record CollectedReferenceListResponse(
            String jobId,
            List<CollectedReferenceResponse> items
    ) {
    }
}
