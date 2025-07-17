package com.marketinghub.experiment.mapper;

import com.marketinghub.experiment.AdSet;
import com.marketinghub.experiment.dto.AdSetDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for AdSet.
 */
@Mapper(componentModel = "spring")
public interface AdSetMapper {
    @Mapping(target = "experimentId", source = "experiment.id")
    AdSetDto toDto(AdSet adSet);
}
