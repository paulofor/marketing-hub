package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.service.pending.AgentTaskPreparationBudgetGuidance;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.controller.LearningCycleController;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.*;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jdbc.learningcycle.CyclePreparationCostRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: comprovar aprovação financeira recuperável, segregada e entregue aos agentes.
 */
class LearningCycleFinancialReviewTest {
  final LearningCycleJson json = new LearningCycleJson(new ObjectMapper().findAndRegisterModules());
  final LearningSalesCycleEventRepository events = mock(LearningSalesCycleEventRepository.class);
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final ProductRepository products = mock(ProductRepository.class);
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final CyclePreparationCostRepository costs = mock(CyclePreparationCostRepository.class);
  final CommercialPlanRepository commercial = mock(CommercialPlanRepository.class);
  final FinancialPlanService plans = mock(FinancialPlanService.class);
  final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  final LearningCyclePreparationBudget budget = new LearningCyclePreparationBudget(events, json);
  final LearningCycleVideoBudget video = new LearningCycleVideoBudget(events, json);
  final LearningCycleFinancialReview review =
      new LearningCycleFinancialReview(
          costs, budget, video, commercial, plans, new LearningCycleFinancialAdvice(tasks, json));
  final List<LearningSalesCycleEvent> history = new ArrayList<>();
  final LearningSalesCycle cycle = new LearningSalesCycle();
  final Product product = Product.builder().id(7L).name("Produto de teste").build();
  final Instant now = Instant.parse("2026-10-30T16:00:00Z");
  MockMvc http;

