package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Responsabilidade: limitar a continuidade automática à preparação no foco já escolhido. */
public final class LearningCyclePreparationPolicy {
  public static final String CONTRACT = "LEARNING_CYCLE_SAFE_PREPARATION_V1";

  /** Impede instanciação da política determinística. */
  private LearningCyclePreparationPolicy() {}

  /** Recusa propostas legadas, expansão de mercado e ações que possam alterar gasto ou operação. */
  public static boolean eligible(JsonNode proposal) {
    return "LEARNING_CYCLE_DECISION_PROPOSAL_V2".equals(proposal.path("contractVersion").asText())
        && "ADJUST".equals(proposal.path("action").asText())
        && "KEEP_FOCUS".equals(proposal.path("marketReview").path("recommendedScope").asText())
        && proposal.path("marketReview").path("requiresNewCycle").asBoolean(false)
        && !proposal.path("nextHypothesis").asText().isBlank()
        && !proposal.path("learning").asText().isBlank()
        && !proposal.path("rootCause").asText().isBlank();
  }
}
