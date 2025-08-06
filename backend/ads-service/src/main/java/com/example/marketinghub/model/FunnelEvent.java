package com.example.marketinghub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted by the sales funnel for a given lead.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Enumerated(EnumType.STRING)
    private FunnelStimulus stimulus;

    private Instant createdAt;
}