  /** Monta controller e serviço reais com ledger simulado e nenhuma integração externa. */
  @BeforeEach
  void setup() {
    cycle.setId(19L);
    cycle.setProductId(7L);
    cycle.setExperimentId(112L);
    cycle.setChainDefinitionId(27L);
    cycle.setProductVersion("teste-v3");
    cycle.setStatus("OPEN");
    cycle.setStage("PLANNING");
    cycle.setRevision(1);
    cycle.setVersionChangedAt(now.minusSeconds(60));
    cycle.setBudgetLimitBrl(new BigDecimal("89.95"));
    when(events.findByCycleIdAndActionOrderByRevisionDesc(anyLong(), anyString()))
        .thenAnswer(
            inv ->
                history.reversed().stream()
                    .filter(
                        e ->
                            e.getCycleId().equals(inv.getArgument(0))
                                && e.getAction().equals(inv.getArgument(1)))
                    .toList());
    when(events.findByCycleIdAndRequestKey(anyLong(), anyString()))
        .thenAnswer(
            inv ->
                history.stream()
                    .filter(
                        e ->
                            e.getCycleId().equals(inv.getArgument(0))
                                && e.getRequestKey().equals(inv.getArgument(1)))
                    .findFirst());
    when(events.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              LearningSalesCycleEvent e = inv.getArgument(0);
              e.setId((long) history.size() + 1);
              history.add(e);
              return e;
            });
    when(costs.summarize(7L))
        .thenReturn(List.of(new CycleFinancialReview.CostSlice(110L, new BigDecimal("38.89"), 2)));
    when(cycles.findById(19L)).thenReturn(Optional.of(cycle));
    when(cycles.findLocked(7L, 19L)).thenReturn(Optional.of(cycle));
    when(cycles.findByExperimentId(112L)).thenReturn(Optional.of(cycle));
    when(products.findById(7L)).thenReturn(Optional.of(product));
    when(products.findLockedById(7L)).thenReturn(Optional.of(product));
    var exp = new Experiment();
    exp.setId(112L);
    exp.setProduct(product);
    when(experiments.findById(112L)).thenReturn(Optional.of(exp));
    var service =
        new LearningCycleService(
            cycles,
            events,
            products,
            experiments,
            null,
            null,
            null,
            json,
            null,
            null,
            null,
            null,
            null);
    ReflectionTestUtils.setField(service, "financialReview", review);
    ReflectionTestUtils.setField(service, "preparationBudget", budget);
    http = MockMvcBuilders.standaloneSetup(new LearningCycleController(service)).build();
  }

  /** Gera uma decisão de teste mantendo o mesmo orçamento total entre ciclos. */
  private PreparationBudgetRequest decision() {
    return new PreparationBudgetRequest(
        UUID.randomUUID(),
        1L,
        27L,
        112L,
        "teste-v3",
        new BigDecimal("50.00"),
        new BigDecimal("3.50"),
        "Administrador local",
        "Reserva histórica conferida nas fontes",
        true);
  }

  /** Valida ida, aprovação, recibo e recuperação sem iniciar tarefas ou duplicar custo. */
  @Test
  void readsApprovesAndReplaysWithoutChangingMediaOrStage() throws Exception {
    String url = "/api/business-process-chains/learning-cycles/v1/products/7/19";
    http.perform(get(url + "/financial-review?chainId=27"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorization").isEmpty())
        .andExpect(jsonPath("$.availableUsd").isEmpty())
        .andExpect(jsonPath("$.advice.status").value("UNAVAILABLE"))
        .andExpect(jsonPath("$.unreportedTasks").value(2));
    String input = json.write(decision());
    for (int i = 0; i < 2; i++)
      http.perform(
              post(url + "/preparation-budget")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(input))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.availableUsd").value(7.61))
          .andExpect(jsonPath("$.revision").value(2));
    assertThat(history).hasSize(1);
    assertThat(cycle.getStage()).isEqualTo("PLANNING");
    assertThat(cycle.getBudgetLimitBrl()).isEqualByComparingTo("89.95");
    assertThat(json.read(history.getFirst().getEvidenceJson()).path("mediaAuthorized").asBoolean())
        .isFalse();
    verifyNoInteractions(plans);
  }

  /** Entrega conselho do planejamento pelo endpoint mesmo sem plano comercial vinculado. */
  @Test
  void exposesPlanningAdviceWithoutCreatingFinancialRevisionOrTasks() throws Exception {
    when(tasks.findFinancialAdviceSnapshots(eq("experiment:112"), any()))
        .thenReturn(
            List.of(
                new FinancialTaskSnapshot(
                    90883L,
                    126L,
                    "COMPLETED",
                    now,
                    now,
                    true,
                    """
            {"mode":"AGENT_VALIDATION_HYPOTHESIS","decision":"APPROVE",
             "economics":{"offerPriceBrl":100,"variableCostPerSaleBrl":60,"contributionPerSaleBrl":40},
             "rationale":"Hipótese interna de teste, antes dos anúncios."}
            """)));
    http.perform(
            get(
                "/api/business-process-chains/learning-cycles/v1/products/7/19/financial-review?chainId=27"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.financialPlan").isEmpty())
        .andExpect(jsonPath("$.advice.status").value("PREPARATION_ONLY"))
        .andExpect(jsonPath("$.advice.sourceId").value(90883))
        .andExpect(jsonPath("$.advice.figures[2].value").value(40))
        .andExpect(
            jsonPath("$.advice.caution")
                .value(org.hamcrest.Matchers.containsString("não é lucro líquido")));
    verify(tasks, never()).save(any());
    verifyNoInteractions(plans);
    assertThat(history).isEmpty();
  }

  /** Recusa revisão vencida, confirmação ausente, valores negativos e contexto divergente. */
  @ParameterizedTest
  @CsvSource({
    "expectedRevision,0,409",
    "experimentId,999,409",
    "chainDefinitionId,999,409",
    "confirmed,false,400",
    "totalLimitUsd,-1,400",
    "reserveUsd,-1,400"
  })
  void rejectsInvalidDecisions(String field, String value, int status) throws Exception {
    var data = (com.fasterxml.jackson.databind.node.ObjectNode) json.read(json.write(decision()));
    data.set(field, json.read(value));
    http.perform(
            post("/api/business-process-chains/learning-cycles/v1/products/7/19/preparation-budget")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.write(data)))
        .andExpect(status().is(status));
    assertThat(history).isEmpty();
  }

  /** Uma chave já gravada não admite alterar limite silenciosamente. */
  @Test
  void rejectsChangedReplayAndClosedCycle() {
    var request = decision();
    budget.authorize(cycle, request, BigDecimal.ONE, 0, now);
    var changed =
        new PreparationBudgetRequest(
            request.requestKey(),
            1L,
            27L,
            112L,
            "teste-v3",
            BigDecimal.TEN,
            BigDecimal.ZERO,
            "Outro",
            "Mudança",
            true);
    assertThatThrownBy(() -> budget.authorize(cycle, changed, BigDecimal.ONE, 0, now))
        .hasMessageContaining("outros valores");
    cycle.setStatus("ADJUSTED");
    var closed =
        new PreparationBudgetRequest(
            UUID.randomUUID(),
            2L,
            27L,
            112L,
            "teste-v3",
            BigDecimal.TEN,
            BigDecimal.ZERO,
            "Outro",
            "Mudança",
            true);
    assertThatThrownBy(() -> budget.authorize(cycle, closed, BigDecimal.ONE, 0, now))
        .hasMessageContaining("encerrado");
  }

  /** O recibo chega ao executor correto e não transita para outro produto, versão ou sucessor. */
  @Test
  void deliversReceiptWithoutInheritingAuthorization() {
    budget.authorize(cycle, decision(), BigDecimal.ONE, 0, now);
    var guidance = new AgentTaskPreparationBudgetGuidance(cycles, budget);
    var context = guidance.read(7L, "experiment:112");
    assertThat(context.contractVersion()).isEqualTo("PREPARATION_BUDGET_V1");
    assertThat(context.authorization().totalLimitUsd()).isEqualByComparingTo("50.00");
    assertThat(context.financialReviewUrl()).contains("cycleId=19");
    assertThat(context.authorizesMediaOrAudiovisual()).isFalse();
    assertThat(guidance.read(4L, "experiment:112")).isNull();
    assertThat(guidance.read(7L, "experiment:113")).isNull();
    cycle.setProductVersion("teste-v4");
    assertThat(guidance.read(7L, "experiment:112")).isNull();
    cycle.setProductVersion("teste-v3");
    cycle.setStatus("ADJUSTED");
    assertThat(guidance.read(7L, "experiment:112")).isNull();
  }

  /**
   * Verifica que pending entrega objeto JSON nativo preservando integralmente as notas históricas.
   */
  @Test
  void serializesStructuredReceiptAlongsideOriginalNotes() {
    budget.authorize(cycle, decision(), BigDecimal.ONE, 0, now);
    product.setCommercialNotes("Notas anteriores preservadas");
    var reader = new com.marketinghub.agenttask.service.pending.AgentTaskOperatorGuidance(products);
    ReflectionTestUtils.setField(
        reader, "preparationGuidance", new AgentTaskPreparationBudgetGuidance(cycles, budget));
    var target = mock(com.marketinghub.agenttask.AgentTaskTargetResponse.class);
    when(target.productId()).thenReturn(7L);
    when(target.sourceReference()).thenReturn("experiment:112");
    var task = mock(com.marketinghub.agenttask.AgentTaskPendingResponse.class);
    when(task.taskTarget()).thenReturn(target);
    when(task.sourceReference()).thenReturn("experiment:112");
    var context = json.read(json.write(reader.read(task)));
    assertThat(context.path("commercialNotes").asText()).isEqualTo("Notas anteriores preservadas");
    assertThat(context.path("preparationBudget").isObject()).isTrue();
    assertThat(context.at("/preparationBudget/authorization/totalLimitUsd").decimalValue())
        .isEqualByComparingTo("50.00");
  }

  /** Mantém parecer da revisão associada e não escolhe arbitrariamente entre planos. */
  @Test
  void showsActualPlutusAndPreservesStaleness() {
    var commercialPlan = new com.marketinghub.planning.CommercialPlan();
    commercialPlan.setId(800L);
    var plan =
        new com.marketinghub.financialplan.v1.service.getplan.PlanView(
            300L,
            "products",
            7L,
            com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment.LIVE,
            "Plano local",
            2,
            null,
            800L,
            1,
            "Operador",
            now,
            null,
            new com.marketinghub.financialplan.v1.service.getplan.PlanEvaluation(
                "PROJECTED", "Projeção", List.of(), List.of()),
            true,
            List.of("Atualizar referências"),
            false,
            new com.marketinghub.financialplan.v1.service.getplan.PlanView.Analysis(
                900L,
                "COMPLETED",
                "Conferir custos",
                json.read("{\"decision\":\"ADJUST\"}"),
                null,
                "modelo",
                BigDecimal.ONE,
                "ESTIMATED",
                now));
    when(commercial.findByExperimentReference(112L)).thenReturn(List.of(commercialPlan));
    when(plans.latestForPlan(
            "products",
            7L,
            com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment.LIVE,
            800L))
        .thenReturn(Optional.of(plan));
    var result = review.read(cycle, product, now);
    assertThat(result.financialPlan()).isSameAs(plan);
    assertThat(result.financialPlan().stale()).isTrue();
    assertThat(result.planUrl()).contains("commercialPlanId=800", "revisionId=300");
    when(commercial.findByExperimentReference(112L))
        .thenReturn(List.of(commercialPlan, new com.marketinghub.planning.CommercialPlan()));
    assertThat(review.read(cycle, product, now).financialPlan()).isNull();
  }

  /** Recusa acesso cruzado e preserva notas anteriores sem converter valor textual em teto. */
  @Test
  void rejectsOtherProductAndDoesNotParseOldNotes() throws Exception {
    product.setCommercialNotes("Autorizo US$ 500 para outro ciclo");
    assertThat(review.read(cycle, product, now).authorization()).isNull();
    http.perform(
            get(
                "/api/business-process-chains/learning-cycles/v1/products/4/19/financial-review?chainId=27"))
        .andExpect(status().isNotFound());
  }

  /** Mantém custo desconhecido e saldo ausente em outro produto sem estimativas recuperáveis. */
  @Test
  void unknownCostsAreNotZeroForAnotherProduct() {
    cycle.setProductId(4L);
    cycle.setId(18L);
    cycle.setExperimentId(111L);
    when(costs.summarize(4L))
        .thenReturn(List.of(new CycleFinancialReview.CostSlice(111L, null, 1)));
    var response = review.read(cycle, Product.builder().id(4L).name("Outro produto").build(), now);
    assertThat(response.knownPreparationUsd()).isNull();
    assertThat(response.availableUsd()).isNull();
    assertThat(response.cycleUrl()).contains("productId=4", "cycleId=18");
    assertThat(response.financialPlan()).isNull();
  }
}
