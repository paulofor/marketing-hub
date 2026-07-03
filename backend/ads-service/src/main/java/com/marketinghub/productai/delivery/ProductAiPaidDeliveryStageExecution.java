package com.marketinghub.productai.delivery;

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

/** Responsabilidade: representar a execução auditável da entrega paga de Produto IA. */
@Entity
@Table(name = "product_ai_paid_delivery_stage_execution")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAiPaidDeliveryStageExecution {
    @Id
    @Column(name = "id_job", nullable = false, length = 36)
    private String idJob;

    @Column(name = "purchase_id", nullable = false)
    private Long purchaseId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "experiment_id", nullable = false)
    private Long experimentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false, insertable = false, updatable = false)
    private com.marketinghub.experiment.Experiment experiment;

    @Column(name = "pipeline_code", nullable = false, length = 80)
    private String pipelineCode;

    @Column(name = "stage_code", nullable = false, length = 80)
    private String stageCode;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "execution_requested_at", nullable = false)
    private Instant executionRequestedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "prompt_template_key", length = 191)
    private String promptTemplateKey;

    @Column(name = "prompt_template_version", length = 40)
    private String promptTemplateVersion;

    @Column(name = "schema_name", length = 191)
    private String schemaName;

    @Column(name = "prompt", columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(name = "schema_json", columnDefinition = "LONGTEXT")
    private String schemaJson;

    @Column(name = "openai_request_body", columnDefinition = "LONGTEXT")
    private String openAiRequestBody;

    @Column(name = "openai_response_body", columnDefinition = "LONGTEXT")
    private String openAiResponseBody;

    @Column(name = "functional_output", columnDefinition = "LONGTEXT")
    private String functionalOutput;

    @Column(name = "artifact_url", length = 1024)
    private String artifactUrl;

    @Column(name = "openai_model", length = 120)
    private String openAiModel;

    @Column(name = "service_tier", length = 40)
    private String serviceTier;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 12, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;
}
