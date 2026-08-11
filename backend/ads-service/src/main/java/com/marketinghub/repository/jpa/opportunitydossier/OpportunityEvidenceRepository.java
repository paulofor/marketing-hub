package com.marketinghub.repository.jpa.opportunitydossier;

import com.marketinghub.opportunitydossier.OpportunityEvidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir evidências dos dossiês. */
public interface OpportunityEvidenceRepository extends JpaRepository<OpportunityEvidence, Long> {
  /** Lista evidências de um dossiê em ordem cronológica. */
  List<OpportunityEvidence> findByDossierIdOrderByCreatedAtAsc(Long dossierId);
}
