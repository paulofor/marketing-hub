package com.marketinghub.worker.geralanding.imageplanning;

import java.math.BigDecimal;

/** Guarda a resposta final da OpenAI para a etapa imageplanning. */
public record GeraLandingJobCompletionImagePlanningPayload(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {

    /** Converte payload específico da etapa para payload base do GeraLanding. */
    public com.marketinghub.worker.geralanding.GeraLandingJobCompletionPayload toBase() {
        return new com.marketinghub.worker.geralanding.GeraLandingJobCompletionPayload(responseContent, rawResponse, requestBodyJson, openAiJobId, inputTokens, outputTokens, costUsd);
    }
}
