package com.marketinghub.repository.jpa.hypothesis;

import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
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

    /** Busca as execuções mais antigas de uma etapa com nicho carregado. */
    @EntityGraph(attributePaths = {"marketNiche", "hypothesis"})
    List<HypothesisPainStageExecution> findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(String stageCode, String status);

    /** Lista as últimas vinte execuções de uma etapa dentro de um nicho. */
    List<HypothesisPainStageExecution> findTop20ByMarketNicheIdAndStageCodeOrderByExecutionRequestedAtDesc(Long marketNicheId, String stageCode);

    /** Lista as últimas vinte execuções de uma etapa dentro de um nicho excluindo um status operacional. */
    List<HypothesisPainStageExecution> findTop20ByMarketNicheIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
            Long marketNicheId,
            String stageCode,
            String status);
}
