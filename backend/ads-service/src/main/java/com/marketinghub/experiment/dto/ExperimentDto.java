package com.marketinghub.experiment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.ads.dto.FacebookInstantFormDto;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.CreativeGenerationMode;
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
    private String leadPortalFlowModel;
    private boolean schemaFirstLeadPortalEnabled;
    private String creativeTextPrompt;
    private String creativeImagePrompt;
    private String campaignAngle;
    private String adCopy;
    private String adImageBriefing;
    private String landingPageCopy;
    private String landingPageWireframe;
    private String landingPageImagePlanning;
    private String landingPageImageAssets;
    private String landingPageDesignPreset;
    private String htmlGeraLanding;
    private String landingPageQualityReview;
    private String landingPageDeliverables;
    private String landingPageHtml;
    private InstagramAccountDto instagramAccount;
    private String facebookPixelId;
    private String facebookPixelCode;
    private Instant facebookPixelCreatedAt;
    private Instant facebookReleaseRequestedAt;
    @JsonProperty("kpiTarget")
    private BigDecimal kpiTargetCpl;
    private BigDecimal stopLossCpl;
    private Integer sampleSize;
    private BigDecimal baselineCvr;
    private BigDecimal targetCvr;
    @JsonProperty("mde")
    private BigDecimal mdePercent;
    private BigDecimal dailyBudget;
    private BigDecimal unitPrice;
    private BigDecimal cost;
    private BigDecimal totalCost;
    private BigDecimal expense;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExperimentStatus status;
    private ExperimentPlatform platform;
    private ExperimentStage stage;
    private CreativeGenerationMode creativeGenerationMode;
    private String primaryVariable;
    private String primaryMetric;
    private boolean creativeApproved;
    private Instant createdAt;
    private Instant updatedAt;
    private String metricPresetId;
    private Integer creativesToGenerate;
    private Integer instantFormsToGenerate;
    private Integer emailsToGenerate;
    private Integer sampleEmailsToGenerate;
    private Integer deliverablesToGenerate;
    private Integer leadPortalFlowsToGenerate;
    private Integer imagesPerPackage;
    private Integer openImagesPerPackage;
    private Integer compressedImagesPerPackage;
    private Long imageModelId;
    private String imageModelName;
    private Long imageModelQualityId;
    private String imageModelQualityName;
    private Long journeyTemplateId;
    private String journeyTemplateName;
    private Long leadPortalFlowId;
    private String leadPortalFlowName;
    private String leadPortalFlowSlug;
    private Long selectedSampleEmailId;
    private String selectedSampleEmailSubject;
    private String selectedSampleEmailPreviewText;
    private String selectedSampleEmailCallToAction;
    private String selectedSampleEmailModel;
    private Instant selectedSampleEmailUpdatedAt;
    private ExperimentCampaignMetricDto campaignMetric;
}
