package com.marketinghub.experiment.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Request body for updating an experiment.
 */
@Data
public class UpdateExperimentRequest {
    private String name;
    private String hypothesis;
    private BigDecimal kpiTargetCpl;
    private String metricPresetId;
    private Integer sampleSize;
    private BigDecimal baselineCvr;
    private BigDecimal targetCvr;
    private BigDecimal mdePercent;
    private LocalDate startDate;
    private LocalDate endDate;
}
