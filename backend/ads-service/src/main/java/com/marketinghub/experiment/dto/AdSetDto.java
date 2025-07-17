package com.marketinghub.experiment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

/**
 * DTO for AdSet.
 */
@Data
public class AdSetDto {
    private Long id;
    private java.util.UUID experimentId;
    private String location;
    private String interests;
    private String lookalikes;
    private BigDecimal budget;
    private Integer durationDays;
    private Instant createdAt;
    private Instant updatedAt;
}
