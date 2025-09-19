package com.marketinghub.journey.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Canonical storage for multi-channel events.
 */
@Entity
@Table(name = "journey_event_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "actor_id", columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id")
    private Journey journey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_step_id")
    private JourneyStep journeyStep;

    private String source;

    @Column(name = "campaign_id")
    private String campaignId;

    @Lob
    private String metadata;

    @Column(name = "event_value", precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    private Instant receivedAt;
}
