package com.marketinghub.repository.jpa.hypothesis;

import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    /** Busca a execução concluída mais recente ainda não vinculada a uma hipótese fechada. */
    Optional<HypothesisPainStageExecution> findTopByMarketNicheIdAndStageCodeAndStatusAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
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

    /** Lista execuções vinculadas a uma hipótese para rastrear prompts/schemas usados no experimento. */
    List<HypothesisPainStageExecution> findByHypothesisIdOrderByExecutionRequestedAtAsc(UUID hypothesisId);

    /** Lista execuções ainda não vinculadas a uma hipótese fechada para a tela de criação limpa. */
    List<HypothesisPainStageExecution> findByMarketNicheIdAndStageCodeAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
            Long marketNicheId,
            String stageCode);

    /** Lista execuções vinculadas a uma hipótese específica para auditoria da hipótese selecionada. */
    List<HypothesisPainStageExecution> findByMarketNicheIdAndStageCodeAndHypothesisIdOrderByExecutionRequestedAtDesc(
            Long marketNicheId,
            String stageCode,
            UUID hypothesisId);

    /** Lista as últimas vinte execuções de uma etapa dentro de um nicho excluindo um status operacional. */
    List<HypothesisPainStageExecution> findTop20ByMarketNicheIdAndStageCodeAndStatusNotAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
            Long marketNicheId,
            String stageCode,
            String status);
}
