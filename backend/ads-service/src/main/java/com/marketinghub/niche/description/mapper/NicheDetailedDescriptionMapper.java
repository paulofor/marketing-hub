package com.marketinghub.niche.description.mapper;

import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.niche.description.dto.NicheDetailedDescriptionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper para descrições detalhadas de nicho.
 */
@Mapper(componentModel = "spring")
public interface NicheDetailedDescriptionMapper {
    @Mapping(target = "marketNicheId", source = "marketNiche.id")
    NicheDetailedDescriptionDto toDto(NicheDetailedDescription description);
}
