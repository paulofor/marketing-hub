package com.marketinghub.mds.dto;

public record MdsLineageResponse(
        Long id,
        Long parentArtifactId,
        Long childArtifactId,
        String relationType
) {
}
