package com.marketinghub.businessprocesschain.learningcycle.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar a identidade, o contexto e o estado de um ciclo comercial. */
@Entity
@Table(name = "learning_sales_cycle_v1")
@Getter
@Setter
public class LearningSalesCycle {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "chain_definition_id", nullable = false)
  private Long chainDefinitionId;

  @Column(name = "chain_code", nullable = false, length = 100)
  private String chainCode;

  @Column(name = "process_definition_id", nullable = false)
  private Long processDefinitionId;

  @Column(name = "experiment_id", nullable = false, unique = true)
  private Long experimentId;

  @Column(name = "previous_cycle_id", unique = true)
  private Long previousCycleId;

  @Column(name = "request_key", nullable = false, length = 36)
  private String requestKey;

  @Column(name = "creation_json", nullable = false, columnDefinition = "LONGTEXT")
  private String creationJson;

  @Column(name = "brief_json", nullable = false, columnDefinition = "LONGTEXT")
  private String briefJson;

  @Column(name = "inherited_learning_json", nullable = false, columnDefinition = "LONGTEXT")
  private String inheritedLearningJson;

  @Column(name = "stage", nullable = false, length = 32)
  private String stage;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "product_version", nullable = false, length = 160)
  private String productVersion;

  @Column(name = "budget_limit_brl", nullable = false, precision = 12, scale = 2)
  private BigDecimal budgetLimitBrl;

  @Column(name = "window_start", nullable = false)
  private Instant windowStart;

  @Column(name = "window_end", nullable = false)
  private Instant windowEnd;

  @Column(name = "return_process_id")
  private Long returnProcessId;

  @Column(name = "return_activity_id", length = 100)
  private String returnActivityId;

  @Column(name = "current_instance_id")
  private Long currentInstanceId;

  @Column(name = "open_slot")
  private Integer openSlot;

  @Column(name = "revision", nullable = false)
  private long revision;

  @Column(name = "baseline", nullable = false)
  private boolean baseline;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "version_changed_at", nullable = false)
  private Instant versionChangedAt;

  @Column(name = "closed_at")
  private Instant closedAt;
}
