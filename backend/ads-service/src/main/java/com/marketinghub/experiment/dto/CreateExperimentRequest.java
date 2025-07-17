package com.marketinghub.experiment.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Request body for creating an experiment.
 */
@Data
public class CreateExperimentRequest {
    private String hypothesis;
    private BigDecimal kpiGoal;
    private LocalDate startDate;
    private LocalDate endDate;
}
