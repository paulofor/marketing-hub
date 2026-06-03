package com.marketinghub.geralanding.qualityreview.service.recebeResposta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Representa o payload enviado pelo Worker AI para concluir ou falhar a revisão visual. */
public record RecebeRespostaRequest(
        @NotNull Long experimentId,
        @NotBlank String stageCode,
        String modelResponse,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String openAiJobId,
        String errorMessage,
        String errorDetail
) {
    /** Mantém o contrato imutável do callback de resposta da revisão visual. */
    public RecebeRespostaRequest {}
}
