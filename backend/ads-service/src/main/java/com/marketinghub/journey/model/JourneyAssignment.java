package com.marketinghub.journey.model;

import com.marketinghub.model.Lead;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Links a lead or segment to a specific {@link Journey} execution.
 */
@Entity
@Table(name = "journey_assignment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneyAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyAssignmentType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Column(name = "segment_identifier")
    private String segmentIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyAssignmentStatus status = JourneyAssignmentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_step_id")
    private JourneyStep currentStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_step_id")
    private JourneyStep nextStep;

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    @Lob
    @Column(name = "context_payload")
    private String contextPayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
