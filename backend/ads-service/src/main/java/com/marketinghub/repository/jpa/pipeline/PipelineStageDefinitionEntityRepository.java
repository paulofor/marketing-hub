package com.marketinghub.repository.jpa.pipeline;

import com.marketinghub.pipeline.PipelineStageDefinitionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA centralizado para definições persistentes de etapas oficiais de pipeline.
 */
public interface PipelineStageDefinitionEntityRepository extends JpaRepository<PipelineStageDefinitionEntity, Long> {
    /**
     * Busca uma definição de etapa pelo pipeline persistido e código canônico.
     */
    Optional<PipelineStageDefinitionEntity> findByPipelineDefinitionIdAndCanonicalCode(
            Long pipelineDefinitionId, String canonicalCode);
}
