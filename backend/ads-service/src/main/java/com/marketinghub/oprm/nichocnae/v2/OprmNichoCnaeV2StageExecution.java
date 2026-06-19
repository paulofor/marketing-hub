package com.marketinghub.oprm.nichocnae.v2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/** Entidade responsável por registrar tentativas imutáveis de etapa do pipeline NichoCNAE v2. */
@Entity
@Data
@Table(name = "oprm_nichocnae_v2_stage_execution")
public class OprmNichoCnaeV2StageExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 96)
    private String jobId;

    @Column(name = "research_cycle_id")
    private Long researchCycleId;

    @Column(name = "source_niche_id", nullable = false)
    private Long sourceNicheId;

    @Column(name = "cnae_code", nullable = false, length = 7)
    private String cnaeCode;

    @Column(name = "stage_code", nullable = false, length = 64)
    private String stageCode;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "technical_retry_number", nullable = false)
    private Integer technicalRetryNumber;

    @Column(name = "knowledge_version", nullable = false)
    private Integer knowledgeVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private OprmNichoCnaeV2StageExecutionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type", length = 32)
    private OprmNichoCnaeV2FailureType failureType;

    @Column(name = "input_payload", columnDefinition = "LONGTEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "LONGTEXT")
    private String outputPayload;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "next_stage_code", length = 64)
    private String nextStageCode;

    @Column(name = "materialization_enabled", nullable = false)
    private Boolean materializationEnabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
