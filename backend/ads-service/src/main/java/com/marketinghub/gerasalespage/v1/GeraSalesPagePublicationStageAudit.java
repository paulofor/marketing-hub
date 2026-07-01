package com.marketinghub.gerasalespage.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Responsabilidade: preservar os dados de uma etapa usada em uma publicacao historica do GeraSalesPage v1. */
@Entity
@Table(name = "gera_sales_page_publication_stage_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeraSalesPagePublicationStageAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "publication_audit_id", nullable = false)
    private Long publicationAuditId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_audit_id", nullable = false, insertable = false, updatable = false)
    private GeraSalesPagePublicationAudit publicationAudit;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(name = "id_job", nullable = false, length = 36)
    private String idJob;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_job", nullable = false, insertable = false, updatable = false)
    private GeraSalesPageStageExecution stageExecution;

    @Column(name = "stage_code", nullable = false, length = 100)
    private String stageCode;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

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

    @Column(name = "openai_model", length = 120)
    private String openAiModel;

    @Column(name = "openai_request_body", columnDefinition = "LONGTEXT")
    private String openAiRequestBody;

    @Column(name = "model_response", columnDefinition = "LONGTEXT")
    private String modelResponse;

    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 12, scale = 6)
    private BigDecimal costUsd;
}
