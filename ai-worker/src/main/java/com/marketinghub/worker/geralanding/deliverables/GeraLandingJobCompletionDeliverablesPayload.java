package com.marketinghub.worker.geralanding.deliverables;

import java.math.BigDecimal;

/** Guarda a resposta final da OpenAI para a etapa deliverables. */
public record GeraLandingJobCompletionDeliverablesPayload(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
