package com.marketinghub.geralanding.wireframe.service.receberesposta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Representa o payload final enviado pelo Worker AI para concluir a resposta da etapa wireframe. */
public record RecebeRespostaRequest(
        @NotNull Long experimentId,
        @NotBlank String stageCode,
        String modelResponse,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String openAiJobId) {}
