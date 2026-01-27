package com.marketinghub.targeting.mapper;

import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.dto.TargetingElementDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper para {@link TargetingElement}.
 */
@Mapper(componentModel = "spring")
public interface TargetingElementMapper {
    @Mapping(target = "marketNicheId", source = "niche.id")
    @Mapping(target = "hypothesisId", source = "hypothesis.id")
    TargetingElementDto toDto(TargetingElement element);
}
