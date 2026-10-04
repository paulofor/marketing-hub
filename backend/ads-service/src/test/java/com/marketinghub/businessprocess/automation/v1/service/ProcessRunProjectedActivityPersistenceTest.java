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

/** Responsabilidade: comprovar que projeção de medição não substitui execução real no motor. */
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
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
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
