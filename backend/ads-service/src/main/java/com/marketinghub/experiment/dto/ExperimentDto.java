package com.marketinghub.experiment.dto;

import com.marketinghub.experiment.ExperimentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;

/**
 * DTO for Experiment.
 */
@Data
public class ExperimentDto {
    private Long id;
    private String hypothesis;
    private BigDecimal kpiGoal;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExperimentStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
