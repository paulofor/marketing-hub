package com.marketinghub.experiment.frameworkimage.dto;

import java.time.Instant;
import java.util.UUID;

public record FrameworkImageGenerationItemStatusDto(
        String planningItemKey,
        String sectionName,
        String prompt,
        UUID jobId,
        String status,
        String stage,
        String model,
        Long assetId,
        String sourceUrl,
        String webUrl,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt) {
}
