package com.marketinghub.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Responsabilidade: registrar quais prompts e schemas de IA foram associados a um experimento. */
@Entity
@Table(
        name = "experiment_ai_prompt_schema_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_experiment_ai_prompt_schema_usage",
                columnNames = {"experiment_id", "template_key", "usage_context", "stage_code"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentAiPromptSchemaUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(name = "experiment_id", nullable = false, insertable = false, updatable = false)
    private Long experimentId;

    @Column(name = "template_key", nullable = false, length = 191)
    private String templateKey;

    @Column(name = "pipeline_code", nullable = false, length = 100)
    private String pipelineCode;

    @Column(name = "stage_code", nullable = false, length = 100)
    private String stageCode;

    @Column(name = "template_version", nullable = false, length = 40)
    private String templateVersion;

    @Column(name = "openai_model", nullable = false, length = 120)
    private String openAiModel;

    @Column(name = "schema_name", nullable = false, length = 120)
    private String schemaName;

    @Column(name = "usage_context", nullable = false, length = 80)
    private String usageContext;

    @Column(name = "source_job_id", length = 191)
    private String sourceJobId;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
