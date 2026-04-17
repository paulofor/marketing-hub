package com.marketinghub.experiment.mapper;

import com.marketinghub.ads.FacebookPage;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.ads.mapper.FacebookInstantFormMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.dto.ExperimentCampaignMetricDto;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.dto.FacebookPageDto;
import com.marketinghub.experiment.dto.InstagramAccountDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Experiment.
 */
@Mapper(componentModel = "spring", uses = FacebookInstantFormMapper.class)
public interface ExperimentMapper {
    @org.mapstruct.Mapping(target = "nicheId", source = "niche.id")
    @org.mapstruct.Mapping(target = "hypothesisId", source = "hypothesisRef.id")
    @org.mapstruct.Mapping(target = "metricPresetId", source = "metricPreset.id")
    @org.mapstruct.Mapping(target = "journeyTemplateId", source = "journeyTemplate.id")
    @org.mapstruct.Mapping(target = "journeyTemplateName", source = "journeyTemplate.name")
    @org.mapstruct.Mapping(target = "leadPortalFlowId", source = "leadPortalFlow.id")
    @org.mapstruct.Mapping(target = "leadPortalFlowName", source = "leadPortalFlow.name")
    @org.mapstruct.Mapping(target = "leadPortalFlowSlug", source = "leadPortalFlow.slug")
    @org.mapstruct.Mapping(target = "facebookPixelId", source = "niche.facebookPixelId")
    @org.mapstruct.Mapping(target = "facebookPixelCode", source = "niche.facebookPixelCode")
    @org.mapstruct.Mapping(target = "facebookPixelCreatedAt", source = "niche.facebookPixelCreatedAt")
    @org.mapstruct.Mapping(target = "imageModelId", source = "imageGenerationModel.id")
    @org.mapstruct.Mapping(target = "imageModelName", source = "imageGenerationModel.displayName")
    @org.mapstruct.Mapping(target = "imageModelQualityId", source = "imageGenerationQuality.id")
    @org.mapstruct.Mapping(target = "imageModelQualityName", source = "imageGenerationQuality.displayName")
    @org.mapstruct.Mapping(target = "selectedSampleEmailId", source = "selectedSampleEmail.id")
    @org.mapstruct.Mapping(target = "selectedSampleEmailSubject", source = "selectedSampleEmail.subject")
    @org.mapstruct.Mapping(target = "selectedSampleEmailPreviewText", source = "selectedSampleEmail.previewText")
    @org.mapstruct.Mapping(target = "selectedSampleEmailCallToAction", source = "selectedSampleEmail.callToAction")
    @org.mapstruct.Mapping(target = "selectedSampleEmailModel", source = "selectedSampleEmail.model")
    @org.mapstruct.Mapping(target = "selectedSampleEmailUpdatedAt", source = "selectedSampleEmail.updatedAt")
    @org.mapstruct.Mapping(target = "campaignMetric", expression = "java(toCampaignMetricDto(experiment.getCampaignMetric()))")
    ExperimentDto toDto(Experiment experiment);

    @org.mapstruct.Mapping(target = "accountId", source = "account.id")
    FacebookPageDto toDto(FacebookPage page);

    InstagramAccountDto toDto(InstagramAccount account);

    default ExperimentCampaignMetricDto toCampaignMetricDto(ExperimentCampaignMetric metric) {
        if (metric == null) {
            return null;
        }
        ExperimentCampaignMetricDto dto = new ExperimentCampaignMetricDto();
        dto.setDateStart(metric.getDateStart());
        dto.setDateStop(metric.getDateStop());
        dto.setImpressions(metric.getImpressions());
        dto.setClicks(metric.getClicks());
        dto.setLeads(metric.getLeads());
        dto.setSpend(metric.getSpend());
        dto.setCpc(metric.getCpc());
        dto.setCpl(metric.getCpl());
        if (metric.getCampaign() != null) {
            dto.setLastSyncedAt(metric.getCampaign().getMetricsLastSyncedAt());
            dto.setLastSyncError(metric.getCampaign().getMetricsLastError());
        }
        return dto;
    }
}
