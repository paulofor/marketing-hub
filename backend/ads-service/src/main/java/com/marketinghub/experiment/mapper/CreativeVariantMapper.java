package com.marketinghub.experiment.mapper;

import com.marketinghub.experiment.CreativeVariant;
import com.marketinghub.experiment.dto.CreativeVariantDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for CreativeVariant.
 */
@Mapper(componentModel = "spring")
public interface CreativeVariantMapper {
    @Mapping(target = "experimentId", source = "experiment.id")
    CreativeVariantDto toDto(CreativeVariant creative);
}
