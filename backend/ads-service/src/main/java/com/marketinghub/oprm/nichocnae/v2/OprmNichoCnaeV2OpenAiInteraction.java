package com.marketinghub.oprm.nichocnae.v2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

/** Entidade responsável por auditar cada interação OpenAI executada por etapas do pipeline NichoCNAE v2. */
@Entity
@Data
@Table(name = "oprm_nichocnae_v2_openai_interaction")
public class OprmNichoCnaeV2OpenAiInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_execution_id", nullable = false)
    private Long stageExecutionId;

    @Column(name = "job_id", nullable = false, length = 96)
    private String jobId;

    @Column(name = "stage_code", nullable = false, length = 64)
    private String stageCode;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "technical_retry_number", nullable = false)
    private Integer technicalRetryNumber;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "service_tier", length = 32)
    private String serviceTier;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "cost_usd", precision = 12, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "openai_response_id", length = 128)
    private String openAiResponseId;

    @Column(name = "raw_request", columnDefinition = "LONGTEXT")
    private String rawRequest;

    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "status", length = 40)
    private String status;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
