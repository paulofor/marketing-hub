package com.marketinghub.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a resolução relacional da execução que originou um produto. */
class ProductOriginExecutionReferenceResolverTest {
  private final OpportunityDossierRepository dossiers = mock(OpportunityDossierRepository.class);
  private final ProductOriginExecutionReferenceResolver resolver =
      new ProductOriginExecutionReferenceResolver(dossiers);

  /** Retorna a referência exata do ciclo que materializou Mira. */
  @Test
  void resolvesPersistedProductDiscoveryCycle() {
    when(dossiers.findProductDiscoveryCycleIdByCreatedProductId(10L)).thenReturn(Optional.of(64L));

    assertThat(resolver.resolve(10L)).contains("product-discovery-cycle:64");
  }

  /** Mantém vazio o produto legado que não possui origem factual persistida. */
  @Test
  void keepsUnlinkedLegacyProductWithoutInventedOrigin() {
    when(dossiers.findProductDiscoveryCycleIdByCreatedProductId(9L)).thenReturn(Optional.empty());

    assertThat(resolver.resolve(9L)).isEmpty();
    assertThat(resolver.resolve(null)).isEmpty();
  }
}
