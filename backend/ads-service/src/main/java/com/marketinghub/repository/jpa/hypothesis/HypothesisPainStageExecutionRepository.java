package com.marketinghub.repository.jpa.hypothesis;

import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório responsável por consultar e persistir execuções da etapa Dor do pipeline de hipótese. */
public interface HypothesisPainStageExecutionRepository extends JpaRepository<HypothesisPainStageExecution, byte[]> {
    /** Busca a execução mais recente pelo identificador técnico do job. */
    Optional<HypothesisPainStageExecution> findTopByIdJobOrderByExecutionRequestedAtDesc(byte[] idJob);

    /** Busca a execução mais recente de uma etapa dentro de um nicho. */
    Optional<HypothesisPainStageExecution> findTopByMarketNicheIdAndStageCodeOrderByExecutionRequestedAtDesc(Long marketNicheId, String stageCode);

    /** Busca a execução concluída mais recente de uma etapa dentro de um nicho. */
    Optional<HypothesisPainStageExecution> findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
            Long marketNicheId,
            String stageCode,
            String status);

    /** Busca as execuções mais antigas de uma etapa com nicho carregado. */
    @EntityGraph(attributePaths = {"marketNiche", "hypothesis"})
    List<HypothesisPainStageExecution> findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(String stageCode, String status);

    /** Busca execuções antigas sem conclusão para aplicação da política de lease operacional. */
    List<HypothesisPainStageExecution> findTop50ByStageCodeAndStatusInAndCompletedAtIsNullAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
            String stageCode,
            List<String> statuses,
            Instant threshold);

    /** Lista todas as execuções de uma etapa dentro de um nicho com hipótese carregada para totalização e contexto. */
    @EntityGraph(attributePaths = {"hypothesis"})
    List<HypothesisPainStageExecution> findByMarketNicheIdAndStageCodeOrderByExecutionRequestedAtDesc(Long marketNicheId, String stageCode);

    /** Lista as últimas vinte execuções de uma etapa dentro de um nicho excluindo um status operacional. */
    List<HypothesisPainStageExecution> findTop20ByMarketNicheIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
            Long marketNicheId,
            String stageCode,
            String status);
}
