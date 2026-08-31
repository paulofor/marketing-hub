package com.marketinghub.repository.jpa.opportunitydossier;

import com.marketinghub.opportunitydossier.OpportunityDossier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e listar dossiês de oportunidade. */
public interface OpportunityDossierRepository extends JpaRepository<OpportunityDossier, Long> {
  /** Lista os dossiês priorizando os atualizados recentemente. */
  List<OpportunityDossier> findAllByOrderByUpdatedAtDesc();

  /** Lista todos os dossiês candidatos criados por um mesmo ciclo de Argos. */
  List<OpportunityDossier> findAllByProductDiscoveryCycleIdOrderByIdAsc(
      Long productDiscoveryCycleId);

  /** Localiza o dossiê idempotente da candidata factual informada. */
  Optional<OpportunityDossier> findByProductDiscoveryOpportunityId(
      Long productDiscoveryOpportunityId);

  /** Reutiliza a candidata de mesmo ciclo e título após uma reanálise auditável de Argos. */
  Optional<OpportunityDossier> findFirstByProductDiscoveryCycleIdAndTitleIgnoreCase(
      Long productDiscoveryCycleId, String title);
}
