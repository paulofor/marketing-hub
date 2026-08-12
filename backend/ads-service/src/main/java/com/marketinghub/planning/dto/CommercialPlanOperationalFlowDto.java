package com.marketinghub.planning.dto;

import java.util.List;

/** Responsabilidade: expor o fluxo comercial simplificado e sua próxima ação canônica. */
public record CommercialPlanOperationalFlowDto(
    Long commercialPlanId,
    String currentStage,
    String status,
    String nextAction,
    String blocker,
    String expectedMetric,
    String decisionCriterion,
    List<Stage> stages,
    List<SpecialistDecision> specialistDecisions) {

  /** Responsabilidade: representar uma etapa visível do fluxo comercial. */
  public record Stage(String code, String label, String status) {}

  /** Responsabilidade: resumir a decisão operacional mais recente de um especialista. */
  public record SpecialistDecision(
      String specialist, String responsibility, String decision, String nextAction) {}
}
