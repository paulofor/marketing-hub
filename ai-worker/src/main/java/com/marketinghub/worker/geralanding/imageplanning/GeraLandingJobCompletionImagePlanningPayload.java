package com.marketinghub.worker.geralanding.imageplanning;

import java.math.BigDecimal;

/** Guarda a resposta final da OpenAI para a etapa image planning. */
public record GeraLandingJobCompletionImagePlanningPayload(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
