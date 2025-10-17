package com.marketinghub.appidea.mapper;

import com.marketinghub.appidea.AppIdea;
import com.marketinghub.appidea.dto.AppIdeaDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link AppIdea} entities.
 */
@Mapper(componentModel = "spring")
public interface AppIdeaMapper {
    AppIdeaDto toDto(AppIdea idea);
}
