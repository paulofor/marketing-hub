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
