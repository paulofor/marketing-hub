package com.marketinghub.experiment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * Request body for updating an experiment.
 */
@Data
public class UpdateExperimentRequest {
    private String name;
    private String hypothesis;
    @JsonProperty("kpiTarget")
    private BigDecimal kpiTargetCpl;
    private String metricPresetId;
    private Integer sampleSize;
    @JsonProperty("mde")
    private BigDecimal mdePercent;
    private BigDecimal dailyBudget;
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
    @JsonIgnore
    private boolean openImagesPerPackagePresent;
    private Integer compressedImagesPerPackage;
    @JsonIgnore
    private boolean compressedImagesPerPackagePresent;
    private Boolean creativeApproved;
    private Long journeyTemplateId;
    @JsonIgnore
    private boolean journeyTemplateIdPresent;
    private Long facebookPageId;
    @JsonIgnore
    private boolean facebookPageIdPresent;
    private Long facebookInstantFormId;
    @JsonIgnore
    private boolean facebookInstantFormIdPresent;
    private Long instagramAccountId;
    @JsonIgnore
    private boolean instagramAccountIdPresent;
    private String followUpActionUrl;
    @JsonIgnore
    private boolean followUpActionUrlPresent;
    @JsonIgnore
    private boolean dailyBudgetPresent;
    private Long leadPortalFlowId;
    @JsonIgnore
    private boolean leadPortalFlowIdPresent;
    private Long imageModelId;
    @JsonIgnore
    private boolean imageModelIdPresent;
    private Long imageModelQualityId;
    @JsonIgnore
    private boolean imageModelQualityIdPresent;

    @JsonSetter(value = "facebookPageId", nulls = Nulls.SET)
    public void setFacebookPageId(Long facebookPageId) {
        this.facebookPageId = facebookPageId;
        this.facebookPageIdPresent = true;
    }

    @JsonSetter(value = "journeyTemplateId", nulls = Nulls.SET)
    public void setJourneyTemplateId(Long journeyTemplateId) {
        this.journeyTemplateId = journeyTemplateId;
        this.journeyTemplateIdPresent = true;
    }

    @JsonSetter(value = "facebookInstantFormId", nulls = Nulls.SET)
    public void setFacebookInstantFormId(Long facebookInstantFormId) {
        this.facebookInstantFormId = facebookInstantFormId;
        this.facebookInstantFormIdPresent = true;
    }

    @JsonSetter(value = "instagramAccountId", nulls = Nulls.SET)
    public void setInstagramAccountId(Long instagramAccountId) {
        this.instagramAccountId = instagramAccountId;
        this.instagramAccountIdPresent = true;
    }

    @JsonSetter(value = "followUpActionUrl", nulls = Nulls.SET)
    public void setFollowUpActionUrl(String followUpActionUrl) {
        this.followUpActionUrl = followUpActionUrl;
        this.followUpActionUrlPresent = true;
    }

    @JsonSetter(value = "openImagesPerPackage", nulls = Nulls.SET)
    public void setOpenImagesPerPackage(Integer openImagesPerPackage) {
        this.openImagesPerPackage = openImagesPerPackage;
        this.openImagesPerPackagePresent = true;
    }

    @JsonSetter(value = "compressedImagesPerPackage", nulls = Nulls.SET)
    public void setCompressedImagesPerPackage(Integer compressedImagesPerPackage) {
        this.compressedImagesPerPackage = compressedImagesPerPackage;
        this.compressedImagesPerPackagePresent = true;
    }

    @JsonSetter(value = "leadPortalFlowId", nulls = Nulls.SET)
    public void setLeadPortalFlowId(Long leadPortalFlowId) {
        this.leadPortalFlowId = leadPortalFlowId;
        this.leadPortalFlowIdPresent = true;
    }

    @JsonSetter(value = "dailyBudget", nulls = Nulls.SET)
    public void setDailyBudget(BigDecimal dailyBudget) {
        this.dailyBudget = dailyBudget;
        this.dailyBudgetPresent = true;
    }
    @JsonSetter(value = "imageModelId", nulls = Nulls.SET)
    public void setImageModelId(Long imageModelId) {
        this.imageModelId = imageModelId;
        this.imageModelIdPresent = true;
    }

    @JsonSetter(value = "imageModelQualityId", nulls = Nulls.SET)
    public void setImageModelQualityId(Long imageModelQualityId) {
        this.imageModelQualityId = imageModelQualityId;
        this.imageModelQualityIdPresent = true;
    }

}

