package com.marketinghub.repository.jpa.pipeline;

import com.marketinghub.pipeline.PipelineStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA centralizado para persistir etapas de pipelines operacionais.
 */
public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {
    /**
     * Lista as etapas de um pipeline na ordem operacional configurada.
     */
    List<PipelineStage> findByPipelineIdOrderByPositionAscIdAsc(Long pipelineId);
}
