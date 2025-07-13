package com.marketinghub.ai.mapper;

import com.marketinghub.ai.AiService;
import com.marketinghub.ai.dto.AiServiceDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link AiService}.
 */
@Mapper(componentModel = "spring")
public interface AiServiceMapper {
    AiServiceDto toDto(AiService service);
}
