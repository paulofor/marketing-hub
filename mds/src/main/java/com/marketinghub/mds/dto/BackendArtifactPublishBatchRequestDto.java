package com.marketinghub.mds.dto;

import java.util.List;

public record BackendArtifactPublishBatchRequestDto(Long requestId, List<ArtifactPayloadDto> artifacts) {
    public record ArtifactPayloadDto(
            String artifactType,
            String schemaVersion,
            String version,
            String status,
            String producerModule,
            String ownerModule,
            Object content,
            String hash,
            List<Long> parentArtifactIds
    ) {
    }
}
