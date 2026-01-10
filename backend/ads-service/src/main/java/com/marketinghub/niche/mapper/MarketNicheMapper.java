package com.marketinghub.niche.mapper;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.dto.MarketNicheDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link MarketNiche}.
 */
@Mapper(componentModel = "spring")
public interface MarketNicheMapper {
    @Mapping(target = "chatDialogId", source = "chatDialog.id")
    @Mapping(target = "differentiatedTechnologyId", source = "differentiatedTechnology.id")
    MarketNicheDto toDto(MarketNiche niche);
}
