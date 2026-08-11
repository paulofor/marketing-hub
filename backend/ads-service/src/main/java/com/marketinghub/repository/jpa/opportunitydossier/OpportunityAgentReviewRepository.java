package com.marketinghub.repository.jpa.opportunitydossier;

import com.marketinghub.opportunitydossier.OpportunityAgentReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir solicitações e pareceres dos agentes. */
public interface OpportunityAgentReviewRepository
    extends JpaRepository<OpportunityAgentReview, Long> {
  /** Lista os pareceres de um dossiê por agente. */
  List<OpportunityAgentReview> findByDossierIdOrderByAgentKeyAsc(Long dossierId);

  /** Localiza o parecer reservado a um agente. */
  Optional<OpportunityAgentReview> findByDossierIdAndAgentKey(Long dossierId, String agentKey);
}
