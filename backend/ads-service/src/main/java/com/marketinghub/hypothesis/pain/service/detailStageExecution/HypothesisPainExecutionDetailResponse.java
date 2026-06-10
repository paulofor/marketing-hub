package com.marketinghub.hypothesis.pain.service.detailStageExecution;

import java.math.BigDecimal;
import java.time.Instant;

/** Detalhe auditável de uma execução da etapa Dor com prompts, schema, request e resposta. */
public record HypothesisPainExecutionDetailResponse(
        String jobid,
        Long marketNicheId,
        String hypothesisId,
        String stageCode,
        String status,
        Instant executionRequestedAt,
        Instant processingStartedAt,
        Instant completedAt,
        String promptTemplateId,
        String promptContent,
        String prompt,
        String promptMarkdownContent,
        String schemaJson,
        String openAiRequestBody,
        String openAiModel,
        String openAiJobId,
        String modelResponse,
        String errorMessage,
        String errorDetail,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd
) {
}
