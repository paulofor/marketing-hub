package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "gera_landing_stage_execution")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeraLandingStageExecution {

    @Column(name = "experiment_id", nullable = false)
    private Long experimentId;

    @Column(name = "stage_code", nullable = false, length = 100)
    private String stageCode;

    @CreationTimestamp
    @Column(name = "execution_requested_at", nullable = false, updatable = false)
    private Instant executionRequestedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false, insertable = false, updatable = false)
    private Experiment experiment;

    @Column(name = "prompt_template_id", length = 191)
    private String promptTemplateId;

    @Lob
    @Column(name = "prompt_content", nullable = false)
    private String promptContent;

    @Lob
    @Column(name = "prompt", columnDefinition = "LONGTEXT")
    private String prompt;

    @Lob
    @Column(name = "openai_request_body", columnDefinition = "LONGTEXT")
    private String openAiRequestBody;

    @Column(name = "openai_model", length = 120)
    private String openAiModel;

    @Lob
    @Column(name = "schema_json", columnDefinition = "LONGTEXT")
    private String schemaJson;

    @Lob
    @Column(name = "prompt_markdown_content", columnDefinition = "LONGTEXT")
    private String promptMarkdownContent;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "openai_job_id", length = 120)
    private String openAiJobId;

    @Lob
    @Column(name = "model_response", columnDefinition = "LONGTEXT")
    private String modelResponse;

    @Lob
    @Column(name = "provisional_html", columnDefinition = "LONGTEXT")
    private String provisionalHtml;

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Lob
    @Column(name = "error_detail", columnDefinition = "LONGTEXT")
    private String errorDetail;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 12, scale = 6)
    private java.math.BigDecimal costUsd;

    @Id
    @Column(name = "id_job", nullable = false, length = 36)
    private byte[] idJob;
}
