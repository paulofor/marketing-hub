package com.marketinghub.agentlearning.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Responsabilidade: persistir uma tentativa visual real e seu resultado independente. */
@Entity
@Table(name = "temis_visual_learning_case")
@Getter
@Setter
@NoArgsConstructor
public class TemisVisualLearningCase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 24)
  private TemisVisualLearningSourceType sourceType;

  @Column(name = "source_id", nullable = false)
  private Long sourceId;

  @Column(name = "commercial_plan_id")
  private Long commercialPlanId;

  @Column(name = "experiment_id")
  private Long experimentId;

  @Column(name = "context_key", nullable = false, length = 120)
  private String contextKey;

  @Column(name = "playbook_version", nullable = false, length = 80)
  private String playbookVersion;

  @Column(name = "placement", nullable = false, length = 32)
  private String placement;

  @Column(name = "format", nullable = false, length = 32)
  private String format;

  @Column(name = "attempt_number", nullable = false)
  private int attemptNumber;

  @Column(name = "approved", nullable = false)
  private boolean approved;

  @Column(name = "quality_score", nullable = false, precision = 8, scale = 4)
  private BigDecimal qualityScore;

  @Column(name = "cost_usd", nullable = false, precision = 12, scale = 4)
  private BigDecimal costUsd;

  @Column(name = "issue_codes_json", nullable = false, columnDefinition = "LONGTEXT")
  private String issueCodesJson;

  @Column(name = "evidence_json", nullable = false, columnDefinition = "LONGTEXT")
  private String evidenceJson;

  @Column(name = "learning_run_id")
  private Long learningRunId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
