package com.marketinghub.mds.dto;

import java.util.List;

public record BackendArtifactPublishBatchResponseDto(Long requestId, int publishedCount, List<Long> artifactIds) {
}
