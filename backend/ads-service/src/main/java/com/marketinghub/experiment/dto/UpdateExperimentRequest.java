package com.marketinghub.experiment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.productai.ProductAiSubtype;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * Recebe os dados mutáveis de um experimento comercial.
 */
@Data
public class UpdateExperimentRequest {
    private String name;
    private String hypothesis;
    private String singlePain;
    @JsonIgnore
    private boolean singlePainPresent;
    private String freeReward;
    @JsonIgnore
    private boolean freeRewardPresent;
    private String funnelPromise;
    @JsonIgnore
    private boolean funnelPromisePresent;
    private String primaryCta;
    @JsonIgnore
    private boolean primaryCtaPresent;
    private ExperimentType experimentType;
    @JsonIgnore
    private boolean experimentTypePresent;
    private ProductAiSubtype productAiSubtype;
    @JsonIgnore
    private boolean productAiSubtypePresent;
    private ExperimentCampaignObjective campaignObjective;
    @JsonIgnore
    private boolean campaignObjectivePresent;
    private ExperimentStage stage;
    private String primaryVariable;
    private String primaryMetric;
    @JsonProperty("kpiTarget")
    private BigDecimal kpiTargetCpl;
    private String metricPresetId;
    private Integer sampleSize;
    @JsonProperty("mde")
    private BigDecimal mdePercent;
    private BigDecimal dailyBudget;
    private BigDecimal unitPrice;
    @JsonIgnore
    private boolean unitPricePresent;
    private BigDecimal cost;
    @JsonIgnore
    private boolean costPresent;
    private BigDecimal expense;
    @JsonIgnore
    private boolean expensePresent;
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
    private String leadPortalFlowModel;
    @JsonIgnore
    private boolean leadPortalFlowModelPresent;
    private Boolean schemaFirstLeadPortalEnabled;
    @JsonIgnore
    private boolean schemaFirstLeadPortalEnabledPresent;
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
    private String creativeTextPrompt;
    @JsonIgnore
    private boolean creativeTextPromptPresent;
    private String creativeImagePrompt;
    @JsonIgnore
    private boolean creativeImagePromptPresent;

    /** Registra a presença da dor única no payload de atualização. */
    @JsonSetter(value = "singlePain", nulls = Nulls.SET)
    public void setSinglePain(String singlePain) {
        this.singlePain = singlePain;
        this.singlePainPresent = true;
    }

    /** Registra a presença da recompensa gratuita no payload de atualização. */
    @JsonSetter(value = "freeReward", nulls = Nulls.SET)
    public void setFreeReward(String freeReward) {
        this.freeReward = freeReward;
        this.freeRewardPresent = true;
    }

    /** Registra a presença da promessa do funil no payload de atualização. */
    @JsonSetter(value = "funnelPromise", nulls = Nulls.SET)
    public void setFunnelPromise(String funnelPromise) {
        this.funnelPromise = funnelPromise;
        this.funnelPromisePresent = true;
    }

    /** Registra a presença do CTA principal no payload de atualização. */
    @JsonSetter(value = "primaryCta", nulls = Nulls.SET)
    public void setPrimaryCta(String primaryCta) {
        this.primaryCta = primaryCta;
        this.primaryCtaPresent = true;
    }

    /** Registra a presença do tipo comercial do experimento no payload de atualização. */
    @JsonSetter(value = "experimentType", nulls = Nulls.SET)
    public void setExperimentType(ExperimentType experimentType) {
        this.experimentType = experimentType;
        this.experimentTypePresent = true;
    }

    /** Registra a presença do subtipo de Produto IA no payload de atualização. */
    @JsonSetter(value = "productAiSubtype", nulls = Nulls.SET)
    public void setProductAiSubtype(ProductAiSubtype productAiSubtype) {
        this.productAiSubtype = productAiSubtype;
        this.productAiSubtypePresent = true;
    }

    /** Registra a presença do objetivo de campanha no payload de atualização. */
    @JsonSetter(value = "campaignObjective", nulls = Nulls.SET)
    public void setCampaignObjective(ExperimentCampaignObjective campaignObjective) {
        this.campaignObjective = campaignObjective;
        this.campaignObjectivePresent = true;
    }


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

    @JsonSetter(value = "leadPortalFlowModel", nulls = Nulls.SET)
    public void setLeadPortalFlowModel(String leadPortalFlowModel) {
        this.leadPortalFlowModel = leadPortalFlowModel;
        this.leadPortalFlowModelPresent = true;
    }

    @JsonSetter(value = "schemaFirstLeadPortalEnabled", nulls = Nulls.SET)
    public void setSchemaFirstLeadPortalEnabled(Boolean schemaFirstLeadPortalEnabled) {
        this.schemaFirstLeadPortalEnabled = schemaFirstLeadPortalEnabled;
        this.schemaFirstLeadPortalEnabledPresent = true;
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

    @JsonSetter(value = "unitPrice", nulls = Nulls.SET)
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        this.unitPricePresent = true;
    }

    @JsonSetter(value = "cost", nulls = Nulls.SET)
    public void setCost(BigDecimal cost) {
        this.cost = cost;
        this.costPresent = true;
    }

    @JsonSetter(value = "expense", nulls = Nulls.SET)
    public void setExpense(BigDecimal expense) {
        this.expense = expense;
        this.expensePresent = true;
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

    @JsonSetter(value = "creativeTextPrompt", nulls = Nulls.SET)
    public void setCreativeTextPrompt(String creativeTextPrompt) {
        this.creativeTextPrompt = creativeTextPrompt;
        this.creativeTextPromptPresent = true;
    }

    @JsonSetter(value = "creativeImagePrompt", nulls = Nulls.SET)
    public void setCreativeImagePrompt(String creativeImagePrompt) {
        this.creativeImagePrompt = creativeImagePrompt;
        this.creativeImagePromptPresent = true;
    }

}
