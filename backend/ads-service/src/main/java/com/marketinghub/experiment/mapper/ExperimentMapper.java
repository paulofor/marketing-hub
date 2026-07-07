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
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;

/**
 * Mapeia experimentos para contratos usados pela interface.
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
    @org.mapstruct.Mapping(target = "auditableTotalCost", ignore = true)
    @org.mapstruct.Mapping(target = "legacyTotalCost", ignore = true)
    @org.mapstruct.Mapping(target = "unreconciledLegacyCost", ignore = true)
    @org.mapstruct.Mapping(target = "campaignMetric", expression = "java(toCampaignMetricDto(experiment.getCampaignMetric()))")
    ExperimentDto toDto(Experiment experiment);

    /** Completa campos financeiros reconciliados depois do mapeamento principal. */
    @AfterMapping
    default void fillCostReconciliation(Experiment experiment, @MappingTarget ExperimentDto dto) {
        if (experiment == null || dto == null) {
            return;
        }
        BigDecimal auditableTotal = money(experiment.getCost())
                .add(money(experiment.getExpense()))
                .add(money(experiment.getCampaignMetric() != null
                        ? experiment.getCampaignMetric().getSpend()
                        : null))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal legacyTotal = money(experiment.getTotalCost()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unreconciled = legacyTotal.subtract(auditableTotal).setScale(2, RoundingMode.HALF_UP);
        dto.setAuditableTotalCost(auditableTotal);
        dto.setLegacyTotalCost(legacyTotal);
        dto.setUnreconciledLegacyCost(unreconciled.compareTo(BigDecimal.ZERO) > 0 ? unreconciled : BigDecimal.ZERO);
    }

    /** Converte valor monetário opcional para zero quando ausente. */
    default BigDecimal money(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Mapeia página do Facebook vinculada ao experimento. */
    @org.mapstruct.Mapping(target = "accountId", source = "account.id")
    FacebookPageDto toDto(FacebookPage page);

    /** Mapeia conta do Instagram vinculada ao experimento. */
    InstagramAccountDto toDto(InstagramAccount account);

    /** Mapeia métricas agregadas da campanha do experimento. */
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
