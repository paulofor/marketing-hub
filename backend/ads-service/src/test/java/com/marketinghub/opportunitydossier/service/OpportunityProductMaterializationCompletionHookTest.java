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
    assertThat(product.getValue().getValidationDefinitionVersion())
        .isEqualTo("PDE_PRIVATE_VALIDATION_V1");
    assertThat(product.getValue().getValidationDefinitionJson())
        .contains(
            "WAITING_PRIVATE_PROTOTYPE",
            "privateValidationPlan",
            "privatePrototype",
            "minimumIndependentReadings");
    assertThat(product.getValue().getPdeExperienceJson())
        .contains(
            "PDE_HARNESS_PLAN_V1",
            "private-validation-v1",
            "publicationBoundary",
            "privateValidationPlan",
            "dossierId");
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

  /** Bloqueia contrato antigo que tentava exigir prontidão operacional antes do protótipo. */
  @Test
  void rejectsStrategyWithoutPrivateValidationReadiness() {
    Fixture fixture = new Fixture(ProductDiscoveryOpportunityMaturity.DOSSIER_READY, false);

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
      this(maturity, true);
    }

    /** Permite alternar entre o contrato novo e o legado para proteger a fronteira do backend. */
    private Fixture(ProductDiscoveryOpportunityMaturity maturity, boolean privateValidationReady) {
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
      AgentTask strategy =
          task(701L, process, atena, "marketStrategy", strategyResult(privateValidationReady));
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
    private String strategyResult(boolean privateValidationReady) {
      if (!privateValidationReady) {
        return """
            {
              "decision":"APPROVE",
              "selectedDossierId":301,
              "selectedOpportunityId":501,
              "marketStrategicContract":{
                "contractVersion":"MARKET_STRATEGY_V2",
                "status":"READY_FOR_OPERATION"
              }
            }
            """;
      }
      return """
          {
            "decision":"APPROVE",
            "selectedDossierId":301,
            "selectedOpportunityId":501,
            "marketStrategicContract":{
              "contractVersion":"MARKET_STRATEGY_V3",
              "status":"READY_FOR_PRIVATE_VALIDATION",
              "segment":"Moda e bem-estar 40+",
              "buyer":"Mulheres brasileiras de 40 a 55 anos",
              "problem":"Escolher peças confortáveis ainda exige tentativa manual",
              "desiredOutcome":"Receber combinações pessoais prontas",
              "offerThesis":"Experiência pessoal de cápsula sensorial",
              "valueMechanism":"IA organiza contexto e devolve combinações utilizáveis",
              "causalHypothesis":"Menos esforço aumenta o início da experiência",
              "privateValidationPlan":{
                "minimumIndependentReadings":2,
                "minimumEligibleParticipantsPerReading":1,
                "prototypeObjective":"Comprovar resultado pronto em até dez minutos.",
                "purchaseScene":{
                  "trigger":"Compromisso confirmado.",
                  "deadline":"Antes de sair hoje.",
                  "costOfError":"Perder confiança e tempo.",
                  "budgetEvidence":"Compara alternativas pagas.",
                  "failedAttempt":"Tentou montar manualmente.",
                  "currentPaidBehavior":"Compra orientação especializada."
                },
                "strongestFreeAlternative":"Montagem manual com IA genérica.",
                "prototypeAdvantage":"Resultado pessoal pronto sem prompting.",
                "humanValueDelivery":{
                  "territories":["RECOGNITION","EFFORT_RELIEF"],
                  "desiredTransformation":"Sentir segurança com menos esforço.",
                  "evidenceSourceIds":["source-1","source-2"],
                  "evidencePathways":["CURRENT_LANGUAGE","PAID_BEHAVIOR"],
                  "readyMadeOutcome":"Recomendação visual pronta.",
                  "minimumCustomerInput":"Contexto e preferência em linguagem comum.",
                  "requiresPromptEngineering":false,
                  "requiresManualAssembly":false,
                  "usableWithoutAiKnowledge":true,
                  "customerStepsToValue":3,
                  "timeToUsableResultMinutes":8,
                  "automationBoundary":"A pessoa revisa antes de aplicar."
                },
                "requiredSignals":[
                  "EXPERIENCE_STARTED","VALUE_MOMENT","READY_RESULT_USED",
                  "PREFERRED_OVER_FREE","CHECKOUT_STARTED"
                ],
                "minimumExperienceStartRate":1,
                "minimumValueMomentRate":1,
                "minimumReadyResultUseRate":1,
                "minimumPrototypePreferenceRate":1,
                "minimumCheckoutStartRate":1,
                "sourceMaxAgeDays":30,
                "sourceRefreshRequired":false,
                "sourceRefreshAction":"Nenhuma atualização pendente.",
                "publicationBoundary":"Uso privado sem contato, publicação, cobrança ou gasto."
              }
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
              "deliverables":["%s"],
              "privatePrototype":{
                "scope":"Uma decisão pessoal completa.",
                "simpleInput":"Contexto e preferência em linguagem comum.",
                "readyResult":"Recomendação visual pronta.",
                "maxValueTimeMinutes":10,
                "instrumentationEvents":[
                  "EXPERIENCE_STARTED","VALUE_MOMENT","READY_RESULT_USED",
                  "PREFERRED_OVER_FREE","CHECKOUT_STARTED"
                ],
                "checkoutMode":"SIMULATED_NO_CHARGE",
                "excludedFromPrototype":["Pagamento real","Campanha"]
              }
            }
          }
          """
              .formatted(format, deliverable),
          "{\"sources\":[\"dossier:301\"]}");
    }
  }
}
