package com.marketinghub.geralanding.copy.service.listStageExecutions;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsável por representar o resumo de uma execução da etapa copy. */
public record GeraLandingCopyExecutionSummaryResponse(
        String idJob,
        String status,
        Instant executionRequestedAt,
        BigDecimal costUsd) {
}
