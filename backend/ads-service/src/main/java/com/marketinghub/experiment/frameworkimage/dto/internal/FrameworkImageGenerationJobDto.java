package com.marketinghub.experiment.frameworkimage.dto.internal;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FrameworkImageGenerationJobDto(
        UUID id,
        Long experimentId,
        String planningItemKey,
        String status,
        String stage,
        String workerId,
        String model,
        String prompt,
        String batchId,
        Long assetId,
        String sourceUrl,
        String webUrl,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt) {
}
