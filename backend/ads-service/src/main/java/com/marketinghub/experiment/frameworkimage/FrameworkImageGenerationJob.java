package com.marketinghub.experiment.frameworkimage;

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
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Representa um job de geração de imagem ligado a um item planejado do experimento.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrameworkImageGenerationJob {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(name = "planning_item_key", nullable = false, length = 191)
    private String planningItemKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private FrameworkImageGenerationJobStatus status = FrameworkImageGenerationJobStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private FrameworkImageGenerationJobStage stage = FrameworkImageGenerationJobStage.WAITING_AI_WORKER;

    @Column(name = "worker_id", length = 191)
    private String workerId;

    @Column(length = 191)
    private String model;

    @Column(columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(name = "batch_id", length = 191)
    private String batchId;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(name = "web_url", length = 1024)
    private String webUrl;

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
