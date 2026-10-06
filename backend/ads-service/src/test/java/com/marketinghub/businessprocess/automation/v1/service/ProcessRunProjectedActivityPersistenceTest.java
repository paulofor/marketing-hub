package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

/** Responsabilidade: comprovar que o motor distingue provas, projeções e tarefas reais. */
@DataJpaTest(showSql = false)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.businessprocess.automation.v1.service.ProcessRunProjectedActivityPersistenceTest-${random.uuid};DB_CLOSE_DELAY=0"
    })
class ProcessRunProjectedActivityPersistenceTest {
  @Autowired private ProcessRunRepository runs;
  @Autowired private ProcessRunEventRepository events;
  @Autowired private PlatformTransactionManager transactions;
  @Autowired private EntityManager entityManager;
  private final ObjectMapper json =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final ProcessRunContext context = mock(ProcessRunContext.class);
  private final ProcessRunNavigation navigation = mock(ProcessRunNavigation.class);
  private final ProcessRunSubprocesses subprocesses = mock(ProcessRunSubprocesses.class);
  private final ProcessRunGuidance guidance = mock(ProcessRunGuidance.class);
  private final BusinessProcessActivityExecutionService activities =
      mock(BusinessProcessActivityExecutionService.class);
  private final Map<Long, ProductProcessActivityExecutionHistoryResponse> snapshots =
      new HashMap<>();
  private ProcessRunService service;
  private ProcessRun parent;
  private ProcessRun next;
  private boolean authorized;

