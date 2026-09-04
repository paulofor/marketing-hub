package com.marketinghub.repository.jpa.opportunitydossier;

import com.marketinghub.opportunitydossier.OpportunityDossier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /** Localiza a linhagem factual do produto planejado materializado pela descoberta. */
  Optional<OpportunityDossier> findByCreatedProductId(Long createdProductId);

  /** Projeta o ciclo factual que originou o produto sem carregar o dossiê completo. */
  @Query(
      """
      select cycle.id
      from OpportunityDossier dossier
      join dossier.productDiscoveryCycle cycle
      where dossier.createdProduct.id = :productId
      """)
  Optional<Long> findProductDiscoveryCycleIdByCreatedProductId(@Param("productId") Long productId);

  /** Reutiliza a candidata de mesmo ciclo e título após uma reanálise auditável de Argos. */
  Optional<OpportunityDossier> findFirstByProductDiscoveryCycleIdAndTitleIgnoreCase(
      Long productDiscoveryCycleId, String title);
}
