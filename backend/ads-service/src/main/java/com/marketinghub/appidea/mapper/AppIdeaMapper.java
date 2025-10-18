package com.marketinghub.appidea.mapper;

import com.marketinghub.appidea.AppIdea;
import com.marketinghub.appidea.dto.AppIdeaDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link AppIdea} entities.
 */
@Mapper(componentModel = "spring")
public interface AppIdeaMapper {
    @Mapping(target = "nicheId", source = "niche.id")
    @Mapping(target = "nicheName", source = "niche.name")
    AppIdeaDto toDto(AppIdea idea);
}
