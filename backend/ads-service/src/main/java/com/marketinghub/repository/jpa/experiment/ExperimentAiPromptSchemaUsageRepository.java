package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.ExperimentAiPromptSchemaUsage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar associações de prompt/schema de IA usadas por experimentos. */
public interface ExperimentAiPromptSchemaUsageRepository extends JpaRepository<ExperimentAiPromptSchemaUsage, Long> {
    /** Busca uso existente para manter registro idempotente por experimento, etapa e contexto. */
    Optional<ExperimentAiPromptSchemaUsage> findByExperimentIdAndTemplateKeyAndUsageContextAndStageCode(
            Long experimentId,
            String templateKey,
            String usageContext,
            String stageCode);

    /** Conta quantos templates foram associados a um experimento em determinado pipeline. */
    long countByExperimentIdAndPipelineCode(Long experimentId, String pipelineCode);
}
