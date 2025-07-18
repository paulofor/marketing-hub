package com.marketinghub.creative.mapper;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreativeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Creative.
 */
@Mapper(componentModel = "spring")
public interface CreativeMapper {
    @Mapping(target = "experimentId", source = "experiment.id")
    CreativeDto toDto(Creative creative);
}
