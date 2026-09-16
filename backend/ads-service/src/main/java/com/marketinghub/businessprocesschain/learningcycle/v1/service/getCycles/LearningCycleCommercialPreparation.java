package com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles;

import java.util.List;

/**
 * Responsabilidade: apresentar insumos comerciais que precisam existir antes das revisões pagas.
 */
public record LearningCycleCommercialPreparation(
    boolean readyForReview,
    String guidance,
    String experimentUrl,
    List<Requirement> requirements,
    String preparationUrl,
    String preparationLabel,
    boolean catalogoVivoApplicable) {
  /** Preserva consumidores anteriores ao comando de adesão explícita ao catálogo. */
  public LearningCycleCommercialPreparation(
      boolean readyForReview,
      String guidance,
      String experimentUrl,
      List<Requirement> requirements,
      String preparationUrl,
      String preparationLabel) {
    this(
        readyForReview,
        guidance,
        experimentUrl,
        requirements,
        preparationUrl,
        preparationLabel,
        false);
  }

  /** Mantém compatibilidade dos ciclos anteriores sem subprocesso Opala. */
  public LearningCycleCommercialPreparation(
      boolean readyForReview,
      String guidance,
      String experimentUrl,
      List<Requirement> requirements) {
    this(readyForReview, guidance, experimentUrl, requirements, null, null);
  }

  /** Expõe um requisito já calculado pelo gate canônico do experimento. */
  public record Requirement(
      String code, String title, boolean ready, String detail, String recommendation) {}
}
