package com.marketinghub.funnel;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

/** Snapshot of metrics for a funnel step. */
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
