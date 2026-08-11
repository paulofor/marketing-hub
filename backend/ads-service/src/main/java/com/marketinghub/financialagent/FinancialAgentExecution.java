package com.marketinghub.financialagent;

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

/** Responsabilidade: persistir uma conciliacao financeira somente leitura e seu relatorio. */
@Getter
@Setter
@Entity
@Table(name = "financial_agent_execution")
public class FinancialAgentExecution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commercial_plan_id", nullable = false)
  private CommercialPlan commercialPlan;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private FinancialAgentExecutionStatus status;

  @Column(name = "authority_mode", nullable = false)
  private String authorityMode;

  @Column(name = "commercial_plan_version", nullable = false)
  private Integer commercialPlanVersion;

  @Column(name = "agent_task_id")
  private Long agentTaskId;

  @Column(name = "projection_request", columnDefinition = "LONGTEXT")
  private String projectionRequest;

  @Column(name = "financial_snapshot", nullable = false, columnDefinition = "LONGTEXT")
  private String financialSnapshot;

  @Column(name = "reconciliation_json", columnDefinition = "LONGTEXT")
  private String reconciliationJson;

  @Column(name = "daily_report", columnDefinition = "LONGTEXT")
  private String dailyReport;

  @Column(name = "raw_model_response", columnDefinition = "LONGTEXT")
  private String rawModelResponse;

  @Column(name = "model")
  private String model;

  @Column(name = "estimated_cost", precision = 12, scale = 4)
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

  /** Inicializa os horarios de auditoria. */
  @PrePersist
  void initializeAuditTimestamps() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  /** Atualiza o horario de auditoria. */
  @PreUpdate
  void updateAuditTimestamp() {
    updatedAt = Instant.now();
  }
}
