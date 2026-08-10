package com.marketinghub.creative.convergence.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/** Responsabilidade: persistir uma correção verificável destinada ao executor responsável. */
@Entity
@Table(name = "creative_convergence_task")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreativeConvergenceTask {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cycle_id", nullable = false)
  private Long cycleId;

  @Column(name = "creative_id", nullable = false)
  private Long creativeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target", nullable = false, length = 32)
  private ConvergenceTaskTarget target;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private ConvergenceTaskStatus status;

  @Column(name = "issue_code", nullable = false, length = 100)
  private String issueCode;

  @Column(name = "requirement_text", nullable = false, columnDefinition = "LONGTEXT")
  private String requirement;

  @Column(name = "acceptance_criterion", nullable = false, columnDefinition = "LONGTEXT")
  private String acceptanceCriterion;

  @Column(name = "fingerprint", nullable = false, length = 64)
  private String fingerprint;

  @Column(name = "evidence_json", columnDefinition = "LONGTEXT")
  private String evidenceJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "completed_at")
  private Instant completedAt;
}
