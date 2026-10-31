package com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar números e orientação de uma fonte financeira identificada. */
public record FinancialAdvice(
    String status,
    String title,
    String recommendation,
    String basis,
    Long sourceId,
    Instant producedAt,
    String sourceUrl,
    List<Figure> figures,
    String caution,
    String fullRationale) {
  /**
   * Responsabilidade: transportar um número com unidade e base explícitas, preservando ausência.
   */
  public record Figure(String label, BigDecimal value, String unit) {}
}
