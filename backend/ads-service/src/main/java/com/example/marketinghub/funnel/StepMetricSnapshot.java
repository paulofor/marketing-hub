package com.example.marketinghub.funnel;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Snapshot of metrics for a funnel step.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepMetricSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funnel_step_id")
    private FunnelStep funnelStep;

    private Long impressions;
    private Long responses;
    private Long conversions;
    private BigDecimal revenue;
    private BigDecimal grossProfit;
    private BigDecimal cvr;
    private Instant capturedAt;
}
