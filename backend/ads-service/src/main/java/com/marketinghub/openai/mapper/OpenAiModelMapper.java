package com.marketinghub.openai.mapper;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.dto.OpenAiModelDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OpenAiModelMapper {
    OpenAiModelDto toDto(OpenAiModel model);
}
