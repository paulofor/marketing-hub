package com.marketinghub.hypothesis.mapper;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HypothesisMapper {
    @Mapping(target = "experimentId", source = "experiment.id")
    @Mapping(target = "premiseAngleId", source = "premiseAngle.id")
    HypothesisDto toDto(Hypothesis hypothesis);
}
