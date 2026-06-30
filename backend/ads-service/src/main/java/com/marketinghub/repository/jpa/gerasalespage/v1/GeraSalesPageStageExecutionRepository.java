package com.marketinghub.repository.jpa.gerasalespage.v1;

import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar e persistir execuções do GeraSalesPage v1. */
public interface GeraSalesPageStageExecutionRepository extends JpaRepository<GeraSalesPageStageExecution, String> {
    /** Busca uma execução pelo identificador técnico do job. */
    Optional<GeraSalesPageStageExecution> findTopByIdJobOrderByExecutionRequestedAtDesc(String idJob);

    /** Busca a execução mais recente da etapa dentro do experimento. */
    Optional<GeraSalesPageStageExecution> findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
            Long experimentId, String stageCode);

    /** Lista pendências de uma etapa com experimento, nicho e hipótese carregados para montar o contrato do worker. */
    @EntityGraph(attributePaths = {"experiment", "experiment.niche", "experiment.hypothesisRef"})
    List<GeraSalesPageStageExecution> findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            String stageCode, String status);
}
