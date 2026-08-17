package com.marketinghub.agentlearning.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Responsabilidade: congelar replay, holdout e decisão de uma candidata visual de Têmis. */
@Entity
@Table(name = "temis_visual_learning_run")
@Getter
@Setter
@NoArgsConstructor
public class TemisVisualLearningRun {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "context_key", nullable = false, length = 120)
  private String contextKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private TemisVisualLearningRunStatus status;

  @Column(name = "baseline_version", nullable = false, length = 80)
  private String baselineVersion;

  @Column(name = "candidate_version", nullable = false, unique = true, length = 80)
  private String candidateVersion;

  @Column(name = "replay_case_ids_json", nullable = false, columnDefinition = "LONGTEXT")
  private String replayCaseIdsJson;

  @Column(name = "holdout_case_ids_json", nullable = false, columnDefinition = "LONGTEXT")
  private String holdoutCaseIdsJson;

  @Column(name = "input_json", nullable = false, columnDefinition = "LONGTEXT")
  private String inputJson;

  @Column(name = "output_json", columnDefinition = "LONGTEXT")
  private String outputJson;

  @Column(name = "producer_execution_id", length = 64)
  private String producerExecutionId;

  @Column(name = "error", columnDefinition = "LONGTEXT")
  private String error;

  @Column(name = "memory_id")
  private Long memoryId;

  @Column(name = "learning_experiment_id")
  private Long learningExperimentId;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
