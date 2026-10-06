package com.marketinghub.businessprocesschain.learningcycle.v1.service.reconcileMeasurement;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: transportar uma leitura atribuída do Lead Portal sem dados pessoais. */
public record LeadPortalCycleMeasurement(
    Long publicationId,
    String publicationUrl,
    boolean paymentSourceAvailable,
    long humanEvents,
    long rawEvents,
    long humanVisitors,
    long humanSessions,
    long rawSessions,
    long pageViews,
    long checkouts,
    Instant lastHumanEventAt,
    Payments payments) {

  /**
   * Responsabilidade: resumir pagamentos reconciliáveis e entrega, sem inferir uso ou satisfação.
   */
  public record Payments(
      long checkoutAccesses,
      long purchases,
      long refunds,
      BigDecimal grossRevenueBrl,
      BigDecimal refundedRevenueBrl,
      long deliveredNetSales,
      String blocker) {}
}
