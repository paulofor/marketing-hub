package com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles;

import java.util.List;

/**
 * Responsabilidade: apresentar insumos comerciais que precisam existir antes das revisões pagas.
 */
public record LearningCycleCommercialPreparation(
    boolean readyForReview, String guidance, String experimentUrl, List<Requirement> requirements) {
  /** Expõe um requisito já calculado pelo gate canônico do experimento. */
  public record Requirement(
      String code, String title, boolean ready, String detail, String recommendation) {}
}
