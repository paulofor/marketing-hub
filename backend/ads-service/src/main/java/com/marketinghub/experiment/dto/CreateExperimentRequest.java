package com.marketinghub.experiment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import com.marketinghub.experiment.ExperimentStage;
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
    private ExperimentStage stage;
    private String primaryVariable;
    private String primaryMetric;
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
    private BigDecimal dailyBudget;
    private BigDecimal unitPrice;
    private BigDecimal cost;
    private BigDecimal expense;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer creativesToGenerate;
    private Integer instantFormsToGenerate;
    private Integer emailsToGenerate;
    private Integer sampleEmailsToGenerate;
    private Integer deliverablesToGenerate;
    private Integer leadPortalFlowsToGenerate;
    private Integer imagesPerPackage;
    private Integer openImagesPerPackage;
    private Integer compressedImagesPerPackage;
    private Long journeyTemplateId;
    private Long facebookPageId;
    private Long facebookInstantFormId;
    private Long instagramAccountId;
    private String followUpActionUrl;
    private String leadPortalFlowModel;
    private Boolean schemaFirstLeadPortalEnabled;
    private Long leadPortalFlowId;
    private Long imageModelId;
    private Long imageModelQualityId;
    private String creativeTextPrompt;
    private String creativeImagePrompt;
}
