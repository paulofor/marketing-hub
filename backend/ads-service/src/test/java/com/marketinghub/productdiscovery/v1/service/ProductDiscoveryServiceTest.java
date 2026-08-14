package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.opportunitydossier.service.OpportunityDossierResearchSyncService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
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

  @Mock private OpportunityDossierResearchSyncService dossierResearchSyncService;

  /** Deve preservar plano, resposta bruta e modelo sem credenciais de marketplace. */
  @Test
  void registerDirectedResearchPlan() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(20L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    when(cycleRepository.findById(20L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService);

    ProductDiscoveryResearchPlanResponse response =
        service.registerResearchPlan(
            20L,
            new ProductDiscoveryResearchPlanRequest(
                "{\"marketplaceRequests\":[{\"marketplace\":\"HOTMART\"}]}",
                "{\"questions\":[\"Quais produtos vendem?\"]}",
                "gpt-5.6-sol"));

    assertThat(response.cycleId()).isEqualTo(20L);
    assertThat(response.model()).isEqualTo("gpt-5.6-sol");
    assertThat(cycle.getResearchPlanRawResponse()).contains("Quais produtos vendem?");
  }

  /** Deve concluir sem oportunidade quando a busca real não trouxer evidência suficiente. */
  @Test
  void completeWithoutArtificialOpportunityWhenResearchIsEmpty() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(20L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    when(cycleRepository.findById(20L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(20L)).thenReturn(List.of());
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService);

    ProductDiscoveryCycleDetailResponse response =
        service.complete(
            20L,
            new ProductDiscoveryResultRequest(
                "Nenhuma evidência real encontrada; pesquisar mais.", List.of()));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.COMPLETED);
    assertThat(response.opportunities()).isEmpty();
    assertThat(cycle.getDecisionSummary()).contains("pesquisar mais");
  }

  /** Deve manter renda extra como primeira trilha de pesquisa recomendada com travas comerciais. */
  @Test
  void getMaturityRanking() {
    when(opportunityRepository.findTop50ByCycleStatusOrderByScoreDesc(
            ProductDiscoveryCycleStatus.COMPLETED))
        .thenReturn(List.of());
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService);

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
    when(opportunityRepository.findTop50ByCycleStatusOrderByScoreDesc(
            ProductDiscoveryCycleStatus.COMPLETED))
        .thenReturn(List.of(opportunity));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService);

    ProductDiscoveryMaturityRankingResponse ranking = service.getMaturityRanking();

    assertThat(ranking.items()).hasSize(1);
    assertThat(ranking.items().getFirst().niche()).contains("agenda de manicure");
    assertThat(ranking.recommendedPriority()).contains("agenda de manicure");
  }

  /** Deve arquivar apenas ciclos cujo resultado inteiro seja o fallback artificial legado. */
  @Test
  void archiveArtificialLegacyEvidence() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(17L);
    cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    opportunity.setEvidenceJson(
        "[{\"snippet\":\"Busca brave não retornou resultados estruturados suficientes; pesquisar mais.\"}]");
    when(cycleRepository.findTop50ByOrderByUpdatedAtDesc()).thenReturn(List.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(17L))
        .thenReturn(List.of(opportunity));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService);

    ProductDiscoveryLegacyCleanupResponse response = service.archiveArtificialLegacyEvidence();

    assertThat(response.archivedCycles()).isEqualTo(1);
    assertThat(response.archivedOpportunities()).isEqualTo(1);
    assertThat(response.cycleIds()).containsExactly(17L);
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.ARCHIVED);
    assertThat(cycle.getDecisionSummary()).contains("não comprova dor");
  }

  /** Deve reconhecer também a frase usada pelo primeiro fallback DuckDuckGo. */
  @Test
  void archiveOldestArtificialLegacyEvidence() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(1L);
    cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    opportunity.setEvidenceJson(
        "[{\"snippet\":\"Busca pública não retornou tópicos estruturados suficientes; pesquisar mais.\"}]");
    when(cycleRepository.findTop50ByOrderByUpdatedAtDesc()).thenReturn(List.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(1L))
        .thenReturn(List.of(opportunity));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService);

    ProductDiscoveryLegacyCleanupResponse response = service.archiveArtificialLegacyEvidence();

    assertThat(response.cycleIds()).containsExactly(1L);
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.ARCHIVED);
  }
}
