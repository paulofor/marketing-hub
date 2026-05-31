package com.marketinghub.geralanding.presetdesign.service.listStageExecutions;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsável por representar o resumo de uma execução da etapa preset design. */
public record GeraLandingPresetDesignExecutionSummaryResponse(
        String idJob,
        String status,
        Instant executionRequestedAt,
        BigDecimal costUsd) {
}
