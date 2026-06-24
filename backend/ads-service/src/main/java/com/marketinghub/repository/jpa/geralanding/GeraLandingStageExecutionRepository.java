package com.marketinghub.repository.jpa.geralanding;

import com.marketinghub.geralanding.GeraLandingStageExecution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Repositório responsável por consultar e persistir execuções das etapas do GeraLanding. */
public interface GeraLandingStageExecutionRepository extends JpaRepository<GeraLandingStageExecution, byte[]> {
    /** Busca a execução mais recente pelo identificador técnico do job. */
    Optional<GeraLandingStageExecution> findTopByIdJobOrderByExecutionRequestedAtDesc(byte[] idJob);

    /** Busca a execução mais recente de um job dentro de um experimento. */
    Optional<GeraLandingStageExecution> findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(Long experimentId,
                                                                                                       byte[] idJob);

    /** Busca a execução mais recente de uma etapa dentro de um experimento. */
    Optional<GeraLandingStageExecution> findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(Long experimentId,
                                                                                                           String stageCode);

    /** Lista as próximas vinte execuções de um status em ordem de solicitação. */
    List<GeraLandingStageExecution> findTop20ByStatusOrderByExecutionRequestedAtAsc(String status);

    /** Lista as próximas vinte execuções que estejam em qualquer um dos status informados. */
    List<GeraLandingStageExecution> findTop20ByStatusInOrderByExecutionRequestedAtAsc(List<String> statuses);

    /** Busca todas as execuções de um experimento em ordem cronológica para auditoria completa. */
    List<GeraLandingStageExecution> findByExperimentIdOrderByExecutionRequestedAtAsc(Long experimentId);

    /** Busca as execuções mais antigas de uma etapa com experimento e hipótese carregados. */
    @EntityGraph(attributePaths = {"experiment", "experiment.hypothesisRef"})
    List<GeraLandingStageExecution> findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(String stageCode,
                                                                                                String status);

    /** Lista execuções iniciadas antigas de uma etapa para diagnóstico operacional do worker. */
    List<GeraLandingStageExecution> findTop20ByStageCodeAndStatusAndExecutionRequestedAtBeforeOrderByExecutionRequestedAtAsc(
            String stageCode,
            String status,
            Instant executionRequestedAtBefore);

    /** Lista as últimas vinte execuções de uma etapa dentro de um experimento. */
    List<GeraLandingStageExecution>
    findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(Long experimentId, String stageCode);

    /** Lista as últimas vinte execuções de uma etapa excluindo um status operacional. */
    List<GeraLandingStageExecution>
    findTop20ByExperimentIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
            Long experimentId,
            String stageCode,
            String status);

    /**
     * Lê do banco os melhores insumos de páginas de venda já persistidos pelo MOIS para enriquecer prompts do GeraLanding.
     */
    @Query(value = """
            SELECT e.sales_page_id,
                   p.url_canonical,
                   p.title,
                   e.score_total,
                   e.geralanding_wireframe_json,
                   e.geralanding_copy_json,
                   e.geralanding_image_prompt_json,
                   e.geralanding_design_preset_json
            FROM mois_sales_page_job_execution e
            INNER JOIN mois_sales_page p ON p.id = e.sales_page_id
            WHERE e.job_type = 'PAGE_ANALYSIS'
              AND e.status = 'DONE'
              AND e.score_total IS NOT NULL
              AND e.geralanding_wireframe_json IS NOT NULL
              AND e.geralanding_copy_json IS NOT NULL
              AND e.geralanding_image_prompt_json IS NOT NULL
              AND e.geralanding_design_preset_json IS NOT NULL
            ORDER BY e.score_total DESC, e.finished_at DESC, e.id DESC
            LIMIT ?1
            """, nativeQuery = true)
    List<Object[]> findTopPersistedMoisGeraLandingReferenceRows(int limit);
}
