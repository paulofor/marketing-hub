package com.marketinghub.repository.jpa.opportunitydossier;

import com.marketinghub.opportunitydossier.OpportunityAgentReview;
import com.marketinghub.opportunitydossier.OpportunityReviewExecutionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Responsabilidade: persistir solicitações e pareceres dos agentes. */
public interface OpportunityAgentReviewRepository
    extends JpaRepository<OpportunityAgentReview, Long> {
  /** Lista os pareceres de um dossiê por agente. */
  List<OpportunityAgentReview> findByDossierIdOrderByAgentKeyAsc(Long dossierId);

  /** Localiza o parecer reservado a um agente. */
  Optional<OpportunityAgentReview> findByDossierIdAndAgentKey(Long dossierId, String agentKey);

  /** Localiza a próxima execução do agente sem misturar as filas dos pareceristas. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<OpportunityAgentReview> findByAgentKeyAndExecutionStatusOrderByRequestedAtAsc(
      String agentKey, OpportunityReviewExecutionStatus status, Pageable pageable);

  /** Lista leases do agente que podem exigir retomada controlada. */
  List<OpportunityAgentReview> findByAgentKeyAndExecutionStatusAndUpdatedAtBefore(
      String agentKey, OpportunityReviewExecutionStatus status, java.time.Instant cutoff);
}
