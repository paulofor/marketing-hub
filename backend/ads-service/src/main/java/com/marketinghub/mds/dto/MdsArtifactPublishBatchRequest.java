package com.marketinghub.mds.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MdsArtifactPublishBatchRequest(
        @NotNull Long requestId,
        @NotEmpty List<@Valid MdsArtifactPayload> artifacts
) {
    public record MdsArtifactPayload(
            @NotBlank String artifactType,
            @NotBlank String schemaVersion,
            @NotBlank String version,
            @NotBlank String status,
            @NotBlank String producerModule,
            @NotBlank String ownerModule,
            Object content,
            String hash,
            List<Long> parentArtifactIds
    ) {
    }
}
