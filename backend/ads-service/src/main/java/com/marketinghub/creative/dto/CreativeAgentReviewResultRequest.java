package com.marketinghub.creative.dto;

import com.marketinghub.creative.CreativeAgentReviewStatus;
import java.math.BigDecimal;
import java.util.List;

/** Responsabilidade: receber o parecer estruturado e a auditoria bruta do agente especialista. */
public record CreativeAgentReviewResultRequest(
    CreativeAgentReviewStatus decision,
    Integer attentionScore,
    Integer clarityScore,
    Integer desireScore,
    Integer credibilityScore,
    Integer actionScore,
    String copyAssessment,
    String commercialAestheticAssessment,
    String destinationIntegrationAssessment,
    String summary,
    String issuesJson,
    String recommendationsJson,
    String model,
    String requestJson,
    String responseJson,
    Integer inputTokens,
    Integer outputTokens,
    BigDecimal costUsd,
    String error,
    String revisedHeadline,
    String revisedPrimaryText,
    String revisedDescription,
    String revisedCta,
    String revisedImagePrompt,
    List<String> mandatoryVisualRequirements,
    List<String> forbiddenVisualElements,
    List<String> visualAcceptanceCriteria,
    List<ConvergenceCorrectionTarget> correctionTargets) {
  /** Responsabilidade: descrever uma correção verificável e o executor responsável. */
  public record ConvergenceCorrectionTarget(
      String target, String issueCode, String requirement, String acceptanceCriterion) {}
}
