package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.service.*;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.safira.commercial.v1.service.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar histórico Safira pela API real sem reabrir revisão paga. */
class SafiraHistoricalPreparationTest {
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
  private final SafiraCommercialChecks checks = mock(SafiraCommercialChecks.class);
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final SafiraCommercialContext context =
      spy(
          new SafiraCommercialContext(
              experiments,
              cycles,
              mock(PdeProductionSlotRepository.class),
              mock(IntegratedPdeJourneyEvidenceService.class),
              mock(PdeCommercialCheckoutContractResolver.class),
              mock(CreativeRepository.class),
              mock(ExperimentTargetingSelectionService.class),
              finances,
              plans,
              json));
  private final SafiraCommercialService preparation =
      new SafiraCommercialService(
          context, checks, instances, definitions, processes, tasks, predecessors);
  private final SafiraCommercialAgentReadiness readiness =
      new SafiraCommercialAgentReadiness(context, preparation, instances);
  private final Product product =
      Product.builder()
          .id(97601L)
          .name("Produto sintético")
          .internalName("Safira de teste")
          .automaticExecutionEnabled(true)
          .productTypeDefinition(ProductTypeDefinition.builder().code("AI_PRODUCT").build())
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
    experiment.setId(97602L);
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    when(experiments.findById(experiment.getId())).thenReturn(Optional.of(experiment));
    when(experiments.existsByIdAndProductId(experiment.getId(), product.getId())).thenReturn(true);
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(product.getId()))
        .thenReturn(List.of(experiment));
    process.setId(97603L);
    process.setProcessCode(SafiraCommercialContext.CODE);
    process.setName("Preparar operação comercial Safira");
    process.setVersionNumber(2);
    process.setExecutionScope("PRODUCT");
    process.setStatus("PUBLISHED");
    var diagram = json.createObjectNode();
    var nodes = diagram.putArray("nodes");
    var flows = diagram.putArray("flows");
    String previous = null;
    for (String step :
        List.of(
            "journey",
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
          SafiraCommercialService.REVIEWS.contains(step)
              ? "{\"responsibleAgentKeys\":[\"review-agent\"]}"
              : "{\"responsibleAgentKeys\":[]}");
      steps.add(definition);
      nodes.addObject().put("id", step).put("type", "TASK").put("label", step);
      if (previous != null) flows.addObject().put("from", previous).put("to", step);
      previous = step;
      var proof = new BusinessProcessActivityInstance();
      proof.setId(97800L + proofs.size());
      proof.setActivityDefinition(definition);
      proof.setSourceReference("experiment:97602");
      proof.setOccurrenceNumber(1);
      proof.setStatus("COMPLETED");
      proof.setObjectiveAchieved(true);
      proof.setCreatedAt(Instant.parse("2026-09-01T00:00:00Z"));
      proof.setUpdatedAt(proof.getCreatedAt());
      proof.setKnownCostUsd(new BigDecimal("0.20"));
      proof.setCostCoverage("COMPLETE");
      proof.setObjectiveEvidenceJson("{\"fingerprint\":\"historical-evidence\"}");
      proofs.add(proof);
      when(definitions.findByProcessDefinitionIdAndActivityId(process.getId(), step))
          .thenReturn(Optional.of(definition));
      when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
              definition.getId(), "experiment:97602"))
          .thenAnswer(
              inv ->
                  proofs.stream().filter(p -> p.getActivityDefinition() == definition).findFirst());
    }
    process.setDiagramJson(diagram.toString());
    when(processes.findById(process.getId())).thenReturn(Optional.of(process));
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(process.getId())).thenReturn(steps);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                process.getProcessCode(), "experiment:97602"))
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
            List.of(readiness));
    http =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service))
            .build();
  }

  /**
   * As cinco conclusões reaparecem sem revalidar fontes vencidas ou executar comandos pelo HTTP.
   */
  @Test
  void preservesFiveHistoricalObjectivesAndRejectsNewTasks() throws Exception {
    String response =
        http.perform(
                get("/api/business-processes/97603/products/97601/activity-executions")
                    .param("sourceReference", "experiment:97602")
                    .param("includePromptAudit", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedActivityCount").value(5))
            .andExpect(jsonPath("$.objectiveAchieved").value(true))
            .andExpect(
                jsonPath(
                    "$.activities[*].executionControl.actionAvailable",
                    org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(false))))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    for (var step : steps) {
      assertThat(readiness.readiness(process, step, product, "experiment:97602").ready()).isFalse();
      http.perform(
              post(
                      "/api/business-processes/97603/products/97601/activities/{step}/execution-requests",
                      step.getActivityId())
                  .param("sourceReference", "experiment:97602")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isConflict());
    }
    verify(context, never()).snapshot(anyString());
    verifyNoInteractions(finances, checks, agentTasks);
    verify(instances, never()).saveAndFlush(any());
    String output = System.getProperty("safira-history.fixture-output");
    if (output != null) Files.writeString(Path.of(output), response);
  }

  /** Prova ausente continua pendente; ciclo encerrado não fabrica o quinto objetivo. */
  @Test
  void preservesMissingObjectiveAndClosedCycleWithoutNavigationId() throws Exception {
    proofs.removeLast();
    experiment.setStatus(ExperimentStatus.PLANNED);
    var cycle = new LearningSalesCycle();
    cycle.setId(97604L);
    cycle.setProductId(product.getId());
    cycle.setStatus("ADJUSTED");
    when(cycles.findByExperimentId(experiment.getId())).thenReturn(Optional.of(cycle));
    http.perform(
            get("/api/business-processes/97603/products/97601/activity-executions")
                .param("sourceReference", "experiment:97602")
                .param("includePromptAudit", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedActivityCount").value(4))
        .andExpect(jsonPath("$.remainingActivityCount").value(1))
        .andExpect(jsonPath("$.objectiveAchieved").value(false))
        .andExpect(
            jsonPath(
                "$.currentActivityStateReason", org.hamcrest.Matchers.containsString("encerrado")));
    verifyNoInteractions(finances, checks, agentTasks);
  }

  /** Candidata aberta continua renovando uma prova cuja fonte realmente mudou. */
  @Test
  void keepsLiveFingerprintValidationAndForeignIdentityGate() {
    experiment.setStatus(ExperimentStatus.PLANNED);
    var changed =
        json.createObjectNode()
            .put("productId", product.getId())
            .put("experimentId", experiment.getId())
            .put("productVersion", "new-version")
            .put("fingerprint", "changed");
    doReturn(changed).when(context).snapshot("experiment:97602");
    assertThat(
            readiness.requiresFreshExecution(
                process, steps.getFirst(), product, "experiment:97602"))
        .isTrue();
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    var foreign =
        Product.builder()
            .id(97699L)
            .productTypeDefinition(product.getProductTypeDefinition())
            .build();
    assertThat(
            readiness.requiresFreshExecution(
                process, steps.getFirst(), foreign, "experiment:97602"))
        .isTrue();
    assertThat(readiness.readiness(process, steps.getFirst(), foreign, "experiment:97602").ready())
        .isFalse();
    verifyNoInteractions(finances, agentTasks);
  }
}
