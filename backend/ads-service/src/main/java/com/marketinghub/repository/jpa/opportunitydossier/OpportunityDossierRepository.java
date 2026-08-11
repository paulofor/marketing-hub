package com.marketinghub.repository.jpa.opportunitydossier;

import com.marketinghub.opportunitydossier.OpportunityDossier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e listar dossiês de oportunidade. */
public interface OpportunityDossierRepository extends JpaRepository<OpportunityDossier, Long> {
  /** Lista os dossiês priorizando os atualizados recentemente. */
  List<OpportunityDossier> findAllByOrderByUpdatedAtDesc();
}
