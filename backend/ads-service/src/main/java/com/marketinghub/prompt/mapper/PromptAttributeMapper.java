package com.marketinghub.prompt.mapper;

import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.dto.PromptAttributeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromptAttributeMapper {
    @Mapping(target = "name", source = "attr.name")
    @Mapping(target = "description", source = "desc.description")
    @Mapping(target = "version", source = "desc.version")
    PromptAttributeDto toDto(PromptAttribute attr, PromptAttributeDescription desc);
}
