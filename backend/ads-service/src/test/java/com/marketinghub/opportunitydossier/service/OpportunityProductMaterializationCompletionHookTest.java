package com.marketinghub.opportunitydossier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskCompletionHook;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.opportunitydossier.OpportunityDossierStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CreateCommercialPlanRequest;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.service.ProductService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityMaturity;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: validar a materialização governada e idempotente do produto PDE planejado. */
class OpportunityProductMaterializationCompletionHookTest {

  /** Cria plano e produto somente depois dos três contratos aprovados. */
  @Test
  void materializesOnePlannedProductAfterApprovedContracts() {
    Fixture fixture = new Fixture(ProductDiscoveryOpportunityMaturity.DOSSIER_READY);

    var disposition = fixture.hook.apply(fixture.architectureTask, fixture.architectureRequest());

    assertThat(disposition).isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
    ArgumentCaptor<CreateCommercialPlanRequest> plan =
        ArgumentCaptor.forClass(CreateCommercialPlanRequest.class);
    verify(fixture.commercialPlanService).create(plan.capture());
    assertThat(plan.getValue().mainChannel()).isEqualTo("Instagram");
    ArgumentCaptor<CreateProductRequest> product =
        ArgumentCaptor.forClass(CreateProductRequest.class);
    verify(fixture.productService).createProduct(product.capture());
    verify(fixture.productService).updateAutomaticExecution(901L, false, "pde-discovery-handoff");
    assertThat(product.getValue().getCommercialStatus()).isEqualTo("PLANNED");
    assertThat(product.getValue().getDeliveryMode()).isEqualTo("EXPERIÊNCIA_PERSONALIZADA_POR_IA");
    assertThat(product.getValue().getPdeExperienceJson())
        .contains("PDE_HARNESS_PLAN_V1", "publicationBoundary", "dossierId");
    assertThat(fixture.dossier.getStatus()).isEqualTo(OpportunityDossierStatus.CONVERTED_TO_PLAN);
    assertThat(fixture.dossier.getCreatedProduct()).isSameAs(fixture.product);
    assertThat(fixture.product.getAutomaticExecutionEnabled()).isFalse();
  }

