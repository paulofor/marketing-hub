package com.example.marketinghub.model;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lead captured from webhook.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(unique = true)
    private Long leadgenId;
    private Long instagramUserId;
    private Long adId;
    private Long campaignId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    private NurtureStage nurtureStage = NurtureStage.NEW;

    private BigDecimal cpl;
}
