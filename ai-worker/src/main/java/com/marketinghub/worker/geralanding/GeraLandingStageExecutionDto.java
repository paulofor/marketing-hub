package com.marketinghub.worker.geralanding;

import java.time.LocalDateTime;

public record GeraLandingStageExecutionDto(
        Long experimentId,
        String stageCode,
        LocalDateTime executionRequestedAt,
        String promptTemplateId,
        String promptContent,
        String status,
        String idJob,
        LocalDateTime createdAt,
        LocalDateTime processingStartedAt,
        LocalDateTime completedAt) {
}
