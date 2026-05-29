package com.marketinghub.geralanding.wireframe.service.listStageExecutions;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsável por representar o resumo de uma execução da etapa wireframe. */
public record GeraLandingWireframeExecutionSummaryResponse(
        String idJob,
        String status,
        Instant executionRequestedAt,
        BigDecimal costUsd) {
}
