package com.marketinghub.experiment.mapper;

import com.marketinghub.ads.FacebookPage;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.ads.mapper.FacebookInstantFormMapper;
import com.marketinghub.experiment.Experiment;
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
    ExperimentDto toDto(Experiment experiment);

    @org.mapstruct.Mapping(target = "accountId", source = "account.id")
    FacebookPageDto toDto(FacebookPage page);

    InstagramAccountDto toDto(InstagramAccount account);
}
