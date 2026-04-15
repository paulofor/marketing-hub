package com.marketinghub.oprm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OprmFeedbackSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private OprmJob job;

    @Column(name = "correlation_id", nullable = false, length = 191)
    private String correlationId;

    @Column(name = "occupation_name", nullable = false, length = 191)
    private String occupationName;

    @Column(name = "persona_label", nullable = false, length = 191)
    private String personaLabel;

    @Column(name = "baseline_routine_artifact_id", nullable = false, length = 191)
    private String baselineRoutineArtifactId;

    @Column(name = "baseline_framework_artifact_id", nullable = false, length = 191)
    private String baselineFrameworkArtifactId;

    @Column(name = "recalibrated_pain_signals_json", nullable = false, columnDefinition = "LONGTEXT")
    private String recalibratedPainSignalsJson;

    @Column(name = "recalibrated_mechanism_signals_json", nullable = false, columnDefinition = "LONGTEXT")
    private String recalibratedMechanismSignalsJson;

    @Column(name = "hypothesis_comparison_json", nullable = false, columnDefinition = "LONGTEXT")
    private String hypothesisComparisonJson;

    @Column(name = "score_reweighting_json", nullable = false, columnDefinition = "LONGTEXT")
    private String scoreReweightingJson;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
