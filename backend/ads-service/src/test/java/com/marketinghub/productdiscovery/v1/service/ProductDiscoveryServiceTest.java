package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.math.BigDecimal;
import java.util.List;
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
    when(opportunityRepository.findTop50ByOrderByScoreDesc()).thenReturn(List.of());
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

  /** Deve derivar o ranking dos ciclos persistidos quando houver pesquisa real. */
  @Test
  void getMaturityRankingFromPersistedOpportunities() {
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    opportunity.setName("Diagnóstico de decisão para agenda de manicure");
    opportunity.setRootPain("Clientes somem e a agenda perde previsibilidade.");
    opportunity.setScaleEvidence("Três fontes independentes.");
    opportunity.setUnmetnessEvidence("Reviews mostram soluções confusas.");
    opportunity.setCommercialRisk("Validar disposição de compra.");
    opportunity.setScore(new BigDecimal("82.00"));
    opportunity.setDecision(ProductDiscoveryOpportunityDecision.APPROVE);
    when(opportunityRepository.findTop50ByOrderByScoreDesc()).thenReturn(List.of(opportunity));
    ProductDiscoveryService service =
        new ProductDiscoveryService(cycleRepository, opportunityRepository);

    ProductDiscoveryMaturityRankingResponse ranking = service.getMaturityRanking();

    assertThat(ranking.items()).hasSize(1);
    assertThat(ranking.items().getFirst().niche()).contains("agenda de manicure");
    assertThat(ranking.recommendedPriority()).contains("agenda de manicure");
  }
}
