package com.marketinghub.experiment.pipeline;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentPipelineGenerationJob {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private ExperimentPipelineSection section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ExperimentPipelineGenerationJobStatus status = ExperimentPipelineGenerationJobStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 32)
    @Builder.Default
    private ExperimentPipelineGenerationJobStage stage = ExperimentPipelineGenerationJobStage.WAITING_AI_WORKER;

    @Column(length = 191)
    private String model;

    @Column(name = "worker_id", length = 191)
    private String workerId;

    @Column(name = "custom_instructions", columnDefinition = "LONGTEXT")
    private String customInstructions;

    @Column(name = "prompt", columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(name = "request_body_json", columnDefinition = "LONGTEXT")
    private String requestBodyJson;

    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "response_content", columnDefinition = "LONGTEXT")
    private String responseContent;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 10, scale = 4)
    private BigDecimal costUsd;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
