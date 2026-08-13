package com.marketinghub.salesvideo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir um ciclo financeiro e criativo governado de produção de vídeo. */
@Entity
@Table(name = "video_production_cycle")
@Getter
@Setter
public class VideoProductionCycle {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "video_project_id", nullable = false)
  private Long videoProjectId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "commercial_plan_id")
  private Long commercialPlanId;

  @Column(name = "experiment_id")
  private Long experimentId;

  @Column(name = "requested_by", nullable = false, length = 191)
  private String requestedBy;

  @Column(name = "status", nullable = false, length = 40)
  private String status;

  @Column(name = "budget_limit_usd", nullable = false, precision = 12, scale = 4)
  private BigDecimal budgetLimitUsd;

  @Column(name = "known_cost_usd", nullable = false, precision = 12, scale = 4)
  private BigDecimal knownCostUsd;

  @Column(name = "learning_objective", nullable = false, columnDefinition = "TEXT")
  private String learningObjective;

  @Column(name = "success_criterion", nullable = false, columnDefinition = "TEXT")
  private String successCriterion;

  @Column(name = "financial_decision", length = 30)
  private String financialDecision;

  @Column(name = "financial_reason", columnDefinition = "LONGTEXT")
  private String financialReason;

  @Column(name = "financial_decided_at")
  private Instant financialDecidedAt;

  @Column(name = "sales_video_job_id")
  private Long salesVideoJobId;

  @Column(name = "agent_task_id")
  private Long agentTaskId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
