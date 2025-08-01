package com.marketinghub.experiment.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Request body for creating an experiment.
 */
@Data
public class CreateExperimentRequest {
    private Long marketNicheId;
    private java.util.UUID hypothesisId;
    private String name;
    private String hypothesis;
    private BigDecimal kpiTargetCpl;
    private BigDecimal stopLossCpl;
    private Integer sampleSize;
    private BigDecimal baselineCvr;
    private BigDecimal targetCvr;
    private BigDecimal mdePercent;
    private LocalDate startDate;
    private LocalDate endDate;
}
