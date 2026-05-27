package com.marketinghub.worker.geralanding.presetdesign;

import java.math.BigDecimal;

/** Guarda a resposta final da OpenAI para a etapa preset design. */
public record GeraLandingJobCompletionPresetDesignPayload(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
