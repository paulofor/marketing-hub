package com.marketinghub.hypothesis.mapper;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkMapperSupport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Responsabilidade: converter a entidade de hipótese em seu contrato público. */
@Mapper(componentModel = "spring", uses = HypothesisFrameworkMapperSupport.class)
public interface HypothesisMapper {
  /** Converte uma hipótese persistida em DTO, incluindo produto, nicho e framework. */
  @Mapping(target = "framework", source = ".", qualifiedByName = "frameworkFromHypothesis")
  @Mapping(target = "marketNicheId", source = "marketNiche.id")
  @Mapping(target = "productId", source = "product.id")
  @Mapping(target = "productName", source = "product.name")
  @Mapping(target = "sourceHypothesisId", source = "sourceHypothesis.id")
  @Mapping(target = "rootHypothesisId", source = "rootHypothesis.id")
  @Mapping(target = "premiseAngleId", source = "premiseAngle.id")
  @Mapping(target = "offerPackageId", source = "offerPackage.id")
  @Mapping(target = "offerPackageName", source = "offerPackage.name")
  @Mapping(
      target = "promptAttributeDescriptionIds",
      expression =
          "java(hypothesis.getPromptAttributeDescriptions().stream().map(com.marketinghub.prompt.PromptAttributeDescription::getId).toList())")
  @Mapping(target = "createdAt", source = "createdAt")
  HypothesisDto toDto(Hypothesis hypothesis);
}
