package com.marketinghub.geralanding.imageplanning.service;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsável por representar o resumo de uma execução da etapa imageplanning. */
public record GeraLandingImagePlanningExecutionSummaryResponse(
        String idJob,
        String status,
        Instant executionRequestedAt,
        BigDecimal costUsd) {
}
