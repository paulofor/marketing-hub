package com.marketinghub.experiment.run;

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

import java.time.Instant;

/**
 * Registra o resultado atual de um gate de preparação vinculado a um run de experimento.
 */
@Entity
@Table(name = "experiment_run_gate_result", uniqueConstraints = @UniqueConstraint(columnNames = {"experiment_run_id", "gate_code"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRunGateResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_run_id", nullable = false)
    private ExperimentRun experimentRun;

    @Column(name = "gate_code", length = 96, nullable = false)
    private String gateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_group", length = 48, nullable = false)
    private ExperimentRunGateGroup gateGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    private ExperimentRunGateStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 24, nullable = false)
    private ExperimentRunGateSeverity severity;

    @Column(name = "summary", length = 512, nullable = false)
    private String summary;

    @Column(name = "evidence_reference", length = 512)
    private String evidenceReference;

    @Column(name = "remediation_code", length = 96)
    private String remediationCode;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluator_type", length = 24, nullable = false)
    private ExperimentRunGateEvaluatorType evaluatorType;

    @Column(name = "evaluator_version", length = 64)
    private String evaluatorVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
