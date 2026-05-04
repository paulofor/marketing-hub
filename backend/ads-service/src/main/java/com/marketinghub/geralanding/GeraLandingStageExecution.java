package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "gera_landing_stage_execution")
@IdClass(GeraLandingStageExecutionId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeraLandingStageExecution {

    @Id
    @Column(name = "experiment_id", nullable = false)
    private Long experimentId;

    @Id
    @Column(name = "stage_code", nullable = false, length = 100)
    private String stageCode;

    @Id
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
    @Column(name = "prompt")
    private String prompt;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "id_job", nullable = false, length = 36)
    private String idJob;
}
