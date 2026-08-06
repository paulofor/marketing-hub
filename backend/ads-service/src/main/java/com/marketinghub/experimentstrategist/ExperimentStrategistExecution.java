package com.marketinghub.experimentstrategist;

import com.marketinghub.agent.AgentVersion;
import com.marketinghub.planning.CommercialPlan;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir entrada, evidencias e parecer de uma pesquisa estrategica. */
@Getter
@Setter
@Entity
@Table(name = "experiment_strategist_execution")
public class ExperimentStrategistExecution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commercial_plan_id", nullable = false)
  private CommercialPlan commercialPlan;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agent_version_id")
  private AgentVersion agentVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ExperimentStrategistExecutionStatus status;

  @Column(name = "authority_mode", nullable = false)
  private String authorityMode;

  @Column(name = "research_question", nullable = false, columnDefinition = "TEXT")
  private String researchQuestion;

  @Column(name = "evidence_snapshot", nullable = false, columnDefinition = "LONGTEXT")
  private String evidenceSnapshot;

  @Column(name = "alternatives_json", columnDefinition = "LONGTEXT")
  private String alternativesJson;

  @Column(name = "recommendation_json", columnDefinition = "LONGTEXT")
  private String recommendationJson;

  @Column(name = "public_sources_json", columnDefinition = "LONGTEXT")
  private String publicSourcesJson;

  @Column(name = "raw_model_response", columnDefinition = "LONGTEXT")
  private String rawModelResponse;

  @Column(name = "model_name")
  private String modelName;

  @Column(name = "estimated_cost")
  private BigDecimal estimatedCost;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Inicializa os horarios de auditoria da pesquisa. */
  @PrePersist
  void initializeAuditTimestamps() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  /** Atualiza o horario da ultima alteracao da pesquisa. */
  @PreUpdate
  void updateAuditTimestamp() {
    updatedAt = Instant.now();
  }
}
