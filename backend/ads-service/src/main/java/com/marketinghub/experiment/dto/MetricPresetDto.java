package com.marketinghub.experiment.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * DTO for {@link com.marketinghub.experiment.MetricPreset}.
 */
@Data
public class MetricPresetDto {
    private String id;
    private String name;
    private Integer sampleSize;
    private BigDecimal stopLossFactor;
    private BigDecimal defaultMdePp;
}
