package com.marketinghub.worker.geralanding.wireframe.response;

import java.math.BigDecimal;

/** Guarda a resposta final da OpenAI para a etapa wireframe. */
public record RecordWireframeResponse(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
