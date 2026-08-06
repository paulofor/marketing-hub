package com.marketinghub.financialagent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: registrar custo e atribuição comercial de cada tentativa do Estúdio. */
@Entity
@Table(name = "studio_cost_ledger_entry")
@Getter
@Setter
@NoArgsConstructor
public class StudioCostLedgerEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "commercial_plan_id")
  private Long commercialPlanId;

  @Column(name = "product_id")
  private Long productId;

  @Column(name = "experiment_id")
  private Long experimentId;

  @Column(name = "asset_type", nullable = false, length = 32)
  private String assetType;

  @Column(name = "source_type", nullable = false, length = 64)
  private String sourceType;

  @Column(name = "source_id", nullable = false, length = 96)
  private String sourceId;

  @Column(name = "provider", nullable = false, length = 64)
  private String provider;

  @Column(name = "model", length = 128)
  private String model;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "estimated_cost_usd", precision = 14, scale = 6)
  private BigDecimal estimatedCostUsd;

  @Column(name = "provider_cost_usd", precision = 14, scale = 6)
  private BigDecimal providerCostUsd;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency = "USD";

  @Column(name = "cost_evidence", nullable = false, length = 64)
  private String costEvidence;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
