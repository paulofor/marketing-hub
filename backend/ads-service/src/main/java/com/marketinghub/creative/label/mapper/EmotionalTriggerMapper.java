package com.marketinghub.creative.label.mapper;

import com.marketinghub.creative.label.EmotionalTrigger;
import com.marketinghub.creative.label.dto.EmotionalTriggerDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmotionalTriggerMapper {
    EmotionalTriggerDto toDto(EmotionalTrigger trigger);
}
