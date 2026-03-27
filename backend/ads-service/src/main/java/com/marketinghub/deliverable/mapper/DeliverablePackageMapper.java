package com.marketinghub.deliverable.mapper;

import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.deliverable.dto.DeliverablePackageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper converting {@link DeliverablePackage} to DTOs.
 */
@Mapper(componentModel = "spring", uses = DeliverableMapper.class)
public interface DeliverablePackageMapper {
    @Mapping(target = "experimentId", source = "experiment.id")
    @Mapping(target = "experimentName", source = "experiment.name")
    @Mapping(target = "hypothesisId", source = "hypothesis.id")
    @Mapping(target = "hypothesisTitle", source = "hypothesis.title")
    @Mapping(target = "nicheId", expression = "java(resolveNicheId(entity))")
    @Mapping(target = "nicheName", expression = "java(resolveNicheName(entity))")
    DeliverablePackageDto toDto(DeliverablePackage entity);

    default Long resolveNicheId(DeliverablePackage entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getExperiment() != null && entity.getExperiment().getNiche() != null) {
            return entity.getExperiment().getNiche().getId();
        }
        if (entity.getHypothesis() != null && entity.getHypothesis().getMarketNiche() != null) {
            return entity.getHypothesis().getMarketNiche().getId();
        }
        return null;
    }

    default String resolveNicheName(DeliverablePackage entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getExperiment() != null && entity.getExperiment().getNiche() != null) {
            return entity.getExperiment().getNiche().getName();
        }
        if (entity.getHypothesis() != null && entity.getHypothesis().getMarketNiche() != null) {
            return entity.getHypothesis().getMarketNiche().getName();
        }
        return null;
    }
}
