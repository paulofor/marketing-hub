package com.marketinghub.geralanding.agent.v1;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** Contrato auditável devolvido pelo executor independente do Agente de Landing. */
public record LandingAgentResultRequest(
    @NotBlank String decisionJson,
    @NotBlank String requestJson,
    @NotBlank String responseJson,
    @NotBlank String model,
    Integer inputTokens,
    Integer outputTokens,
    BigDecimal costUsd,
    String error) {}
