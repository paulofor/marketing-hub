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
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir uma acao recomendada ate sua comprovacao ou cancelamento. */
@Getter
@Setter
@Entity
@Table(name = "growth_operator_task")
public class GrowthOperatorTask {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commercial_plan_id", nullable = false)
  private CommercialPlan commercialPlan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_execution_id", nullable = false)
  private GrowthOperatorExecution sourceExecution;

  @Column(name = "action_key", nullable = false, length = 64)
  private String actionKey;

  @Column(name = "action_text", nullable = false, columnDefinition = "TEXT")
  private String actionText;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private GrowthOperatorTaskStatus status;

  @Column(name = "resolution_evidence", columnDefinition = "TEXT")
  private String resolutionEvidence;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Inicializa os horarios da pendencia. */
  @PrePersist
  void initializeAuditTimestamps() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  /** Atualiza o horario da ultima alteracao. */
  @PreUpdate
  void updateAuditTimestamp() {
    updatedAt = Instant.now();
  }
}
