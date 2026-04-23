package com.marketinghub.mois.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MoisDiscoveryDtos {

    private MoisDiscoveryDtos() {
    }

    public record CreateDiscoveryRequest(
            @NotBlank String nicheName,
            @NotBlank String marketTheme,
            String painOrOutcomeFocus,
            List<String> seedQueries,
            List<String> seedUrls,
            List<String> channels,
            String country,
            String language,
            Map<String, Object> discoveryPolicy
    ) {
    }

    public record DiscoveryRequestAcceptedResponse(String requestId, String status) {
    }

    public record DiscoveryRequestSummaryResponse(
            String requestId,
            String nicheName,
            String marketTheme,
            String painOrOutcomeFocus,
            String status,
            Instant createdAt
    ) {
    }

    public record ArtifactRefResponse(String artifactId, String artifactType, String schemaVersion) {
    }

    public record DiscoveryRequestDetailResponse(
            String requestId,
            String nicheName,
            String marketTheme,
            String painOrOutcomeFocus,
            String status,
            Instant createdAt,
            List<ArtifactRefResponse> artifacts
    ) {
    }

    public record DiscoveryRequestListResponse(List<DiscoveryRequestSummaryResponse> items) {
    }

    public record AsyncAcceptedResponse(String status, String correlationId) {
    }
}
