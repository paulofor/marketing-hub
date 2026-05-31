package com.marketinghub.geralanding.imageplanning.service.recebeResposta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Representa o payload enviado pelo Worker AI para concluir ou falhar a resposta da etapa image planning. */
public record RecebeRespostaRequest(
        @NotNull Long experimentId,
        @NotBlank String stageCode,
        String modelResponse,
        String provisionalHtml,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String openAiJobId,
        String errorMessage,
        String errorDetail) {}
