package com.marketinghub.agentorchestration;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir a coordenacao auditavel de agentes para um experimento. */
@Getter
@Setter
@Entity
@Table(
    name = "agent_orchestration_case",
    uniqueConstraints = @UniqueConstraint(columnNames = {"commercial_plan_id", "experiment_id"}))
public class AgentOrchestrationCase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commercial_plan_id", nullable = false)
  private CommercialPlan commercialPlan;

  @Column(name = "experiment_id", nullable = false)
  private Long experimentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private AgentOrchestrationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "strategist_state", nullable = false)
  private AgentTaskState strategistState;

  @Enumerated(EnumType.STRING)
  @Column(name = "growth_operator_state", nullable = false)
  private AgentTaskState growthOperatorState;

  @Enumerated(EnumType.STRING)
  @Column(name = "ad_specialist_state", nullable = false)
  private AgentTaskState adSpecialistState;

  @Column(name = "strategist_execution_id")
  private Long strategistExecutionId;

  @Column(name = "growth_operator_execution_id")
  private Long growthOperatorExecutionId;

  @Column(name = "creative_id")
  private Long creativeId;

  @Column(name = "blocker", columnDefinition = "TEXT")
  private String blocker;

  @Column(name = "evidence_snapshot", nullable = false, columnDefinition = "LONGTEXT")
  private String evidenceSnapshot;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Inicializa os horarios de auditoria do caso. */
  @PrePersist
  void initializeAuditTimestamps() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  /** Atualiza o horario da ultima reconciliacao. */
  @PreUpdate
  void updateAuditTimestamp() {
    updatedAt = Instant.now();
  }
}
