package com.marketinghub.product.service;

import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: resolver a referência auditável da execução independente que originou um
 * produto.
 */
@Service
public class ProductOriginExecutionReferenceResolver {
  private static final String PRODUCT_DISCOVERY_CYCLE_PREFIX = "product-discovery-cycle:";

  private final OpportunityDossierRepository opportunityDossierRepository;

  /** Configura a fonte relacional da linhagem entre dossiê, ciclo factual e produto. */
  public ProductOriginExecutionReferenceResolver(
      OpportunityDossierRepository opportunityDossierRepository) {
    this.opportunityDossierRepository = opportunityDossierRepository;
  }

  /** Retorna a referência original somente quando o vínculo factual está persistido. */
  public Optional<String> resolve(Long productId) {
    if (productId == null) return Optional.empty();
    return opportunityDossierRepository
        .findProductDiscoveryCycleIdByCreatedProductId(productId)
        .map(cycleId -> PRODUCT_DISCOVERY_CYCLE_PREFIX + cycleId);
  }
}
