package com.marketinghub.experiment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
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
    private String jobTitles;
    private String behaviors;
    private String targetingJson;
    private BigDecimal budget;
    private Integer durationDays;
    private String prompt;
    private String model;
    private UUID targetingRequestId;
    private Instant createdAt;
    private Instant updatedAt;
}

