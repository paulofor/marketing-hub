package com.marketinghub.repository.jpa.pipeline;

import com.marketinghub.pipeline.PipelineStageConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório JPA centralizado para configurações operacionais editáveis de etapas de pipeline.
 */
public interface PipelineStageConfigRepository extends JpaRepository<PipelineStageConfig, Long> {
    /**
     * Busca a configuração operacional ligada a uma definição persistente de etapa.
     */
    Optional<PipelineStageConfig> findByPipelineStageDefinitionId(Long pipelineStageDefinitionId);

    /**
     * Busca a configuração operacional de uma etapa oficial pelo código do pipeline, versão canônica e etapa.
     */
    @Query("""
            select config
            from PipelineStageConfig config
            join fetch config.pipelineStageDefinition stageDefinition
            join fetch stageDefinition.pipelineDefinition pipelineDefinition
            left join fetch config.openAiModel
            where pipelineDefinition.code = :pipelineCode
              and pipelineDefinition.canonicalVersion = :canonicalVersion
              and stageDefinition.canonicalCode = :canonicalCode
            """)
    Optional<PipelineStageConfig> findOfficialStageConfig(
            @Param("pipelineCode") String pipelineCode,
            @Param("canonicalVersion") String canonicalVersion,
            @Param("canonicalCode") String canonicalCode);
}
