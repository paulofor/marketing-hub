package com.marketinghub.creative.convergence.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

/** Responsabilidade: persistir o progresso, custo e decisão de um ciclo de convergência. */
@Entity
@Table(name = "creative_convergence_cycle")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreativeConvergenceCycle {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "experiment_id", nullable = false)
  private Long experimentId;

  @Column(name = "root_creative_id", nullable = false)
  private Long rootCreativeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private ConvergenceCycleStatus status;

  @Column(name = "iteration_count", nullable = false)
  private Integer iterationCount;

  @Column(name = "repeated_issue_count", nullable = false)
  private Integer repeatedIssueCount;

  @Column(name = "last_score")
  private Integer lastScore;

  @Column(name = "best_score")
  private Integer bestScore;

  @Column(name = "cost_usd", nullable = false, precision = 12, scale = 4)
  private BigDecimal costUsd;

  @Column(name = "stop_reason", columnDefinition = "LONGTEXT")
  private String stopReason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
