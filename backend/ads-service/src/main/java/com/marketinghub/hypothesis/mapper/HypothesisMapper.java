package com.marketinghub.hypothesis.mapper;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkMapperSupport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = HypothesisFrameworkMapperSupport.class)
public interface HypothesisMapper {
    @Mapping(target = "framework", source = ".", qualifiedByName = "frameworkFromHypothesis")
    @Mapping(target = "marketNicheId", source = "marketNiche.id")
    @Mapping(target = "premiseAngleId", source = "premiseAngle.id")
    @Mapping(target = "offerPackageId", source = "offerPackage.id")
    @Mapping(target = "offerPackageName", source = "offerPackage.name")
    @Mapping(target = "promptAttributeDescriptionIds", expression = "java(hypothesis.getPromptAttributeDescriptions().stream().map(com.marketinghub.prompt.PromptAttributeDescription::getId).toList())")
    @Mapping(target = "createdAt", source = "createdAt")
    HypothesisDto toDto(Hypothesis hypothesis);
}
