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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Representa uma tentativa operacional auditável de executar um experimento comercial.
 */
@Entity
@Table(name = "experiment_run", uniqueConstraints = @UniqueConstraint(columnNames = {"experiment_id", "run_number"}))
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
    @Column(name = "mode", length = 24, nullable = false)
    private ExperimentRunMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ExperimentRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_validity", length = 32, nullable = false)
    private ExperimentEvidenceValidity evidenceValidity;

    @Column(name = "strategy_version")
    private Integer strategyVersion;

    @Column(name = "asset_bundle_version")
    private Integer assetBundleVersion;

    @Column(name = "audience_version")
    private Integer audienceVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_policy", length = 48, nullable = false)
    private ExperimentRunStopPolicy stopPolicy;

    @Column(name = "stop_reason", length = 96)
    private String stopReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_classification", length = 64)
    private ExperimentRunFailureClassification failureClassification;

    @Column(name = "failure_detail", columnDefinition = "LONGTEXT")
    private String failureDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_quality_status", length = 32, nullable = false)
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
