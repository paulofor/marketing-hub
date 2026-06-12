package com.marketinghub.repository.jpa.pipeline;

import com.marketinghub.pipeline.PipelineStage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório JPA centralizado para persistir etapas de pipelines operacionais.
 */
public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {
    /**
     * Lista as etapas de um pipeline na ordem operacional configurada.
     */
    List<PipelineStage> findByPipelineIdOrderByPositionAscIdAsc(Long pipelineId);

    /**
     * Busca uma etapa operacional pelo código do pipeline legado e pelo código da etapa.
     */
    @Query("""
            select stage
            from PipelineStage stage
            join fetch stage.pipeline pipeline
            left join fetch stage.openAiModel
            where pipeline.code = :pipelineCode
              and stage.code = :stageCode
            """)
    Optional<PipelineStage> findByPipelineCodeAndStageCode(
            @Param("pipelineCode") String pipelineCode, @Param("stageCode") String stageCode);
}
