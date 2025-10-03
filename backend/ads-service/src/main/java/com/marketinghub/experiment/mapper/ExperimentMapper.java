package com.marketinghub.experiment.mapper;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.experiment.dto.FacebookPageDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Experiment.
 */
@Mapper(componentModel = "spring")
public interface ExperimentMapper {
    @org.mapstruct.Mapping(target = "nicheId", source = "niche.id")
    @org.mapstruct.Mapping(target = "hypothesisId", source = "hypothesisRef.id")
    @org.mapstruct.Mapping(target = "metricPresetId", source = "metricPreset.id")
    @org.mapstruct.Mapping(target = "salesFunnelId", source = "salesFunnel.id")
    @org.mapstruct.Mapping(target = "salesFunnelName", source = "salesFunnel.name")
    ExperimentDto toDto(Experiment experiment);

    @org.mapstruct.Mapping(target = "accountId", source = "account.id")
    FacebookPageDto toDto(FacebookPage page);
}
