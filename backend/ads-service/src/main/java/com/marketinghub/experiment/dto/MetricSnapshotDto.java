package com.marketinghub.experiment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

/**
 * DTO for MetricSnapshot.
 */
@Data
public class MetricSnapshotDto {
    private Long id;
    private Long creativeId;
    private Long adSetId;
    private Integer impressions;
    private Integer clicks;
    private BigDecimal cost;
    private BigDecimal roas;
    private Double ctr;
    private BigDecimal cpa;
    private Instant createdAt;
}
