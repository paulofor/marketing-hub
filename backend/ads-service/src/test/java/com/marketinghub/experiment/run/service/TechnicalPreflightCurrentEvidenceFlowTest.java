package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.controller.ProcessRunController;
import com.marketinghub.businessprocess.automation.v1.service.*;
import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.run.*;
import com.marketinghub.experiment.run.controller.BackendExperimentRunController;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.experiment.*;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Responsabilidade: homologar plano, relatório e renovação técnica pelo mesmo fluxo BPM local. */
class TechnicalPreflightCurrentEvidenceFlowTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final AgentTaskService paidTasks = mock(AgentTaskService.class);
  private final ExperimentTechnicalPreflightEvidenceService evidence =
      mock(ExperimentTechnicalPreflightEvidenceService.class);
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final Map<String, BusinessProcessActivityDefinition> stages = new LinkedHashMap<>();
  private final List<BusinessProcessActivityInstance> history = new ArrayList<>();
  private final Map<String, ExperimentTechnicalPreflightEvidenceService.Evidence> current =
      new HashMap<>();
  private final Product product =
      Product.builder().id(96001L).internalName("Produto local").build();
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final ExperimentTechnicalPreflightActivityExecutor executor =
      new ExperimentTechnicalPreflightActivityExecutor(predecessors, evidence, instances, json);
  private final BusinessProcessActivityExecutionService service =
      new BusinessProcessActivityExecutionService(
          processes,
          definitions,
          mock(AgentTaskRepository.class),
          mock(AgentTaskActivityCoverageRepository.class),
          instances,
          plans,
          null,
          products,
          experiments,
          paidTasks,
          json,
          List.of(executor),
          List.of(),
          List.of());
  private String reference = "experiment:96002";

  /** Prepara três provas válidas e uma pendência financeira sem recursos externos. */
  @BeforeEach
  void setup() {
    process.setId(96003L);
    process.setName("Homologação técnica local");
    process.setProcessCode("experiment-homologation-activation");
    process.setVersionNumber(1);
    process.setStatus("PUBLISHED");
    process.setDiagramJson(
        """
        {"nodes":[{"id":"surfaces","type":"TASK","label":"Superfícies"},
        {"id":"transaction","type":"TASK","label":"Compra"},
        {"id":"measurement","type":"TASK","label":"Medição"},
        {"id":"financialGuardrails","type":"TASK","label":"Limites financeiros"}],
        "flows":[{"from":"surfaces","to":"transaction"},
        {"from":"transaction","to":"measurement"},
        {"from":"measurement","to":"financialGuardrails"}]}
        """);
    when(processes.findById(process.getId())).thenReturn(Optional.of(process));
    bindProductAndExperiment(product.getId(), 96002L);
    var governingPlan = plan(96004L, "Plano da referência local");
    when(plans.findByProductId(product.getId()))
        .thenReturn(List.of(plan(96006L, "Outra candidata local"), governingPlan));
    when(plans.findByExperimentReference(96002L)).thenReturn(List.of(governingPlan));
    for (String code : List.of("surfaces", "transaction", "measurement", "financialGuardrails")) {
      var stage = new BusinessProcessActivityDefinition();
      stage.setId(96100L + stages.size());
      stage.setActivityId(code);
      stage.setName(
          switch (code) {
            case "surfaces" -> "Validar superfícies candidatas";
            case "transaction" -> "Testar compra, acesso e entrega";
            case "measurement" -> "Validar eventos e deduplicação";
            default -> "Validar limites financeiros persistidos";
          });
      stage.setProcessDefinition(process);
      stage.setDefinitionJson("{\"responsibleAgentKeys\":[]}");
      stages.put(code, stage);
      when(definitions.findByProcessDefinitionIdAndActivityId(process.getId(), code))
          .thenReturn(Optional.of(stage));
      if (!"financialGuardrails".equals(code)) {
        var proof = proof(code, "original-" + code);
        current.put(code, proof);
        var instance = new BusinessProcessActivityInstance();
        instance.setId(96200L + history.size());
        instance.setActivityDefinition(stage);
        instance.setSourceReference(reference);
        instance.setStatus("COMPLETED");
        instance.setObjectiveAchieved(true);
        instance.setOccurrenceNumber(1);
        instance.setObjectiveEvidenceJson(proof.objectiveEvidence().toString());
        instance.setCreatedAt(Instant.parse("2026-10-02T18:00:00Z"));
        history.add(instance);
      }
    }
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(process.getId()))
        .thenReturn(new ArrayList<>(stages.values()));
    when(predecessors.readiness(eq(process), any(), anyString()))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Ordem local conferida."));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), anyString()))
        .thenAnswer(
            inv ->
                history.stream()
                    .filter(i -> i.getActivityDefinition().getId().equals(inv.getArgument(0)))
                    .filter(i -> i.getSourceReference().equals(inv.getArgument(1)))
                    .max(
                        Comparator.comparingInt(
                            BusinessProcessActivityInstance::getOccurrenceNumber)));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), anyString()))
        .thenAnswer(
            inv ->
                history.stream()
                    .filter(i -> i.getActivityDefinition().getId().equals(inv.getArgument(0)))
                    .filter(i -> i.getSourceReference().equals(inv.getArgument(1)))
                    .max(
                        Comparator.comparingInt(
                            BusinessProcessActivityInstance::getOccurrenceNumber)));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                eq(process.getProcessCode()), anyString()))
        .thenAnswer(
            inv ->
                history.stream()
                    .filter(i -> i.getSourceReference().equals(inv.getArgument(1)))
                    .toList());
    when(evidence.evaluate(anyString(), eq(product), anyString()))
        .thenAnswer(
            inv -> {
              String stage = inv.getArgument(0);
              var proof = current.get(stage);
              if (proof == null)
                throw new IllegalStateException(
                    "financialGuardrails".equals(stage)
                        ? "Plano financeiro local vencido; nenhuma análise paga autorizada."
                        : "A publicação, o HTML ou o contrato Quartzo mudaram após a homologação.");
              return proof;
            });
    when(instances.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              var saved = inv.<BusinessProcessActivityInstance>getArgument(0);
              saved.setId(96300L + history.size());
              history.add(saved);
              return saved;
            });
  }

  /**
   * HTTP e controle pausado preservam as três provas encerradas, mesmo após mudança da superfície.
   */
  @Test
  void reportsClosedReferenceWithoutLosingPartialProgress() throws Exception {
    current.remove("surfaces");
    when(evidence.executionBlockReason(product, reference))
        .thenReturn("Experimento encerrado; preserve as provas.");
    var http =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service))
            .build();
    String response =
        http.perform(
                get(
                        "/api/business-processes/{process}/products/{product}/activity-executions",
                        process.getId(),
                        product.getId())
                    .param("sourceReference", reference)
                    .param("includePromptAudit", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedActivityCount").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    var result =
        json.readValue(
            response,
            com.marketinghub
                .businessprocess
                .execution
                .service
                .productProcessExecutions
                .ProductProcessActivityExecutionHistoryResponse
                .class);
    assertThat(result.operationalState()).isEqualTo("CLOSED");
    assertThat(result.objectiveAchieved()).isFalse();
    assertThat(result.completedActivityCount()).isEqualTo(3);
    assertThat(result.remainingActivityCount()).isEqualTo(1);
    assertThat(result.currentActivityId()).isEqualTo("financialGuardrails");
    assertThat(result.activities().getFirst().activityInstanceId()).isEqualTo(96200L);
    assertThat(result.activities().getFirst().objectiveAchieved()).isTrue();
    assertThat(result.activities())
        .allSatisfy(a -> assertThat(a.executionRequestAvailable()).isFalse());
    assertThat(result.currentActivityStateReason()).contains("encerrado");
    String output = System.getProperty("closed-history.fixture-output");
    if (output != null) Files.writeString(Path.of(output), json.writeValueAsString(result));
    verifiesConsultativeControl(result);
    verifiesHistoricalWorkspace();
    verify(evidence, never()).evaluate(anyString(), any(), anyString());
    verify(instances, never()).saveAndFlush(any());
    verifyNoInteractions(paidTasks);
  }

  /** Exporta o run e o preflight históricos pelo HTTP real, sem inventar comandos na tela local. */
  private void verifiesHistoricalWorkspace() throws Exception {
    var experiment = experiments.findById(96002L).orElseThrow();
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    var run = new ExperimentRun();
    run.setId(96005L);
    run.setExperiment(experiment);
    run.setRunNumber(2);
    run.setMode(ExperimentRunMode.PRODUCTION);
    run.setStatus(ExperimentRunStatus.COMPLETED);
    run.setDataQualityStatus(ExperimentRunDataQualityStatus.VALID);
    run.setPreflightCompletedAt(Instant.parse("2026-10-02T18:00:00Z"));
    run.setEndedAt(Instant.parse("2026-10-03T18:00:00Z"));
    var runs = mock(ExperimentRunRepository.class);
    var gates = mock(ExperimentRunGateResultRepository.class);
    var dossiers = mock(MoisCommercialDossierPreflightService.class);
    when(experiments.existsById(experiment.getId())).thenReturn(true);
    when(runs.findByExperimentIdOrderByRunNumberAsc(experiment.getId())).thenReturn(List.of(run));
    when(runs.findById(run.getId())).thenReturn(Optional.of(run));
    var gate = new ExperimentRunGateResult();
    gate.setExperimentRun(run);
    gate.setGateCode(ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED);
    gate.setGateGroup(ExperimentRunGateGroup.UPSTREAM_QUALITY);
    gate.setStatus(ExperimentRunGateStatus.PASS);
    gate.setSummary("Superfície da versão local comprovada no histórico.");
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(run.getId()))
        .thenReturn(List.of(gate));
    var service = new BackendExperimentRunService(experiments, runs, gates, dossiers);
    var http = MockMvcBuilders.standaloneSetup(new BackendExperimentRunController(service)).build();
    String runResponse =
        http.perform(get("/api/experiments/{id}/runs", experiment.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(run.getId()))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    String preflightResponse =
        http.perform(get("/api/experiment-runs/{id}/preflight", run.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canRenewTechnicalHomologation").value(false))
            .andExpect(jsonPath("$.executionBlockReason").isNotEmpty())
            .andExpect(jsonPath("$.gates[0].status").value("PASS"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    String output = System.getProperty("historical-preflight.workspace-output");
    if (output != null) {
      var fixture = json.createObjectNode();
      fixture.set("runs", json.readTree(runResponse));
      fixture.set("preflight", json.readTree(preflightResponse));
      Files.writeString(Path.of(output), json.writeValueAsString(fixture));
    }
    verify(runs, never()).save(any());
    verify(gates, never()).saveAll(any());
    verifyNoInteractions(dossiers);
  }

  /** Consulta o motor com a resposta real do BPM, sem mudar pausa, custos, diário ou tarefas. */
  private void verifiesConsultativeControl(
      com.marketinghub
              .businessprocess
              .execution
              .service
              .productProcessExecutions
              .ProductProcessActivityExecutionHistoryResponse
          history)
      throws Exception {
    var run = new ProcessRun();
    run.setId(96401L);
    run.setProductId(product.getId());
    run.setProcessDefinitionId(process.getId());
    run.setChainDefinitionId(96402L);
    run.setSourceReference(reference);
    run.setTotalActivities(4);
    run.setCompletedActivities(2);
    run.setRemainingActivities(2);
    run.setCurrentActivityId("surfaces");
    run.setCurrentActivityName("Validar superfícies candidatas");
    run.setReason("Experimento encerrado; preserve as provas.");
    var runs = mock(ProcessRunRepository.class);
    var events = mock(ProcessRunEventRepository.class);
    var context = mock(ProcessRunContext.class);
    var subprocesses = mock(ProcessRunSubprocesses.class);
    var transactions = mock(PlatformTransactionManager.class);
    when(transactions.getTransaction(any())).thenAnswer(inv -> new SimpleTransactionStatus());
    when(runs.findByScopeKey(anyString())).thenReturn(Optional.of(run));
    when(context.read(run, false)).thenReturn(history);
    when(context.dispatchBlockReason(run)).thenReturn(run.getReason());
    var motor =
        new ProcessRunService(
            runs,
            events,
            products,
            context,
            mock(ProcessRunNavigation.class),
            subprocesses,
            mock(ProcessRunGuidance.class),
            service,
            json,
            transactions);
    var http =
        MockMvcBuilders.standaloneSetup(new ProcessRunController(motor, "fixture-only", ""))
            .build();
    for (String state : List.of("PAUSED", "CLOSED")) {
      run.setStatus(state);
      String response =
          http.perform(
                  get(
                          "/api/business-processes/{process}/products/{product}/automation/v1",
                          process.getId(),
                          product.getId())
                      .param("chainId", run.getChainDefinitionId().toString())
                      .param("sourceReference", reference))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.status").value(state))
              .andExpect(jsonPath("$.completedActivities").value(3))
              .andExpect(jsonPath("$.remainingActivities").value(1))
              .andExpect(jsonPath("$.currentActivityId").value("financialGuardrails"))
              .andExpect(jsonPath("$.currentSequence").value(4))
              .andExpect(jsonPath("$.canResume").value(false))
              .andReturn()
              .getResponse()
              .getContentAsString(StandardCharsets.UTF_8);
      assertThat(run.getStatus()).isEqualTo(state);
      assertThat(run.getCompletedActivities()).isEqualTo(2);
      assertThat(run.getCurrentActivityId()).isEqualTo("surfaces");
      String output = System.getProperty("historical-preflight.control-output");
      if (output != null && "PAUSED".equals(state)) Files.writeString(Path.of(output), response);
    }
    verify(runs, never()).save(any());
    verify(runs, never()).saveAndFlush(any());
    verifyNoInteractions(events, subprocesses, paidTasks);
  }

  /** O relatório usa o plano do experimento mesmo com outra candidata mais recente do produto. */
  @ParameterizedTest
  @CsvSource({"7,88,2,35", "97001,97002,97003,97004"})
  void selectsOnlyThePlanOfTheExplicitExperiment(
      long productId, long experimentId, long planId, long otherId) {
    bindProductAndExperiment(productId, experimentId);
    var governing = plan(planId, "Plano da referência");
    var other = plan(otherId, "Outro experimento");
    when(plans.findByProductId(productId)).thenReturn(List.of(other, governing));
    when(plans.findByExperimentReference(experimentId)).thenReturn(List.of(governing));

    var report =
        service.productProcessExecutions(process.getId(), productId, null, null, false, reference);

    assertThat(report.commercialPlanId()).isEqualTo(planId);
    assertThat(report.commercialPlanName()).isEqualTo("Plano da referência");
    assertThat(report.currentExecutionReference()).isEqualTo(reference);
    verifyNoInteractions(paidTasks);
  }

  /** Referência sem plano não herda a economia de outra candidata nem de outro produto. */
  @Test
  void neverFallsBackToUnrelatedProductPlan() {
    when(plans.findByProductId(product.getId()))
        .thenReturn(List.of(plan(97004L, "Outra candidata")));
    when(plans.findByExperimentReference(96002L))
        .thenReturn(List.of(plan(98001L, "Outro produto")));

    assertThat(report().commercialPlanId()).isNull();
    verifyNoInteractions(paidTasks);
  }

  /** Uma prova vigente continua concluída e o bloqueio financeiro não causa nova execução paga. */
  @Test
  void preservesCurrentEvidenceAndFinancialWait() throws Exception {
    var report = report();

    assertThat(report.completedActivityCount()).isEqualTo(3);
    assertThat(report.objectiveAchieved()).isFalse();
    assertThat(report.currentActivityId()).isEqualTo("financialGuardrails");
    assertThat(report.currentActivityStateReason()).contains("financeiro local vencido");
    assertThat(report.activities().getFirst().objectiveAchieved()).isTrue();
    verify(instances, never()).saveAndFlush(any());
    verifyNoInteractions(paidTasks);
    writeReport("current-report.json", report);
  }

  /** Fonte inválida retira somente a conclusão vigente e preserva a tentativa anterior intacta. */
  @Test
  void staleSurfaceDoesNotCountAsCurrentObjective() throws Exception {
    var original = history.getFirst();
    var originalEvidence = original.getObjectiveEvidenceJson();
    current.remove("surfaces");

    var report = report();

    assertThat(report.completedActivityCount()).isEqualTo(2);
    assertThat(report.currentActivityId()).isEqualTo("surfaces");
    assertThat(report.activities().getFirst().objectiveAchieved()).isFalse();
    assertThat(report.activities().getFirst().executionRequestAvailable()).isFalse();
    assertThat(report.currentActivityStateReason()).contains("mudaram após a homologação");
    assertThat(original.getStatus()).isEqualTo("COMPLETED");
    assertThat(original.isObjectiveAchieved()).isTrue();
    assertThat(original.getObjectiveEvidenceJson()).isEqualTo(originalEvidence);
    assertThat(report.activities().get(1).objectiveAchieved()).isTrue();
    assertThat(report.activities().get(2).objectiveAchieved()).isTrue();
    verify(instances, never()).saveAndFlush(any());
    verifyNoInteractions(paidTasks);
    writeReport("stale-report.json", report);
  }

  /** A tentativa pendente preserva o workspace mesmo quando predecessoras impedem o comando. */
  @Test
  void pendingRenewalKeepsWorkspaceAndHistoryWithoutCompletingActivities() throws Exception {
    current.clear();
    for (String code : List.of("transaction", "measurement", "financialGuardrails")) {
      when(predecessors.readiness(process, stages.get(code), reference))
          .thenReturn(
              new ProductProcessActivityPredecessorReadiness(
                  false, "Conclua a atividade anterior."));
    }

    var report = report();

    assertThat(report.completedActivityCount()).isZero();
    assertThat(report.objectiveAchieved()).isFalse();
    assertThat(report.activities())
        .allSatisfy(
            a -> {
              assertThat(a.executionControl().workspaceCode()).isEqualTo("EXPERIMENT_PREFLIGHT");
              assertThat(a.executionControl().workspaceReferenceId()).isEqualTo(96002L);
              assertThat(a.executionRequestAvailable()).isFalse();
              assertThat(a.objectiveAchieved()).isFalse();
            });
    assertThat(history)
        .hasSize(3)
        .allSatisfy(i -> assertThat(i.getStatus()).isEqualTo("COMPLETED"));
    verify(instances, never()).saveAndFlush(any());
    verifyNoInteractions(paidTasks);
    writeReport("pending-report.json", report);
  }

  /** Prova renovada passa pelo comando oficial e repetição conserva uma única nova ocorrência. */
  @Test
  void renewsThroughCanonicalCommandAndRemainsIdempotent() {
    var original = history.getFirst();
    current.put("surfaces", proof("surfaces", "renewed-surfaces"));
    assertThat(report().activities().getFirst().executionRequestAvailable()).isTrue();

    var result =
        service.requestProductActivityExecution(
            process.getId(), product.getId(), "surfaces", null, null, reference);
    executor.execute(process, stages.get("surfaces"), product, reference);

    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(history).hasSize(4);
    assertThat(history.getLast().getOccurrenceNumber()).isEqualTo(2);
    assertThat(history.getLast().getKnownCostUsd()).isZero();
    assertThat(history.getFirst()).isSameAs(original);
    assertThat(report().completedActivityCount()).isEqualTo(3);
    assertThat(report().currentActivityId()).isEqualTo("financialGuardrails");
    verify(instances, times(1)).saveAndFlush(any());
    verifyNoInteractions(paidTasks);
  }

  /** Mantém produto e experimento explicitamente vinculados nos seletores da consulta local. */
  private void bindProductAndExperiment(long productId, long experimentId) {
    product.setId(productId);
    reference = "experiment:" + experimentId;
    var experiment = Experiment.builder().id(experimentId).product(product).build();
    when(products.findById(productId)).thenReturn(Optional.of(product));
    when(experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(evidence.referencedExperimentId(product, reference)).thenReturn(experimentId);
    when(experiments.existsByIdAndProductId(experimentId, productId)).thenReturn(true);
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(productId))
        .thenReturn(List.of(experiment));
  }

  /** Consulta o mesmo contrato que alimenta a tela e o conciliador de processos. */
  private com.marketinghub
          .businessprocess
          .execution
          .service
          .productProcessExecutions
          .ProductProcessActivityExecutionHistoryResponse
      report() {
    return service.productProcessExecutions(
        process.getId(), product.getId(), null, null, false, reference);
  }

  /** Cria evidência estruturada com impressão estável e identidade segregada. */
  private ExperimentTechnicalPreflightEvidenceService.Evidence proof(
      String activity, String fingerprint) {
    var data =
        json.createObjectNode()
            .put("runId", 96005L)
            .put("activityId", activity)
            .put("inputFingerprint", fingerprint);
    return new ExperimentTechnicalPreflightEvidenceService.Evidence(
        96002L, 96005L, fingerprint, data);
  }

  /** Cria uma referência comercial local sem autorização nem credencial externa. */
  private CommercialPlan plan(long id, String name) {
    var plan = new CommercialPlan();
    plan.setId(id);
    plan.setName(name);
    return plan;
  }

  /** Exporta a resposta real do serviço local para homologação visual sem reconstruir estados. */
  private void writeReport(String filename, Object report) throws Exception {
    String output = System.getProperty("preflight.output");
    if (output == null) return;
    Files.createDirectories(Path.of(output));
    Files.writeString(Path.of(output, filename), json.writeValueAsString(report));
  }
}
