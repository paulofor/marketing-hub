package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.controller.LearningCycleController;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.planningAlignment.*;
import com.marketinghub.experiment.*;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.product.Product;
import com.marketinghub.product.bigidea.v1.*;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: impedir oferta antiga, reescrita histórica e consumo na correção do
 * planejamento.
 */
class LearningCyclePlanningAlignmentTest {
  private final ProductRepository products = mock(ProductRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final ProductBigIdeaBindingRepository bindings =
      mock(ProductBigIdeaBindingRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final LearningCycleJson json = new LearningCycleJson(mapper);
  private final LearningCyclePlanningDefinition definitions =
      new LearningCyclePlanningDefinition(bindings, json);
  private final LearningCyclePlanningAlignment alignment =
      new LearningCyclePlanningAlignment(
          products, cycles, experiments, events, bindings, tasks, definitions, json);
  private Product product;
  private LearningSalesCycle cycle;
  private Experiment experiment;
  private ProductBigIdeaBinding binding;
  private LearningSalesCycleEvent receipt;

  /** Reproduz a contradição real com a oferta antiga e a direção contínua já registrada. */
  private void fixture(long id) {
    var idea = new ProductBigIdea();
    idea.setId(id + 100);
    idea.setProductId(id);
    idea.setRevision(3);
    product =
        Product.builder()
            .id(id)
            .name("Aplicativo " + id)
            .bigIdea(idea)
            .automaticExecutionEnabled(true)
            .productFormat("Aplicativo de uso contínuo")
            .revenueModel("Base gratuita e premium opcional")
            .validationDefinitionVersion("continuo-v1")
            .validationDefinitionJson(
                "{\"economics\":{\"priceBrl\":null,\"billingModel\":\"FREEMIUM_OPTIONAL_SUBSCRIPTION\",\"pricingStatus\":\"A definir com Plutus\",\"usageLimits\":\"Limites sustentáveis antes da oferta\"}}")
            .build();
    cycle = new LearningSalesCycle();
    cycle.setId(id + 200);
    cycle.setProductId(id);
    cycle.setExperimentId(id + 300);
    cycle.setProductVersion("continuous-v1");
    cycle.setStatus("OPEN");
    cycle.setStage("PLANNING");
    cycle.setRevision(1);
    cycle.setBudgetLimitBrl(BigDecimal.ZERO);
    cycle.setCreatedAt(Instant.parse("2026-11-07T15:14:35Z"));
    cycle.setBriefJson(
        "{\"hypothesis\":\"Uso contínuo, combinações salvas e premium opcional\",\"offer\":\"Dia 1 gratuito; Dias 2 a 7 por R$ 67, pagamento único e 90 dias\",\"preparationOnly\":true}");
    cycle.setCreationJson(cycle.getBriefJson());
    cycle.setInheritedLearningJson("{\"previousCycleId\":21}");
    var hypothesis = new Hypothesis();
    hypothesis.setId(UUID.randomUUID());
    experiment =
        Experiment.builder()
            .id(cycle.getExperimentId())
            .product(product)
            .hypothesisRef(hypothesis)
            .hypothesis("Hipótese antiga")
            .unitPrice(new BigDecimal("67"))
            .status(ExperimentStatus.PLANNED)
            .dailyBudget(BigDecimal.ZERO)
            .mediaSpendLimit(BigDecimal.ZERO)
            .totalCost(new BigDecimal("1.754278"))
            .build();
    binding = new ProductBigIdeaBinding();
    binding.setProductId(id);
    binding.setBigIdea(idea);
    when(products.findById(id)).thenReturn(Optional.of(product));
    when(products.findLockedById(id)).thenReturn(Optional.of(product));
    when(cycles.findById(cycle.getId())).thenReturn(Optional.of(cycle));
    when(cycles.findLocked(id, cycle.getId())).thenReturn(Optional.of(cycle));
    when(experiments.findById(experiment.getId())).thenReturn(Optional.of(experiment));
    when(bindings.findByProductIdAndSourceReference(id, "experiment:" + experiment.getId()))
        .thenReturn(Optional.of(binding));
    when(events.save(any()))
        .thenAnswer(
            call -> {
              receipt = call.getArgument(0);
              return receipt;
            });
  }

  /** Produz uma decisão de correção, sem valor de teto nem permissão comercial. */
  private PlanningAlignmentRequest request() {
    return new PlanningAlignmentRequest(
        UUID.randomUUID(), 1, 3, "Operador de teste", "Corrigir entrada já aprovada");
  }

  /** O caso original e outro produto recebem a mesma correção, sem exceções por nome ou ID. */
  @ParameterizedTest
  @ValueSource(longs = {4, 83})
  void alignsPrivatePlanningAndPreservesHistoryCostsAndPermissions(long id) {
    fixture(id);
    String creation = cycle.getCreationJson();
    String inherited = cycle.getInheritedLearningJson();
    var oldHypothesis = experiment.getHypothesisRef();
    var before = alignment.preview(id, cycle.getId());
    assertThat(before.available()).isTrue();
    assertThat(before.proposedPriceBrl()).isNull();
    assertThat(before.proposedOffer())
        .contains("uso contínuo", "Base gratuita", "premium opcional", "Plutus")
        .doesNotContain("67", "90 dias", "sete dias");
    verify(events, never()).save(any());
    alignment.apply(id, cycle.getId(), request());
    assertThat(experiment.getUnitPrice()).isNull();
    assertThat(experiment.getHypothesisRef()).isNull();
    assertThat(experiment.getHypothesis()).contains("Uso contínuo");
    assertThat(oldHypothesis.getId()).isNotNull();
    assertThat(cycle.getCreationJson()).isEqualTo(creation);
    assertThat(cycle.getInheritedLearningJson()).isEqualTo(inherited);
    assertThat(cycle.getRevision()).isEqualTo(2);
    assertThat(cycle.getStage()).isEqualTo("PLANNING");
    assertThat(experiment.getTotalCost()).isEqualByComparingTo("1.754278");
    assertThat(experiment.getDailyBudget()).isZero();
    assertThat(experiment.getMediaSpendLimit()).isZero();
    assertThat(experiment.getCommercialCheckoutUrl()).isNull();
    assertThat(experiment.getFacebookReleaseRequestedAt()).isNull();
    assertThat(receipt.getAction()).isEqualTo("RECONCILE_PLANNING");
    assertThat(json.read(receipt.getEvidenceJson()).at("/before/brief/offer").asText())
        .contains("67");
    assertThat(
            json.read(receipt.getEvidenceJson())
                .at("/planningDefinition/billingAuthorized")
                .asBoolean())
        .isFalse();
    assertThat(alignment.preview(id, cycle.getId()).available()).isFalse();
    verify(tasks, never()).save(any());
    verify(events).save(any());
  }

  /**
   * Repetição idêntica reutiliza o recibo; outra intenção e revisão concorrente não alteram nada.
   */
  @Test
  void idempotencyAndStaleRevision() {
    fixture(4);
    var request = request();
    alignment.apply(4L, cycle.getId(), request);
    when(events.findByCycleIdAndRequestKey(cycle.getId(), request.requestKey().toString()))
        .thenReturn(Optional.of(receipt));
    alignment.apply(4L, cycle.getId(), request);
    assertThat(cycle.getRevision()).isEqualTo(2);
    verify(events).save(any());
    assertThatThrownBy(
            () ->
                alignment.apply(
                    4L,
                    cycle.getId(),
                    new PlanningAlignmentRequest(
                        request.requestKey(), 1, 3, "Outro", "Outra decisão")))
        .hasMessageContaining("chave");
    assertThatThrownBy(() -> alignment.apply(4L, cycle.getId(), request()))
        .hasMessageContaining("ciclo mudou");
    verify(experiments).save(any());
  }

  /** Histórico, STOP, direção divergente e trabalho já aceito não permitem mudar a entrada. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "CLOSED",
        "STAGE",
        "BASELINE",
        "RUNNING",
        "EXPOSED",
        "STOP",
        "WRONG_PRODUCT",
        "WRONG_CYCLE",
        "BINDING",
        "TASK",
        "BIG_IDEA_REVISION",
        "PRICE",
        "MISSING_ECONOMICS"
      })
  void refusesIneligibleContextWithoutWrites(String scenario) {
    fixture(4);
    switch (scenario) {
      case "CLOSED" -> cycle.setStatus("CLOSED");
      case "STAGE" -> cycle.setStage("VALIDATION");
      case "BASELINE" -> cycle.setBaseline(true);
      case "RUNNING" -> experiment.setStatus(ExperimentStatus.RUNNING);
      case "EXPOSED" -> experiment.setFacebookReleaseRequestedAt(Instant.now());
      case "STOP" -> product.setAutomaticExecutionEnabled(false);
      case "WRONG_PRODUCT" -> experiment.setProduct(Product.builder().id(55L).build());
      case "WRONG_CYCLE" -> cycle.setProductId(55L);
      case "BINDING" -> {
        var old = new ProductBigIdea();
        old.setId(999L);
        binding.setBigIdea(old);
      }
      case "TASK" ->
          when(tasks.existsBySourceReferenceAndStatusIn(anyString(), anyList())).thenReturn(true);
      case "BIG_IDEA_REVISION" -> product.getBigIdea().setRevision(4);
      case "PRICE" -> product.setCurrentPriceBrl(new BigDecimal("67"));
      case "MISSING_ECONOMICS" -> product.setValidationDefinitionJson("{}");
      default -> throw new IllegalArgumentException(scenario);
    }
    assertThatThrownBy(() -> alignment.apply(4L, cycle.getId(), request()))
        .isInstanceOf(RuntimeException.class);
    verify(events, never()).save(any());
    verify(cycles, never()).save(any());
    verify(experiments, never()).save(any());
    verify(tasks, never()).save(any());
  }

  /** A nova definição só substitui a oferta do sucessor quando existe mudança rastreável. */
  @Test
  void detectsChangedAndStableDirection() {
    fixture(4);
    assertThat(definitions.changedSince(experiment)).isFalse();
    var old = new ProductBigIdea();
    old.setId(22L);
    binding.setBigIdea(old);
    assertThat(definitions.changedSince(experiment)).isTrue();
    assertThat(definitions.snapshot(product).path("economics").path("priceBrl").isNull()).isTrue();
  }

  /** O contrato HTTP entrega a prévia e aplica o mesmo serviço sem executar agentes externos. */
  @Test
  void httpPreviewAndApplyUseRealAlignment() throws Exception {
    fixture(4);
    var facade = mock(LearningCycleService.class);
    when(facade.planningAlignment(4L, cycle.getId()))
        .thenAnswer(call -> alignment.preview(4L, cycle.getId()));
    doAnswer(
            call -> {
              alignment.apply(4L, cycle.getId(), call.getArgument(2));
              return null;
            })
        .when(facade)
        .reconcilePlanning(eq(4L), eq(cycle.getId()), any());
    var http = MockMvcBuilders.standaloneSetup(new LearningCycleController(facade)).build();
    String path =
        "/api/business-process-chains/learning-cycles/v1/products/4/"
            + cycle.getId()
            + "/planning-alignment";
    http.perform(get(path))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.bigIdeaRevision").value(3));
    http.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.write(request())))
        .andExpect(status().isOk());
    assertThat(experiment.getUnitPrice()).isNull();
    assertThat(receipt).isNotNull();
    http.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  /** O alinhamento real libera uma análise e uma nova recusa volta a bloquear a mesma entrada. */
  @ParameterizedTest
  @ValueSource(longs = {4, 83})
  void resumesAtStrategyOnlyAfterActualCorrectionAndNeverLoops(long id) {
    fixture(id);
    var process = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    process.setId(126L);
    process.setProcessCode("pde-commercial-plan-offer");
    process.setVersionNumber(13);
    var activity = new com.marketinghub.businessprocess.BusinessProcessActivityDefinition();
    activity.setActivityId("marketStrategy");
    var input = mock(LearningCycleImplementedInputContext.class);
    when(input.resolve(cycle)).thenReturn(Optional.empty());
    when(cycles.findByExperimentId(experiment.getId())).thenReturn(Optional.of(cycle));
    var history = new java.util.ArrayList<com.marketinghub.agenttask.AgentTaskFunctionalSnapshot>();
    history.add(
        new com.marketinghub.agenttask.AgentTaskFunctionalSnapshot(
            906L,
            126L,
            process.getProcessCode(),
            "marketStrategy",
            "experiment-strategist",
            "BLOCKED",
            Instant.parse("2026-11-10T20:38:38Z"),
            null,
            "{\"decision\":\"ADJUST\"}"));
    when(tasks.findFunctionalSnapshotsByProcessSince(
            126L, "experiment:" + experiment.getId(), cycle.getCreatedAt()))
        .thenReturn(history);
    var gate =
        new com.marketinghub.product.service.agentvalidation
            .PdeImplementedInputPlanningReadinessProvider(cycles, input, tasks, mapper, alignment);
    String reference = "experiment:" + experiment.getId();
    assertThat(gate.readiness(process, activity, product, reference).ready()).isFalse();
    var run = new com.marketinghub.businessprocess.automation.v1.ProcessRun();
    run.setProductId(id);
    run.setLearningCycleId(cycle.getId());
    run.setSourceReference(reference);
    var executionContext =
        new com.marketinghub.businessprocess.automation.v1.service.ProcessRunContext(
            null, null, null, cycles, products, mapper, null, tasks);
    String oldInput = executionContext.inputVersion(run);
    alignment.apply(id, cycle.getId(), request());
    String correctedInput = executionContext.inputVersion(run);
    assertThat(correctedInput).isNotEqualTo(oldInput).contains("planningDefinition");
    assertThat(executionContext.inputVersion(run)).isEqualTo(correctedInput);
    when(events.findByCycleIdAndActionOrderByRevisionDesc(cycle.getId(), "RECONCILE_PLANNING"))
        .thenReturn(List.of(receipt));
    assertThat(gate.readiness(process, activity, product, reference).ready()).isTrue();
    assertThat(gate.requiresFreshExecution(process, activity, product, reference)).isTrue();
    history.add(
        new com.marketinghub.agenttask.AgentTaskFunctionalSnapshot(
            907L,
            126L,
            process.getProcessCode(),
            "marketStrategy",
            "experiment-strategist",
            "BLOCKED",
            receipt.getCreatedAt().plusSeconds(1),
            null,
            "{\"decision\":\"ADJUST\"}"));
    assertThat(gate.readiness(process, activity, product, reference).ready()).isFalse();
    assertThat(gate.requiresFreshExecution(process, activity, product, reference)).isFalse();
    history.removeLast();
    var proof =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.read(receipt.getEvidenceJson());
    proof.put("experimentId", 99999);
    receipt.setEvidenceJson(proof.toString());
    assertThat(gate.readiness(process, activity, product, reference).ready()).isFalse();
    verify(tasks, never()).save(any());
  }

  /** Sobe o contrato real com persistência simulada para a navegação local sem efeitos externos. */
  public static void main(String[] args) throws Exception {
    var fixture = new LearningCyclePlanningAlignmentTest();
    fixture.fixture(4);
    var facade = mock(LearningCycleService.class);
    when(facade.planningAlignment(4L, 204L))
        .thenAnswer(call -> fixture.alignment.preview(4L, 204L));
    when(facade.reconcilePlanning(eq(4L), eq(204L), any()))
        .thenAnswer(
            call -> {
              fixture.alignment.apply(4L, 204L, call.getArgument(2));
              return fixture.mapper.readValue(
                  "{\"id\":204,\"productId\":4,\"experimentId\":304,\"revision\":2,\"status\":\"OPEN\",\"stage\":\"PLANNING\"}",
                  com.marketinghub
                      .businessprocesschain
                      .learningcycle
                      .v1
                      .service
                      .getCycles
                      .LearningCycleResponse
                      .class);
            });
    var http = MockMvcBuilders.standaloneSetup(new LearningCycleController(facade)).build();
    var server =
        com.sun.net.httpserver.HttpServer.create(
            new java.net.InetSocketAddress("127.0.0.1", 18094), 0);
    server.createContext(
        "/__fixtures/reset",
        exchange -> {
          fixture.fixture(4);
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    server.createContext(
        "/api/",
        exchange -> {
          try {
            var builder =
                "POST".equals(exchange.getRequestMethod())
                    ? post(exchange.getRequestURI().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exchange.getRequestBody().readAllBytes())
                    : get(exchange.getRequestURI().toString());
            var result = http.perform(builder).andReturn().getResponse();
            byte[] body = result.getContentAsByteArray();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(result.getStatus(), body.length);
            exchange.getResponseBody().write(body);
          } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(LearningCyclePlanningAlignmentTest.class)
                .error("Falha no contrato local de alinhamento", ex);
            exchange.sendResponseHeaders(500, -1);
          } finally {
            exchange.close();
          }
        });
    Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
    server.start();
    System.out.println("Contrato local disponível em http://127.0.0.1:18094");
    Thread.currentThread().join();
  }
}
