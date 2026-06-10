package com.marketinghub.hypothesis.pain.service.listStageExecutions;

import java.math.BigDecimal;
import java.time.Instant;

/** Resumo de execução da etapa Dor exibido no acompanhamento operacional da hipótese. */
public record HypothesisPainExecutionSummaryResponse(
        String jobid,
        Long marketNicheId,
        String hypothesisId,
        String stageCode,
        String status,
        Instant executionRequestedAt,
        Instant processingStartedAt,
        Instant completedAt,
        String openAiModel,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String errorMessage,
        String modelResponse
) {
}
