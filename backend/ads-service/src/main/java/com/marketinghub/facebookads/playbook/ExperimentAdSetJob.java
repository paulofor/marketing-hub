package com.marketinghub.facebookads.playbook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Individual task executed by either the AI worker or Facebook Ads worker.
 */
@Entity
@Table(name = "experiment_adset_job")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentAdSetJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @ToString.Exclude
    private ExperimentAdSetWorkflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(length = 64, nullable = false)
    private ExperimentAdSetJobType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ExperimentAdSetWorker worker;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Builder.Default
    private ExperimentAdSetJobStatus status = ExperimentAdSetJobStatus.PENDING;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "result_payload", columnDefinition = "LONGTEXT")
    private String resultPayload;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "locked_by", length = 191)
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean isTerminal() {
        return status == ExperimentAdSetJobStatus.SUCCEEDED || status == ExperimentAdSetJobStatus.FAILED;
    }
}
