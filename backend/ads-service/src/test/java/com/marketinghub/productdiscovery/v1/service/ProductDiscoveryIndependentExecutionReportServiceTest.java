package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.opportunitydossier.OpportunityDossierStatus;
import com.marketinghub.product.Product;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityMaturity;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: validar o relatório legível da descoberta autônoma até o produto. */
class ProductDiscoveryIndependentExecutionReportServiceTest {

  /** Expõe fontes, seleção, gates e produto sem exigir leitura de JSON técnico. */
  @Test
  void reportsFullLineageForSelectedCandidate() {
    ProductDiscoveryCycleRepository cycles = mock(ProductDiscoveryCycleRepository.class);
    ProductDiscoveryOpportunityRepository opportunities =
        mock(ProductDiscoveryOpportunityRepository.class);
    OpportunityDossierRepository dossiers = mock(OpportunityDossierRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    ProductDiscoveryCycle cycle = cycle();
    ProductDiscoveryOpportunity selected =
        opportunity(
            501L,
            cycle,
            "Guarda-roupa cápsula sensorial 40+",
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    ProductDiscoveryOpportunity preserved =
        opportunity(
            502L,
            cycle,
            "Viagem solo guiada 40+",
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    OpportunityDossier selectedDossier =
        OpportunityDossier.builder()
            .id(301L)
            .title(selected.getName())
            .status(OpportunityDossierStatus.CONVERTED_TO_PLAN)
            .productDiscoveryCycle(cycle)
            .productDiscoveryOpportunity(selected)
            .createdProduct(
                Product.builder()
                    .id(901L)
                    .name("Cápsula sensorial PDE")
                    .commercialStatus("PLANNED")
                    .build())
            .build();
    OpportunityDossier preservedDossier =
        OpportunityDossier.builder()
            .id(302L)
            .title(preserved.getName())
            .status(OpportunityDossierStatus.UNDER_REVIEW)
            .productDiscoveryCycle(cycle)
            .productDiscoveryOpportunity(preserved)
            .build();
    List<AgentTask> taskList =
        List.of(
            task(700L, "marketEvidence", "Argos", "COMPLETED", "{\"decision\":\"APPROVE\"}"),
            task(
                699L,
                "marketStrategy",
                "Atena",
                "BLOCKED",
                "{\"decision\":\"ADJUST\",\"rationale\":\"Tentativa histórica\"}"),
            task(
                701L,
                "marketStrategy",
                "Atena",
                "COMPLETED",
                "{\"decision\":\"APPROVE\",\"selectedDossierId\":301,\"selectedOpportunityId\":501,\"rationale\":\"Melhor fronteira factual\"}"),
            task(702L, "economics", "Plutus", "COMPLETED", "{\"decision\":\"APPROVE\"}"),
            task(
                703L,
                "productArchitecture",
                "Dédalo",
                "COMPLETED",
                "{\"decision\":\"APPROVE\",\"selectedApproach\":\"Harness sensorial\"}"));
    when(cycles.findById(42L)).thenReturn(Optional.of(cycle));
    when(opportunities.findAllByCycleIdOrderByScoreDesc(42L))
        .thenReturn(List.of(selected, preserved));
    when(dossiers.findAllByProductDiscoveryCycleIdOrderByIdAsc(42L))
        .thenReturn(List.of(selectedDossier, preservedDossier));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:42"))
        .thenReturn(taskList);
    ProductDiscoveryIndependentExecutionReportService service =
        new ProductDiscoveryIndependentExecutionReportService(
            cycles, opportunities, dossiers, tasks, new ObjectMapper());

    var report = service.report("product-discovery-cycle:42");

    assertThat(report.status()).isEqualTo("COMPLETED");
    assertThat(report.candidateCount()).isEqualTo(2);
    assertThat(report.dossierReadyCount()).isEqualTo(2);
    assertThat(report.plannedProductCount()).isEqualTo(1);
    assertThat(report.sourceCoverage())
        .extracting(item -> item.sourceCode() + ":" + item.itemCount())
        .contains("WEB:1", "META:1", "PESQUISAS:1", "MARKETPLACE:1");
    var winner = report.candidates().get(0);
    assertThat(winner.productId()).isEqualTo(901L);
    assertThat(winner.nextAction()).contains("Abrir o produto planejado");
    assertThat(winner.sources()).extracting(item -> item.sourceType()).contains("WEB", "META");
    assertThat(report.candidates().get(1).stages())
        .filteredOn(item -> "ATENA".equals(item.stageCode()))
        .extracting(item -> item.status())
        .containsExactly("NOT_SELECTED");
  }

  /** Torna explícita a necessidade de pesquisar mais quando nenhuma candidata amadureceu. */
  @Test
  void reportsBlockerWhenArgosHasNoReadyDossier() {
    ProductDiscoveryCycleRepository cycles = mock(ProductDiscoveryCycleRepository.class);
    ProductDiscoveryOpportunityRepository opportunities =
        mock(ProductDiscoveryOpportunityRepository.class);
    OpportunityDossierRepository dossiers = mock(OpportunityDossierRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    ProductDiscoveryCycle cycle = cycle();
    ProductDiscoveryOpportunity signal =
        opportunity(503L, cycle, "Sinal inicial", ProductDiscoveryOpportunityMaturity.RESEARCHABLE);
    when(cycles.findById(42L)).thenReturn(Optional.of(cycle));
    when(opportunities.findAllByCycleIdOrderByScoreDesc(42L)).thenReturn(List.of(signal));
    when(dossiers.findAllByProductDiscoveryCycleIdOrderByIdAsc(42L)).thenReturn(List.of());
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:42"))
        .thenReturn(List.of());
    ProductDiscoveryIndependentExecutionReportService service =
        new ProductDiscoveryIndependentExecutionReportService(
            cycles, opportunities, dossiers, tasks, new ObjectMapper());

    var report = service.report("product-discovery-cycle:42");

    assertThat(report.status()).isEqualTo("BLOCKED");
    assertThat(report.candidates().get(0).nextAction()).contains("aprofundar");
    assertThat(report.plannedProductCount()).isZero();
  }

  /** Cria o ciclo concluído com as quatro fontes cobertas. */
  private ProductDiscoveryCycle cycle() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(42L);
    cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    cycle.setAcquisitionChannel("Instagram");
    cycle.setDecisionSummary("Duas candidatas factuais formadas; uma foi priorizada.");
    cycle.setResearchEvidenceReportJson(
        """
        {
          "publicEvidence":[{}],
          "marketplaceOffers":[{}],
          "metaAdEvidence":[{}],
          "repositoryEvidence":[{}]
        }
        """);
    return cycle;
  }

  /** Cria uma candidata com fontes rastreáveis e linguagem comercial observada. */
  private ProductDiscoveryOpportunity opportunity(
      Long id,
      ProductDiscoveryCycle cycle,
      String name,
      ProductDiscoveryOpportunityMaturity maturity) {
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    ReflectionTestUtils.setField(opportunity, "id", id);
    opportunity.setCycle(cycle);
    opportunity.setName(name);
    opportunity.setPrimaryAudience("Mulheres brasileiras de 40 a 55 anos");
    opportunity.setRootPain("A decisão ainda exige montagem manual.");
    opportunity.setCommercialRisk("Validar pagamento real.");
    opportunity.setScore(new BigDecimal("81"));
    opportunity.setMaturity(maturity);
    opportunity.setDecision(ProductDiscoveryOpportunityDecision.APPROVE);
    opportunity.setEvidenceJson(
        """
        {
          "candidateEvidence":{
            "purchaseSituation":"Evento marcado durante mudança corporal",
            "observedLanguage":["Quero me sentir eu de novo"],
            "currentAlternatives":["Consultoria de imagem"],
            "residualEffort":"Montar as combinações",
            "instagramFitEvidence":"Transformação visual demonstrável"
          },
          "publicEvidence":[{"url":"https://example.test/study","title":"Estudo","snippet":"Dor observada"}],
          "marketplaceOffers":[{"url":"https://example.test/offer","title":"Oferta","tractionSignal":"avaliações"}],
          "metaAdEvidence":[{"adLibraryUrl":"https://facebook.test/ad","advertiserName":"Marca","adText":"Texto comercial"}],
          "repositoryEvidence":[{"sourceReference":"/pesquisas/artigo.md","title":"Artigo interno","excerpt":"Hipótese de mercado"}]
        }
        """);
    return opportunity;
  }

  /** Cria uma tarefa consolidável no relatório de negócio. */
  private AgentTask task(
      Long id, String activity, String nickname, String status, String resultJson) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setProcessDefinition(new BusinessProcessDefinition());
    task.setProcessActivityId(activity);
    task.setAssignedAgent(Agent.builder().nickname(nickname).agentKey(nickname).build());
    task.setStatus(status);
    task.setResultJson(resultJson);
    return task;
  }
}
