package com.marketinghub.mds.dto;

import java.util.List;

public record MdsArtifactPublishBatchResponse(
        Long requestId,
        int publishedCount,
        List<Long> artifactIds
) {
}
