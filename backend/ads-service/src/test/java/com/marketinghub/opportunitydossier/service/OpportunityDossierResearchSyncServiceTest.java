package com.marketinghub.opportunitydossier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.product.Product;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityMaturity;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.producttype.ProductTypeStatus;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityEvidenceRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: proteger a correlação entre candidatas de Argos, dossiês e gates comerciais.
 */
class OpportunityDossierResearchSyncServiceTest {

  /** Materializa todas as candidatas e abre uma única cadeia sequencial para o ciclo. */
  @Test
  void materializesCandidatesAndOpensOneCommercialChain() {
    Fixture fixture = new Fixture();
    ProductDiscoveryOpportunity first =
        opportunity(
            11L, "Guarda-roupa cápsula 40+", ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    ProductDiscoveryOpportunity second =
        opportunity(12L, "Viagem solo 40+", ProductDiscoveryOpportunityMaturity.RESEARCHABLE);

    fixture.service().synchronize(42L, List.of(first, second));

    verify(fixture.dossiers, times(2)).save(any(OpportunityDossier.class));
    verify(fixture.evidence, times(8)).save(any());
    ArgumentCaptor<CreateAgentTaskRequest> tasks =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(fixture.agentTaskService, times(3)).retryBlockedByHumanOrRefreshPending(tasks.capture());
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::processActivityId)
        .containsExactly("marketStrategy", "economics", "productArchitecture");
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::sourceReference)
        .containsOnly("product-discovery-cycle:42");
    assertThat(tasks.getAllValues().get(0).description())
        .contains(
            "Guarda-roupa cápsula 40+",
            "Viagem solo 40+",
            "DOSSIER_READY",
            "PRODUCT_IDENTITY_V1",
            "\"reservedInternalNames\":[\"Mira\"]",
            "\"code\":\"AI_PRODUCT\"",
            "\"internalName\":\"Safira\"");
    assertThat(tasks.getAllValues().get(1).description())
        .contains("MARKET_STRATEGY_V3", "PRODUCT_IDENTITY_V1", "contexto do processo")
        .doesNotContain("Guarda-roupa cápsula 40+");
    assertThat(tasks.getAllValues().get(2).description())
        .contains("economia aprovada por Plutus", "PRODUCT_IDENTITY_V1", "contexto do processo")
        .doesNotContain("Viagem solo 40+");
  }

  /** Reinicia Atena e as sucessoras quando a tentativa concluída usa o contrato v2 obsoleto. */
  @Test
  void restartsCommercialChainWhenCompletedAtenaContractIsStale() {
    Fixture fixture = new Fixture();
    ProductDiscoveryOpportunity candidate =
        opportunity(
            16L, "Rotina de pele madura", ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    when(fixture.taskRepository.findBySourceReferenceOrderByCreatedAtAscIdAsc(
            "product-discovery-cycle:42"))
        .thenReturn(
            List.of(
                task(
                    321L,
                    "marketStrategy",
                    "COMPLETED",
                    """
                    {"marketStrategicContract":{"contractVersion":"MARKET_STRATEGY_V2","status":"READY_FOR_OPERATION"}}
                    """),
                task(322L, "economics", "BLOCKED", null),
                task(323L, "productArchitecture", "PENDING", null)));

    fixture.service().synchronize(42L, List.of(candidate));

    verify(fixture.agentTaskService)
        .cancelActiveTasksBySourceReference(
            "product-discovery-cycle:42",
            "Cadeia reiniciada porque Atena não registrou a identidade PRODUCT_IDENTITY_V1 exigida pelo Processo 2.");
    verify(fixture.agentTaskService, times(3)).createByHuman(any(CreateAgentTaskRequest.class));
    verify(fixture.agentTaskService, never()).retryBlockedByHumanOrRefreshPending(any());
  }

  /** Preserva a cadeia quando a tentativa mais recente já usa estratégia privada v3. */
  @Test
  void preservesCommercialChainWhenAtenaContractIsCurrent() {
    Fixture fixture = new Fixture();
    ProductDiscoveryOpportunity candidate =
        opportunity(
            17L, "Presença antes de sair", ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    when(fixture.taskRepository.findBySourceReferenceOrderByCreatedAtAscIdAsc(
            "product-discovery-cycle:42"))
        .thenReturn(
            List.of(
                task(
                    324L,
                    "marketStrategy",
                    "COMPLETED",
                    """
                    {
                      "marketStrategicContract":{
                        "contractVersion":"MARKET_STRATEGY_V3",
                        "status":"READY_FOR_PRIVATE_VALIDATION",
                        "privateValidationPlan":{
                          "minimumIndependentReadings":2,
                          "requiredSignals":[
                            "EXPERIENCE_STARTED",
                            "VALUE_MOMENT",
                            "READY_RESULT_USED",
                            "PREFERRED_OVER_FREE",
                            "CHECKOUT_STARTED"
                          ]
                        }
                      },
                      "productIdentity":{
                        "contractVersion":"PRODUCT_IDENTITY_V1",
                        "mode":"CREATE",
                        "internalName":"Alcyone",
                        "productTypeCode":"AI_PRODUCT",
                        "productTypeInternalName":"Safira",
                        "classificationRationale":"Personalização por IA é o mecanismo de valor."
                      }
                    }
                    """)));

    fixture.service().synchronize(42L, List.of(candidate));

    verify(fixture.agentTaskService, never()).cancelActiveTasksBySourceReference(any(), any());
    verify(fixture.agentTaskService, times(3))
        .retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class));
    verify(fixture.agentTaskService, never()).createByHuman(any());
  }

  /** Preserva sinais imaturos em dossiês sem avançar Atena, Plutus ou Dédalo. */
  @Test
  void doesNotOpenCommercialChainWithoutReadyDossier() {
    Fixture fixture = new Fixture();
    ProductDiscoveryOpportunity signal =
        opportunity(13L, "Sinal ainda fraco", ProductDiscoveryOpportunityMaturity.RESEARCHABLE);

    fixture.service().synchronize(42L, List.of(signal));

    verify(fixture.dossiers).save(any(OpportunityDossier.class));
    verify(fixture.agentTaskService, never()).retryBlockedByHumanOrRefreshPending(any());
  }

  /** Não reabre Atena nem consome modelo quando o dossiê pronto já originou um produto. */
  @Test
  void doesNotReopenCommercialChainForMaterializedDossier() {
    Fixture fixture = new Fixture();
    ProductDiscoveryOpportunity candidate =
        opportunity(
            18L, "Decisão de look para ocasião", ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    OpportunityDossier existing =
        OpportunityDossier.builder()
            .id(118L)
            .createdProduct(Product.builder().id(11L).internalName("Alcyone").build())
            .build();
    when(fixture.dossiers.findByProductDiscoveryOpportunityId(18L))
        .thenReturn(Optional.of(existing));

    fixture.service().synchronize(42L, List.of(candidate));

    verify(fixture.agentTaskService, never()).retryBlockedByHumanOrRefreshPending(any());
    verify(fixture.agentTaskService, never()).createByHuman(any());
  }

  /** Reutiliza o dossiê já vinculado à candidata durante uma reanálise do mesmo ciclo. */
  @Test
  void reusesDossierLinkedToOpportunity() {
    Fixture fixture = new Fixture();
    ProductDiscoveryOpportunity candidate =
        opportunity(14L, "Mercado reanalisado", ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    OpportunityDossier existing = OpportunityDossier.builder().id(114L).build();
    when(fixture.dossiers.findByProductDiscoveryOpportunityId(14L))
        .thenReturn(Optional.of(existing));

    fixture.service().synchronize(42L, List.of(candidate));

    ArgumentCaptor<OpportunityDossier> dossier = ArgumentCaptor.forClass(OpportunityDossier.class);
    verify(fixture.dossiers).save(dossier.capture());
    assertThat(dossier.getValue().getId()).isEqualTo(114L);
    assertThat(dossier.getValue().getProductDiscoveryOpportunity()).isSameAs(candidate);
  }

  /** Limita o rótulo externo da oferta sem alterar a evidência bruta preservada por Argos. */
  @Test
  void boundsExternalReferenceProductToDossierColumn() {
    Fixture fixture = new Fixture();
    ProductDiscoveryOpportunity candidate =
        opportunity(
            15L, "Mercado com oferta extensa", ProductDiscoveryOpportunityMaturity.RESEARCHABLE);
    String longTitle = "A".repeat(700);
    candidate.setEvidenceJson(
        "{\"marketplaceOffers\":[{\"url\":\"https://example.test/oferta\",\"title\":\""
            + longTitle
            + "\",\"marketplace\":\"HOTMART\"}]}");

    fixture.service().synchronize(42L, List.of(candidate));

    ArgumentCaptor<OpportunityDossier> dossier = ArgumentCaptor.forClass(OpportunityDossier.class);
    verify(fixture.dossiers).save(dossier.capture());
    assertThat(dossier.getValue().getReferenceProduct()).hasSize(512);
    assertThat(candidate.getEvidenceJson()).contains(longTitle);
  }

  /** Cria uma candidata coerente com o contrato factual persistido por Argos. */
  private static ProductDiscoveryOpportunity opportunity(
      Long id, String name, ProductDiscoveryOpportunityMaturity maturity) {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(42L);
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    ReflectionTestUtils.setField(opportunity, "id", id);
    opportunity.setCycle(cycle);
    opportunity.setName(name);
    opportunity.setPrimaryAudience("Mulheres brasileiras de 40 a 55 anos");
    opportunity.setRootPain("Decisão relevante ainda exige pesquisa e montagem manual.");
    opportunity.setPdeExperience("Resultado pronto, personalizado e utilizável sem conhecer IA.");
    opportunity.setCommercialRisk("Validar pagamento real antes de aquisição paga.");
    opportunity.setScore(new BigDecimal("78"));
    opportunity.setMaturity(maturity);
    opportunity.setDecision(
        maturity == ProductDiscoveryOpportunityMaturity.DOSSIER_READY
            ? ProductDiscoveryOpportunityDecision.APPROVE
            : ProductDiscoveryOpportunityDecision.RESEARCH_MORE);
    opportunity.setEvidenceJson(
        """
        {
          "publicEvidence":[{"url":"https://example.test/evidencia","snippet":"Dor factual observada"}],
          "marketplaceOffers":[{"url":"https://example.test/oferta","title":"Oferta comparável","marketplace":"HOTMART"}],
          "metaAdEvidence":[{"adLibraryUrl":"https://facebook.test/ad/1","advertiserName":"Marca","adText":"Linguagem comercial"}],
          "repositoryEvidence":[{"path":"pesquisas/mercado/artigo.md","title":"Artigo interno","excerpt":"Hipótese confrontada"}]
        }
        """);
    return opportunity;
  }

  /** Cria uma tentativa histórica suficiente para validar a recuperação da cadeia. */
  private static AgentTask task(Long id, String activityId, String status, String resultJson) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setProcessActivityId(activityId);
    task.setStatus(status);
    task.setSourceReference("product-discovery-cycle:42");
    task.setResultJson(resultJson);
    return task;
  }

  /** Agrupa os colaboradores simulados e o serviço sob teste. */
  private static final class Fixture {
    private final OpportunityDossierRepository dossiers = mock(OpportunityDossierRepository.class);
    private final OpportunityEvidenceRepository evidence =
        mock(OpportunityEvidenceRepository.class);
    private final AgentTaskRepository taskRepository = mock(AgentTaskRepository.class);
    private final BusinessProcessDefinitionRepository processes =
        mock(BusinessProcessDefinitionRepository.class);
    private final AgentTaskService agentTaskService = mock(AgentTaskService.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final ProductTypeDefinitionRepository productTypes =
        mock(ProductTypeDefinitionRepository.class);

    /** Prepara IDs estáveis e o processo comercial publicado. */
    private Fixture() {
      BusinessProcessDefinition process = new BusinessProcessDefinition();
      process.setId(88L);
      process.setVersionNumber(9);
      when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
              "pde-commercial-plan-offer", "PUBLISHED"))
          .thenReturn(Optional.of(process));
      when(products.findAllAssignedInternalNames()).thenReturn(List.of("Mira"));
      when(productTypes.findAllByStatusOrderByNameAsc(ProductTypeStatus.ACTIVE))
          .thenReturn(
              List.of(
                  ProductTypeDefinition.builder()
                      .id(3L)
                      .code("AI_PRODUCT")
                      .name("Produto IA")
                      .internalName("Safira")
                      .description("Valor depende de geração ou personalização por IA.")
                      .status(ProductTypeStatus.ACTIVE)
                      .build()));
      when(dossiers.findByProductDiscoveryOpportunityId(any())).thenReturn(Optional.empty());
      when(dossiers.findFirstByProductDiscoveryCycleIdAndTitleIgnoreCase(any(), any()))
          .thenReturn(Optional.empty());
      when(dossiers.save(any(OpportunityDossier.class)))
          .thenAnswer(
              invocation -> {
                OpportunityDossier dossier = invocation.getArgument(0);
                if (dossier.getId() == null) {
                  dossier.setId(100L + dossier.getProductDiscoveryOpportunity().getId());
                }
                return dossier;
              });
    }

    /** Monta o serviço com um serializador real e integrações isoladas. */
    private OpportunityDossierResearchSyncService service() {
      return new OpportunityDossierResearchSyncService(
          dossiers,
          evidence,
          taskRepository,
          processes,
          agentTaskService,
          products,
          productTypes,
          new ObjectMapper());
    }
  }
}
