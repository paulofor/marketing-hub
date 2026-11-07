package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: homologar a recuperação 2→3→4 com persistência e locks reais, sem serviços
 * pagos.
 */
@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.businessprocess.automation.v1.service.ProcessRunConstructionContinuationPersistenceTest-${random.uuid};DB_CLOSE_DELAY=0"
    })
class ProcessRunConstructionContinuationPersistenceTest {
  @Autowired private ProcessRunRepository runs;
  @Autowired private ProcessRunEventRepository events;
  @Autowired private ProductRepository products;
  @Autowired private PlatformTransactionManager manager;
  private final ObjectMapper json =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final BusinessProcessDefinitionRepository definitions =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessActivityExecutionService activities =
      mock(BusinessProcessActivityExecutionService.class);
  private final LearningCycleConstructionPrerequisite prerequisite =
      mock(LearningCycleConstructionPrerequisite.class);
  private final AtomicBoolean approved = new AtomicBoolean();
  private ProcessRunContext context;
  private ProcessRunService service;
  private ProcessRun planning;
  private ProcessRun communication;
  private LearningSalesCycle cycle;
  private ProcessRunCommand command;
  private TransactionTemplate transaction;
  private Long productId;

  /** Persiste um produto local e a comunicação legada que pulou a construção. */
  @BeforeEach
  void setup() {
    transaction = new TransactionTemplate(manager);
    productId =
        transaction.execute(
            ignored ->
                products
                    .saveAndFlush(
                        Product.builder()
                            .slug("qa-" + UUID.randomUUID())
                            .name("PDE local")
                            .automaticExecutionEnabled(true)
                            .build())
                    .getId());
    cycle = new LearningSalesCycle();
    cycle.setId(productId + 2000);
    cycle.setProductId(productId);
    cycle.setChainDefinitionId(27L);
    cycle.setExperimentId(productId + 3000);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    cycle.setReturnProcessId(113L);
    cycle.setProductVersion("qa-current-v2");
    command = new ProcessRunCommand(27L, cycle.getId(), "experiment:" + cycle.getExperimentId());
    when(cycles.findById(cycle.getId())).thenReturn(Optional.of(cycle));
    when(cycles.findByExperimentId(cycle.getExperimentId())).thenReturn(Optional.of(cycle));
    when(cycles.findLocked(productId, cycle.getId())).thenReturn(Optional.of(cycle));
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(new ArrayList<>());
    String[] codes = {
      "pde-commercial-plan-offer", "pde-construction-approval", "pde-communication-sales-journey"
    };
    long[] ids = {126, 117, 113};
    for (int i = 0; i < ids.length; i++) {
      long id = ids[i];
      String code = codes[i];
      String activity =
          id == 117
              ? "agentValidationGate"
              : id == 113 ? "communicationContract" : "productArchitecture";
      var definition = new BusinessProcessDefinition();
      definition.setId(id);
      definition.setName(code);
      definition.setStatus("PUBLISHED");
      definition.setProcessCode(code);
      definition.setDiagramJson(
          "{\"nodes\":[{\"id\":\"" + activity + "\",\"type\":\"TASK\"}],\"flows\":[]}");
      var item = new BusinessProcessChainItem();
      item.setSequenceNumber(i + 2);
      item.setProcessDefinition(definition);
      chain.getItems().add(item);
      when(definitions.findById(id)).thenReturn(Optional.of(definition));
      when(activities.productProcessExecutions(id, productId, cycle.getId(), 27L, false))
          .thenAnswer(inv -> snapshot(id, activity));
      when(activities.productProcessExecutions(
              id, productId, cycle.getId(), 27L, false, command.sourceReference()))
          .thenAnswer(inv -> snapshot(id, activity));
    }
    when(chains.findById(27L)).thenReturn(Optional.of(chain));
    when(prerequisite.pending(cycle))
        .thenAnswer(
            inv ->
                approved.get()
                    ? null
                    : new LearningCycleConstructionPrerequisite.Pending(
                        117L,
                        3,
                        LearningCycleConstructionPrerequisite.pendingReason(cycle),
                        "/products/"
                            + productId
                            + "/value-chain-history/processes/117/activities?learningCycleId="
                            + cycle.getId()
                            + "&chainId=27"));
    context =
        new ProcessRunContext(
            activities,
            definitions,
            chains,
            cycles,
            products,
            json,
            mock(com.marketinghub.repository.jpa.experiment.ExperimentRepository.class),
            mock(com.marketinghub.repository.jpa.agenttask.AgentTaskRepository.class));
    ReflectionTestUtils.setField(context, "constructionPrerequisites", prerequisite);
    service =
        new ProcessRunService(
            runs,
            events,
            products,
            context,
            mock(ProcessRunNavigation.class),
            mock(ProcessRunSubprocesses.class),
            mock(ProcessRunGuidance.class),
            activities,
            json,
            manager,
            mock(ProcessRunOwnerPortrait.class));
    planning = runs.findById(service.start(productId, 126L, command).id()).orElseThrow();
    transaction.executeWithoutResult(
        ignored -> {
          var saved = runs.findById(planning.getId()).orElseThrow();
          saved.setKnownCostUsd(new BigDecimal("8.945868"));
        });
    communication =
        transaction.execute(
            ignored -> {
              var legacy = new ProcessRun();
              legacy.setProductId(productId);
              legacy.setProcessDefinitionId(113L);
              legacy.setChainDefinitionId(27L);
              legacy.setLearningCycleId(cycle.getId());
              legacy.setSourceReference(command.sourceReference());
              legacy.setScopeKey(
                  ReflectionTestUtils.invokeMethod(service, "scope", productId, 113L, command));
              legacy.setStatus("WAITING_INPUT");
              legacy.setReason("Pendência antiga genérica");
              legacy.setCreatedAt(Instant.now());
              legacy.setUpdatedAt(Instant.now());
              legacy.setLastReconciledAt(Instant.now());
              return runs.saveAndFlush(legacy);
            });
    ReflectionTestUtils.setField(
        service,
        "cycleContinuation",
        new LearningCycleProcessContinuation(
            cycles,
            chains,
            products,
            new LearningCycleWorkResolver(chains, activities, prerequisite),
            mock(LearningCycleService.class)));
  }

