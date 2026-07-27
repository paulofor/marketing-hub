package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: valida regras comerciais do serviço de descoberta PDE. */
@ExtendWith(MockitoExtension.class)
class ProductDiscoveryServiceTest {

  @Mock private ProductDiscoveryCycleRepository cycleRepository;

  @Mock private ProductDiscoveryOpportunityRepository opportunityRepository;

  /** Deve manter renda extra como primeira trilha de pesquisa recomendada com travas comerciais. */
  @Test
  void getMaturityRanking() {
    ProductDiscoveryService service =
        new ProductDiscoveryService(cycleRepository, opportunityRepository);

    ProductDiscoveryMaturityRankingResponse ranking = service.getMaturityRanking();

    assertThat(ranking.strategyName()).isEqualTo("Ranking por maturidade comercial");
    assertThat(ranking.items())
        .extracting(ProductDiscoveryMaturityItemResponse::niche)
        .contains("Renda extra");
    assertThat(ranking.recommendedTracks())
        .first()
        .extracting(ProductDiscoveryResearchTrackResponse::name)
        .isEqualTo("Renda extra para autônomos/MEIs");
    assertThat(ranking.recommendedTracks().getFirst().forbiddenCategories())
        .contains("Promessa de renda garantida");
  }
}
