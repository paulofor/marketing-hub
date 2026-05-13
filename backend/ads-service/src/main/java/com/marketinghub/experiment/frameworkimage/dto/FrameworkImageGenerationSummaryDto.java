package com.marketinghub.experiment.frameworkimage.dto;

import java.time.Instant;

public record FrameworkImageGenerationSummaryDto(
        int totalItems,
        int plannedCount,
        int processingCount,
        int waitingOpenAiBatchCount,
        int completedCount,
        int failedCount,
        Instant updatedAt) {
}
