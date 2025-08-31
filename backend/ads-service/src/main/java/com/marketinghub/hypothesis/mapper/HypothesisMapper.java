package com.marketinghub.hypothesis.mapper;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HypothesisMapper {
    @Mapping(target = "marketNicheId", source = "marketNiche.id")
    @Mapping(target = "premiseAngleId", source = "premiseAngle.id")
    @Mapping(target = "promptAttributeDescriptionIds", expression = "java(hypothesis.getPromptAttributeDescriptions().stream().map(com.marketinghub.prompt.PromptAttributeDescription::getId).toList())")
    @Mapping(target = "createdAt", source = "createdAt")
    HypothesisDto toDto(Hypothesis hypothesis);
}
