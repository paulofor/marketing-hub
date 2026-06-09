package com.marketinghub.repository.jpa.geralanding;

import com.marketinghub.geralanding.GeraLandingStageExecution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

    /** Lista as últimas vinte execuções de uma etapa dentro de um experimento. */
    List<GeraLandingStageExecution>
    findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(Long experimentId, String stageCode);

    /** Lista as últimas vinte execuções de uma etapa excluindo um status operacional. */
    List<GeraLandingStageExecution>
    findTop20ByExperimentIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
            Long experimentId,
            String stageCode,
            String status);
}
