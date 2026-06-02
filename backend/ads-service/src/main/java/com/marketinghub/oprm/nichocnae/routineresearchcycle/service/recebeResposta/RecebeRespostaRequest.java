package com.marketinghub.oprm.nichocnae.routineresearchcycle.service.recebeResposta;

import java.math.BigDecimal;

/** Contrato para registrar a resposta da IA quando a etapa passar a receber callbacks assíncronos. */
public record RecebeRespostaRequest(
    Long researchCycleId,
    String stageCode,
    String modelResponse,
    Integer inputTokens,
    Integer outputTokens,
    BigDecimal costUsd,
    String openAiJobId,
    String errorMessage,
    String errorDetail) {}
