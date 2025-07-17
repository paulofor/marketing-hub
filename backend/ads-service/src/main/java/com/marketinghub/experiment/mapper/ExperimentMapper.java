package com.marketinghub.experiment.mapper;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Experiment.
 */
@Mapper(componentModel = "spring")
public interface ExperimentMapper {
    @org.mapstruct.Mapping(target = "nicheId", source = "niche.id")
    ExperimentDto toDto(Experiment experiment);
}
