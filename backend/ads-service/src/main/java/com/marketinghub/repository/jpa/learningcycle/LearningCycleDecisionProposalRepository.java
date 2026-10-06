package com.marketinghub.repository.jpa.learningcycle;

import com.marketinghub.businessprocesschain.learningcycle.v1.decision.LearningCycleDecisionProposal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir propostas imutáveis por ciclo, revisão e tentativa. */
public interface LearningCycleDecisionProposalRepository
    extends JpaRepository<LearningCycleDecisionProposal, Long> {
  /** Seleciona somente novas ocorrências aderentes à política, sem migrar decisões históricas. */
  @Query(
      value =
          """
      SELECT p.id FROM learning_cycle_decision_proposal_v1 p
      JOIN learning_sales_cycle_v1 c ON c.id = p.cycle_id
      JOIN product pr ON pr.id = c.product_id
      LEFT JOIN learning_sales_cycle_v1 successor ON successor.previous_cycle_id = c.id
      WHERE p.error IS NULL AND c.stage = 'DECISION' AND successor.id IS NULL
        AND ((p.status = 'READY' AND c.status = 'OPEN' AND c.revision = p.cycle_revision)
          OR (p.status = 'APPROVED' AND c.status IN ('ADJUSTED', 'INCONCLUSIVE') AND c.revision = p.cycle_revision + 1))
        AND pr.automatic_execution_enabled = 1
        AND JSON_UNQUOTE(JSON_EXTRACT(c.brief_json, '$.preparationPolicy')) = 'LEARNING_CYCLE_SAFE_PREPARATION_V1'
        AND JSON_UNQUOTE(JSON_EXTRACT(p.proposal_json, '$.contractVersion')) = 'LEARNING_CYCLE_DECISION_PROPOSAL_V2'
        AND (JSON_UNQUOTE(JSON_EXTRACT(p.proposal_json, '$.action')) = 'ADJUST'
          OR (p.status = 'APPROVED' AND c.status = 'INCONCLUSIVE'
            AND JSON_UNQUOTE(JSON_EXTRACT(p.proposal_json, '$.action')) = 'INCONCLUSIVE'))
        AND JSON_UNQUOTE(JSON_EXTRACT(p.proposal_json, '$.marketReview.recommendedScope')) = 'KEEP_FOCUS'
      ORDER BY p.id
      """,
      nativeQuery = true)
  List<Long> findAutomaticPreparationPending(org.springframework.data.domain.Pageable page);

  /** Lê somente a identidade para bloquear o ciclo antes de carregar o estado da proposta. */
  @Query("select p.cycleId from LearningCycleDecisionProposal p where p.id = :id")
  Optional<Long> findCycleId(@Param("id") Long id);

  /** Recupera a última tentativa de uma revisão exata. */
  Optional<LearningCycleDecisionProposal> findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(
      Long cycleId, long revision);

  /** Expõe a proposta mais recente inclusive depois da aprovação e encerramento. */
  Optional<LearningCycleDecisionProposal> findFirstByCycleIdOrderByIdDesc(Long cycleId);

  /** Preserva a sequência de tentativas na auditoria do ciclo. */
  List<LearningCycleDecisionProposal> findByCycleIdOrderByIdDesc(Long cycleId);
}