  /** Retorna sucesso idempotente sem criar outro produto quando o dossiê já foi convertido. */
  @Test
  void doesNotDuplicateExistingProduct() {
    Fixture fixture = new Fixture(ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    fixture.dossier.setCreatedProduct(fixture.product);

    var disposition = fixture.hook.apply(fixture.architectureTask, fixture.architectureRequest());

    assertThat(disposition).isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
    verify(fixture.commercialPlanService, never()).create(any());
    verify(fixture.productService, never()).createProduct(any());
  }

  /** Bloqueia a seleção de Atena quando Argos não liberou maturidade factual. */
  @Test
  void rejectsSelectedCandidateWithoutFactualMaturity() {
    Fixture fixture = new Fixture(ProductDiscoveryOpportunityMaturity.RESEARCHABLE);

    assertThatThrownBy(
            () -> fixture.hook.apply(fixture.architectureTask, fixture.architectureRequest()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("não puderam materializar");
    verify(fixture.productService, never()).createProduct(any());
  }

  /** Reconhece somente a atividade final canônica de Dédalo na cadeia autônoma. */
  @Test
  void supportsOnlyCanonicalAutonomousArchitectureTask() {
    Fixture fixture = new Fixture(ProductDiscoveryOpportunityMaturity.DOSSIER_READY);

    assertThat(fixture.hook.supports(fixture.architectureTask)).isTrue();
    fixture.architectureTask.setSourceReference("product:77");
    assertThat(fixture.hook.supports(fixture.architectureTask)).isFalse();
  }

  /** Limita campos indexados do cadastro sem truncar o contrato integral do harness. */
  @Test
  void limitsProductColumnsGeneratedByAgents() {
    Fixture fixture = new Fixture(ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    fixture.dossier.setTitle("Mercado ".repeat(30));

    fixture.hook.apply(
        fixture.architectureTask,
        fixture.architectureRequest("Formato sensorial ".repeat(8), "Entregável ".repeat(30)));

    ArgumentCaptor<CreateProductRequest> product =
        ArgumentCaptor.forClass(CreateProductRequest.class);
    verify(fixture.productService).createProduct(product.capture());
    assertThat(product.getValue().getName()).hasSize(191);
    assertThat(product.getValue().getInternalName()).hasSize(191);
    assertThat(product.getValue().getProductFormat()).hasSize(64);
    assertThat(product.getValue().getValueUnit()).hasSize(191);
    assertThat(product.getValue().getPdeExperienceJson()).contains("Entregável");
  }

  /** Prepara contratos e colaboradores isolados para cada cenário. */
  private static final class Fixture {
    private final OpportunityDossierRepository dossierRepository =
        mock(OpportunityDossierRepository.class);
    private final AgentTaskRepository taskRepository = mock(AgentTaskRepository.class);
    private final ProductTypeDefinitionRepository productTypeRepository =
        mock(ProductTypeDefinitionRepository.class);
    private final CommercialPlanService commercialPlanService = mock(CommercialPlanService.class);
    private final ProductService productService = mock(ProductService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpportunityDossier dossier;
    private final CommercialPlan plan = CommercialPlan.builder().id(801L).build();
    private final Product product = Product.builder().id(901L).build();
    private final AgentTask architectureTask;
    private final OpportunityProductMaterializationCompletionHook hook;

    /** Monta a linhagem do ciclo e as duas predecessoras já concluídas. */
    private Fixture(ProductDiscoveryOpportunityMaturity maturity) {
      ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
      cycle.setId(42L);
      ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
      ReflectionTestUtils.setField(opportunity, "id", 501L);
      opportunity.setCycle(cycle);
      opportunity.setMaturity(maturity);
      dossier =
          OpportunityDossier.builder()
              .id(301L)
              .title("Guarda-roupa cápsula sensorial")
              .targetAudience("Mulheres brasileiras de 40 a 55 anos")
              .mainPain("Escolhas ainda exigem tentativa manual.")
              .knownRisks("Validar pagamento real.")
              .productDiscoveryCycle(cycle)
              .productDiscoveryOpportunity(opportunity)
              .status(OpportunityDossierStatus.UNDER_REVIEW)
              .build();
      BusinessProcessDefinition process = new BusinessProcessDefinition();
      process.setId(88L);
      process.setProcessCode("pde-commercial-plan-offer");
      Agent atena = Agent.builder().agentKey("experiment-strategist").build();
      Agent plutus = Agent.builder().agentKey("financial-agent").build();
      Agent dedalo = Agent.builder().agentKey("landing-generator").build();
      AgentTask strategy = task(701L, process, atena, "marketStrategy", strategyResult());
      AgentTask economics = task(702L, process, plutus, "economics", economicsResult());
      architectureTask = task(703L, process, dedalo, "productArchitecture", null);
      architectureTask.setStatus("IN_PROGRESS");
      when(taskRepository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
              88L, "product-discovery-cycle:42"))
          .thenReturn(List.of(strategy, economics, architectureTask));
      when(dossierRepository.findById(301L)).thenReturn(Optional.of(dossier));
      when(productTypeRepository.findByCode("PDE"))
          .thenReturn(Optional.of(ProductTypeDefinition.builder().id(7L).code("PDE").build()));
      when(commercialPlanService.create(any())).thenReturn(plan);
      when(productService.createProduct(any())).thenReturn(product);
      hook =
          new OpportunityProductMaterializationCompletionHook(
              dossierRepository,
              taskRepository,
              productTypeRepository,
              commercialPlanService,
              productService,
              objectMapper);
    }

    /** Cria uma tarefa da mesma instância BPM com resultado opcional. */
    private AgentTask task(
        Long id, BusinessProcessDefinition process, Agent agent, String activity, String result) {
      AgentTask task = new AgentTask();
      task.setId(id);
      task.setProcessDefinition(process);
      task.setProcessActivityId(activity);
      task.setAssignedAgent(agent);
      task.setSourceReference("product-discovery-cycle:42");
      task.setStatus("COMPLETED");
      task.setResultJson(result);
      return task;
    }

    /** Retorna a seleção estruturada de Atena. */
    private String strategyResult() {
      return """
          {
            "decision":"APPROVE",
            "selectedDossierId":301,
            "selectedOpportunityId":501,
            "marketStrategicContract":{
              "segment":"Moda e bem-estar 40+",
              "buyer":"Mulheres brasileiras de 40 a 55 anos",
              "problem":"Escolher peças confortáveis ainda exige tentativa manual",
              "desiredOutcome":"Receber combinações pessoais prontas",
              "offerThesis":"Experiência pessoal de cápsula sensorial",
              "valueMechanism":"IA organiza contexto e devolve combinações utilizáveis",
              "causalHypothesis":"Menos esforço aumenta o início da experiência"
            }
          }
          """;
    }

    /** Retorna a hipótese econômica aprovada por Plutus. */
    private String economicsResult() {
      return """
          {
            "decision":"APPROVE",
            "economics":{
              "offerPriceBrl":97,
              "variableCostPerSaleBrl":12,
              "maxCacBrl":28,
              "fixedInitialCostBrl":300,
              "maxBudgetBrl":400,
              "expectedTraffic":200,
              "expectedConversionPercent":2.5,
              "expectedRefundPercent":5,
              "targetRevenueBrl":485,
              "deadline":"2026-10-31"
            },
            "metrics":{
              "primary":"Vendas aprovadas",
              "delivery":["Combinações utilizadas"],
              "continueCriteria":"Cinco vendas e uso satisfatório",
              "stopCriteria":"CAC acima de R$ 28"
            }
          }
          """;
    }

    /** Retorna o contrato de harness concluído por Dédalo. */
    private CompleteAgentTaskRequest architectureRequest() {
      return architectureRequest("Experiência web personalizada", "Cápsula visual personalizada");
    }

    /** Retorna um contrato de harness com textos configuráveis para validar limites do cadastro. */
    private CompleteAgentTaskRequest architectureRequest(String format, String deliverable) {
      return new CompleteAgentTaskRequest(
          """
          {
            "decision":"APPROVE",
            "productArchitecture":{
              "format":"%s",
              "valueJourney":["Contexto mínimo","Combinações prontas","Ajuste sensorial"],
              "deliverables":["%s"]
            }
          }
          """
              .formatted(format, deliverable),
          "{\"sources\":[\"dossier:301\"]}");
    }
  }
}
