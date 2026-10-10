package com.marketinghub.financialplan.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.financialagent.service.FinancialAgentExecutionResponse;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.financialagent.service.StartRevenueProjectionRequest;
import com.marketinghub.financialplan.v1.FinancialPlanRevision;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.controller.FinancialPlanController;
import com.marketinghub.financialplan.v1.service.prepareplan.*;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.financialplan.v1.service.saveplan.SavePlanRequest;
import com.marketinghub.planning.*;
import com.marketinghub.planning.service.CommercialPlanExecutionSyncService;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.financialplan.FinancialPlanRevisionRepository;
import com.marketinghub.repository.jpa.planning.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: proteger sugestões, fontes, isolamento e concorrência da preparação simples.
 */
class FinancialPlanPreparationTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final FinancialPlanRevisionRepository revisions =
      mock(FinancialPlanRevisionRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final CommercialPlanVersionRepository versions =
      mock(CommercialPlanVersionRepository.class);
  private final FinancialAgentExecutionRepository executions =
      mock(FinancialAgentExecutionRepository.class);
  private final FinancialAgentService plutus = mock(FinancialAgentService.class);
  private final CommercialPlanExecutionSyncService executionSyncService =
      mock(CommercialPlanExecutionSyncService.class);
  private final CommercialPlanMilestoneRepository milestones =
      mock(CommercialPlanMilestoneRepository.class);
  private final Product product = new Product();
  private final CommercialPlan plan = new CommercialPlan();
  private FinancialPlanService service;

  /** Configura fontes sintéticas e persistência capturável sem acessar qualquer serviço externo. */
  @BeforeEach
  void setup() {
    service =
        new FinancialPlanService(
            revisions,
            products,
            mock(ProductTypeDefinitionRepository.class),
            plans,
            versions,
            executions,
            plutus,
            json,
            Validation.buildDefaultValidatorFactory().getValidator(),
            executionSyncService,
            milestones);
    product.setId(95101L);
    product.setValidationDefinitionVersion("fixture-v1");
    product.setValidationDefinitionJson(
        "{\"format\":{\"type\":\"CUSTOM_VISUAL_ASSET_PACK\",\"valueUnit\":\"kit utilizável\"},"
            + "\"delivery\":{\"mode\":\"PERSONALIZED_ASSISTED\",\"personalization\":true},"
            + "\"successEvidence\":{\"firstMilestoneSales\":5}}");
    product.setCurrentPriceBrl(new BigDecimal("67"));
    var type = new ProductTypeDefinition();
    type.setId(951L);
    type.setCode("LOW_TICKET_DIGITAL_PRODUCT");
    product.setProductTypeDefinition(type);
    when(products.findById(95101L)).thenReturn(Optional.of(product));
    when(products.findLockedById(95101L)).thenReturn(Optional.of(product));
    plan.setId(95102L);
    plan.setOfferPriceBrl(new BigDecimal("67"));
    plan.setTargetRevenue(new BigDecimal("335"));
    plan.setOperationalRevenueTarget(new BigDecimal("335"));
    plan.setExpectedCacBrl(new BigDecimal("25"));
    plan.setVariableCostPerSaleBrl(new BigDecimal("13.50"));
    plan.setFixedOperationalCostBrl(new BigDecimal("73.20"));
    plan.setActualCampaignCost(new BigDecimal("59.70"));
    plan.setActualAiCost(new BigDecimal("69.95"));
    plan.setActualTotalCost(new BigDecimal("143.15"));
    plan.setDeadline(LocalDate.of(2026, 8, 16));
    when(plans.findByProductId(95101L)).thenReturn(List.of(plan));
    when(plans.findIdsByProductId(95101L)).thenReturn(List.of(plan.getId()));
    when(plans.findById(plan.getId())).thenReturn(Optional.of(plan));
    when(milestones.findByPlanIdOrderBySequenceOrderAsc(plan.getId())).thenReturn(List.of());
    var version = new CommercialPlanVersion();
    version.setVersionNumber(4);
    when(versions.findTopByPlanIdOrderByVersionNumberDesc(plan.getId()))
        .thenReturn(Optional.of(version));
    when(revisions.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              FinancialPlanRevision r = i.getArgument(0);
              r.setId(999L);
              return r;
            });
  }

  /** Monta requisição com contexto canônico e somente duas decisões operacionais. */
  private PreparePlanRequest request(int revision, int days, Boolean ai) {
    return new PreparePlanRequest(revision, plan.getId(), 4, "fixture-v1", days, ai);
  }

  /** Lê premissas completas independentes dos produtos e tarifas produtivos. */
  private PlanAssumptions complete() throws Exception {
    var node = json.readTree(getClass().getResourceAsStream("/financial-plan/assumptions.json"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) node)
        .putObject("preparation")
        .put("supportDays", 7)
        .put("personalizedAi", true);
    return json.treeToValue(node, PlanAssumptions.class);
  }

  /** Converte premissas em histórico segregado com a mesma identidade comercial. */
  private FinancialPlanRevision prior(PlanAssumptions a) throws Exception {
    var r = new FinancialPlanRevision();
    r.setId(998L);
    r.setScopeKind("PRODUCT");
    r.setScopeId(product.getId());
    r.setProductId(product.getId());
    r.setProductTypeId(951L);
    r.setEnvironment(Environment.LIVE);
    r.setRevisionNumber(1);
    r.setName("Plano sintético");
    r.setCommercialPlanId(plan.getId());
    r.setCommercialPlanVersion(4);
    r.setCreatedAt(Instant.now());
    r.setAssumptionsJson(json.writeValueAsString(a));
    r.setEvaluationJson(json.writeValueAsString(FinancialPlanCalculator.evaluate(a)));
    when(revisions.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", product.getId(), Environment.LIVE))
        .thenReturn(List.of(r));
    return r;
  }

  /** Usa o envelope oficial sem inventar decomposição nem transformar análise em aprovação. */
  @Test
  void defaultsAndMissingSources() {
    var preview = service.preparation(product.getId(), Environment.LIVE);
    assertThat(preview.supportDays()).isEqualTo(7);
    assertThat(preview.personalizedAi()).isTrue();
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, null), null);
    assertThat(saved.assumptions().priceBrl()).isEqualByComparingTo("67");
    assertThat(saved.assumptions().maximumCacBrl()).isEqualByComparingTo("25");
    assertThat(saved.assumptions().periodDays()).isEqualTo(30);
    assertThat(saved.assumptions().preparation().supportDays()).isEqualTo(7);
    assertThat(saved.assumptions().ai().perAttempt()).isNull();
    assertThat(saved.assumptions().costs().otherVariableBrl()).isNull();
    assertThat(saved.assumptions().costs().fixedPerPeriodBrl()).isEqualByComparingTo("73.20");
    assertThat(saved.assumptions().variableCostEnvelope().amountPerCustomerBrl())
        .isEqualByComparingTo("13.50");
    assertThat(saved.assumptions().variableCostEnvelope().sourceReference())
        .isEqualTo("commercial-plan:95102@v4:variableCostPerSaleBrl");
    assertThat(saved.assumptions().fixedCostEnvelope().amountPerPeriodBrl())
        .isEqualByComparingTo("73.20");
    assertThat(saved.assumptions().fixedCostEnvelope().sourceReference())
        .isEqualTo("commercial-plan:95102@v4:fixedOperationalCostBrl");
    assertThat(saved.canRequestAnalysis()).isTrue();
    assertThat(saved.evaluation().status()).isEqualTo("READY_FOR_ANALYSIS");
    var preparedAgain =
        FinancialPlanPreparation.prepare(
            saved.assumptions(), request(0, 7, true), plan, saved.assumptions().priceBrl());
    assertThat(preparedAgain)
        .usingRecursiveComparison()
        .ignoringFields("realizedCostBaseline")
        .isEqualTo(saved.assumptions());
    assertThat(preparedAgain.realizedCostBaseline()).isNull();
    verifyNoInteractions(plutus);
  }

  /** Sugere trinta dias para tipos diferentes sem atrelar regra ao nome Capella. */
  @Test
  void otherTypeSuggestion() {
    product.getProductTypeDefinition().setCode("PDE");
    assertThat(service.preparation(product.getId(), Environment.TEST).supportDays()).isEqualTo(30);
  }

  /** Mantém custos e cálculos documentados quando as escolhas permanecem compatíveis. */
  @Test
  void preservesCompatibleSources() throws Exception {
    var a = complete();
    var result = FinancialPlanPreparation.prepare(a, request(0, 7, true), plan, a.priceBrl());
    assertThat(result.costs()).isEqualTo(a.costs());
    assertThat(result.ai()).isEqualTo(a.ai());
    assertThat(FinancialPlanCalculator.evaluate(result))
        .isEqualTo(FinancialPlanCalculator.evaluate(a));
    assertThat(result.evidence()).contains(a.evidence());
    var next = FinancialPlanPreparation.prepare(result, request(1, 7, true), plan, a.priceBrl());
    assertThat(next).isEqualTo(result);
  }

  /** Mudar suporte invalida seu custo sem apagar custos independentes nem alterar período. */
  @Test
  void changingSupportInvalidatesOnlySupportCost() throws Exception {
    var a = complete();
    var result = FinancialPlanPreparation.prepare(a, request(0, 30, true), plan, a.priceBrl());
    assertThat(result.costs().supportBrl()).isNull();
    assertThat(result.costs().initialAiBrl()).isEqualTo(a.costs().initialAiBrl());
    assertThat(result.periodDays()).isEqualTo(a.periodDays());
  }

  /** Alternar IA preserva investimento e exige que Plutus confira o envelope da nova escolha. */
  @Test
  void togglingAiPreservesInitialInvestmentAndRequiresRealPricing() throws Exception {
    var a = complete();
    var without = FinancialPlanPreparation.prepare(a, request(0, 7, false), plan, a.priceBrl());
    assertThat(without.ai().perAttempt()).isZero();
    assertThat(without.scenarios()).allMatch(s -> s.attemptsPerCustomer() == 0);
    assertThat(without.costs().initialAiBrl()).isEqualTo(a.costs().initialAiBrl());
    var with = FinancialPlanPreparation.prepare(without, request(1, 7, true), plan, a.priceBrl());
    assertThat(with.ai().perAttempt()).isNull();
    assertThat(FinancialPlanCalculator.evaluate(with).status()).isEqualTo("READY_FOR_ANALYSIS");
  }

  /** Mantém o bloqueio quando o plano comercial não informa o custo variável agregado. */
  @Test
  void missingCommercialEnvelopeRemainsUnknown() {
    plan.setVariableCostPerSaleBrl(null);
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    assertThat(saved.assumptions().variableCostEnvelope()).isNull();
    assertThat(saved.evaluation().status()).isEqualTo("MISSING_INPUTS");
    assertThat(saved.canRequestAnalysis()).isFalse();
  }

  /** Impede custo de Plutus quando a própria unidade já perde dinheiro após o CAC máximo. */
  @Test
  void negativeAggregateContributionBlocksPaidReview() {
    plan.setVariableCostPerSaleBrl(new BigDecimal("50"));
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    assertThat(saved.evaluation().status()).isEqualTo("REVIEW_REQUIRED");
    assertThat(saved.canRequestAnalysis()).isFalse();
    assertThat(saved.pendingActions()).anyMatch(value -> value.contains("contribuição após o CAC"));
    verifyNoInteractions(plutus);
  }

  /** Enfileira uma única revisão pronta com envelope e identidade comercial congelados. */
  @Test
  void readyAggregateRevisionCanRequestAuditablePlutusReview() {
    var persisted = new java.util.concurrent.atomic.AtomicReference<FinancialPlanRevision>();
    doAnswer(
            invocation -> {
              var revision = invocation.<FinancialPlanRevision>getArgument(0);
              revision.setId(999L);
              persisted.set(revision);
              return revision;
            })
        .when(revisions)
        .saveAndFlush(any());
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    when(revisions.findLockedById(saved.id())).thenReturn(Optional.of(persisted.get()));
    when(plutus.startRevenueProjection(eq(plan.getId()), any(StartRevenueProjectionRequest.class)))
        .thenReturn(
            new FinancialAgentExecutionResponse(
                777L,
                plan.getId(),
                FinancialAgentExecutionStatus.PENDING,
                "READ_ONLY_REVENUE_PROJECTION",
                4,
                888L,
                null,
                "{}",
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now()));
    var execution = new FinancialAgentExecution();
    execution.setId(777L);
    execution.setCommercialPlan(plan);
    execution.setStatus(FinancialAgentExecutionStatus.PENDING);
    when(executions.findById(777L)).thenReturn(Optional.of(execution));

    var requested = service.requestAnalysis(product.getId(), Environment.LIVE, saved.id());
    var retried = service.requestAnalysis(product.getId(), Environment.LIVE, saved.id());

    assertThat(requested.analysis().status()).isEqualTo("PENDING");
    assertThat(retried.analysis().executionId()).isEqualTo(requested.analysis().executionId());
    assertThat(saved.assumptions().realizedCostBaseline().campaignCostBrl())
        .isEqualByComparingTo("59.70");
    assertThat(saved.assumptions().realizedCostBaseline().aiCostBrl())
        .isEqualByComparingTo("69.95");
    assertThat(saved.assumptions().realizedCostBaseline().totalCostBrl())
        .isEqualByComparingTo("143.15");
    var context = org.mockito.ArgumentCaptor.forClass(StartRevenueProjectionRequest.class);
    verify(plutus, times(1)).startRevenueProjection(eq(plan.getId()), context.capture());
    assertThat(context.getValue().decisionContext())
        .contains(
            "READY_FOR_ANALYSIS",
            "ALL_VARIABLE_COSTS_EXCLUDING_CAC",
            "ALL_FIXED_OPERATIONAL_COSTS_FOR_PERIOD",
            "commercial-plan:95102@v4:variableCostPerSaleBrl",
            "CONDITIONAL_COMMERCIAL_TARGET_NOT_DEMAND_FORECAST",
            "DETERMINISTIC_SENSITIVITY_NOT_DEMAND_FORECAST",
            "EXISTING_PRODUCT_VERSION_INCREMENTAL_SALE",
            "CAPTURED_BEFORE_CURRENT_FINANCIAL_REVISION",
            "commercial-plan:95102:relational-realized-costs",
            "\"baseCustomers\":5",
            "\"baseProfitBrl\":69.30",
            "\"optimisticRecoveryCustomers\":8");
  }

  /**
   * Exibe o bloqueio antes do botão e recusa a chamada paga quando o cenário-base não está
   * definido.
   */
  @Test
  void missingCommercialTargetBlocksBeforePlutus() {
    product.setValidationDefinitionJson(
        "{\"format\":{\"type\":\"CUSTOM_VISUAL_ASSET_PACK\",\"valueUnit\":\"kit\"},"
            + "\"delivery\":{\"mode\":\"PERSONALIZED_ASSISTED\",\"personalization\":true}}");
    plan.setTargetRevenue(null);
    plan.setOperationalRevenueTarget(null);
    var persisted = new java.util.concurrent.atomic.AtomicReference<FinancialPlanRevision>();
    doAnswer(
            invocation -> {
              var revision = invocation.<FinancialPlanRevision>getArgument(0);
              revision.setId(999L);
              persisted.set(revision);
              return revision;
            })
        .when(revisions)
        .saveAndFlush(any());
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    when(revisions.findLockedById(saved.id())).thenReturn(Optional.of(persisted.get()));

    assertThat(saved.canRequestAnalysis()).isFalse();
    assertThat(saved.pendingActions())
        .anySatisfy(action -> assertThat(action).contains("meta de clientes ou receita"));

    assertThatThrownBy(() -> service.requestAnalysis(product.getId(), Environment.LIVE, saved.id()))
        .hasMessageContaining("meta de clientes ou receita");
    verifyNoInteractions(plutus);
  }

  /** Bloqueia o contrato de uso aberto antes de pedir que o modelo estime uma quota inexistente. */
  @Test
  void openEndedDeliveryBlocksBeforePlutus() {
    product.setValidationDefinitionJson(
        "{\"format\":{\"type\":\"CONTINUOUS_ASSISTANT\",\"valueUnit\":\"acesso contínuo\"},"
            + "\"delivery\":{\"mode\":\"PERSONALIZED_ASSISTED\",\"personalization\":true},"
            + "\"successEvidence\":{\"firstMilestoneSales\":5}}");
    var persisted = new java.util.concurrent.atomic.AtomicReference<FinancialPlanRevision>();
    doAnswer(
            invocation -> {
              var revision = invocation.<FinancialPlanRevision>getArgument(0);
              revision.setId(999L);
              persisted.set(revision);
              return revision;
            })
        .when(revisions)
        .saveAndFlush(any());
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    when(revisions.findLockedById(saved.id())).thenReturn(Optional.of(persisted.get()));

    assertThatThrownBy(() -> service.requestAnalysis(product.getId(), Environment.LIVE, saved.id()))
        .hasMessageContaining("unidade fixa de entrega");
    verifyNoInteractions(plutus);
  }

  /** Bloqueia custos realizados sobrepostos ao novo período antes de consumir outro parecer. */
  @Test
  void overlappingHistoricalCostsBlockBeforePlutus() {
    plan.setDeadline(LocalDate.now(ZoneOffset.UTC).plusDays(10));
    var persisted = new java.util.concurrent.atomic.AtomicReference<FinancialPlanRevision>();
    doAnswer(
            invocation -> {
              var revision = invocation.<FinancialPlanRevision>getArgument(0);
              revision.setId(999L);
              persisted.set(revision);
              return revision;
            })
        .when(revisions)
        .saveAndFlush(any());
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    when(revisions.findLockedById(saved.id())).thenReturn(Optional.of(persisted.get()));
    plan.setActualTotalCost(new BigDecimal("144.15"));

    assertThatThrownBy(() -> service.requestAnalysis(product.getId(), Environment.LIVE, saved.id()))
        .hasMessageContaining("novos custos realizados após esta revisão financeira");
    verifyNoInteractions(plutus);
  }

  /** Bloqueia meta que não cobre o custo fixo mesmo com contribuição unitária positiva. */
  @Test
  void nonPositiveBaseTargetBlocksBeforePlutus() {
    plan.setTargetRevenue(new BigDecimal("67"));
    plan.setOperationalRevenueTarget(new BigDecimal("67"));
    product.setValidationDefinitionJson(
        "{\"format\":{\"type\":\"CUSTOM_VISUAL_ASSET_PACK\",\"valueUnit\":\"kit\"},"
            + "\"delivery\":{\"mode\":\"PERSONALIZED_ASSISTED\",\"personalization\":true},"
            + "\"successEvidence\":{\"firstMilestoneSales\":1}}");
    var persisted = new java.util.concurrent.atomic.AtomicReference<FinancialPlanRevision>();
    doAnswer(
            invocation -> {
              var revision = invocation.<FinancialPlanRevision>getArgument(0);
              revision.setId(999L);
              persisted.set(revision);
              return revision;
            })
        .when(revisions)
        .saveAndFlush(any());
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    when(revisions.findLockedById(saved.id())).thenReturn(Optional.of(persisted.get()));

    assertThatThrownBy(() -> service.requestAnalysis(product.getId(), Environment.LIVE, saved.id()))
        .hasMessageContaining("não produz resultado-base positivo");
    verifyNoInteractions(plutus);
  }

  /** Substitui uma baseline enviada pelo cliente pelos custos relacionais calculados no backend. */
  @Test
  void forgedRealizedCostBaselineIsOverwritten() throws Exception {
    var assumptions =
        FinancialPlanPreparation.prepare(
            null, request(0, 7, true), plan, product.getCurrentPriceBrl());
    var node = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(assumptions);
    node.putObject("realizedCostBaseline")
        .put("campaignCostBrl", 999)
        .put("aiCostBrl", 999)
        .put("totalCostBrl", 1998)
        .put("sourceReference", "commercial-plan:95102:relational-realized-costs");
    var forged = json.treeToValue(node, PlanAssumptions.class);

    var saved =
        service.create(
            "PRODUCT",
            product.getId(),
            Environment.LIVE,
            new SavePlanRequest(
                "Plano sintético", "Operador local", 0, plan.getId(), null, forged));

    assertThat(saved.assumptions().realizedCostBaseline().campaignCostBrl())
        .isEqualByComparingTo("59.70");
    assertThat(saved.assumptions().realizedCostBaseline().aiCostBrl())
        .isEqualByComparingTo("69.95");
    assertThat(saved.assumptions().realizedCostBaseline().totalCostBrl())
        .isEqualByComparingTo("143.15");
  }

  /** Recusa envelopes textuais que não correspondem aos valores e à versão oficiais do plano. */
  @Test
  void forgedAggregateReferenceCannotUnlockReview() throws Exception {
    var assumptions =
        FinancialPlanPreparation.prepare(
            null, request(0, 7, true), plan, product.getCurrentPriceBrl());
    var node = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(assumptions);
    ((com.fasterxml.jackson.databind.node.ObjectNode) node.path("variableCostEnvelope"))
        .put("sourceReference", "commercial-plan:999@v1:variableCostPerSaleBrl");
    var forged = json.treeToValue(node, PlanAssumptions.class);

    assertThatThrownBy(
            () ->
                service.create(
                    "PRODUCT",
                    product.getId(),
                    Environment.LIVE,
                    new SavePlanRequest(
                        "Plano sintético", "Operador local", 0, plan.getId(), null, forged)))
        .hasMessageContaining("envelope variável diverge");

    ((com.fasterxml.jackson.databind.node.ObjectNode) node.path("variableCostEnvelope"))
        .put("sourceReference", "commercial-plan:95102@v4:variableCostPerSaleBrl");
    ((com.fasterxml.jackson.databind.node.ObjectNode) node.path("fixedCostEnvelope"))
        .put("amountPerPeriodBrl", 0);
    var forgedFixed = json.treeToValue(node, PlanAssumptions.class);
    assertThatThrownBy(
            () ->
                service.create(
                    "PRODUCT",
                    product.getId(),
                    Environment.LIVE,
                    new SavePlanRequest(
                        "Plano sintético", "Operador local", 0, plan.getId(), null, forgedFixed)))
        .hasMessageContaining("envelope fixo diverge");
    verifyNoInteractions(plutus);
  }

  /** Edição avançada não pode apresentar custo zero enquanto promete geração personalizada. */
  @Test
  void rejectsPricingContradictingPersonalization() throws Exception {
    var a = complete();
    var without = FinancialPlanPreparation.prepare(a, request(0, 7, false), plan, a.priceBrl());
    var node = json.valueToTree(without);
    ((com.fasterxml.jackson.databind.node.ObjectNode) node.path("preparation"))
        .put("personalizedAi", true);
    assertThat(
            FinancialPlanCalculator.evaluate(json.treeToValue(node, PlanAssumptions.class))
                .blockers())
        .anyMatch(b -> b.contains("personalizada incluída no envelope"));
  }

  /** Recusa submissão após outra revisão e não busca fontes de LIVE durante TEST. */
  @Test
  void concurrencyAndEnvironmentIsolation() throws Exception {
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 14, false), null);
    prior(saved.assumptions());
    assertThat(service.preparation(product.getId(), Environment.LIVE).personalizedAi()).isFalse();
    assertThat(service.preparation(product.getId(), Environment.LIVE).supportDays()).isEqualTo(14);
    assertThatThrownBy(
            () -> service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("contexto mudou");
    assertThat(service.preparation(product.getId(), Environment.TEST).sourceRevisionId()).isNull();
    assertThatThrownBy(() -> service.preparation(95103L, Environment.LIVE))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Alteração comercial ou contratual cria rebase sem reaproveitar custos da revisão anterior. */
  @Test
  void commercialAndProductDriftRebaseCurrentReferences() throws Exception {
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    var old = prior(saved.assumptions());
    old.setCommercialPlanVersion(3);
    product.setValidationDefinitionVersion("fixture-v2");
    plan.setVariableCostPerSaleBrl(new BigDecimal("14.75"));
    plan.setFixedOperationalCostBrl(new BigDecimal("80"));
    var currentVersion = new CommercialPlanVersion();
    currentVersion.setVersionNumber(5);
    when(versions.findTopByPlanIdOrderByVersionNumberDesc(plan.getId()))
        .thenReturn(Optional.of(currentVersion));

    var preview = service.preparation(product.getId(), Environment.LIVE);
    assertThat(preview.canPrepare()).isTrue();
    assertThat(preview.productVersion()).isEqualTo("fixture-v2");
    assertThat(preview.commercialPlanVersion()).isEqualTo(5);
    assertThat(preview.suggestion()).contains("descarta custos anteriores");

    var rebased =
        service.prepare(
            product.getId(),
            Environment.LIVE,
            new PreparePlanRequest(1, plan.getId(), 5, "fixture-v2", 7, true),
            null);
    assertThat(rebased.stale()).isFalse();
    assertThat(rebased.assumptions().productVersion()).isEqualTo("fixture-v2");
    assertThat(rebased.assumptions().variableCostEnvelope().amountPerCustomerBrl())
        .isEqualByComparingTo("14.75");
    assertThat(rebased.assumptions().variableCostEnvelope().sourceReference())
        .isEqualTo("commercial-plan:95102@v5:variableCostPerSaleBrl");
    assertThat(rebased.assumptions().fixedCostEnvelope().amountPerPeriodBrl())
        .isEqualByComparingTo("80");
    assertThat(rebased.assumptions().evidence()).doesNotContain(saved.assumptions().evidence());
  }

  /** Validade encerrada ou tipo alterado continuam exigindo revisão avançada das fontes. */
  @Test
  void expiredOrTypeChangedRevisionCannotRebaseAutomatically() throws Exception {
    var saved = service.prepare(product.getId(), Environment.LIVE, request(0, 7, true), null);
    var expiredNode =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(saved.assumptions());
    expiredNode.put("validUntil", LocalDate.now(ZoneOffset.UTC).minusDays(1).toString());
    var expired = prior(json.treeToValue(expiredNode, PlanAssumptions.class));
    expired.setCommercialPlanVersion(3);
    assertThat(service.preparation(product.getId(), Environment.LIVE).canPrepare()).isFalse();

    var current = prior(saved.assumptions());
    current.setCommercialPlanVersion(3);
    product.getProductTypeDefinition().setId(952L);
    assertThat(service.preparation(product.getId(), Environment.LIVE).canPrepare()).isFalse();
  }

  /** Seleção ambígua sem histórico continua bloqueada para evitar escolher outro plano. */
  @Test
  void ambiguousCommercialPlanWithoutHistoryBlocks() {
    when(plans.findByProductId(product.getId())).thenReturn(List.of(plan, new CommercialPlan()));
    assertThat(service.preparation(product.getId(), Environment.TEST).canPrepare()).isFalse();
  }

  /** Valida limites pelo endpoint real, inclusive números fracionários que não podem truncar. */
  @Test
  void httpRejectsInvalidDaysAndAcceptsTwoChoices() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new FinancialPlanController(
                    service,
                    new FinancialContributionTargetService(
                        service, Validation.buildDefaultValidatorFactory().getValidator())))
            .build();
    for (String days : List.of("0", "-1", "1.5", "3661", "null")) {
      mvc.perform(
              post("/api/financial-plans/v1/products/95101/preparation")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"expectedRevision\":0,\"commercialPlanId\":95102,\"commercialPlanVersion\":4,\"productVersion\":\"fixture-v1\",\"supportDays\":"
                          + days
                          + "}"))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            post("/api/financial-plans/v1/products/95101/preparation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request(0, 7, true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assumptions.preparation.personalizedAi").value(true));
  }

  /**
   * A meta chega pelo controller real, conserva custos desconhecidos e não solicita parecer pago.
   */
  @Test
  void contributionTargetUsesCanonicalPersistenceAndPreservesUnknowns() throws Exception {
    var node = json.valueToTree(complete());
    ((com.fasterxml.jackson.databind.node.ObjectNode) node).putNull("minimumMarginPercent");
    ((com.fasterxml.jackson.databind.node.ObjectNode) node.path("ai")).putNull("perAttempt");
    var source = prior(json.treeToValue(node, PlanAssumptions.class));
    when(revisions.findById(source.getId())).thenReturn(Optional.of(source));
    String original = source.getAssumptionsJson();
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new FinancialPlanController(
                    service,
                    new FinancialContributionTargetService(
                        service, Validation.buildDefaultValidatorFactory().getValidator())))
            .build();
    var response =
        mvc.perform(
                post("/api/financial-plans/v1/products/95101/contribution-target")
                    .principal(() -> "Operador local")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sourceRevisionId\":998,\"minimumMarginPercent\":31.25}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revision").value(2))
            .andExpect(jsonPath("$.assumptions.minimumMarginPercent").value(31.25))
            .andExpect(jsonPath("$.assumptions.ai.perAttempt").isEmpty())
            .andExpect(jsonPath("$.canRequestAnalysis").value(false))
            .andExpect(jsonPath("$.createdBy").value("Operador local"))
            .andReturn();
    assertThat(source.getAssumptionsJson()).isEqualTo(original);
    var saved = json.readTree(response.getResponse().getContentAsByteArray());
    var expected = json.readTree(original);
    expected
        .fields()
        .forEachRemaining(
            field -> {
              if (!Set.of("minimumMarginPercent", "realizedCostBaseline").contains(field.getKey()))
                assertThat(saved.path("assumptions").path(field.getKey()))
                    .isEqualTo(field.getValue());
            });
    verifyNoInteractions(plutus);
    assertThat(saved.path("analysis").isNull()).isTrue();
  }

  /** O endpoint recusa metas inválidas e IDs fracionários antes de qualquer gravação. */
  @Test
  void contributionTargetRejectsInvalidHttpInputs() throws Exception {
    var target = mock(FinancialContributionTargetService.class);
    var mvc = MockMvcBuilders.standaloneSetup(new FinancialPlanController(service, target)).build();
    for (String value : List.of("null", "0", "-1", "100"))
      mvc.perform(
              post("/api/financial-plans/v1/products/95101/contribution-target")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"sourceRevisionId\":998,\"minimumMarginPercent\":" + value + "}"))
          .andExpect(status().isBadRequest());
    for (String id : List.of("null", "0", "-1", "998.5", "\"998\"", "9223372036854775808"))
      mvc.perform(
              post("/api/financial-plans/v1/products/95101/contribution-target")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"sourceRevisionId\":" + id + ",\"minimumMarginPercent\":31.25}"))
          .andExpect(status().isBadRequest());
    verifyNoInteractions(target);
    verify(revisions, never()).saveAndFlush(any());
  }

  /** Voltar a uma meta anterior cria a nova decisão vigente em vez de responder com história. */
  @Test
  void restoringHistoricalTargetCreatesCurrentRevision() throws Exception {
    var seed =
        service.create(
            "PRODUCT",
            product.getId(),
            Environment.LIVE,
            new SavePlanRequest(
                "Plano sintético", "Harness local", 0, plan.getId(), null, complete()));
    var historical = prior(seed.assumptions());
    historical.setId(996L);
    var changed = json.valueToTree(seed.assumptions());
    ((com.fasterxml.jackson.databind.node.ObjectNode) changed)
        .put("minimumMarginPercent", new BigDecimal("35"));
    var latest = prior(json.treeToValue(changed, PlanAssumptions.class));
    latest.setId(998L);
    latest.setRevisionNumber(2);
    when(revisions.findById(998L)).thenReturn(Optional.of(latest));
    when(revisions.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", product.getId(), Environment.LIVE))
        .thenReturn(List.of(latest, historical));
    clearInvocations(revisions);
    var target =
        new FinancialContributionTargetService(
            service, Validation.buildDefaultValidatorFactory().getValidator());
    var saved =
        target.save(
            product.getId(),
            Environment.LIVE,
            new com.marketinghub.financialplan.v1.service.contributiontarget
                .SaveContributionTargetRequest(998L, seed.assumptions().minimumMarginPercent()),
            "Operador local");
    assertThat(saved.revision()).isEqualTo(3);
    assertThat(saved.id()).isEqualTo(999L);
    verify(revisions).saveAndFlush(any());
    verifyNoInteractions(plutus);
  }
}
