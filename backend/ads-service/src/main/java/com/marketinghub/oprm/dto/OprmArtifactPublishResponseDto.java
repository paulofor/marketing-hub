package com.marketinghub.oprm.dto;

public record OprmArtifactPublishResponseDto(
        String artifactId,
        String artifactType,
        String artifactVersion,
        String persistedAt,
        String status,
        boolean duplicated
) {
}
