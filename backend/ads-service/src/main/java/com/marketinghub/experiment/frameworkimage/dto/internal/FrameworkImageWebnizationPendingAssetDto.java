package com.marketinghub.experiment.frameworkimage.dto.internal;

import java.time.Instant;
import java.util.UUID;

public record FrameworkImageWebnizationPendingAssetDto(
        UUID jobId,
        Long experimentId,
        String planningItemKey,
        Long assetId,
        String sourceUrl,
        Instant updatedAt) {
}
