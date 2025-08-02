package com.example.marketinghub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Event stored for reliable delivery.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "BINARY(16)")
    private UUID aggregateId;

    private String eventType;

    @Column(columnDefinition = "json")
    private String payload;

    private Instant createdAt;
    private Instant processedAt;
}