  /** Persiste a recuperação uma única vez e não transforma o pré-requisito em aceite de Íris. */
  @Test
  void automaticallyRecoversMissingHandoffWithoutRepeatingAudit() throws Exception {
    authorized = true;
    snapshot(96011L, "NOT_STARTED", false, false, false, false);
    var source =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(snapshots.get(96011L));
    var namedActivity =
        (com.fasterxml.jackson.databind.node.ObjectNode) source.path("activities").get(0);
    namedActivity.put("activityName", "Preparar comunicação");
    namedActivity.put("activityOwnerName", "Íris");
    namedActivity.put("sequenceNumber", 1);
    snapshots.put(
        96011L, json.treeToValue(source, ProductProcessActivityExecutionHistoryResponse.class));
    var recovery = mock(ProcessRunAssumptionRecovery.class);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "assumptionRecovery", recovery);
    when(recovery.recover(any(), any()))
        .thenReturn(
            Optional.of(new ProcessRunAssumptionRecovery.Recovered(96119L, 96171L, 96589L)));
    var result = service.reconcile(parent.getId());
    assertThat(result.status()).isEqualTo("WAITING_INPUT");
    assertThat(result.completedActivities()).isZero();
    service.reconcile(parent.getId());
    entityManager.flush();
    entityManager.clear();
    var recovered =
        events.findAll().stream()
            .filter(e -> "PREREQUISITE_RECOVERED".equals(e.getEventType()))
            .toList();
    assertThat(recovered).hasSize(1);
    assertThat(recovered.getFirst().getDetailsJson())
        .contains("experiment:96021", "96119", "96171", "96589");
    assertThat(runs.findById(parent.getId()).orElseThrow().getCompletedActivities()).isZero();
    verifyNoInteractions(activities);
    String output = System.getProperty("preparation-recovery.fixture-output");
    if (output != null) {
      var history =
          (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(snapshots.get(96011L));
      history.put("productId", 96001L);
      history.put("productName", "Produto sintético");
      history.put("selectedProcessDefinitionId", 96011L);
      history.put("selectedProcessVersionNumber", 2);
      history.put("selectedProcessStatus", "PUBLISHED");
      history.put("currentExecutionReference", "experiment:96021");
      history.put("operationalState", "BLOCKED");
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output),
          json.writeValueAsString(
              Map.of(
                  "control", result,
                  "history", history,
                  "events", service.history(96001L, 96011L, parent.getId(), null))));
    }
  }

  /** STOP, pausa, versão encerrada e falta de vez impedem recuperar passagem ou criar tarefa. */
  @ParameterizedTest
  @ValueSource(strings = {"STOP", "PAUSED", "CLOSED", "QUEUED", "RETIRED"})
  void guardsAutomaticHandoffRecovery(String guard) throws Exception {
    authorized = !"RETIRED".equals(guard);
    snapshot(96011L, "NOT_STARTED", false, false, false, false);
    var recovery = mock(ProcessRunAssumptionRecovery.class);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "assumptionRecovery", recovery);
    if (Set.of("PAUSED", "CLOSED").contains(guard)) parent.setStatus(guard);
    if ("STOP".equals(guard)) {
      var products =
          (ProductRepository)
              org.springframework.test.util.ReflectionTestUtils.getField(service, "products");
      products.findById(96001L).orElseThrow().setAutomaticExecutionEnabled(false);
    }
    service.reconcile("QUEUED".equals(guard) ? next.getId() : parent.getId());
    verifyNoInteractions(recovery, activities);
  }

  /** Contratos prontos seguem automaticamente para a tarefa, sem comando humano por agente. */
  @Test
  void readyInputsAutomaticallyDispatchOnceAndWaitForRealTask() throws Exception {
    authorized = true;
    snapshot(96011L, "NOT_STARTED", false, false, false, false);
    var value =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(snapshots.get(96011L));
    var activity = (com.fasterxml.jackson.databind.node.ObjectNode) value.path("activities").get(0);
    activity.put("executionRequestAvailable", true);
    ((com.fasterxml.jackson.databind.node.ObjectNode) activity.path("executionControl"))
        .put("actionAvailable", true)
        .put("executorType", "AGENT");
    snapshots.put(
        96011L, json.treeToValue(value, ProductProcessActivityExecutionHistoryResponse.class));
    var dispatched =
        mock(
            com.marketinghub
                .businessprocess
                .execution
                .service
                .requestProductProcessActivityExecution
                .ProductProcessActivityExecutionRequestResponse
                .class);
    when(dispatched.sourceReference()).thenReturn("experiment:96021");
    when(dispatched.tasks()).thenReturn(List.of());
    when(activities.requestProductActivityExecution(
            eq(96011L), eq(96001L), eq("a"), isNull(), isNull(), eq("experiment:96021")))
        .thenReturn(dispatched);
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("WAITING_ACTIVITY");
    snapshot(96011L, "PENDING", true, true, false, false);
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("WAITING_ACTIVITY");
    verify(activities, times(1))
        .requestProductActivityExecution(
            eq(96011L), eq(96001L), eq("a"), isNull(), isNull(), eq("experiment:96021"));
  }

  /**
   * Conclusão, início com provas e recuperação explícita preservam o destino, inclusive pausado.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "QUEUED, reconcile",
    "PAUSED, reconcile",
    "QUEUED, resume",
    "PAUSED, resume",
    "QUEUED, start",
    "PAUSED, start"
  })
  void continuesCycleToExistingProcessWithoutResumingIt(String destinationStatus, String command)
      throws Exception {
    authorized = true;
    parent.setLearningCycleId(96031L);
    next.setStatus(destinationStatus);
    next.setLearningCycleId(96031L);
    next.setScopeKey(
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(
                        "96001|96012|96014|96031|experiment:96021"
                            .getBytes(StandardCharsets.UTF_8))));
    when(context.command(any()))
        .thenReturn(
            new com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand(
                96014L, 96031L, "experiment:96021"));
    snapshot(96011L, "COMPLETED", false, true, false, true);
    var pending =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(snapshots.get(96012L));
    pending.put("currentActivityId", "a");
    snapshots.put(
        96012L, json.treeToValue(pending, ProductProcessActivityExecutionHistoryResponse.class));
    var cycle = new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle();
    cycle.setId(96031L);
    cycle.setProductId(96001L);
    cycle.setChainDefinitionId(96014L);
    cycle.setExperimentId(96021L);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    cycle.setReturnProcessId(96011L);
    var cycles =
        mock(com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository.class);
    when(cycles.findLocked(96001L, 96031L)).thenReturn(Optional.of(cycle));
    var chains =
        mock(
            com.marketinghub.repository.jpa.businessprocesschain
                .BusinessProcessChainDefinitionRepository.class);
    var chain = new com.marketinghub.businessprocesschain.BusinessProcessChainDefinition();
    chain.setItems(new ArrayList<>());
    for (long id : List.of(96011L, 96012L)) {
      var definition = new BusinessProcessDefinition();
      definition.setId(id);
      definition.setProcessCode(
          id == 96011L ? "pde-commercial-plan-offer" : "pde-construction-approval");
      var item = new com.marketinghub.businessprocesschain.BusinessProcessChainItem();
      item.setProcessDefinition(definition);
      item.setSequenceNumber((int) (id - 96009));
      chain.getItems().add(item);
      when(activities.productProcessExecutions(id, 96001L, 96031L, 96014L))
          .thenAnswer(inv -> snapshots.get(id));
    }
    when(chains.findById(96014L)).thenReturn(Optional.of(chain));
    var cycleService =
        mock(
            com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleService
                .class);
    var continuation =
        new com.marketinghub.businessprocesschain.learningcycle.v1.service
            .LearningCycleProcessContinuation(
            cycles,
            chains,
            (ProductRepository)
                org.springframework.test.util.ReflectionTestUtils.getField(service, "products"),
            new com.marketinghub.businessprocesschain.learningcycle.v1.service
                .LearningCycleWorkResolver(chains, activities),
            cycleService);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "cycleContinuation", continuation);
    if ("start".equals(command)) {
      runs.delete(parent);
      runs.flush();
      when(context.executableVersion(eq(96001L), eq(96011L), any(), any())).thenReturn(true);
      var started = service.start(96001L, 96011L, context.command(parent));
      assertThat(started.status()).isEqualTo("COMPLETED");
      parent = runs.findById(started.id()).orElseThrow();
    } else if ("resume".equals(command)) {
      parent.setStatus("COMPLETED");
      assertThat(service.resume(96001L, 96011L, parent.getId()).status()).isEqualTo("COMPLETED");
      service.resume(96001L, 96011L, parent.getId());
    } else assertThat(service.reconcile(parent.getId()).status()).isEqualTo("COMPLETED");
    service.reconcile(parent.getId());
    entityManager.flush();
    entityManager.clear();
    assertThat(runs.count()).isEqualTo(2);
    assertThat(runs.findById(next.getId()).orElseThrow().getStatus()).isEqualTo(destinationStatus);
    assertThat(
            events.findAll().stream()
                .filter(e -> "CYCLE_PROCESS_CONTINUED".equals(e.getEventType())))
        .hasSize(1)
        .allSatisfy(
            e -> assertThat(e.getDetailsJson()).contains("96031", "experiment:96021", "nextRunId"));
    verifyNoInteractions(cycleService);
  }

  /**
   * Uma pausa pedida durante o último trabalho não dispara o processo seguinte ao receber sua
   * prova.
   */
  @Test
  void completionWhilePausingDoesNotContinueCycle() throws Exception {
    authorized = true;
    parent.setStatus("PAUSING");
    parent.setLearningCycleId(96031L);
    snapshot(96011L, "COMPLETED", false, true, false, true);
    var continuation =
        mock(
            com.marketinghub.businessprocesschain.learningcycle.v1.service
                .LearningCycleProcessContinuation.class);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "cycleContinuation", continuation);
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("COMPLETED");
    verifyNoInteractions(continuation);
  }

  /** Persiste uma reserva antiga e sua candidata em identidades exclusivamente locais. */
  @BeforeEach
  void setup() throws Exception {
    parent = runs.saveAndFlush(run(96011L, "WAITING_ACTIVITY"));
    parent.setCurrentActivityId("a");
    next = runs.saveAndFlush(run(96012L, "QUEUED"));
    var products = mock(ProductRepository.class);
    var product = new Product();
    product.setId(96001L);
    product.setAutomaticExecutionEnabled(true);
    when(products.findById(96001L)).thenReturn(Optional.of(product));
    when(products.findLockedById(96001L)).thenReturn(Optional.of(product));
    when(context.read(any(ProcessRun.class), eq(false)))
        .thenAnswer(inv -> snapshots.get(inv.<ProcessRun>getArgument(0).getProcessDefinitionId()));
    when(context.read(anyLong(), anyLong(), any(), eq(true)))
        .thenAnswer(inv -> snapshots.get(inv.<Long>getArgument(1)));
    when(context.command(any()))
        .thenReturn(
            new com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand(
                96014L, null, "experiment:96021"));
    when(context.dispatchBlockReason(any()))
        .thenAnswer(
            inv ->
                inv.<ProcessRun>getArgument(0).getProcessDefinitionId().equals(96011L)
                        && !authorized
                    ? "A versão retirada não está publicada nem fixada nesta referência."
                    : null);
    when(context.graph(anyLong()))
        .thenReturn(
            new ProcessExecutionGraph(
                json.readTree("{\"nodes\":[{\"id\":\"a\",\"type\":\"TASK\"}],\"flows\":[]}")));
    when(navigation.parents(any())).thenReturn(List.of());
    when(navigation.children(any())).thenReturn(List.of());
    var definition = new BusinessProcessDefinition();
    definition.setName("Processo de teste");
    definition.setVersionNumber(2);
    when(context.process(anyLong())).thenReturn(definition);
    service =
        new ProcessRunService(
            runs,
            events,
            products,
            context,
            navigation,
            subprocesses,
            guidance,
            activities,
            json,
            transactions);
    snapshot(96011L, "IN_PROGRESS", false, false, false, false);
    snapshot(96012L, "NOT_STARTED", false, false, false, false);
  }

  /** Medição projetada não impede encerrar versão retirada nem preserva uma reserva eterna. */
  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "IN_PROGRESS"})
  void closesProjectedReservationAndExposesOwnGate(String state) throws Exception {
    snapshot(96011L, state, false, false, false, false);
    assertThat(service.reconcile(next.getId()).status()).isEqualTo("QUEUED");
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("CLOSED");
    var result = service.reconcile(next.getId());
    assertThat(result.status()).isEqualTo("WAITING_INPUT");
    assertThat(result.reason()).contains("Estratégia vigente pendente");
    assertThat(result.completedActivities()).isZero();
    service.reconcile(parent.getId());
    entityManager.flush();
    entityManager.clear();
    var closed = runs.findById(parent.getId()).orElseThrow();
    assertThat(closed.getKnownCostUsd()).isEqualByComparingTo("0.75");
    assertThat(closed.getRemainingActivities()).isEqualTo(1);
    assertThat(events.findAll().stream().filter(e -> "CONTEXT_CLOSED".equals(e.getEventType())))
        .hasSize(1);
    verifyNoInteractions(activities, subprocesses);
  }

  /** Tarefa real continua em curso mesmo quando a medição é a fonte do estado exibido. */
  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "IN_PROGRESS"})
  void waitsForRealTaskBeforeClosing(String state) throws Exception {
    snapshot(96011L, state, true, false, false, false);
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("WAITING_ACTIVITY");
    assertThat(service.reconcile(next.getId()).status()).isEqualTo("QUEUED");
    snapshot(96011L, state, false, false, false, false);
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("CLOSED");
    assertThat(service.reconcile(next.getId()).status()).isEqualTo("WAITING_INPUT");
    verifyNoInteractions(activities);
  }

  /** Uma instância registrada permanece protegida mesmo sem tarefa de agente associada. */
  @Test
  void preservesRegisteredActivity() throws Exception {
    snapshot(96011L, "IN_PROGRESS", false, true, false, false);
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("WAITING_ACTIVITY");
    assertThat(service.reconcile(next.getId()).status()).isEqualTo("QUEUED");
    verifyNoInteractions(activities);
  }

  /** Contratos que não são projeções de medição conservam o comportamento de espera anterior. */
  @Test
  void preservesOtherActiveContractsWithoutDispatch() throws Exception {
    authorized = true;
    var value = json.valueToTree(snapshots.get(96011L));
    ((com.fasterxml.jackson.databind.node.ObjectNode) value.path("activities").get(0))
        .put("stateEvidence", "LOCAL_CONTRACT");
    snapshots.put(
        96011L, json.treeToValue(value, ProductProcessActivityExecutionHistoryResponse.class));
    when(guidance.awaitingInput(parent)).thenReturn(true);
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("WAITING_ACTIVITY");
    assertThat(service.reconcile(next.getId()).status()).isEqualTo("QUEUED");
    verifyNoInteractions(activities);
  }

  /** Projeção sem trabalho não impede concluir uma pausa explícita nem falsifica objetivo. */
  @Test
  void pausesProjectionWithoutWaitingForNonexistentCallback() {
    authorized = true;
    service.pause(96001L, 96011L, parent.getId());
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("PAUSED");
    assertThat(service.reconcile(next.getId()).status()).isEqualTo("WAITING_INPUT");
    assertThat(parent.getCompletedActivities()).isZero();
    verifyNoInteractions(activities);
  }

  /** Chamada autorizada reutiliza o filho existente, respeita seu gate e recebe a prova no pai. */
  @Test
  void delegatesProjectedCallWithoutDuplicatingChildAndReturnsProof() throws Exception {
    authorized = true;
    snapshot(96011L, "IN_PROGRESS", false, false, true, false);
    var delegated = service.reconcile(parent.getId());
    assertThat(delegated.status()).isEqualTo("WAITING_SUBPROCESS");
    assertThat(delegated.childRunId()).isEqualTo(next.getId());
    assertThat(next.getParentRunId()).isEqualTo(parent.getId());
    assertThat(service.reconcile(next.getId()).status()).isEqualTo("WAITING_INPUT");
    snapshot(96012L, "COMPLETED", false, false, false, true);
    assertThat(service.reconcile(next.getId()).status()).isEqualTo("COMPLETED");
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("RUNNING");
    verify(subprocesses).complete(eq(parent), any(), eq(next), eq(snapshots.get(96012L)));
    snapshot(96011L, "COMPLETED", false, false, true, true);
    assertThat(service.reconcile(parent.getId()).status()).isEqualTo("COMPLETED");
    assertThat(runs.count()).isEqualTo(2);
    assertThat(events.findAll().stream().filter(e -> "DELEGATION_LINKED".equals(e.getEventType())))
        .hasSize(1);
    verifyNoInteractions(activities);
  }

  /**
   * Encerra espera financeira pelo contrato real do experimento, preservando custos e três provas.
   */
  @Test
  void reconcilesTerminalExperimentWithoutCompletingFinancialGate() throws Exception {
    var definitions =
        mock(
            com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository
                .class);
    var experiments = mock(com.marketinghub.repository.jpa.experiment.ExperimentRepository.class);
    var definition = new BusinessProcessDefinition();
    definition.setId(parent.getProcessDefinitionId());
    definition.setProcessCode("experiment-homologation-activation");
    definition.setStatus("PUBLISHED");
    when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
    var experiment = new com.marketinghub.experiment.Experiment();
    experiment.setId(96021L);
    experiment.setProduct(Product.builder().id(parent.getProductId()).build());
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.INVALIDATED);
    when(experiments.findById(experiment.getId())).thenReturn(Optional.of(experiment));
    var identity = new ProcessRunContext(null, definitions, null, null, null, json, experiments);
    doAnswer(inv -> identity.dispatchBlockReason(inv.getArgument(0)))
        .when(context)
        .dispatchBlockReason(any());
    snapshot(96011L, "NOT_STARTED", false, false, false, false);
    var value = json.valueToTree(snapshots.get(96011L));
    ((com.fasterxml.jackson.databind.node.ObjectNode) value)
        .put("selectedActivityCount", 4)
        .put("completedActivityCount", 3)
        .put("remainingActivityCount", 1);
    snapshots.put(
        96011L, json.treeToValue(value, ProductProcessActivityExecutionHistoryResponse.class));
    parent.setStatus("WAITING_INPUT");
    var result = service.reconcile(parent.getId());
    assertThat(result.status()).isEqualTo("CLOSED");
    assertThat(result.reason()).contains("#96021", "não renove Plutus");
    assertThat(result.completedActivities()).isEqualTo(3);
    assertThat(result.remainingActivities()).isEqualTo(1);
    assertThat(result.canResume()).isFalse();
    service.reconcile(parent.getId());
    assertThatThrownBy(
            () ->
                service.resume(
                    parent.getProductId(), parent.getProcessDefinitionId(), parent.getId()))
        .hasMessageContaining("encerrado");
    entityManager.flush();
    entityManager.clear();
    assertThat(runs.findById(parent.getId()).orElseThrow().getStatus()).isEqualTo("CLOSED");
    assertThat(events.findAll().stream().filter(e -> "CONTEXT_CLOSED".equals(e.getEventType())))
        .hasSize(1);
    assertThat(result.knownCostUsd()).isEqualByComparingTo("0.75");
    verifyNoInteractions(activities, subprocesses);
    String output = System.getProperty("closed-process.fixture-output");
    if (output != null)
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output), json.writeValueAsString(result));
  }

  /**
   * Concilia provas históricas sem abrir tarefas, renovar pareceres ou repetir o retorno ao pai.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {5, 8})
  void reconcilesHistoricalPreparationWithoutNewTasks(int activityCount) throws Exception {
    snapshot(96011L, "COMPLETED", false, true, false, true);
    var value =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(snapshots.get(96011L));
    value.put("selectedActivityCount", activityCount).put("completedActivityCount", activityCount);
    var prototype = value.withArray("activities").get(0).deepCopy();
    var activityList = value.withArray("activities").removeAll();
    var diagram = json.createObjectNode();
    var nodes = diagram.putArray("nodes");
    var flows = diagram.putArray("flows");
    for (int index = 0; index < activityCount; index++) {
      String id = String.valueOf((char) ('a' + index));
      var step = (com.fasterxml.jackson.databind.node.ObjectNode) prototype.deepCopy();
      step.put("activityId", id)
          .put("activityDefinitionId", 96051L + index)
          .put("activityInstanceId", 96031L + index);
      activityList.add(step);
      nodes.addObject().put("id", id).put("type", "TASK");
      if (index > 0)
        flows.addObject().put("from", String.valueOf((char) ('a' + index - 1))).put("to", id);
    }
    when(context.graph(96011L)).thenReturn(new ProcessExecutionGraph(diagram));
    snapshots.put(
        96011L, json.treeToValue(value, ProductProcessActivityExecutionHistoryResponse.class));
    when(context.dispatchBlockReason(parent))
        .thenReturn("O experimento #96021 está encerrado. Provas preservadas.");
    parent.setStatus("WAITING_INPUT");
    var result = service.reconcile(parent.getId());
    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.completedActivities()).isEqualTo(activityCount);
    assertThat(result.remainingActivities()).isZero();
    assertThat(result.knownCostUsd()).isEqualByComparingTo("0.75");
    assertThat(result.canResume()).isFalse();
    service.reconcile(parent.getId());
    entityManager.flush();
    entityManager.clear();
    assertThat(runs.findById(parent.getId()).orElseThrow().getStatus()).isEqualTo("COMPLETED");
    assertThat(events.findAll().stream().filter(e -> "CONTEXT_CLOSED".equals(e.getEventType())))
        .isEmpty();
    assertThat(events.findAll().stream().filter(e -> "COMPLETED".equals(e.getEventType())))
        .hasSize(1);
    verifyNoInteractions(activities, subprocesses);
    String output = System.getProperty("historical-preparation.fixture-output");
    if (output != null)
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output), json.writeValueAsString(result));
  }

  /** Monta a projeção com estado, prova, tarefa e controle independentes, como o contrato real. */
  private void snapshot(
      long process,
      String state,
      boolean task,
      boolean instance,
      boolean subprocess,
      boolean achieved)
      throws Exception {
    var value = json.createObjectNode();
    value.put("selectedActivityCount", 1);
    value.put("completedActivityCount", achieved ? 1 : 0);
    value.put("remainingActivityCount", achieved ? 0 : 1);
    value.put("objectiveAchieved", achieved);
    value.put("knownEstimatedCostUsd", new BigDecimal("0.75"));
    value.put("costCoverage", "COMPLETE");
    value.put("processName", "Processo local");
    var activity = value.putArray("activities").addObject();
    activity.put("activityDefinitionId", 96051L);
    activity.put("activityId", "a");
    activity.put("selectedVersionActivity", true);
    activity.put("objectiveAchieved", achieved);
    activity.put("operationalState", state);
    activity.put("stateEvidence", "SALES_FLOW_EVENT");
    if (instance) activity.put("activityInstanceId", 96031L);
    var tasks = activity.putArray("tasks");
    if (task) tasks.addObject().put("taskId", 96041L).put("status", state);
    var control = activity.putObject("executionControl");
    control.put("interactionType", subprocess ? "SUBPROCESS" : "COMMAND");
    control.put("executorType", "BACKEND");
    control.put("actionAvailable", subprocess);
    control.put("availabilityReason", "Estratégia vigente pendente; não chamar modelo.");
    if (subprocess) control.put("targetProcessDefinitionId", 96012L);
    snapshots.put(
        process, json.treeToValue(value, ProductProcessActivityExecutionHistoryResponse.class));
  }

  /** Preenche identidade congelada e custos para verificar que encerramento preserva auditoria. */
  private ProcessRun run(long process, String status) throws Exception {
    var run = new ProcessRun();
    run.setProductId(96001L);
    run.setProcessDefinitionId(process);
    run.setChainDefinitionId(96014L);
    run.setSourceReference("experiment:96021");
    run.setScopeKey(
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(
                        ("96001|" + process + "|96014|null|experiment:96021")
                            .getBytes(StandardCharsets.UTF_8))));
    run.setStatus(status);
    run.setReason("Aguardando medição projetada.");
    run.setTotalActivities(1);
    run.setRemainingActivities(1);
    run.setKnownCostUsd(new BigDecimal("0.75"));
    run.setCostCoverage("COMPLETE");
    run.setCreatedAt(Instant.parse("2026-10-03T12:00:00Z"));
    run.setUpdatedAt(run.getCreatedAt());
    run.setLastReconciledAt(run.getCreatedAt());
    return run;
  }
}
