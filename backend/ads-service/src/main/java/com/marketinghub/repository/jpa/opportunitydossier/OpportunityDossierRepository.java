package com.marketinghub.repository.jpa.opportunitydossier;

import com.marketinghub.opportunitydossier.OpportunityDossier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e listar dossiês de oportunidade. */
public interface OpportunityDossierRepository extends JpaRepository<OpportunityDossier, Long> {
  /** Lista os dossiês priorizando os atualizados recentemente. */
  List<OpportunityDossier> findAllByOrderByUpdatedAtDesc();

  /** Localiza o dossiê que originou um ciclo executável de pesquisa de Argos. */
  Optional<OpportunityDossier> findByProductDiscoveryCycleId(Long productDiscoveryCycleId);
}