  /**
   * Projeta somente a verdade funcional do contexto; aprovação muda apenas por prova externa
   * simulada.
   */
  private ProductProcessActivityExecutionHistoryResponse snapshot(
      long processId, String activityId) {
    boolean complete = processId == 126 || processId == 117 && approved.get();
    var state = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(state.productId()).thenReturn(productId);
    when(state.productName()).thenReturn("PDE local");
    when(state.selectedProcessDefinitionId()).thenReturn(processId);
    when(state.processName()).thenReturn("Comunicação local");
    when(state.operationalState()).thenReturn(complete ? "COMPLETED" : "NOT_STARTED");
    when(state.processCode())
        .thenReturn(
            processId == 113
                ? "pde-communication-sales-journey"
                : processId == 117 ? "pde-construction-approval" : "pde-commercial-plan-offer");
    when(state.selectedProcessVersionNumber()).thenReturn(1);
    var activity = mock(ProductProcessActivityExecutionGroupResponse.class);
    when(activity.activityDefinitionId()).thenReturn(processId + 1000);
    when(activity.activityId()).thenReturn(activityId);
    when(activity.activityName()).thenReturn(activityId);
    when(activity.activityOwnerName()).thenReturn("Backend");
    when(activity.selectedVersionActivity()).thenReturn(true);
    when(activity.sequenceNumber()).thenReturn(1);
    when(activity.objectiveAchieved()).thenReturn(complete);
    when(activity.operationalState()).thenReturn(complete ? "COMPLETED" : "NOT_STARTED");
    when(state.activities()).thenReturn(List.of(activity));
    when(state.objectiveAchieved()).thenReturn(complete);
    when(state.selectedActivityCount()).thenReturn(1);
    when(state.completedActivityCount()).thenReturn(complete ? 1 : 0);
    when(state.remainingActivityCount()).thenReturn(complete ? 0 : 1);
    when(state.selectedProcessStatus()).thenReturn("PUBLISHED");
    when(state.currentExecutionReference()).thenReturn(command.sourceReference());
    when(state.currentActivityId()).thenReturn(complete ? null : activityId);
    return state;
  }

