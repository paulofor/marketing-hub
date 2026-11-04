package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.controller.ProcessRunController;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jdbc.learningcycle.CyclePreparationCostRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Responsabilidade: homologar consulta e retomada HTTP com recibos e execução persistidos
 * localmente.
 */
@DataJpaTest(showSql = false)
@TestPropertySource(properties = {"spring.liquibase.enabled=false", "spring.jpa.show-sql=false"})
class ProcessRunFinancialEligibilityTest {
  @Autowired ProcessRunRepository runs;
  @Autowired ProcessRunEventRepository runEvents;
  @Autowired LearningSalesCycleEventRepository receipts;
  @Autowired PlatformTransactionManager transactions;
  @Autowired EntityManager entityManager;
  final ObjectMapper json =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  final ProcessRunContext context = mock(ProcessRunContext.class);
  final ProductRepository products = mock(ProductRepository.class);
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final CyclePreparationCostRepository costs = mock(CyclePreparationCostRepository.class);
  final BusinessProcessActivityExecutionService activities =
      mock(BusinessProcessActivityExecutionService.class);
  LearningCyclePreparationBudget budget;
  LearningSalesCycle cycle;
  ProcessRun run;
  ProcessRunService service;
  ProductProcessActivityExecutionHistoryResponse history;
  MockMvc http;

