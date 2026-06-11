package com.marketinghub.hypothesis.pain.service.summary;

import java.time.Instant;

/** Resumo do conteúdo final persistido de uma etapa do framework de hipótese. */
public record HypothesisStageFinalSummaryResponse(
        String slug,
        int stageNumber,
        String stageTitle,
        String stageCode,
        String jobid,
        String status,
        Instant completedAt,
        String finalContent,
        String sourceTable,
        String sourceField) {
}
