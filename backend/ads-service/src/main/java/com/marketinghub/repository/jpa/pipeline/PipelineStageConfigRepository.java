package com.marketinghub.repository.jpa.pipeline;

import com.marketinghub.pipeline.PipelineStageConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA centralizado para configurações operacionais editáveis de etapas de pipeline.
 */
public interface PipelineStageConfigRepository extends JpaRepository<PipelineStageConfig, Long> {
    /**
     * Busca a configuração operacional ligada a uma definição persistente de etapa.
     */
    Optional<PipelineStageConfig> findByPipelineStageDefinitionId(Long pipelineStageDefinitionId);
}