  /** Monta o motor real com guard e ledger reais, isolando tarefas, custos e contexto externos. */
  @BeforeEach
  void setup() throws Exception {
    budget = new LearningCyclePreparationBudget(receipts, new LearningCycleJson(json));
    cycle = new LearningSalesCycle();
    cycle.setId(20L);
    cycle.setProductId(7L);
    cycle.setChainDefinitionId(27L);
    cycle.setExperimentId(113L);
    cycle.setProductVersion("candidata-v4");
    cycle.setStatus("OPEN");
    cycle.setStage("PLANNING");
    cycle.setRevision(1);
    cycle.setVersionChangedAt(Instant.parse("2026-11-04T12:00:00Z"));
    when(cycles.findById(20L)).thenAnswer(inv -> Optional.of(cycle));
    when(products.findById(anyLong()))
        .thenAnswer(
            inv ->
                Optional.of(
                    Product.builder()
                        .id(inv.getArgument(0))
                        .automaticExecutionEnabled(true)
                        .build()));
    when(products.findLockedById(anyLong()))
        .thenAnswer(inv -> products.findById(inv.getArgument(0)));
    when(costs.summarize(anyLong()))
        .thenReturn(
            List.of(new CycleFinancialReview.CostSlice(113L, new BigDecimal("76.3465608"), 0)));
    var guard = new ProcessRunCorrectionBudget(cycles, budget, costs);
    when(context.financialPending(any(), any()))
        .thenAnswer(inv -> guard.inspect(inv.getArgument(0), inv.getArgument(1)));
    when(context.correctionBudgetBlockReason(any(), any()))
        .thenAnswer(inv -> guard.blockReason(inv.getArgument(0), inv.getArgument(1)));
    var command = new ProcessRunCommand(27L, 20L, "experiment:113");
    when(context.command(any())).thenReturn(command);
    run = new ProcessRun();
    run.setProductId(7L);
    run.setProcessDefinitionId(126L);
    run.setChainDefinitionId(27L);
    run.setLearningCycleId(20L);
    run.setSourceReference("experiment:113");
    run.setScopeKey(
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest("7|126|27|20|experiment:113".getBytes(StandardCharsets.UTF_8))));
    run.setStatus("PAUSED");
    run.setReason("Pausa financeira anterior preservada.");
    run.setCurrentActivityId("economics");
    run.setKnownCostUsd(new BigDecimal("2.155494"));
    run.setCreatedAt(Instant.now());
    run.setUpdatedAt(run.getCreatedAt());
    run.setLastReconciledAt(run.getCreatedAt());
    run = runs.saveAndFlush(run);
    history =
        json.readValue(
            """
        {"selectedActivityCount":3,"completedActivityCount":1,"remainingActivityCount":2,
         "currentActivityId":"economics","currentActivityName":"Limitar economia da homologação",
         "operationalState":"IN_PROGRESS","knownEstimatedCostUsd":2.155494,"costCoverage":"COMPLETE",
         "activities":[{"activityId":"economics","selectedVersionActivity":true,
         "activityOwnerName":"Plutus","sequenceNumber":2,"tasks":[],"executionRequestAvailable":true,
         "executionControl":{"executorType":"AGENT","interactionType":"COMMAND","actionAvailable":true}}]}
        """,
            ProductProcessActivityExecutionHistoryResponse.class);
    when(context.read(any(ProcessRun.class), eq(false))).thenAnswer(inv -> history);
    var navigation = mock(ProcessRunNavigation.class);
    service =
        new ProcessRunService(
            runs,
            runEvents,
            products,
            context,
            navigation,
            mock(ProcessRunSubprocesses.class),
            mock(ProcessRunGuidance.class),
            activities,
            json,
            transactions,
            mock(ProcessRunOwnerPortrait.class));
    http =
        MockMvcBuilders.standaloneSetup(new ProcessRunController(service, "local-only", ""))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .build();
  }

  /** Aprovação ausente nunca é descrita como falta de saldo nem permite POST de retomada. */
  @ParameterizedTest
  @ValueSource(longs = {7, 99107})
  void requiresSavedApprovalForTheExactContext(long productId) throws Exception {
    run.setProductId(productId);
    cycle.setProductId(productId);
    var result = readStatus();
    assertThat(result.canResume()).isFalse();
    assertThat(result.financialPending().code()).isEqualTo("APPROVAL_REQUIRED");
    assertThat(result.financialPending().reason()).contains("não comprova falta de saldo");
    assertThat(result.financialPending().actionUrl())
        .isEqualTo(
            "/financial/execution?productId="
                + productId
                + "&chainId=27&cycleId=20#ceiling-AGENTS");
    // Preparar um rascunho não executa o comando de aprovação.
    decision("150.00");
    assertThat(readStatus().canResume()).isFalse();
    http.perform(post(root() + "/" + run.getId() + "/resume")).andExpect(status().isConflict());
    assertPreserved();
  }

  /** Recibo gravado libera somente o comando; retorno GET mantém pausa e não cria tarefas. */
  @Test
  void savedApprovalEnablesResumeWithoutAutomaticallyResuming() throws Exception {
    var before = readStatus();
    approve("150.00");
    entityManager.flush();
    assertThat(receipts.count()).isEqualTo(1);
    http.perform(
            get(root())
                .param("chainId", "27")
                .param("learningCycleId", "20")
                .param("sourceReference", "experiment:113"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.canResume").value(true))
        .andExpect(jsonPath("$.financialPending").isEmpty());
    assertPreserved();
    String output = System.getProperty("plutus-resume.fixture-output");
    if (output != null) {
      var browserHistory =
          (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(history);
      browserHistory
          .put("productId", 7L)
          .put("productName", "Produto sintético")
          .put("productInternalName", "Capella · TEST")
          .put("selectedProcessDefinitionId", 126L)
          .put("selectedProcessVersionNumber", 13)
          .put("selectedProcessStatus", "PUBLISHED")
          .put("processName", "Estratégia e economia de homologação · TEST")
          .put("processCode", "pde-commercial-plan-offer")
          .put("currentExecutionReference", "experiment:113")
          .put("activityCount", 1)
          .put("activitiesWithTasksCount", 0)
          .put("uniqueTaskCount", 0);
      ((com.fasterxml.jackson.databind.node.ObjectNode) browserHistory.at("/activities/0"))
          .put("activityName", "Limitar economia da homologação")
          .put("operationalState", "NOT_STARTED")
          .put("stateReason", "Entrada anterior comprovada.")
          .put("activityDefinitionId", 1233L);
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output),
          json.writeValueAsString(
              Map.of("blocked", before, "eligible", readStatus(), "history", browserHistory)));
    }
    http.perform(post(root() + "/" + run.getId() + "/resume"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"));
    assertThat(runEvents.count()).isEqualTo(1);
    verifyNoInteractions(activities);
  }

  /** Autorização de outra identidade ou versão não substitui o recibo do alvo atual. */
  @ParameterizedTest
  @ValueSource(strings = {"cycle", "product", "version", "chain", "experiment", "changedAt"})
  void rejectsHistoricalOrMismatchedApproval(String changed) {
    approve("150.00");
    switch (changed) {
      case "cycle" -> {
        cycle.setId(21L);
        run.setLearningCycleId(21L);
        when(cycles.findById(21L)).thenReturn(Optional.of(cycle));
      }
      case "version" -> cycle.setProductVersion("candidata-v5");
      case "product" -> {
        cycle.setProductId(99107L);
        run.setProductId(99107L);
      }
      case "chain" -> {
        cycle.setChainDefinitionId(28L);
        run.setChainDefinitionId(28L);
      }
      case "experiment" -> {
        cycle.setExperimentId(114L);
        run.setSourceReference("experiment:114");
      }
      case "changedAt" -> cycle.setVersionChangedAt(Instant.now().plusSeconds(5));
    }
    var result = readStatus();
    assertThat(result.canResume()).isFalse();
    assertThat(result.financialPending().code())
        .isEqualTo("cycle".equals(changed) ? "APPROVAL_REQUIRED" : "APPROVAL_OUTDATED");
    assertPreserved();
  }

  /** Revalida custo e recibo concorrentes no POST, mesmo após GET elegível. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "cost",
        "version",
        "limit",
        "STOP",
        "closed",
        "activity",
        "retired",
        "missingControl",
        "unknownActivity"
      })
  void rechecksAllRequirementsBeforeTransition(String changed) throws Exception {
    approve("150.00");
    assertThat(readStatus().canResume()).isTrue();
    switch (changed) {
      case "cost" ->
          when(costs.summarize(7L))
              .thenReturn(
                  List.of(new CycleFinancialReview.CostSlice(113L, new BigDecimal("160"), 0)));
      case "version" -> cycle.setProductVersion("candidata-v5");
      case "limit" -> approve("80.00");
      case "STOP" ->
          when(products.findById(7L))
              .thenReturn(
                  Optional.of(Product.builder().id(7L).automaticExecutionEnabled(false).build()));
      case "closed" ->
          when(context.dispatchBlockReason(any())).thenReturn("O ciclo está encerrado.");
      case "retired" ->
          when(context.dispatchBlockReason(any())).thenReturn("A versão não está publicada.");
      case "missingControl" -> {
        var node = json.valueToTree(history);
        ((com.fasterxml.jackson.databind.node.ObjectNode) node.at("/activities/0"))
            .remove("executionControl");
        history = json.treeToValue(node, ProductProcessActivityExecutionHistoryResponse.class);
      }
      case "unknownActivity" -> {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(history);
        node.put("currentActivityId", "sem-contrato");
        history = json.treeToValue(node, ProductProcessActivityExecutionHistoryResponse.class);
      }
      case "activity" -> {
        var node = json.valueToTree(history);
        ((com.fasterxml.jackson.databind.node.ObjectNode) node.at("/activities/0/executionControl"))
            .put("actionAvailable", false)
            .put("availabilityReason", "Falta concluir a entrada da atividade.");
        history = json.treeToValue(node, ProductProcessActivityExecutionHistoryResponse.class);
      }
    }
    assertThat(readStatus().canResume()).isFalse();
    http.perform(post(root() + "/" + run.getId() + "/resume")).andExpect(status().isConflict());
    assertPreserved();
  }

  /** Recusa gravação concorrente sem recibo novo, liberando apenas após uma decisão válida. */
  @Test
  void failedApprovalCannotEnableResume() {
    var stale = decision("150.00");
    cycle.setRevision(cycle.getRevision() + 1);
    assertThatThrownBy(
            () -> budget.authorize(cycle, stale, new BigDecimal("76.3465608"), 0, Instant.now()))
        .hasMessageContaining("O ciclo mudou");
    assertThat(receipts.count()).isZero();
    assertThat(readStatus().canResume()).isFalse();
    assertPreserved();
  }

  /** Usa a mesma leitura do servidor sem comandar progressão ou reconstruir resultados. */
  private com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunResponse
      readStatus() {
    try {
      run.setScopeKey(
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(
                          (run.getProductId()
                                  + "|"
                                  + run.getProcessDefinitionId()
                                  + "|"
                                  + run.getChainDefinitionId()
                                  + "|"
                                  + run.getLearningCycleId()
                                  + "|"
                                  + run.getSourceReference())
                              .getBytes(StandardCharsets.UTF_8))));
    } catch (java.security.NoSuchAlgorithmException ex) {
      org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha na identidade local de teste", ex);
      throw new AssertionError(ex);
    }
    return service.status(
        run.getProductId(),
        run.getProcessDefinitionId(),
        new ProcessRunCommand(
            run.getChainDefinitionId(), run.getLearningCycleId(), run.getSourceReference()));
  }

  /** Persiste uma decisão somente no ledger local com a revisão e versão atualmente consultadas. */
  private void approve(String limit) {
    budget.authorize(cycle, decision(limit), new BigDecimal("76.3465608"), 0, Instant.now());
  }

  /** Monta o rascunho de decisão sem gravar nem confundir digitação com aprovação. */
  private PreparationBudgetRequest decision(String limit) {
    return new PreparationBudgetRequest(
        UUID.randomUUID(),
        cycle.getRevision(),
        cycle.getChainDefinitionId(),
        cycle.getExperimentId(),
        cycle.getProductVersion(),
        new BigDecimal(limit),
        new BigDecimal("20.00"),
        "Operador TEST",
        "Homologação isolada sem efeito comercial",
        true);
  }

  /** Mantém o destino HTTP isolado pelas identidades sintéticas da própria execução. */
  private String root() {
    return "/api/business-processes/"
        + run.getProcessDefinitionId()
        + "/products/"
        + run.getProductId()
        + "/automation/v1";
  }

  /** Prova preservação da pausa, custo original e ausência de tarefas ou decisões de retomada. */
  private void assertPreserved() {
    assertThat(run.getStatus()).isEqualTo("PAUSED");
    assertThat(run.getKnownCostUsd()).isEqualByComparingTo("2.155494");
    assertThat(run.getReason()).isEqualTo("Pausa financeira anterior preservada.");
    assertThat(runEvents.count()).isZero();
    verifyNoInteractions(activities);
  }
}
