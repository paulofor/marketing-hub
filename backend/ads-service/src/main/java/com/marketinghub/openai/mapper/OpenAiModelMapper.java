package com.marketinghub.openai.mapper;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.dto.OpenAiModelDto;
import org.mapstruct.Mapper;

/** Responsabilidade: converter entidades de modelo OpenAI para DTOs do contrato HTTP. */
@Mapper(componentModel = "spring")
public interface OpenAiModelMapper {
    /** Converte um modelo persistido para DTO de leitura usado pelas telas administrativas. */
    OpenAiModelDto toDto(OpenAiModel model);
}
