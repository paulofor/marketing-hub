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
    private Long experimentId;
    private String location;
    private String interests;
    private String lookalikes;
    private String targetingJson;
    private BigDecimal budget;
    private Integer durationDays;
    private String prompt;
    private String model;
    private Instant createdAt;
    private Instant updatedAt;
}
