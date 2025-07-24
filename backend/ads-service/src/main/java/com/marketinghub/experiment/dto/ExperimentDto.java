package com.marketinghub.experiment.dto;

import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentPlatform;
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
    private Long nicheId;
    private java.util.UUID hypothesisId;
    private String name;
    private String hypothesis;
    private BigDecimal kpiTarget;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExperimentStatus status;
    private ExperimentPlatform platform;
    private Instant createdAt;
    private Instant updatedAt;
}
