package com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction;

import java.math.BigDecimal;

/** Contrato de auditoria recebido do executor para registrar uma chamada OpenAI feita por uma etapa v2. */
public record OpenAiInteractionAuditRequest(
        String model,
        String serviceTier,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        BigDecimal costUsd,
        String openAiResponseId,
        String rawRequest,
        String rawResponse,
        String status,
        String errorMessage) {}
