package com.marketinghub.informationsource.mapper;

import com.marketinghub.informationsource.InformationSource;
import com.marketinghub.informationsource.dto.InformationSourceDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link InformationSource}.
 */
@Mapper(componentModel = "spring")
public interface InformationSourceMapper {
    @Mapping(target = "marketNicheId", source = "niche.id")
    InformationSourceDto toDto(InformationSource source);
}
