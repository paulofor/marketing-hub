package com.marketinghub.businessprocess.automation.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar o controle durável de um processo no contexto exato do produto. */
@Entity
@Table(name = "product_process_run_v1")
@Getter
@Setter
public class ProcessRun {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "process_definition_id", nullable = false)
  private Long processDefinitionId;

  @Column(name = "chain_definition_id", nullable = false)
  private Long chainDefinitionId;

  @Column(name = "learning_cycle_id")
  private Long learningCycleId;

  @Column(name = "parent_run_id")
  private Long parentRunId;

  @Column(name = "scope_key", nullable = false, unique = true, length = 64)
  private String scopeKey;

  @Column(name = "source_reference", nullable = false, length = 255)
  private String sourceReference;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
  private String reason;

  @Column(name = "current_activity_id", length = 100)
  private String currentActivityId;

  @Column(name = "current_activity_name", length = 255)
  private String currentActivityName;

  @Column(name = "current_owner_name", length = 160)
  private String currentOwnerName;

  @Column(name = "current_sequence")
  private Integer currentSequence;

  @Column(name = "child_run_id")
  private Long childRunId;

  @Column(name = "navigation_url", length = 1000)
  private String navigationUrl;

  @Column(name = "total_activities", nullable = false)
  private int totalActivities;

  @Column(name = "completed_activities", nullable = false)
  private int completedActivities;

  @Column(name = "remaining_activities", nullable = false)
  private int remainingActivities;

  @Column(name = "omitted_activities", nullable = false)
  private int omittedActivities;

  @Column(name = "retry_epoch", nullable = false)
  private int retryEpoch;

  @Column(name = "failure_count", nullable = false)
  private int failureCount;

  @Column(name = "known_cost_usd", precision = 19, scale = 8)
  private BigDecimal knownCostUsd;

  @Column(name = "cost_coverage", length = 32)
  private String costCoverage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "last_reconciled_at", nullable = false)
  private Instant lastReconciledAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Version
  @Column(name = "revision", nullable = false)
  private long revision;
}
