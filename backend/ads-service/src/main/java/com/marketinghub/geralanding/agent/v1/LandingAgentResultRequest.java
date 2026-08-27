package com.marketinghub.geralanding.agent.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Contrato auditável devolvido pelo executor independente do Agente de Landing. */
public record LandingAgentResultRequest(
    @NotBlank String decisionJson,
    @NotBlank String requestJson,
    @NotBlank String responseJson,
    @NotBlank String model,
    @NotBlank @Size(max = 32) String reasoningEffort,
    Integer inputTokens,
    Integer cachedInputTokens,
    Integer outputTokens,
    BigDecimal costUsd,
    String error) {

  /** Mantém compatibilidade com callbacks anteriores à medição de cache por tarefa. */
  public LandingAgentResultRequest(
      String decisionJson,
      String requestJson,
      String responseJson,
      String model,
      String reasoningEffort,
      Integer inputTokens,
      Integer outputTokens,
      BigDecimal costUsd,
      String error) {
    this(
        decisionJson,
        requestJson,
        responseJson,
        model,
        reasoningEffort,
        inputTokens,
        null,
        outputTokens,
        costUsd,
        error);
  }
}
