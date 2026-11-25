package com.marketinghub.consumerexpectation.v1.service.plan;

import java.util.List;

/** Expõe um desenho consultivo reproduzível, sem alegar criação ou publicação de ativos. */
public record ExpectationPlanResponse(
    String contractVersion,
    String fingerprint,
    String status,
    ExpectationPlanRequest input,
    List<Variant> variants,
    List<String> gaps,
    List<String> validation,
    List<String> metrics,
    String persistence,
    boolean commercialAuthorization) {
  /** Descreve uma alternativa de construção e a hipótese que ainda exige evidência. */
  public record Variant(
      String code,
      String label,
      String owner,
      String concept,
      String familiarAnchor,
      String change,
      String comprehensionCheck,
      String hypothesis,
      String risk) {}
}
