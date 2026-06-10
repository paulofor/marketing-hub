package com.marketinghub.hypothesis.pain.service.recebeResposta;

import java.math.BigDecimal;

/** Payload interno com a resposta ou erro da OpenAI para concluir a etapa Dor. */
public record RecebeRespostaRequest(
        Long marketNicheId,
        String stageCode,
        String modelResponse,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String openAiJobId,
        String errorMessage,
        String errorDetail
) {
}
