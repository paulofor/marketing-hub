package com.marketinghub.experiment.run;

import com.marketinghub.experiment.Experiment;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Representa uma tentativa operacional auditável de executar um experimento comercial. */
@Entity
@Table(
    name = "experiment_run",
    uniqueConstraints = @UniqueConstraint(columnNames = {"experiment_id", "run_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRun {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_id", nullable = false)
  private Experiment experiment;

  @Column(name = "run_number", nullable = false)
  private Integer runNumber;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "mode", columnDefinition = "VARCHAR(24)", length = 24, nullable = false)
  private ExperimentRunMode mode;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "status", columnDefinition = "VARCHAR(32)", length = 32, nullable = false)
  private ExperimentRunStatus status;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(
      name = "evidence_validity",
      columnDefinition = "VARCHAR(32)",
      length = 32,
      nullable = false)
  private ExperimentEvidenceValidity evidenceValidity;

  @Column(name = "strategy_version")
  private Integer strategyVersion;

  @Column(name = "asset_bundle_version")
  private Integer assetBundleVersion;

  @Column(name = "audience_version")
  private Integer audienceVersion;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "stop_policy", columnDefinition = "VARCHAR(48)", length = 48, nullable = false)
  private ExperimentRunStopPolicy stopPolicy;

  @Column(name = "stop_reason", length = 96)
  private String stopReason;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "failure_classification", columnDefinition = "VARCHAR(64)", length = 64)
  private ExperimentRunFailureClassification failureClassification;

  @Column(name = "failure_detail", columnDefinition = "LONGTEXT")
  private String failureDetail;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(
      name = "data_quality_status",
      columnDefinition = "VARCHAR(32)",
      length = 32,
      nullable = false)
  private ExperimentRunDataQualityStatus dataQualityStatus;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "preflight_started_at")
  private Instant preflightStartedAt;

  @Column(name = "preflight_completed_at")
  private Instant preflightCompletedAt;

  @Column(name = "publication_requested_at")
  private Instant publicationRequestedAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "first_verified_impression_at")
  private Instant firstVerifiedImpressionAt;

  @Column(name = "commercial_window_started_at")
  private Instant commercialWindowStartedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "created_by", length = 191)
  private String createdBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
