package com.marketinghub.oprm.integration.contract;

public record OprmArtifactPublishResponse(
        String artifactId,
        String artifactType,
        String artifactVersion,
        String persistedAt,
        String status,
        boolean duplicated
) {
}
