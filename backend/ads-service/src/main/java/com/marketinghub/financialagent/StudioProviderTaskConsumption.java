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

/** Responsabilidade: registrar o consumo financeiro de cada task/cena aceita por um provedor. */
@Entity
@Table(name = "studio_provider_task_consumption")
@Getter
@Setter
@NoArgsConstructor
public class StudioProviderTaskConsumption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "sales_video_job_id", nullable = false)
  private Long salesVideoJobId;

  @Column(name = "video_production_cycle_id")
  private Long videoProductionCycleId;

  @Column(name = "provider", nullable = false, length = 64)
  private String provider;

  @Column(name = "provider_task_id", nullable = false, length = 191)
  private String providerTaskId;

  @Column(name = "model", nullable = false, length = 128)
  private String model;

  @Column(name = "scene_number", nullable = false)
  private Integer sceneNumber;

  @Column(name = "planned_scene_count", nullable = false)
  private Integer plannedSceneCount;

  @Column(name = "duration_seconds", nullable = false)
  private Integer durationSeconds;

  @Column(name = "estimated_credits", nullable = false)
  private Integer estimatedCredits;

  @Column(name = "estimated_cost_usd", nullable = false, precision = 14, scale = 6)
  private BigDecimal estimatedCostUsd;

  @Column(name = "billed_credits")
  private Integer billedCredits;

  @Column(name = "billed_cost_usd", precision = 14, scale = 6)
  private BigDecimal billedCostUsd;

  @Column(name = "settlement_status", length = 32)
  private String settlementStatus;

  @Column(name = "settlement_basis", length = 32)
  private String settlementBasis;

  @Column(name = "billing_evidence", length = 96)
  private String billingEvidence;

  @Column(name = "settled_at")
  private Instant settledAt;

  @Column(name = "accepted_at", nullable = false)
  private Instant acceptedAt;

  @Column(name = "commercial_evaluation_status", length = 24)
  private String commercialEvaluationStatus;

  @Column(name = "commercial_utilization_percent")
  private Integer commercialUtilizationPercent;

  @Column(name = "commercial_evaluation_notes", columnDefinition = "LONGTEXT")
  private String commercialEvaluationNotes;

  @Column(name = "commercial_evaluated_by", length = 191)
  private String commercialEvaluatedBy;

  @Column(name = "commercial_evaluated_at")
  private Instant commercialEvaluatedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
