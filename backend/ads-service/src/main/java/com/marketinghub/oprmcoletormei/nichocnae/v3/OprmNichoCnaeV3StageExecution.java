package com.marketinghub.oprmcoletormei.nichocnae.v3;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Persiste uma execução auditável de etapa do pipeline OPRM NichoCNAE v3. */
@Entity
@Table(name = "oprm_nichocnae_v3_stage_execution")
public class OprmNichoCnaeV3StageExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "job_id", nullable = false, length = 96)
    private String jobId;

    @Column(name = "cnae_code", nullable = false, length = 16)
    private String cnaeCode;

    @Column(name = "stage_code", nullable = false, length = 80)
    private String stageCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OprmNichoCnaeV3StageExecutionStatus status = OprmNichoCnaeV3StageExecutionStatus.PENDING;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Column(name = "knowledge_version", nullable = false)
    private Integer knowledgeVersion = 1;

    @Column(name = "input_payload", columnDefinition = "LONGTEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "LONGTEXT")
    private String outputPayload;

    @Column(name = "next_stage_code", length = 80)
    private String nextStageCode;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /** Retorna o identificador interno da execução. */
    public Long getId() { return id; }
    /** Define o identificador interno da execução. */
    public void setId(Long id) { this.id = id; }
    /** Retorna o job funcional do pipeline. */
    public String getJobId() { return jobId; }
    /** Define o job funcional do pipeline. */
    public void setJobId(String jobId) { this.jobId = jobId; }
    /** Retorna o CNAE pesquisado. */
    public String getCnaeCode() { return cnaeCode; }
    /** Define o CNAE pesquisado. */
    public void setCnaeCode(String cnaeCode) { this.cnaeCode = cnaeCode; }
    /** Retorna a etapa da execução. */
    public String getStageCode() { return stageCode; }
    /** Define a etapa da execução. */
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    /** Retorna o status operacional. */
    public OprmNichoCnaeV3StageExecutionStatus getStatus() { return status; }
    /** Define o status operacional. */
    public void setStatus(OprmNichoCnaeV3StageExecutionStatus status) { this.status = status; }
    /** Retorna a tentativa cognitiva. */
    public Integer getAttemptNumber() { return attemptNumber; }
    /** Define a tentativa cognitiva. */
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    /** Retorna a versão de conhecimento. */
    public Integer getKnowledgeVersion() { return knowledgeVersion; }
    /** Define a versão de conhecimento. */
    public void setKnowledgeVersion(Integer knowledgeVersion) { this.knowledgeVersion = knowledgeVersion; }
    /** Retorna o payload de entrada. */
    public String getInputPayload() { return inputPayload; }
    /** Define o payload de entrada. */
    public void setInputPayload(String inputPayload) { this.inputPayload = inputPayload; }
    /** Retorna o payload de saída. */
    public String getOutputPayload() { return outputPayload; }
    /** Define o payload de saída. */
    public void setOutputPayload(String outputPayload) { this.outputPayload = outputPayload; }
    /** Retorna a próxima etapa decidida pelo backend. */
    public String getNextStageCode() { return nextStageCode; }
    /** Define a próxima etapa decidida pelo backend. */
    public void setNextStageCode(String nextStageCode) { this.nextStageCode = nextStageCode; }
    /** Retorna a mensagem de erro persistida. */
    public String getErrorMessage() { return errorMessage; }
    /** Define a mensagem de erro persistida. */
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    /** Retorna a data de criação. */
    public Instant getCreatedAt() { return createdAt; }
    /** Define a data de criação. */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    /** Retorna a data de atualização. */
    public Instant getUpdatedAt() { return updatedAt; }
    /** Define a data de atualização. */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
