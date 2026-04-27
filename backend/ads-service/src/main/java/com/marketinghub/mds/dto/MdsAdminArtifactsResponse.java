package com.marketinghub.mds.dto;

import java.util.List;

public record MdsAdminArtifactsResponse(
        Long requestId,
        List<MdsAdminArtifactItemResponse> artifacts,
        List<MdsAdminArtifactLineageEdgeResponse> lineage
) {
}
