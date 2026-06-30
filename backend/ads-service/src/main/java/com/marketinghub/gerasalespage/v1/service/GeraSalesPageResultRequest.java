package com.marketinghub.gerasalespage.v1.service;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** Payload recebido do AI Worker com resposta final ou erro da etapa. */
public record GeraSalesPageResultRequest(
        Long experimentId,
        @NotBlank String stageCode,
        String modelResponse,
        String rawResponse,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String openAiJobId,
        String errorMessage,
        String errorDetail
) {}