  /**
   * Recupera o mesmo ciclo, impede repetição de Íris e libera 4 somente depois da prova atual de 3.
   */
  @Test
  void recoversCurrentCycleThroughConstructionWithoutReplacingPaidPlanning() throws Exception {
    writeBrowserFixture();
    assertThat(service.status(productId, 113L, command).canResume()).isFalse();
    assertThat(service.status(productId, 113L, command).navigationUrl())
        .contains("processes/117", "learningCycleId=" + cycle.getId());
    assertThatThrownBy(() -> service.resume(productId, 113L, communication.getId()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Processo 3 pendente");
    service.reconcile(communication.getId());
    service.resume(productId, 126L, planning.getId());
    service.resume(productId, 126L, planning.getId());
    var construction =
        runs.activeRoots(productId).stream()
            .filter(r -> r.getProcessDefinitionId() == 117)
            .findFirst()
            .orElseThrow();
    assertThat(service.status(productId, 117L, command).queueBlocker()).isNull();
    assertThat(runs.findById(planning.getId()).orElseThrow().getKnownCostUsd())
        .isEqualByComparingTo("8.945868");
    assertThat(runs.findById(communication.getId()).orElseThrow().getStatus())
        .isEqualTo("WAITING_INPUT");
    assertThat(runs.findAll().stream().filter(r -> r.getProductId().equals(productId))).hasSize(3);
    approved.set(true);
    assertThat(service.reconcile(construction.getId()).status()).isEqualTo("COMPLETED");
    assertThat(service.resume(productId, 113L, communication.getId()).status()).isEqualTo("QUEUED");
    assertThat(runs.findAll().stream().filter(r -> r.getProductId().equals(productId))).hasSize(3);
    verify(activities, never())
        .requestProductActivityExecution(anyLong(), anyLong(), anyString(), any(), any(), any());
  }

  /** Duas retomadas concorrentes preservam uma execução por escopo e um vínculo de continuação. */
  @Test
  void concurrentRecoveryCreatesOnlyOneConstruction() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var first =
          pool.submit(
              () -> {
                start.await();
                return service.resume(productId, 126L, planning.getId());
              });
      var second =
          pool.submit(
              () -> {
                start.await();
                return service.resume(productId, 126L, planning.getId());
              });
      start.countDown();
      first.get(30, TimeUnit.SECONDS);
      second.get(30, TimeUnit.SECONDS);
      assertThat(
              runs.findAll().stream()
                  .filter(
                      r -> r.getProductId().equals(productId) && r.getProcessDefinitionId() == 117))
          .hasSize(1);
      assertThat(
              events.findAll().stream()
                  .filter(
                      e ->
                          e.getRunId().equals(planning.getId())
                              && "CYCLE_PROCESS_CONTINUED".equals(e.getEventType())))
          .hasSize(1);
      assertThat(runs.findById(planning.getId()).orElseThrow().getKnownCostUsd())
          .isEqualByComparingTo("8.945868");
    } finally {
      pool.shutdownNow();
    }
  }

  /** Exporta respostas reais do serviço para o navegador local sem consultar serviços externos. */
  private void writeBrowserFixture() throws Exception {
    String output = System.getProperty("construction-prerequisite.fixture-output");
    if (output != null)
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output),
          json.writeValueAsString(
              Map.of(
                  "control",
                  service.status(productId, 113L, command),
                  "history",
                  snapshot(113, "communicationContract"))));
  }

  /** O comando usado pela tela recupera diretamente o predecessor uma única vez. */
  @Test
  void startsConstructionFromUiWithoutReexecutingPlanning() {
    var first = service.start(productId, 117L, command);
    var second = service.start(productId, 117L, command);
    assertThat(second.id()).isEqualTo(first.id());
    assertThat(first.status()).isEqualTo("QUEUED");
    assertThat(service.status(productId, 117L, command).queueBlocker()).isNull();
    assertThat(service.status(productId, 113L, command).canResume()).isFalse();
    assertThat(runs.findById(planning.getId()).orElseThrow().getKnownCostUsd())
        .isEqualByComparingTo("8.945868");
    verify(activities, never())
        .requestProductActivityExecution(anyLong(), anyLong(), anyString(), any(), any(), any());
  }
}
