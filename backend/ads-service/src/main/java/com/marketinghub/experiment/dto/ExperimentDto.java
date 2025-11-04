package com.marketinghub.experiment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.ads.dto.FacebookInstantFormDto;
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
    private FacebookPageDto facebookPage;
    private FacebookInstantFormDto facebookInstantForm;
    private String followUpActionUrl;
    private InstagramAccountDto instagramAccount;
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
    private boolean creativeApproved;
    private Instant createdAt;
    private Instant updatedAt;
    private String metricPresetId;
    private Integer creativesToGenerate;
    private Integer instantFormsToGenerate;
    private Integer emailsToGenerate;
    private Integer deliverablesToGenerate;
    private Integer leadPortalFlowsToGenerate;
    private Long journeyTemplateId;
    private String journeyTemplateName;
    private Long leadPortalFlowId;
    private String leadPortalFlowName;
    private String leadPortalFlowSlug;
}
