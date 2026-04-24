package com.marketinghub.mois.dto;

import java.time.Instant;
import java.util.Map;

public final class MoisArtifactDtos {

    private MoisArtifactDtos() {
    }

    public record ArtifactEnvelopeResponse(
            String artifactId,
            String artifactType,
            String schemaVersion,
            String status,
            String module,
            String createdBy,
            Instant createdAt,
            Instant updatedAt,
            Map<String, Object> lineage,
            Map<String, Object> metadata,
            Map<String, Object> content
    ) {
    }
}
