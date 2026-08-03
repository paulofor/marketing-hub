package com.marketinghub.growthoperator;

import com.marketinghub.planning.CommercialPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir contexto, evidencias e resultado de um diagnostico autonomo. */
@Getter
@Setter
@Entity
@Table(name = "growth_operator_execution")
public class GrowthOperatorExecution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commercial_plan_id", nullable = false)
  private CommercialPlan commercialPlan;

  @Column(name = "week_number", nullable = false)
  private Integer weekNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private GrowthOperatorExecutionStatus status;

  @Column(name = "authority_mode", nullable = false)
  private String authorityMode;

  @Column(name = "objective", nullable = false, columnDefinition = "TEXT")
  private String objective;

  @Column(name = "blocker", columnDefinition = "TEXT")
  private String blocker;

  @Column(name = "evidence_snapshot", columnDefinition = "LONGTEXT")
  private String evidenceSnapshot;

  @Column(name = "alternatives_json", columnDefinition = "LONGTEXT")
  private String alternativesJson;

  @Column(name = "diagnosis_json", columnDefinition = "LONGTEXT")
  private String diagnosisJson;

  @Column(name = "raw_model_response", columnDefinition = "LONGTEXT")
  private String rawModelResponse;

  @Enumerated(EnumType.STRING)
  @Column(name = "recommended_decision")
  private GrowthOperatorDecision recommendedDecision;

  @Column(name = "recommended_action", columnDefinition = "TEXT")
  private String recommendedAction;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "model")
  private String model;

  @Column(name = "input_tokens")
  private Long inputTokens;

  @Column(name = "output_tokens")
  private Long outputTokens;

  @Column(name = "estimated_cost")
  private BigDecimal estimatedCost;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Inicializa os horarios de auditoria antes da primeira persistencia. */
  @PrePersist
  void initializeAuditTimestamps() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  /** Atualiza o horario de auditoria antes de uma alteracao. */
  @PreUpdate
  void updateAuditTimestamp() {
    updatedAt = Instant.now();
  }
}
