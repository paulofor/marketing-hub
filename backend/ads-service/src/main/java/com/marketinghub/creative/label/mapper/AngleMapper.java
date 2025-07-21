package com.marketinghub.creative.label.mapper;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.dto.AngleDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AngleMapper {
    AngleDto toDto(Angle angle);
}
