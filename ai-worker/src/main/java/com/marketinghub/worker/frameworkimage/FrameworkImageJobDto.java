package com.marketinghub.worker.frameworkimage;

import java.time.Instant;
import java.util.UUID;

public record FrameworkImageJobDto(
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
