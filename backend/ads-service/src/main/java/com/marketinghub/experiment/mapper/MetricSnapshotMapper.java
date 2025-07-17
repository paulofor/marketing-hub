package com.marketinghub.experiment.mapper;

import com.marketinghub.experiment.MetricSnapshot;
import com.marketinghub.experiment.dto.MetricSnapshotDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for MetricSnapshot.
 */
@Mapper(componentModel = "spring")
public interface MetricSnapshotMapper {
    @Mapping(target = "creativeId", source = "creative.id")
    @Mapping(target = "adSetId", source = "adSet.id")
    MetricSnapshotDto toDto(MetricSnapshot snapshot);
}
