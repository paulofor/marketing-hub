package com.marketinghub.gerasalespage.v1;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Responsabilidade: representar uma execução auditável de etapa do GeraSalesPage v1. */
@Entity
@Table(name = "gera_sales_page_stage_execution")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeraSalesPageStageExecution {
    @Id
    @Column(name = "id_job", nullable = false, length = 36)
    private String idJob;

    @Column(name = "experiment_id", nullable = false)
    private Long experimentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false, insertable = false, updatable = false)
    private Experiment experiment;

    @Column(name = "stage_code", nullable = false, length = 100)
    private String stageCode;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "execution_requested_at", nullable = false)
    private Instant executionRequestedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "prompt_template_key", length = 191)
    private String promptTemplateKey;

    @Column(name = "prompt", columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(name = "prompt_markdown_content", columnDefinition = "LONGTEXT")
    private String promptMarkdownContent;

    @Column(name = "schema_json", columnDefinition = "LONGTEXT")
    private String schemaJson;

    @Column(name = "openai_request_body", columnDefinition = "LONGTEXT")
    private String openAiRequestBody;

    @Column(name = "openai_model", length = 120)
    private String openAiModel;

    @Column(name = "openai_job_id", length = 120)
    private String openAiJobId;

    @Column(name = "model_response", columnDefinition = "LONGTEXT")
    private String modelResponse;

    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "error_detail", columnDefinition = "LONGTEXT")
    private String errorDetail;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 12, scale = 6)
    private BigDecimal costUsd;
}
