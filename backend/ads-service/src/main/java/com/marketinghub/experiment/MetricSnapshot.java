package com.marketinghub.experiment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Snapshot of metrics for a creative/ad set combination.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creative_id", nullable = false)
    private CreativeVariant creative;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ad_set_id", nullable = false)
    private AdSet adSet;

    private Integer impressions;
    private Integer clicks;
    private java.math.BigDecimal cost;
    private java.math.BigDecimal roas;
    private Double ctr;
    private java.math.BigDecimal cpa;

    @CreationTimestamp
    private Instant createdAt;
}
