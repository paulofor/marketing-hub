package com.marketinghub.salesvideo;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Persiste fila, auditoria, artefatos e resultado da análise automática de uma referência. */
@Entity
@Table(name = "video_reference_analysis_execution")
@Getter
@Setter
@NoArgsConstructor
public class VideoReferenceAnalysisExecution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reference_id", nullable = false)
  private Long referenceId;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private VideoReferenceAnalysisStatus status;

  @Column(name = "attempt_number", nullable = false)
  private Integer attemptNumber;

  @Column(name = "worker_id", length = 191)
  private String workerId;

  @Column(name = "producer_execution_id", length = 64)
  private String producerExecutionId;

  @Column(name = "input_json", nullable = false, columnDefinition = "LONGTEXT")
  private String inputJson;

  @Column(name = "output_json", columnDefinition = "LONGTEXT")
  private String outputJson;

  @Column(name = "artifacts_json", columnDefinition = "LONGTEXT")
  private String artifactsJson;

  @Column(name = "raw_request_json", columnDefinition = "LONGTEXT")
  private String rawRequestJson;

  @Column(name = "raw_response_json", columnDefinition = "LONGTEXT")
  private String rawResponseJson;

  @Column(name = "model", length = 120)
  private String model;

  @Column(name = "input_tokens")
  private Long inputTokens;

  @Column(name = "cached_input_tokens")
  private Long cachedInputTokens;

  @Column(name = "output_tokens")
  private Long outputTokens;

  @Column(name = "cost_usd", precision = 14, scale = 6)
  private BigDecimal costUsd;

  @Column(name = "decision", length = 64)
  private String decision;

  @Column(name = "error", columnDefinition = "LONGTEXT")
  private String error;

  @Column(name = "claimed_at")
  private Instant claimedAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
