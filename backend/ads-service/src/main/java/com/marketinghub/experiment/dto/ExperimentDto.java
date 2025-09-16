package com.marketinghub.experiment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.experiment.ExperimentPlatform;
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
    private Long nicheId;
    private java.util.UUID hypothesisId;
    private String name;
    private String hypothesis;
    @JsonProperty("kpiTarget")
    private BigDecimal kpiTargetCpl;
    private BigDecimal stopLossCpl;
    private Integer sampleSize;
    private BigDecimal baselineCvr;
    private BigDecimal targetCvr;
    @JsonProperty("mde")
    private BigDecimal mdePercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExperimentStatus status;
    private ExperimentPlatform platform;
    private boolean audienceApproved;
    private boolean creativeApproved;
    private Instant createdAt;
    private Instant updatedAt;
    private String metricPresetId;
    private Integer creativesToGenerate;
    private java.util.UUID salesFunnelId;
    private String salesFunnelName;
}
