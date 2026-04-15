package com.marketinghub.oprm.dto;

import com.marketinghub.oprm.OprmArtifactStatus;

public record OprmArtifactSummaryDto(
        String artifactId,
        String artifactType,
        String artifactVersion,
        OprmArtifactStatus artifactStatus,
        String occupationSeedRef,
        String correlationId,
        String createdAt
) {
}
