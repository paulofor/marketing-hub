package com.marketinghub.salesvideo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: reservar e reconciliar créditos de uma conta para um único ciclo de vídeo. */
@Entity
@Table(name = "video_credit_reservation")
@Getter
@Setter
public class VideoCreditReservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "video_production_cycle_id", nullable = false, unique = true)
  private Long videoProductionCycleId;

  @Column(name = "provider_preflight_id", nullable = false, unique = true)
  private Long providerPreflightId;

  @Column(name = "provider_account_id", nullable = false)
  private Long providerAccountId;

  @Column(name = "status", nullable = false, length = 24)
  private String status;

  @Column(name = "reserved_credits", nullable = false, precision = 14, scale = 4)
  private BigDecimal reservedCredits;

  @Column(name = "reserved_cost_usd", nullable = false, precision = 12, scale = 6)
  private BigDecimal reservedCostUsd;

  @Column(name = "actual_credits", precision = 14, scale = 4)
  private BigDecimal actualCredits;

  @Column(name = "actual_cost_usd", precision = 12, scale = 6)
  private BigDecimal actualCostUsd;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "reserved_at", nullable = false)
  private Instant reservedAt;

  @Column(name = "settled_at")
  private Instant settledAt;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
