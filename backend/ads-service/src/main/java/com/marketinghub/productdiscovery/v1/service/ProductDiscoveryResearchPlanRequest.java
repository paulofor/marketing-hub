package com.marketinghub.productdiscovery.v1.service;

import jakarta.validation.constraints.NotBlank;

/** Responsabilidade: transportar o plano auditável de pesquisa dirigida criado por Argos. */
public record ProductDiscoveryResearchPlanRequest(
    @NotBlank String executionLeaseId,
    @NotBlank String planJson,
    @NotBlank String rawResponse,
    @NotBlank String model,
    String executionMode,
    String promptSent,
    String agentPromptPart,
    String activityPromptPart,
    String reasoningEffort,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    Boolean reusedPlan) {

  /** Mantém compatibilidade com executores anteriores à auditoria BPM persistida. */
  public ProductDiscoveryResearchPlanRequest(
      String executionLeaseId, String planJson, String rawResponse, String model) {
    this(
        executionLeaseId,
        planJson,
        rawResponse,
        model,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false);
  }

  /** Mantém compatibilidade dos callbacks anteriores ao reaproveitamento explícito do plano. */
  public ProductDiscoveryResearchPlanRequest(
      String executionLeaseId,
      String planJson,
      String rawResponse,
      String model,
      String executionMode,
      String promptSent,
      String agentPromptPart,
      String activityPromptPart,
      String reasoningEffort,
      Long inputTokens,
      Long cachedInputTokens,
      Long outputTokens) {
    this(
        executionLeaseId,
        planJson,
        rawResponse,
        model,
        executionMode,
        promptSent,
        agentPromptPart,
        activityPromptPart,
        reasoningEffort,
        inputTokens,
        cachedInputTokens,
        outputTokens,
        false);
  }
}
