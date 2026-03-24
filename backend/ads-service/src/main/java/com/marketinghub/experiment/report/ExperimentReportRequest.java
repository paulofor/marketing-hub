package com.marketinghub.experiment.report;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Solicitação para gerar um relatório objetivo de um experimento.
 */
@Entity
@Table(name = "experiment_report_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentReportRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ExperimentReportStatus status;

    @Column(name = "requested_by", length = 191)
    private String requestedBy;

    @Column(name = "download_url", length = 512)
    private String downloadUrl;

    @Lob
    @Column(name = "payload_snapshot", columnDefinition = "LONGTEXT")
    private String payloadSnapshot;

    @Lob
    @Column(name = "failure_reason", columnDefinition = "LONGTEXT")
    private String failureReason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
