package com.marketinghub.experiment.mapper;

import com.marketinghub.experiment.MetricPreset;
import com.marketinghub.experiment.dto.MetricPresetDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for MetricPreset.
 */
@Mapper(componentModel = "spring")
public interface MetricPresetMapper {
    MetricPresetDto toDto(MetricPreset preset);
}
