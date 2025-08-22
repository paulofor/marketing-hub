package com.marketinghub.prompt.mapper;

import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.dto.PromptEntityDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromptEntityMapper {
    PromptEntityDto toDto(PromptEntity entity);
}
