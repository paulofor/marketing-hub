package com.marketinghub.hypothesis.pain;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Responsabilidade: representar a execução persistida da etapa Dor do pipeline de hipótese e seus artefatos auditáveis. */
@Entity
@Table(name = "hypothesis_pain_stage_execution")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HypothesisPainStageExecution {

    @Column(name = "market_niche_id", nullable = false)
    private Long marketNicheId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_niche_id", nullable = false, insertable = false, updatable = false)
    private MarketNiche marketNiche;

    @Column(name = "hypothesis_id")
    private java.util.UUID hypothesisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id", insertable = false, updatable = false)
    private Hypothesis hypothesis;

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

    @Column(name = "prompt_template_id", length = 191)
    private String promptTemplateId;

    @Lob
    @Column(name = "prompt_content", nullable = false, columnDefinition = "LONGTEXT")
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
    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Lob
    @Column(name = "provisional_html", columnDefinition = "LONGTEXT")
    private String provisionalHtml;

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Lob
    @Column(name = "error_detail", columnDefinition = "LONGTEXT")
    private String errorDetail;

    @Lob
    @Column(name = "quality_review_audit", columnDefinition = "LONGTEXT")
    private String qualityReviewAudit;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 12, scale = 6)
    private BigDecimal costUsd;

    @Id
    @Column(name = "id_job", nullable = false, length = 36)
    private byte[] idJob;
}
