package com.marketinghub.creative.mapper;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreativeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Responsabilidade: converter criativos do domínio para DTOs de API.
 */
@Mapper(componentModel = "spring")
public interface CreativeMapper {
    /** Converte a entidade de criativo para o DTO usado pela API. */
    @Mapping(target = "experimentId", source = "experiment.id")
    CreativeDto toDto(Creative creative);
}
