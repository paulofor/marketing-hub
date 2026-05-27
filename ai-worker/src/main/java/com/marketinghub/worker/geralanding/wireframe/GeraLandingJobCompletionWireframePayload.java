package com.marketinghub.worker.geralanding.wireframe;

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

    /** Converte payload específico de wireframe para payload base do GeraLanding. */
    public com.marketinghub.worker.geralanding.GeraLandingJobCompletionPayload toBase() {
        return new com.marketinghub.worker.geralanding.GeraLandingJobCompletionPayload(
                responseContent,
                rawResponse,
                requestBodyJson,
                openAiJobId,
                inputTokens,
                outputTokens,
                costUsd);
    }
}
