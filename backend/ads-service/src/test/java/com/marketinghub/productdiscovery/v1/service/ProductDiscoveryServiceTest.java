package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
    when(opportunityRepository.findTop10ByOrderByScoreDescUpdatedAtDesc()).thenReturn(List.of());
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

  /** Deve impedir que uma oportunidade genérica única conclua um ciclo comercial. */
  @Test
  void completeRejectsCycleWithoutCompetingHypotheses() {
    ProductDiscoveryService service =
        new ProductDiscoveryService(cycleRepository, opportunityRepository);
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(42L);
    when(cycleRepository.findById(42L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryOpportunityResultRequest genericOpportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "PDE genérico",
            "Público amplo",
            "Dor genérica",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "{}",
            BigDecimal.valueOf(45),
            ProductDiscoveryOpportunityDecision.RESEARCH_MORE);

    assertThatThrownBy(
            () ->
                service.complete(
                    42L,
                    new ProductDiscoveryResultRequest(
                        "Pesquisa insuficiente", List.of(genericOpportunity))))
        .hasMessageContaining("três hipóteses concorrentes");
  }
}
