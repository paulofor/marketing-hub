package com.marketinghub.niche.mapper;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.dto.MarketNicheDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link MarketNiche}.
 */
@Mapper(componentModel = "spring")
public interface MarketNicheMapper {
    MarketNicheDto toDto(MarketNiche niche);
}
