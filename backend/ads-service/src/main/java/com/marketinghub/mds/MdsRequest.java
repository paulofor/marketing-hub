package com.marketinghub.mds;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "mds_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdsRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MdsRequestStatus status;

    @Column(name = "market", nullable = false, length = 191)
    private String market;

    @Column(name = "problem", nullable = false, columnDefinition = "LONGTEXT")
    private String problem;

    @Column(name = "desired_outcome", nullable = false, columnDefinition = "LONGTEXT")
    private String desiredOutcome;

    @Column(name = "context_json", nullable = false, columnDefinition = "LONGTEXT")
    private String contextJson;

    @Column(name = "delivery_constraint", length = 191)
    private String deliveryConstraint;

    @Column(name = "evidence_preference", length = 64)
    private String evidencePreference;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "failure_reason", columnDefinition = "LONGTEXT")
    private String failureReason;

    @Column(name = "correlation_id", nullable = false, length = 191)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
