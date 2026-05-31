package com.marketinghub.geralanding.imagegeneration.service.listStageExecutions;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsável por representar o resumo de uma execução da etapa image generation. */
public record GeraLandingImageGenerationExecutionSummaryResponse(
        String idJob,
        String status,
        Instant executionRequestedAt,
        BigDecimal costUsd) {
}
