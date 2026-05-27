package com.marketinghub.worker.geralanding.presetdesign.dto;

import java.time.Instant;

/** Responsável por representar o detalhe de uma execução da etapa preset-design no backend. */
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
