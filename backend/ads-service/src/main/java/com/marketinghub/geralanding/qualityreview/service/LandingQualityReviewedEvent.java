package com.marketinghub.geralanding.qualityreview.service;

/** Evento imutável emitido quando o Quality Review persistiu uma decisão da landing. */
public record LandingQualityReviewedEvent(Long experimentId, String reviewJson) {
  /** Mantém o contrato explícito do evento de conclusão da revisão. */
  public LandingQualityReviewedEvent {}
}
