package com.marketinghub.geralanding.deliverables.service.recebeResposta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Representa o payload enviado pelo Worker AI para concluir ou falhar a resposta da etapa deliverables. */
public record RecebeRespostaRequest(
        @NotNull Long experimentId,
        @NotBlank String stageCode,
        String modelResponse,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String openAiJobId,
        String errorMessage,
        String errorDetail) {}
