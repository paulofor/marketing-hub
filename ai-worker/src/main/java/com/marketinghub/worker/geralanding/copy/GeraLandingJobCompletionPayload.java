package com.marketinghub.worker.geralanding.copy;

import java.math.BigDecimal;

/** Responsabilidade: transportar resultado da OpenAI para conclusão da etapa de copy. */
public record GeraLandingJobCompletionPayload(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
