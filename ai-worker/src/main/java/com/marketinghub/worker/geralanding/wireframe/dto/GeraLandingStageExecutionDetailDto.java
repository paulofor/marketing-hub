package com.marketinghub.worker.geralanding.wireframe.dto;

import java.time.Instant;

/**
 * Responsável por representar os detalhes retornados pelo backend para uma execução de etapa do GeraLanding.
 */
public record GeraLandingStageExecutionDetailDto(
        Long experimentId,
        String stageCode,
        String idJob,
        String status,
        Instant executionRequestedAt,
        Instant processingStartedAt,
        Instant completedAt,
        String openAiJobId) {
}
