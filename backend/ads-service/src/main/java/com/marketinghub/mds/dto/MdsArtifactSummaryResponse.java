package com.marketinghub.mds.dto;

public record MdsArtifactSummaryResponse(
        Long artifactId,
        String artifactType,
        String schemaVersion,
        String version,
        String status
) {
}
