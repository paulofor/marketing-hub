package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.service.*;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.quartzo.commercial.v1.service.*;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar histórico Quartzo pela API real sem reabrir revisão paga. */
class QuartzoHistoricalPreparationTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final AgentTaskService agentTasks = mock(AgentTaskService.class);
  private final FinancialPlanService finances = mock(FinancialPlanService.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final QuartzoCommercialChecks checks = mock(QuartzoCommercialChecks.class);
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final QuartzoCommercialContext context =
      spy(
          new QuartzoCommercialContext(
              experiments,
              cycles,
              mock(ExperimentCampaignDestinationPolicy.class),
              mock(CreativeRepository.class),
              mock(com.marketinghub.planning.service.CommercialPlanLandingAssetService.class),
              mock(ExperimentTargetingSelectionService.class),
              finances,
              plans,
              json));
  private final QuartzoCommercialService preparation =
      new QuartzoCommercialService(
          context, checks, instances, definitions, processes, tasks, experiments, predecessors);
  private final QuartzoCommercialAgentReadiness readiness =
      new QuartzoCommercialAgentReadiness(context, preparation, instances);
  private final Product product =
      Product.builder()
          .id(97801L)
          .name("Produto sintético")
          .internalName("Quartzo de teste")
          .validationDefinitionVersion("v1")
          .automaticExecutionEnabled(true)
          .productTypeDefinition(
              ProductTypeDefinition.builder().code("LOW_TICKET_DIGITAL_PRODUCT").build())
          .build();
  private final Experiment experiment = new Experiment();
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final List<BusinessProcessActivityDefinition> steps = new ArrayList<>();
  private final List<BusinessProcessActivityInstance> proofs = new ArrayList<>();
  private MockMvc http;

  /**
   * Liga controller, projeção, gate e contexto reais com fontes locais de uma referência encerrada.
   */
  @BeforeEach
  void setup() throws Exception {
    experiment.setId(97802L);
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setEndDate(java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1));
    when(experiments.findById(experiment.getId())).thenReturn(Optional.of(experiment));
    when(experiments.existsByIdAndProductId(experiment.getId(), product.getId())).thenReturn(true);
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(product.getId()))
        .thenReturn(List.of(experiment));
    process.setId(97803L);
    process.setProcessCode(QuartzoCommercialContext.CODE);
    process.setName("Preparar operação comercial Quartzo");
    process.setVersionNumber(1);
    process.setExecutionScope("PRODUCT");
    process.setStatus("PUBLISHED");
    var diagram = json.createObjectNode();
    var nodes = diagram.putArray("nodes");
    var flows = diagram.putArray("flows");
    String previous = null;
    for (String step :
        List.of(
            "entry",
            "creative",
            "checkout",
            "targeting",
            "economics",
            "humanExperienceReview",
            "commercialIntegrityReview",
            "ready")) {
      var definition = new BusinessProcessActivityDefinition();
      definition.setId(97700L + steps.size());
      definition.setProcessDefinition(process);
      definition.setActivityId(step);
      definition.setName(step);
      definition.setDefinitionJson(
          QuartzoCommercialService.REVIEWS.contains(step)
              ? "{\"responsibleAgentKeys\":[\"review-agent\"]}"
              : "{\"responsibleAgentKeys\":[]}");
      steps.add(definition);
      nodes.addObject().put("id", step).put("type", "TASK").put("label", step);
      if (previous != null) flows.addObject().put("from", previous).put("to", step);
      previous = step;
      var proof = new BusinessProcessActivityInstance();
      proof.setId(97800L + proofs.size());
      proof.setActivityDefinition(definition);
      proof.setSourceReference("experiment:97802");
      proof.setOccurrenceNumber(1);
      proof.setStatus("COMPLETED");
      proof.setObjectiveAchieved(true);
      proof.setCreatedAt(Instant.parse("2026-09-01T00:00:00Z"));
      proof.setUpdatedAt(proof.getCreatedAt());
      proof.setKnownCostUsd(new BigDecimal("0.20"));
      proof.setCostCoverage("COMPLETE");
      var recorded =
          json.createObjectNode()
              .put("productId", product.getId())
              .put("experimentId", experiment.getId())
              .put("productVersion", "v1")
              .put("fingerprint", "past")
              .put("activity", step);
      recorded.putObject("financialPlan").put("stale", false);
      recorded.put(
          "activityFingerprint", QuartzoCommercialContext.activityFingerprint(step, recorded));
      proof.setObjectiveEvidenceJson(recorded.toString());
      proofs.add(proof);
      when(definitions.findByProcessDefinitionIdAndActivityId(process.getId(), step))
          .thenReturn(Optional.of(definition));
      when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
              definition.getId(), "experiment:97802"))
          .thenAnswer(
              inv ->
                  proofs.stream().filter(p -> p.getActivityDefinition() == definition).findFirst());
    }
    process.setDiagramJson(diagram.toString());
    var changed =
        json.createObjectNode()
            .put("productId", product.getId())
            .put("experimentId", experiment.getId())
            .put("productVersion", "v1")
            .put("fingerprint", "current");
    changed.putObject("financialPlan").put("stale", true);
    doReturn(changed).when(context).snapshot("experiment:97802");
    when(processes.findById(process.getId())).thenReturn(Optional.of(process));
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(process.getId())).thenReturn(steps);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                process.getProcessCode(), "experiment:97802"))
        .thenReturn(proofs);
    var products = mock(ProductRepository.class);
    when(products.findById(product.getId())).thenReturn(Optional.of(product));
    var service =
        new BusinessProcessActivityExecutionService(
            processes,
            definitions,
            tasks,
            mock(AgentTaskActivityCoverageRepository.class),
            instances,
            plans,
            null,
            products,
            experiments,
            agentTasks,
            json,
            List.of(preparation),
            List.of(),
            List.of(readiness));
    http =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service))
            .build();
  }

  /** As oito conclusões reaparecem sem revalidar fontes vencidas ou executar comandos pelo HTTP. */
  @Test
  void preservesEightHistoricalObjectivesAndRejectsNewTasks() throws Exception {
    String response =
        http.perform(
                get("/api/business-processes/97803/products/97801/activity-executions")
                    .param("sourceReference", "experiment:97802")
                    .param("includePromptAudit", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedActivityCount").value(8))
            .andExpect(jsonPath("$.objectiveAchieved").value(true))
            .andExpect(
                jsonPath(
                    "$.activities[*].executionControl.actionAvailable",
                    org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(false))))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    for (var step : steps) {
      assertThat(readiness.readiness(process, step, product, "experiment:97802").ready()).isFalse();
      http.perform(
              post(
                      "/api/business-processes/97803/products/97801/activities/{step}/execution-requests",
                      step.getActivityId())
                  .param("sourceReference", "experiment:97802")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isConflict());
    }
    verify(context, never()).snapshot(anyString());
    verifyNoInteractions(finances, checks, agentTasks);
    verify(instances, never()).saveAndFlush(any());
    reconcilesSameHistoricalResponseWithoutDispatch(response);
    String output = System.getProperty("quartzo-history.fixture-output");
    if (output != null) Files.writeString(Path.of(output), response);
  }

  /** Liga a resposta HTTP real ao motor e comprova conclusão idempotente da mesma referência. */
  private void reconcilesSameHistoricalResponseWithoutDispatch(String response) throws Exception {
    var history =
        json.readValue(
            response,
            com.marketinghub
                .businessprocess
                .execution
                .service
                .productProcessExecutions
                .ProductProcessActivityExecutionHistoryResponse
                .class);
    var run = new com.marketinghub.businessprocess.automation.v1.ProcessRun();
    run.setId(97840L);
    run.setProductId(product.getId());
    run.setProcessDefinitionId(process.getId());
    run.setChainDefinitionId(97814L);
    run.setSourceReference(history.currentExecutionReference());
    run.setStatus("WAITING_INPUT");
    run.setCreatedAt(Instant.parse("2026-09-01T00:00:00Z"));
    run.setUpdatedAt(run.getCreatedAt());
    var runs = mock(com.marketinghub.repository.jpa.processautomation.ProcessRunRepository.class);
    var events =
        mock(com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository.class);
    var products = mock(ProductRepository.class);
    var runContext =
        mock(com.marketinghub.businessprocess.automation.v1.service.ProcessRunContext.class);
    var subprocesses =
        mock(com.marketinghub.businessprocess.automation.v1.service.ProcessRunSubprocesses.class);
    var activityCommands = mock(BusinessProcessActivityExecutionService.class);
    var transactions = mock(org.springframework.transaction.PlatformTransactionManager.class);
    when(transactions.getTransaction(any()))
        .thenAnswer(inv -> new org.springframework.transaction.support.SimpleTransactionStatus());
    when(products.findLockedById(product.getId())).thenReturn(Optional.of(product));
    when(runs.identity(run.getId()))
        .thenReturn(
            Optional.of(
                new com.marketinghub.businessprocess.automation.v1.service.status
                    .ProcessRunIdentity(product.getId(), process.getId())));
    when(runs.findById(run.getId())).thenReturn(Optional.of(run));
    when(runContext.read(run, false)).thenReturn(history);
    when(runContext.graph(process.getId()))
        .thenReturn(
            new com.marketinghub.businessprocess.automation.v1.service.ProcessExecutionGraph(
                json.readTree(process.getDiagramJson())));
    String historicalReason =
        context.historicalBlockReason(run.getSourceReference(), product.getId());
    when(runContext.dispatchBlockReason(run)).thenReturn(historicalReason);
    var motor =
        new com.marketinghub.businessprocess.automation.v1.service.ProcessRunService(
            runs,
            events,
            products,
            runContext,
            mock(com.marketinghub.businessprocess.automation.v1.service.ProcessRunNavigation.class),
            subprocesses,
            mock(com.marketinghub.businessprocess.automation.v1.service.ProcessRunGuidance.class),
            activityCommands,
            json,
            transactions);
    var control =
        MockMvcBuilders.standaloneSetup(
                new com.marketinghub.businessprocess.automation.v1.controller.ProcessRunController(
                    motor, "fixture-only", ""))
            .build();
    String result =
        control
            .perform(
                post(
                        "/api/internal/business-processes/automation/v1/stage-executions/{id}/reconcile",
                        run.getId())
                    .header("X-Process-Worker-Token", "fixture-only"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.completedActivities").value(8))
            .andExpect(jsonPath("$.remainingActivities").value(0))
            .andExpect(jsonPath("$.sourceReference").value(history.currentExecutionReference()))
            .andExpect(jsonPath("$.canResume").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    var repeated = motor.reconcile(run.getId());
    assertThat(repeated.knownCostUsd()).isNull();
    assertThat(repeated.costCoverage()).isEqualTo("NO_EXECUTIONS");
    verify(events, times(1)).save(any());
    verifyNoInteractions(activityCommands, subprocesses, finances, checks, agentTasks);
    String output = System.getProperty("quartzo-automation.fixture-output");
    if (output != null) Files.writeString(Path.of(output), result);
  }

  /** Prova ausente continua pendente; ciclo encerrado não fabrica o oitavo objetivo. */
  @Test
  void preservesMissingObjectiveAndClosedCycleWithoutNavigationId() throws Exception {
    proofs.removeLast();
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setEndDate(null);
    var cycle = new LearningSalesCycle();
    cycle.setId(97804L);
    cycle.setProductId(product.getId());
    cycle.setStatus("ADJUSTED");
    when(cycles.findByExperimentId(experiment.getId())).thenReturn(Optional.of(cycle));
    http.perform(
            get("/api/business-processes/97803/products/97801/activity-executions")
                .param("sourceReference", "experiment:97802")
                .param("includePromptAudit", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedActivityCount").value(7))
        .andExpect(jsonPath("$.remainingActivityCount").value(1))
        .andExpect(jsonPath("$.objectiveAchieved").value(false))
        .andExpect(
            jsonPath(
                "$.currentActivityStateReason", org.hamcrest.Matchers.containsString("encerrado")));
    verifyNoInteractions(finances, checks, agentTasks);
  }

  /** Encerramento não inventa comprovação financeira quando a ocorrência não foi concluída. */
  @Test
  void doesNotApproveUnprovenEconomicsInHistoricalReference() throws Exception {
    proofs.get(4).setObjectiveAchieved(false);
    http.perform(
            get("/api/business-processes/97803/products/97801/activity-executions")
                .param("sourceReference", "experiment:97802")
                .param("includePromptAudit", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedActivityCount").value(7))
        .andExpect(jsonPath("$.objectiveAchieved").value(false));
    verify(context, never()).snapshot(anyString());
    verifyNoInteractions(finances, checks, agentTasks);
  }

  /** Candidata aberta continua renovando uma prova cuja fonte realmente mudou. */
  @Test
  void keepsLiveFingerprintValidationAndForeignIdentityGate() {
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setEndDate(null);
    var changed =
        json.createObjectNode()
            .put("productId", product.getId())
            .put("experimentId", experiment.getId())
            .put("productVersion", "new-version")
            .put("fingerprint", "changed");
    doReturn(changed).when(context).snapshot("experiment:97802");
    assertThat(
            readiness.requiresFreshExecution(
                process, steps.getFirst(), product, "experiment:97802"))
        .isTrue();
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    var foreign =
        Product.builder()
            .id(97899L)
            .productTypeDefinition(product.getProductTypeDefinition())
            .build();
    assertThat(
            readiness.requiresFreshExecution(
                process, steps.getFirst(), foreign, "experiment:97802"))
        .isTrue();
    assertThat(readiness.readiness(process, steps.getFirst(), foreign, "experiment:97802").ready())
        .isFalse();
    verifyNoInteractions(finances, agentTasks);
  }
}
