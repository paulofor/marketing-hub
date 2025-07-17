package com.marketinghub.experiment.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Request to create an ad set.
 */
@Data
public class CreateAdSetRequest {
    private java.util.UUID experimentId;
    private String location;
    private String interests;
    private String lookalikes;
    private BigDecimal budget;
    private Integer durationDays;
}
