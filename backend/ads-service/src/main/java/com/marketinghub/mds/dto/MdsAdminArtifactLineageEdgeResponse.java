package com.marketinghub.mds.dto;

public record MdsAdminArtifactLineageEdgeResponse(
        Long id,
        Long parentArtifactId,
        Long childArtifactId,
        String relationType
) {
}
