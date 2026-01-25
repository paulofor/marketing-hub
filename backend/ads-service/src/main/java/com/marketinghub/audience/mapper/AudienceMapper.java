package com.marketinghub.audience.mapper;

import com.marketinghub.audience.Audience;
import com.marketinghub.audience.dto.AudienceDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link Audience}.
 */
@Mapper(componentModel = "spring", uses = AudienceTargetingSeedMapper.class)
public interface AudienceMapper {
    @Mapping(target = "marketNicheId", source = "niche.id")
    @Mapping(target = "hypothesisId", source = "hypothesis.id")
    @Mapping(target = "seeds", source = "targetingSeeds")
    AudienceDto toDto(Audience audience);
}
