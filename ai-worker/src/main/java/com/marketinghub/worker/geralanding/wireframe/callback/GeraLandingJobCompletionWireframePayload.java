package com.marketinghub.worker.geralanding.wireframe.callback;

import java.math.BigDecimal;

/** Guarda a resposta final da OpenAI para a etapa wireframe. */
public record GeraLandingJobCompletionWireframePayload(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
