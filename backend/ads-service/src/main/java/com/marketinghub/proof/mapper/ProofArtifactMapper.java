package com.marketinghub.proof.mapper;

import com.marketinghub.proof.ProofArtifact;
import com.marketinghub.proof.dto.ProofArtifactDto;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper converting {@link ProofArtifact} entities to DTOs.
 */
@Mapper(componentModel = "spring")
public interface ProofArtifactMapper {
    @Mapping(target = "hypothesisId", source = "hypothesis.id")
    @Mapping(target = "experimentId", source = "experiment.id")
    @Mapping(target = "marketNicheId", source = "marketNiche.id")
    @Mapping(target = "marketNicheName", source = "marketNiche.name")
    @Mapping(target = "visualProofId", source = "visualProof.id")
    @Mapping(target = "visualProofName", source = "visualProof.name")
    @Mapping(target = "stage", expression = "java(entity.getStage() != null ? entity.getStage().name() : null)")
    @Mapping(target = "stageLabel", expression = "java(entity.getStage() != null ? entity.getStage().getDisplayName() : null)")
    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
    @Mapping(target = "typeLabel", ignore = true)
    ProofArtifactDto toDto(ProofArtifact entity);

    @AfterMapping
    default void fillTypeLabel(ProofArtifact entity, @MappingTarget ProofArtifactDto dto) {
        if (dto == null) {
            return;
        }
        if (entity.getCustomType() != null && !entity.getCustomType().isBlank()) {
            dto.setTypeLabel(entity.getCustomType());
        } else if (entity.getVisualProof() != null) {
            dto.setTypeLabel(entity.getVisualProof().getName());
        } else {
            dto.setTypeLabel(null);
        }
    }
}
