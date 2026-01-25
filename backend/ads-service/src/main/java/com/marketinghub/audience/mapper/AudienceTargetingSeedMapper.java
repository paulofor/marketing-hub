package com.marketinghub.audience.mapper;

import com.marketinghub.audience.AudienceTargetingSeed;
import com.marketinghub.audience.dto.AudienceTargetingSeedDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AudienceTargetingSeedMapper {
    @Mapping(target = "id", source = "id")
    AudienceTargetingSeedDto toDto(AudienceTargetingSeed seed);
}
