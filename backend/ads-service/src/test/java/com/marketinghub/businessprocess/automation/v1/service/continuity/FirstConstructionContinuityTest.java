package com.marketinghub.businessprocess.automation.v1.service.continuity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRunEvent;
import com.marketinghub.businessprocess.automation.v1.service.*;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequestResponse;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.*;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: homologar a primeira construção pelos comandos oficiais com diário e locks
 * locais.
 */
@DataJpaTest(
    showSql = false,
    properties = {"spring.liquibase.enabled=false"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FirstConstructionContinuityTest {
  @Autowired private ProcessRunRepository runs;
  @Autowired private ProcessRunEventRepository events;
  @Autowired private ProductRepository products;
  @Autowired private PlatformTransactionManager manager;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final BusinessProcessActivityExecutionService activities =
      mock(BusinessProcessActivityExecutionService.class);
  private final ProcessRunCorrectionBudget budget = mock(ProcessRunCorrectionBudget.class);
  private final LearningCycleConstructionPrerequisite prerequisite =
      mock(LearningCycleConstructionPrerequisite.class);
  private TransactionTemplate transaction;
  private ProcessRunService commands;
  private ProcessVersionContinuityService continuity;
  private ProcessVersionContinuityLookup lookup;
  private LearningSalesCycle cycle;
  private BusinessProcessDefinition planning;
  private BusinessProcessDefinition retired;
  private BusinessProcessDefinition published;
  private Product product;
  private ProcessRunCommand command;
  private Long planningRunId;
  private boolean accepted;

  /** Reconstitui o caso com dados sintéticos e os contratos reais de planejamento e automação. */
  @BeforeEach
  void setup() {
    transaction = new TransactionTemplate(manager);
    product =
        transaction.execute(
            ignored ->
                products.saveAndFlush(
                    Product.builder()
                        .slug("qa-" + UUID.randomUUID())
                        .name("PDE local")
                        .automaticExecutionEnabled(true)
                        .build()));
    var id = product.getId();
    var chainId = id + 7000;
    planning =
        definition(
            id + 1000,
            "pde-commercial-plan-offer",
            6,
            "PUBLISHED",
            "[{\"id\":\"marketStrategy\",\"type\":\"TASK\"},{\"id\":\"economics\",\"type\":\"TASK\"},"
                + "{\"id\":\"productArchitecture\",\"type\":\"TASK\"}]");
    retired =
        definition(
            id + 2000,
            "pde-construction-approval",
            7,
            "RETIRED",
            "[{\"id\":\"journey\",\"type\":\"TASK\",\"description\":\"Original\"}]");
    published =
        definition(
            id + 3000,
            retired.getProcessCode(),
            8,
            "PUBLISHED",
            "[{\"id\":\"journey\",\"type\":\"TASK\",\"description\":\"Revisada\"}]");
    var publishedGraph =
        (com.fasterxml.jackson.databind.node.ObjectNode) parse(published.getDiagramJson());
    publishedGraph.put("productValueDeliveryContract", "TURMALINA_VALUE_DELIVERY_V1");
    published.setDiagramJson(publishedGraph.toString());
    var originalChain = chain(chainId, "RETIRED", retired);
    var publishedChain = chain(chainId + 1, "PUBLISHED", published);
    when(chains.findById(chainId)).thenReturn(Optional.of(originalChain));
    when(chains.findFirstByChainCodeAndStatusOrderByVersionNumberDesc("pde", "PUBLISHED"))
        .thenReturn(Optional.of(publishedChain));
    when(processes.findAllByProcessCodeOrderByVersionNumberDesc(retired.getProcessCode()))
        .thenReturn(List.of(published, retired));
    for (var definition : List.of(planning, retired, published))
      when(processes.findById(definition.getId())).thenReturn(Optional.of(definition));
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(retired.getId()))
        .thenReturn(List.of(activity(retired, "journey", "Original")));
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(published.getId()))
        .thenReturn(List.of(activity(published, "journey", "Revisada")));

    cycle = new LearningSalesCycle();
    cycle.setId(id + 4000);
    cycle.setExperimentId(id + 5000);
    cycle.setProductId(id);
    cycle.setChainDefinitionId(chainId);
    cycle.setCreatedAt(Instant.parse("2026-10-15T15:00:00Z"));
    cycle.setProductVersion("qa-candidate-" + id);
    cycle.setStage("PLANNING");
    cycle.setStatus("OPEN");
    cycle.setReturnProcessId(retired.getId());
    cycle.setBriefJson("{}");
    cycle.setInheritedLearningJson("{}");
    command =
        new ProcessRunCommand(chainId, cycle.getId(), "experiment:" + cycle.getExperimentId());
    when(cycles.findById(cycle.getId())).thenReturn(Optional.of(cycle));
    when(cycles.findByExperimentId(cycle.getExperimentId())).thenReturn(Optional.of(cycle));
    when(cycles.findLocked(id, cycle.getId())).thenReturn(Optional.of(cycle));
    var experiment = new Experiment();
    experiment.setId(cycle.getExperimentId());
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.PLANNED);
    when(experiments.findById(cycle.getExperimentId())).thenReturn(Optional.of(experiment));
    for (var definition : List.of(planning, retired, published)) {
      when(activities.productProcessExecutions(
              definition.getId(), id, cycle.getId(), chainId, false))
          .thenAnswer(ignored -> snapshot(definition));
      when(activities.productProcessExecutions(
              definition.getId(), id, cycle.getId(), chainId, false, command.sourceReference()))
          .thenAnswer(ignored -> snapshot(definition));
    }
    setupAcceptedInputs();
    lookup = new ProcessVersionContinuityLookup(events, json);
    ReflectionTestUtils.setField(lookup, "cycles", cycles);
    var work = new LearningCycleWorkResolver(chains, activities, prerequisite);
    ReflectionTestUtils.setField(work, "versionContinuity", lookup);
    ReflectionTestUtils.setField(work, "processDefinitions", processes);
    var cycleCommands = mock(LearningCycleService.class);
    when(cycleCommands.completePreparationFromProcesses(eq(id), eq(cycle.getId()), anyLong()))
        .thenAnswer(
            ignored -> {
              cycle.setStage("ADJUSTMENT");
              cycle.setRevision(2);
              return null;
            });
    var cycleContinuation =
        new LearningCycleProcessContinuation(cycles, chains, products, work, cycleCommands);
    ReflectionTestUtils.setField(cycleContinuation, "versionContinuity", lookup);
    var first =
        new FirstProcessVersionContinuity(
            runs,
            cycles,
            processes,
            definitions,
            instances,
            tasks,
            experiments,
            work,
            new LearningCycleConstructionContext(cycles, chains, tasks, json),
            lookup,
            json);
    var context =
        new ProcessRunContext(
            activities, processes, chains, cycles, products, json, experiments, tasks);
    ReflectionTestUtils.setField(context, "constructionPrerequisites", prerequisite);
    ReflectionTestUtils.setField(context, "versionContinuity", lookup);
    ReflectionTestUtils.setField(context, "correctionBudget", budget);
    commands =
        new ProcessRunService(
            runs,
            events,
            products,
            context,
            new ProcessRunNavigation(processes, chains, runs, products, json),
            mock(ProcessRunSubprocesses.class),
            mock(ProcessRunGuidance.class),
            activities,
            json,
            manager,
            mock(ProcessRunOwnerPortrait.class));
    ReflectionTestUtils.setField(commands, "cycleContinuation", cycleContinuation);
    ReflectionTestUtils.setField(commands, "firstExecution", first);
    ReflectionTestUtils.setField(commands, "versionContinuity", lookup);
    continuity =
        new ProcessVersionContinuityService(
            products,
            cycles,
            processes,
            chains,
            definitions,
            instances,
            runs,
            events,
            activities,
            lookup,
            json);
    ReflectionTestUtils.setField(continuity, "firstExecution", first);
    planningRunId = commands.start(id, planning.getId(), command).id();
    accepted = true;
  }

  /** Conclui o planejamento, repete comandos concorrentes e solicita só a primeira construção. */
  @Test
  void preparesAndResumesAnUnstartedConstructionExactlyOnce() throws Exception {
    var planningStatus = commands.reconcile(planningRunId);
    assertThat(planningStatus.status()).isEqualTo("COMPLETED");
    assertThat(planningStatus.navigationUrl()).contains("/processes/" + retired.getId());
    assertThat(cycle.getStage()).isEqualTo("ADJUSTMENT");
    var preview = preview();
    assertThat(preview.canPrepare()).as(preview.reason()).isTrue();
    assertThat(preview.firstExecution().inputs()).hasSize(3);
    assertThat(preview.firstExecution().updatedRequirements()).containsExactly("journey");
    var confirmations = concurrently(() -> prepare(preview));
    var prepared = confirmations.getFirst();
    assertThat(prepared.receiptId()).isPositive();
    assertThat(confirmations.getLast()).isEqualTo(prepared);
    var runId = prepared.successorRunId();
    assertThat(commands.status(product.getId(), published.getId(), command).status())
        .isEqualTo("PAUSED");
    assertThat(commands.pending(50)).doesNotContain(runId);
    var receipt =
        lookup
            .firstExecutionForReference(command.sourceReference(), published.getId())
            .orElseThrow();
    assertThat(lookup.released(receipt)).isFalse();
    assertThat(
            lookup.permits(
                product.getId() + 1,
                cycle.getId(),
                command.sourceReference(),
                command.chainId(),
                published.getId()))
        .isFalse();
    assertThat(
            lookup.permits(
                product.getId(),
                cycle.getId() + 1,
                command.sourceReference(),
                command.chainId(),
                published.getId()))
        .isFalse();
    assertThatThrownBy(
            () ->
                commands.start(
                    product.getId(),
                    published.getId(),
                    new ProcessRunCommand(command.chainId() + 1, null, command.sourceReference())))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outra cadeia ou ciclo");

    transaction.executeWithoutResult(
        ignored ->
            products.findById(product.getId()).orElseThrow().setAutomaticExecutionEnabled(false));
    assertThatThrownBy(() -> commands.resume(product.getId(), published.getId(), runId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("STOP");
    assertThat(lookup.released(receipt)).isFalse();
    transaction.executeWithoutResult(
        ignored ->
            products.findById(product.getId()).orElseThrow().setAutomaticExecutionEnabled(true));

    assertThat(concurrently(() -> commands.resume(product.getId(), published.getId(), runId)))
        .allMatch(response -> "QUEUED".equals(response.status()));
    assertThat(lookup.released(receipt)).isTrue();
    assertThat(events.history(planningRunId, null, PageRequest.of(0, 20)))
        .extracting(ProcessRunEvent::getEventType)
        .containsExactly(
            "CYCLE_PROCESS_CONTINUED",
            "VERSION_CONTINUATION",
            "CYCLE_PROCESS_VERSION_REQUIRED",
            "COMPLETED",
            "STARTED");
    assertThat(runs.findById(planningRunId).orElseThrow().getKnownCostUsd())
        .isEqualByComparingTo("8.71");
    verify(instances, never()).saveAndFlush(any());
    when(budget.blockReason(any(), any())).thenReturn("Saldo do ciclo indisponível.");
    assertThat(commands.reconcile(runId).status()).isEqualTo("PAUSED");
    verify(activities, never())
        .requestProductActivityExecution(anyLong(), anyLong(), anyString(), any(), any(), any());
    when(budget.blockReason(any(), any())).thenReturn(null);
    commands.resume(product.getId(), published.getId(), runId);
    var task = mock(com.marketinghub.agenttask.AgentTaskResponse.class);
    when(task.id()).thenReturn(8000L);
    var dispatched =
        new ProductProcessActivityExecutionRequestResponse(
            published.getId(),
            product.getId(),
            "journey",
            command.sourceReference(),
            List.of(task),
            "PENDING",
            false,
            "Dédalo recebeu a atividade sintética");
    when(activities.requestProductActivityExecution(
            published.getId(),
            product.getId(),
            "journey",
            null,
            cycle.getId(),
            command.sourceReference()))
        .thenReturn(dispatched);
    assertThat(commands.reconcile(runId).status()).isEqualTo("WAITING_ACTIVITY");
    verify(activities, times(1))
        .requestProductActivityExecution(
            published.getId(),
            product.getId(),
            "journey",
            null,
            cycle.getId(),
            command.sourceReference());
    assertThat(runs.findById(runId).orElseThrow().getSourceReference())
        .isEqualTo(command.sourceReference());
  }

  /**
   * Protege contratação e contexto quando qualquer evidência ou configuração necessária divergir.
   */
  @Test
  void blocksIncompletenessStaleReviewAndIncompatibleGraphInAnotherIdentity() {
    assertThat(preview().canPrepare()).isFalse();
    commands.reconcile(planningRunId);
    var preview = preview();
    assertThat(preview.canPrepare()).as(preview.reason()).isTrue();
    cycle.setRevision(cycle.getRevision() + 1);
    assertThatThrownBy(() -> prepare(preview))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("As provas mudaram");
    var old = published.getDiagramJson();
    published.setDiagramJson(old.replace("\"type\":\"TASK\"", "\"type\":\"APPROVAL\""));
    assertThat(preview().canPrepare()).isFalse();
    assertThat(preview().reason()).contains("mapear a integração");
    published.setDiagramJson(old);
    var prepared = prepare(preview());
    cycle.setProductVersion("outra-candidata");
    assertThatThrownBy(
            () -> commands.resume(product.getId(), published.getId(), prepared.successorRunId()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("candidata deste recibo mudaram");
    assertThat(commands.pending(50)).doesNotContain(prepared.successorRunId());
  }

  /** Introduz somente os três resultados canônicos, com ocorrências e responsáveis distintos. */
  private void setupAcceptedInputs() {
    var created = cycle.getCreatedAt();
    when(tasks.findFunctionalSnapshotsByProcessSince(
            planning.getId(), command.sourceReference(), created))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    701L,
                    planning.getId(),
                    planning.getProcessCode(),
                    "marketStrategy",
                    "experiment-strategist",
                    "COMPLETED",
                    created,
                    created.plusSeconds(1),
                    """
                {"decision":"APPROVE","marketStrategicContract":{"contractVersion":"MARKET_STRATEGY_V4",
                "status":"READY_FOR_AGENT_VALIDATION","agentValidationPlan":{}}}
                """),
                new AgentTaskFunctionalSnapshot(
                    702L,
                    planning.getId(),
                    planning.getProcessCode(),
                    "economics",
                    "financial-agent",
                    "COMPLETED",
                    created,
                    created.plusSeconds(2),
                    """
                {"decision":"APPROVE","contractVersion":"PDE_AGENT_ECONOMICS_V1",
                "economics":{"commercialSpendAuthorized":false}}
                """),
                new AgentTaskFunctionalSnapshot(
                    703L,
                    planning.getId(),
                    planning.getProcessCode(),
                    "productArchitecture",
                    "landing-generator",
                    "COMPLETED",
                    created,
                    created.plusSeconds(3),
                    """
                {"decision":"APPROVE","productArchitecture":{"privatePrototype":{}}}
                """)));
    var ids = List.of("marketStrategy", "economics", "productArchitecture");
    var occurrences = new java.util.ArrayList<BusinessProcessActivityInstance>();
    for (int i = 0; i < ids.size(); i++) {
      var occurrence = new BusinessProcessActivityInstance();
      occurrence.setId(601L + i);
      occurrence.setCreatedAt(created);
      occurrence.setSourceReference(command.sourceReference());
      occurrence.setActivityDefinition(activity(planning, ids.get(i), "Entrega de planejamento"));
      occurrence.setStatus("COMPLETED");
      occurrence.setObjectiveAchieved(true);
      occurrences.add(occurrence);
      when(tasks.existsByIdAndActivityInstance_IdAndStatus(701L + i, 601L + i, "COMPLETED"))
          .thenReturn(true);
    }
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                planning.getId(), command.sourceReference()))
        .thenReturn(occurrences);
  }

  /** Expõe estados do catálogo local sem simular aceites da construção. */
  private ProductProcessActivityExecutionHistoryResponse snapshot(
      BusinessProcessDefinition definition) {
    boolean isPlanning = planning.equals(definition);
    boolean complete = isPlanning && accepted;
    var activityIds =
        isPlanning
            ? List.of("marketStrategy", "economics", "productArchitecture")
            : List.of("journey");
    var groups =
        activityIds.stream().map(activityId -> group(activityId, isPlanning, complete)).toList();
    var response = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(response.selectedProcessStatus()).thenReturn(definition.getStatus());
    when(response.selectedActivityCount()).thenReturn(isPlanning ? 3 : 1);
    when(response.completedActivityCount()).thenReturn(complete ? 3 : 0);
    when(response.remainingActivityCount()).thenReturn(complete ? 0 : isPlanning ? 3 : 1);
    when(response.objectiveAchieved()).thenReturn(complete);
    when(response.activities()).thenReturn(groups);
    when(response.currentActivityId()).thenReturn(complete ? null : activityIds.getFirst());
    when(response.currentExecutionReference()).thenReturn(command.sourceReference());
    when(response.costCoverage()).thenReturn(complete ? "COMPLETE" : "NO_EXECUTIONS");
    when(response.knownEstimatedCostUsd())
        .thenReturn(complete ? new BigDecimal("8.71") : BigDecimal.ZERO);
    return response;
  }

  /** Projeta a situação de uma atividade com o controle que o reconciliador consome. */
  private ProductProcessActivityExecutionGroupResponse group(
      String activityId, boolean isPlanning, boolean complete) {
    var group = mock(ProductProcessActivityExecutionGroupResponse.class);
    when(group.activityId()).thenReturn(activityId);
    when(group.activityName()).thenReturn(activityId);
    when(group.activityOwnerName()).thenReturn("Dédalo");
    when(group.selectedVersionActivity()).thenReturn(true);
    when(group.operationalState()).thenReturn(complete ? "COMPLETED" : "NOT_STARTED");
    when(group.objectiveAchieved()).thenReturn(complete);
    when(group.executionRequestAvailable()).thenReturn(!isPlanning);
    var control = mock(ProductProcessActivityExecutionControlResponse.class);
    when(control.executorType()).thenReturn("AGENT");
    when(control.interactionType()).thenReturn("COMMAND");
    when(control.actionAvailable()).thenReturn(true);
    when(group.executionControl()).thenReturn(control);
    return group;
  }

  /** Prepara a proposta revisada na mesma transação que os comandos HTTP usam. */
  private ProcessVersionContinuityService.Preview prepare(
      ProcessVersionContinuityService.Preview preview) {
    return transaction.execute(
        ignored ->
            continuity.prepare(
                product.getId(),
                cycle.getId(),
                retired.getId(),
                new ProcessVersionContinuityService.Request(
                    preview.fingerprint(), "Operador QA", "Preparar construção", null)));
  }

  /** Consulta o BPM aposentado na cadeia original. */
  private ProcessVersionContinuityService.Preview preview() {
    return transaction.execute(
        ignored -> continuity.preview(product.getId(), cycle.getId(), retired.getId()));
  }

  /** Dispara duas chamadas simultâneas, com timeout e limpeza do executor local. */
  private <T> List<T> concurrently(java.util.concurrent.Callable<T> call) throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      var go = new CountDownLatch(1);
      var left =
          executor.submit(
              () -> {
                go.await();
                return call.call();
              });
      var right =
          executor.submit(
              () -> {
                go.await();
                return call.call();
              });
      go.countDown();
      return List.of(left.get(35, TimeUnit.SECONDS), right.get(35, TimeUnit.SECONDS));
    }
  }

  /** Monta o catálogo mínimo de planejamento e construção. */
  private BusinessProcessChainDefinition chain(
      Long id, String status, BusinessProcessDefinition construction) {
    var chain = new BusinessProcessChainDefinition();
    chain.setId(id);
    chain.setStatus(status);
    chain.setChainCode("pde");
    var before = new BusinessProcessChainItem();
    before.setSequenceNumber(2);
    before.setProcessDefinition(planning);
    var after = new BusinessProcessChainItem();
    after.setSequenceNumber(3);
    after.setProcessDefinition(construction);
    chain.setItems(List.of(before, after));
    return chain;
  }

  /** Define a versão e um grafo sintético do catálogo. */
  private BusinessProcessDefinition definition(
      long id, String code, int version, String status, String nodes) {
    var value = new BusinessProcessDefinition();
    value.setId(id);
    value.setProcessCode(code);
    value.setStatus(status);
    value.setVersionNumber(version);
    value.setName(code);
    value.setDiagramJson("{\"nodes\":" + nodes + ",\"flows\":[]}");
    return value;
  }

  /** Descreve o contrato de uma atividade usado pela conferência de adoção. */
  private BusinessProcessActivityDefinition activity(
      BusinessProcessDefinition process, String id, String description) {
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId(id);
    activity.setName(id);
    activity.setProcessDefinition(process);
    activity.setDefinitionJson(
        json.createObjectNode()
            .put("id", id)
            .put("label", id)
            .put("description", description)
            .toString());
    return activity;
  }

  /** Lê o grafo local criado pelo teste. */
  private com.fasterxml.jackson.databind.JsonNode parse(String value) {
    try {
      return json.readTree(value);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new AssertionError("Grafo do teste inválido.", ex);
    }
  }
}
