package com.marketinghub.product.operations.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.*;
import com.marketinghub.product.Product;
import com.marketinghub.product.operations.v1.controller.ProductOperationsController;
import com.marketinghub.product.operations.v1.service.ProductOperationsService;
import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionService;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.product.operations.ProductOperationsRunRepository;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar a leitura da operação sem confundir PLAY, fila e execução. */
class ProductOperationsTest {
  private final ProductRepository products = mock(ProductRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ProductOperationsRunRepository runs = mock(ProductOperationsRunRepository.class);
  private final ProductValueChainPositionService positions =
      mock(ProductValueChainPositionService.class);
  private final BusinessProcessActivityExecutionService executions =
      mock(BusinessProcessActivityExecutionService.class);
  private final LearningCycleWorkResolver work = mock(LearningCycleWorkResolver.class);
  private final LearningCycleValueFlowProjection value =
      mock(LearningCycleValueFlowProjection.class);
  private final ProductOperationsService service =
      new ProductOperationsService(products, cycles, runs, positions, executions, work, value);

  /**
   * Preserva o caso de Vega e outra identidade, mostrando a atividade do filho no contexto
   * original.
   */
  @ParameterizedTest
  @CsvSource({"4,13,106", "92004,92013,92106"})
  void followsOnlyCurrentDelegation(long productId, long cycleId, long experimentId) {
    var product =
        configure(productId, cycleId, experimentId, "WAITING_INPUT", "BLOCKED", "Vega QA");
    var child =
        runs.findOpenRootsForOperations(productId, cycleId, "experiment:" + experimentId)
            .getFirst();
    var root = run(200L, productId, cycleId, experimentId, "WAITING_SUBPROCESS");
    root.setChildRunId(child.getId());
    child.setParentRunId(root.getId());
    when(runs.findOpenRootsForOperations(productId, cycleId, "experiment:" + experimentId))
        .thenReturn(List.of(root));
    when(runs.findById(child.getId())).thenReturn(Optional.of(child));
    when(products.findAllInPlayState()).thenReturn(List.of(product));

    var result = service.inPlay();

    assertThat(result.blocked()).isEqualTo(1);
    var item = result.products().getFirst();
    assertThat(item.activityName()).isEqualTo("Validar custos, limites e margem");
    assertThat(item.activityNumber()).isEqualTo("5.1.5");
    assertThat(item.chainId()).isEqualTo(26L);
    assertThat(item.activityUrl())
        .contains(
            "chainId=26",
            "learningCycleId=" + cycleId,
            "sourceReference=experiment:" + experimentId,
            "#activity-economics");
    assertThat(item.runId()).isEqualTo(child.getId());
    verifyNoInteractions(positions, work, value);
    verify(products, never()).save(any());
  }

  /** Diferencia estados de tarefa e de processo, inclusive quando PLAY não produz trabalho. */
  @ParameterizedTest
  @CsvSource({
    "WAITING_ACTIVITY,IN_PROGRESS,EXECUTING", "WAITING_ACTIVITY,PENDING,QUEUED",
    "PAUSED,BLOCKED,PAUSED", "WAITING_HUMAN,NOT_STARTED,AWAITING_DECISION",
    "WAITING_INPUT,WAITING,WAITING", "WAITING_INPUT,BLOCKED,BLOCKED",
    "ERROR,NOT_STARTED,BLOCKED", "QUEUED,NOT_STARTED,QUEUED"
  })
  void distinguishesActivityState(String runState, String activityState, String expected) {
    var product = configure(10L, 9L, 102L, runState, activityState, "Mira QA");
    when(products.findAllInPlayState()).thenReturn(List.of(product));
    assertThat(service.inPlay().products().getFirst().status()).isEqualTo(expected);
  }

  /** Uma reprovação anterior do processo não substitui a atividade corrente confirmada. */
  @Test
  void ignoresStaleRunActivity() {
    var product = configure(7L, 12L, 105L, "ERROR", "NOT_STARTED", "Capella QA");
    var run = runs.findOpenRootsForOperations(7L, 12L, "experiment:105").getFirst();
    run.setCurrentActivityId("older");
    run.setReason("Falha histórica");
    when(products.findAllInPlayState()).thenReturn(List.of(product));
    var result = service.inPlay().products().getFirst();
    assertThat(result.status()).isEqualTo("NOT_STARTED");
    assertThat(result.reason()).isEqualTo("Condição oficial da atividade");
  }

  /** A retentativa ativa na mesma atividade não apresenta a falha anterior como motivo atual. */
  @ParameterizedTest
  @CsvSource({"IN_PROGRESS,EXECUTING", "PENDING,QUEUED"})
  void currentRetryDoesNotDisplayPreviousFailure(String activityState, String expected) {
    var product = configure(92007L, 92012L, 92105L, "ERROR", activityState, "Produto QA");
    var run = runs.findOpenRootsForOperations(92007L, 92012L, "experiment:92105").getFirst();
    run.setReason("Falha histórica da tentativa anterior");
    when(products.findAllInPlayState()).thenReturn(List.of(product));
    var item = service.inPlay().products().getFirst();
    assertThat(item.status()).isEqualTo(expected);
    assertThat(item.reason()).isEqualTo("Condição oficial da atividade");
  }

  /** Vínculo a outro produto é indisponibilidade explícita, nunca atividade de outra identidade. */
  @Test
  void refusesCrossProductChildAndPreservesOtherProducts() {
    var bad = configure(4L, 13L, 106L, "WAITING_SUBPROCESS", "BLOCKED", "Vega QA");
    var good = configure(7L, 12L, 105L, "WAITING_ACTIVITY", "IN_PROGRESS", "Capella QA");
    var root = runs.findOpenRootsForOperations(4L, 13L, "experiment:106").getFirst();
    root.setChildRunId(300L);
    when(runs.findById(300L)).thenReturn(Optional.of(run(300L, 7L, 13L, 106L, "WAITING_INPUT")));
    when(products.findAllInPlayState()).thenReturn(List.of(bad, good));
    var result = service.inPlay();
    assertThat(result.total()).isEqualTo(2);
    assertThat(result.unavailable()).isEqualTo(1);
    assertThat(result.executing()).isEqualTo(1);
    assertThat(result.products().getLast().reason()).doesNotContain("Exception", "300");
  }

  /** Um grafo circular deve terminar com diagnóstico explícito, preservando a consulta. */
  @Test
  void rejectsCircularDelegation() {
    var product = configure(4L, 13L, 106L, "WAITING_SUBPROCESS", "BLOCKED", "Vega QA");
    var root = runs.findOpenRootsForOperations(4L, 13L, "experiment:106").getFirst();
    root.setChildRunId(root.getId());
    root.setParentRunId(root.getId());
    when(runs.findById(root.getId())).thenReturn(Optional.of(root));
    when(products.findAllInPlayState()).thenReturn(List.of(product));
    assertThat(service.inPlay().unavailable()).isEqualTo(1);
  }

  /** A ausência de produtos não inicia consultas nem tarefas como efeito da leitura. */
  @Test
  void emptyPlayListIsReadOnly() {
    when(products.findAllInPlayState()).thenReturn(List.of());
    assertThat(service.inPlay().total()).isZero();
    verifyNoInteractions(cycles, runs, positions, executions, work, value);
  }

  /** Sem ciclo, a referência canônica impede selecionar uma pausa de experimento antigo. */
  @Test
  void initialPreparationUsesCurrentReferenceWithoutHistoricalPause() {
    var product = configure(11L, 900L, 97L, "WAITING_INPUT", "NOT_STARTED", "Alcyone QA");
    when(products.findAllInPlayState()).thenReturn(List.of(product));
    when(cycles.findFirstByProductIdAndChainCodeAndOpenSlot(11L, "pde-value-creation-delivery", 1))
        .thenReturn(Optional.empty());
    var position =
        mock(
            com.marketinghub.product.service.valuechainposition.ProductValueChainPositionResponse
                .class);
    when(position.processDefinitionId()).thenReturn(98L);
    when(position.chainDefinitionId()).thenReturn(27L);
    when(positions.getPosition(11L)).thenReturn(position);
    var history = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(history.productId()).thenReturn(11L);
    when(history.selectedProcessDefinitionId()).thenReturn(98L);
    when(history.currentExecutionReference()).thenReturn("experiment:97");
    when(history.currentActivityId()).thenReturn("journey");
    when(history.activities())
        .thenReturn(
            List.of(
                new ProductProcessActivityExecutionGroupResponse(
                    1L,
                    "journey",
                    "Conferir jornada pública",
                    "Conferir prova",
                    "Backend",
                    1,
                    true,
                    "NOT_STARTED",
                    "Preparação disponível",
                    false,
                    null,
                    null,
                    null,
                    0,
                    List.of())));
    when(executions.productProcessExecutions(98L, 11L, null, 27L, false, null)).thenReturn(history);
    when(runs.findOpenRootsForOperations(11L, null, "experiment:97")).thenReturn(List.of());
    var item = service.inPlay().products().getFirst();
    assertThat(item.status()).isEqualTo("NOT_STARTED");
    assertThat(item.activityUrl())
        .contains("chainId=27", "sourceReference=experiment:97")
        .doesNotContain("learningCycleId");
    verify(runs, never()).findOpenRootsForOperations(11L, null, null);
  }

  /** A decisão humana do ciclo prevalece sobre uma atividade genérica marcada como pendente. */
  @Test
  void cycleDecisionIsNotAnAgentQueue() {
    var product = configure(7L, 12L, 105L, "WAITING_ACTIVITY", "PENDING", "Capella QA");
    when(products.findAllInPlayState()).thenReturn(List.of(product));
    var cycle =
        cycles
            .findFirstByProductIdAndChainCodeAndOpenSlot(7L, "pde-value-creation-delivery", 1)
            .orElseThrow();
    var history = executions.productProcessExecutions(77L, 7L, 12L, 26L, false, "experiment:105");
    when(history.selectedProcessDefinitionId()).thenReturn(125L);
    when(executions.productProcessExecutions(125L, 7L, 12L, 26L, false, "experiment:105"))
        .thenReturn(history);
    when(runs.findOpenRootsForOperations(7L, 12L, "experiment:105")).thenReturn(List.of());
    var flow =
        mock(
            com.marketinghub
                .businessprocesschain
                .learningcycle
                .v1
                .service
                .getCycles
                .LearningCycleValueFlow
                .class);
    when(flow.decisionNeeded()).thenReturn(true);
    when(flow.decisionReason()).thenReturn("Escolha a continuação do ciclo.");
    when(value.resolve(cycle, null)).thenReturn(flow);
    var item = service.inPlay().products().getFirst();
    assertThat(item.status()).isEqualTo("AWAITING_DECISION");
    assertThat(item.reason()).isEqualTo("Escolha a continuação do ciclo.");
  }

  /** Executa HTTP, serialização e serviço reais, gerando a massa sintética da navegação local. */
  @Test
  void httpContractProvidesBackendCountsAndResponsiveFixture() throws Exception {
    var first = configure(92004L, 92013L, 92106L, "WAITING_ACTIVITY", "IN_PROGRESS", "Vega QA");
    var second = configure(92007L, 92012L, 92105L, "WAITING_HUMAN", "NOT_STARTED", "Capella QA");
    var third = configure(92010L, 92009L, 92102L, "PAUSED", "BLOCKED", "Mira QA");
    var fourth = configure(92011L, 92011L, 92097L, "WAITING_INPUT", "BLOCKED", "Alcyone QA");
    when(products.findAllInPlayState()).thenReturn(List.of(first, second, third, fourth));
    var mvc = MockMvcBuilders.standaloneSetup(new ProductOperationsController(service)).build();
    var response =
        mvc.perform(get("/api/products/operations/v1/in-play"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(4))
            .andExpect(jsonPath("$.executing").value(1))
            .andExpect(jsonPath("$.waiting").value(1))
            .andExpect(jsonPath("$.blocked").value(1))
            .andExpect(jsonPath("$.paused").value(1))
            .andExpect(jsonPath("$.observedAt").isString())
            .andExpect(jsonPath("$.products[0].lastReconciledAt").isString())
            .andExpect(jsonPath("$.products[0].productName").value("Alcyone QA"))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    Files.createDirectories(Path.of("target"));
    Files.writeString(Path.of("target/products-in-play-fixture.json"), response);
    verify(products).findAllInPlayState();
    verify(products, never()).findAll();
  }

  /** Prepara dados sintéticos com contrato real, sem usar banco ou consumidores produtivos. */
  private Product configure(
      long productId,
      long cycleId,
      long experimentId,
      String runState,
      String activityState,
      String name) {
    var product = new Product();
    product.setId(productId);
    product.setInternalName(name);
    product.setName("Demonstração local · dados sintéticos");
    var cycle = new LearningSalesCycle();
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setChainDefinitionId(26L);
    cycle.setProcessDefinitionId(125L);
    cycle.setStatus("OPEN");
    when(cycles.findFirstByProductIdAndChainCodeAndOpenSlot(
            productId, "pde-value-creation-delivery", 1))
        .thenReturn(Optional.of(cycle));
    var run = run(productId, productId, cycleId, experimentId, runState);
    when(runs.findOpenRootsForOperations(productId, cycleId, "experiment:" + experimentId))
        .thenReturn(List.of(run));
    var activity =
        new ProductProcessActivityExecutionGroupResponse(
            1L,
            "economics",
            "Validar custos, limites e margem",
            "Conferir economia",
            "Plutus",
            5,
            true,
            activityState,
            "Condição oficial da atividade",
            false,
            "Evidência sintética",
            null,
            null,
            0,
            List.of());
    var history = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(history.productId()).thenReturn(productId);
    when(history.selectedProcessDefinitionId()).thenReturn(77L);
    when(history.currentExecutionReference()).thenReturn("experiment:" + experimentId);
    when(history.currentActivityId()).thenReturn("economics");
    when(history.activities()).thenReturn(List.of(activity));
    when(history.chainPosition())
        .thenReturn(new ProductProcessChainPositionResponse("5.1", "preparation", "Preparação"));
    when(executions.productProcessExecutions(
            77L, productId, cycleId, 26L, false, "experiment:" + experimentId))
        .thenReturn(history);
    return product;
  }

  /** Cria controle persistido sintético com identidade e horário verificáveis. */
  private ProcessRun run(Long id, Long productId, Long cycleId, Long experimentId, String state) {
    var run = new ProcessRun();
    run.setId(id);
    run.setProductId(productId);
    run.setLearningCycleId(cycleId);
    run.setChainDefinitionId(26L);
    run.setProcessDefinitionId(77L);
    run.setSourceReference("experiment:" + experimentId);
    run.setStatus(state);
    run.setCurrentActivityId("economics");
    run.setReason("Estado da execução sintética");
    run.setLastReconciledAt(Instant.parse("2026-10-24T12:00:00Z"));
    return run;
  }
}
