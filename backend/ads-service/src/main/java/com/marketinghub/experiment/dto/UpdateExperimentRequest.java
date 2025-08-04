package com.marketinghub.experiment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for updating an experiment.
 */
@Data
public class UpdateExperimentRequest {
    private String name;
    private String hypothesis;
    /** KPI target in CPL. */
    private BigDecimal kpiTarget;
    private String metricPresetId;
    private Integer sampleSize;
    private BigDecimal mde;
    private LocalDate startDate;
    private LocalDate endDate;
}
