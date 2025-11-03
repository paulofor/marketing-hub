package com.marketinghub.experiment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    @JsonProperty("kpiTarget")
    @JsonAlias("kpiTargetCpl")
    private BigDecimal kpiTargetCpl;
    private String metricPresetId;
    private Integer sampleSize;
    private BigDecimal baselineCvr;
    private BigDecimal targetCvr;
    @JsonProperty("mde")
    @JsonAlias("mdePercent")
    private BigDecimal mdePercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer creativesToGenerate;
    private Integer instantFormsToGenerate;
    private Integer emailsToGenerate;
    private Integer deliverablesToGenerate;
    private Long journeyTemplateId;
    private Long facebookPageId;
    private Long facebookInstantFormId;
    private Long instagramAccountId;
    private String followUpActionUrl;
    private Long leadPortalFlowId;
}
