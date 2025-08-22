package com.marketinghub.prompt.mapper;

import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.dto.PromptAttributeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromptAttributeMapper {
    @Mapping(target = "name", source = "name")
    PromptAttributeDto toDto(PromptAttribute attr);
}
