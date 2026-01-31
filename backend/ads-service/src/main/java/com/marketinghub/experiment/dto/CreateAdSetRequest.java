package com.marketinghub.experiment.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

/**
 * Request to create an ad set.
 */
@Data
public class CreateAdSetRequest {
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
}

