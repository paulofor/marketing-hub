package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequestResponse;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.videoBudget.VideoBudgetResponse;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeAgentValidationGateActivityExecutor;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: testar progressão, bloqueios e idempotência entre backend e executores
 * simulados.
 */
class LearningCycleVideoContinuationTest {
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final ProductRepository products = mock(ProductRepository.class);
  final BusinessProcessDefinitionRepository definitions =
      mock(BusinessProcessDefinitionRepository.class);
  final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  final BusinessProcessActivityDefinitionRepository activities =
      mock(BusinessProcessActivityDefinitionRepository.class);
  final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  final LearningCycleVideoBinding binding = mock(LearningCycleVideoBinding.class);
  final LearningCycleVideoBudget budget = mock(LearningCycleVideoBudget.class);
  final LearningCycleService service = mock(LearningCycleService.class);
  final LearningCycleEvidence evidence = mock(LearningCycleEvidence.class);
  final BusinessProcessActivityExecutionService dispatch =
      mock(BusinessProcessActivityExecutionService.class);
  final PdeAgentValidationGateActivityExecutor gate =
      mock(PdeAgentValidationGateActivityExecutor.class);
  final ObjectMapper json = new ObjectMapper();
  final LearningCycleVideoContinuation flow =
      new LearningCycleVideoContinuation(
          cycles,
          products,
          definitions,
          chains,
          activities,
          tasks,
          binding,
          budget,
          service,
          evidence,
          dispatch,
          gate,
          json);
  final ProcessRun run = new ProcessRun();
  final LearningSalesCycle cycle = new LearningSalesCycle();
  final Product product = new Product();
  final BusinessProcessDefinition parent = new BusinessProcessDefinition(),
      construction = new BusinessProcessDefinition();
  final BusinessProcessActivityDefinition gateActivity = new BusinessProcessActivityDefinition();
  final List<AgentTask> history = new ArrayList<>();
  final Instant integratedAt = Instant.parse("2026-09-14T22:50:00Z");

  /** Instala identidade isolada e callbacks simulados, sem chamadas pagas ou publicação. */
  @BeforeEach
  void setup() {
    run.setId(91000L);
    run.setProductId(91004L);
    run.setLearningCycleId(91002L);
    run.setChainDefinitionId(91014L);
    run.setProcessDefinitionId(91075L);
    run.setSourceReference("experiment:91092");
    run.setStatus("WAITING_ACTIVITY");
    cycle.setId(run.getLearningCycleId());
    cycle.setProductId(run.getProductId());
    cycle.setChainDefinitionId(run.getChainDefinitionId());
    cycle.setExperimentId(91092L);
    cycle.setProductVersion("local-v12");
    cycle.setStage("VIDEO_APPROVAL");
    cycle.setStatus("OPEN");
    product.setId(cycle.getProductId());
    product.setAutomaticExecutionEnabled(true);
    parent.setId(run.getProcessDefinitionId());
    parent.setProcessCode("pde-sales-delivery-learning");
    construction.setId(91057L);
    construction.setProcessCode("pde-construction-approval");
    construction.setVersionNumber(8);
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(construction);
    chain.setItems(new ArrayList<>(List.of(item)));
    when(definitions.findById(parent.getId())).thenReturn(Optional.of(parent));
    when(cycles.findLockedById(cycle.getId())).thenReturn(Optional.of(cycle));
    when(products.findById(product.getId())).thenReturn(Optional.of(product));
    when(chains.findById(cycle.getChainDefinitionId())).thenReturn(Optional.of(chain));
    when(binding.supports(cycle)).thenReturn(true);
    when(binding.current(cycle))
        .thenReturn(json.createObjectNode().put("integrationFingerprint", "a".repeat(64)));
    var receipt = new LearningSalesCycleEvent();
    receipt.setCreatedAt(integratedAt);
    when(binding.receipt(cycle)).thenReturn(Optional.of(receipt));
    when(binding.currentTask(eq(cycle), any()))
        .thenAnswer(
            c -> {
              AgentTask task = c.getArgument(1);
              return task != null && !task.getCreatedAt().isBefore(integratedAt);
            });
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            construction.getId(), run.getSourceReference()))
        .thenReturn(history);
    when(budget.current(cycle))
        .thenReturn(
            new VideoBudgetResponse.Authorization(
                1L,
                "internal://fixture/budget",
                new BigDecimal("20"),
                "Responsável",
                "Revisão autorizada",
                integratedAt,
                cycle.getProductVersion(),
                true));
    when(activities.findByProcessDefinitionIdAndActivityId(
            construction.getId(), "agentValidationGate"))
        .thenReturn(Optional.of(gateActivity));
    when(evidence.approvals(cycle))
        .thenReturn(List.of(new LearningCycleResponse.ApprovalOption(91999L, "Gate de teste")));
    doAnswer(
            c -> {
              cycle.setStage("VALIDATION");
              return null;
            })
        .when(service)
        .integrateApprovedVideos(product.getId(), cycle.getId());
    when(service.completeIntegratedVideoValidation(eq(product.getId()), eq(cycle.getId()), any()))
        .thenAnswer(
            c -> {
              cycle.setStage("AUTHORIZATION");
              return null;
            });
    when(dispatch.requestProductActivityExecution(
            eq(construction.getId()),
            eq(product.getId()),
            anyString(),
            isNull(),
            eq(cycle.getId())))
        .thenAnswer(
            c -> {
              String code = c.getArgument(2);
              AgentTask task = task(code, "PENDING", integratedAt.plusSeconds(1));
              history.add(task);
              var response = mock(AgentTaskResponse.class);
              when(response.id()).thenReturn(task.getId());
              return new ProductProcessActivityExecutionRequestResponse(
                  construction.getId(),
                  product.getId(),
                  code,
                  run.getSourceReference(),
                  List.of(response));
            });
  }

  /**
   * Monta uma tentativa auditável de um executor, conservando entrada, resultado e custo
   * desconhecido.
   */
  AgentTask task(String code, String status, Instant createdAt) {
    var task = new AgentTask();
    task.setId(92000L + history.size());
    task.setProcessActivityId(code);
    task.setProcessActivityName(code);
    task.setStatus(status);
    task.setCreatedAt(createdAt);
    task.setSourceReference(run.getSourceReference());
    task.setResultJson("{\"decision\":\"APPROVED\"}");
    return task;
  }

  /**
   * Percorre todo o fluxo, inclusive reinício do coordenador, sem repetir aprovações nem autorizar
   * campanha.
   */
  @Test
  void advancesFromApprovalThroughEachCallbackAndStopsAtCommercialAuthorization() {
    assertEquals("WAITING_ACTIVITY", flow.advance(run).status());
    verify(service).integrateApprovedVideos(product.getId(), cycle.getId());
    verifyNoInteractions(dispatch);
    for (String code : LearningCycleVideoBinding.REVIEWS) {
      assertEquals("WAITING_ACTIVITY", flow.advance(run).status());
      assertEquals(code, history.getLast().getProcessActivityId());
      assertEquals("WAITING_ACTIVITY", flow.advance(run).status());
      verify(dispatch, times(1))
          .requestProductActivityExecution(
              construction.getId(), product.getId(), code, null, cycle.getId());
      history.getLast().setStatus("COMPLETED");
    }
    assertNull(flow.advance(run));
    assertEquals("AUTHORIZATION", cycle.getStage());
    assertNull(flow.advance(run));
    assertEquals(5, history.size());
    assertTrue(history.stream().allMatch(t -> t.getEstimatedCostUsd() == null));
    verify(gate, times(1)).execute(construction, gateActivity, product, run.getSourceReference());
    verify(service, times(1))
        .completeIntegratedVideoValidation(
            eq(product.getId()),
            eq(cycle.getId()),
            argThat(c -> c.evidence().path("approvalInstanceId").asLong() == 91999L));
    verify(service, never()).command(anyLong(), anyLong(), any());
  }

  /** Preserva a decisão humana pendente sem aprovar ou integrar em seu lugar. */
  @Test
  void waitsForHumanVideoApproval() {
    when(binding.awaitingApproval(cycle)).thenReturn(true);
    assertNull(flow.advance(run));
    verifyNoInteractions(service, dispatch, gate);
  }

  /** Pausas e encerramento não autorizam novas tarefas por simples polling. */
  @ParameterizedTest
  @ValueSource(strings = {"PAUSING", "PAUSED", "CLOSED", "COMPLETED", "ERROR"})
  void respectsRunStops(String status) {
    run.setStatus(status);
    assertNull(flow.advance(run));
    verifyNoInteractions(service, dispatch, gate);
  }

  /** Mantém STOP do produto mesmo com os vídeos aprovados. */
  @Test
  void respectsProductStop() {
    product.setAutomaticExecutionEnabled(false);
    assertEquals("WAITING_INPUT", flow.advance(run).status());
    verifyNoInteractions(service, dispatch, gate);
  }

  /** Refuta tentativa de aproveitar outro produto, cadeia ou experimento no mesmo run. */
  @ParameterizedTest
  @ValueSource(strings = {"product", "chain", "experiment"})
  void refusesCrossContext(String field) {
    switch (field) {
      case "product" -> run.setProductId(1L);
      case "chain" -> run.setChainDefinitionId(1L);
      case "experiment" -> run.setSourceReference("experiment:1");
    }
    assertThrows(RuntimeException.class, () -> flow.advance(run));
    verifyNoInteractions(service, dispatch, gate);
  }

  /** Custo potencial exige a autorização da versão antes de criar a primeira tarefa. */
  @Test
  void refusesUnbudgetedReview() {
    cycle.setStage("VALIDATION");
    when(budget.current(cycle)).thenReturn(null);
    assertThrows(RuntimeException.class, () -> flow.advance(run));
    verifyNoInteractions(dispatch, gate);
  }

  /**
   * Reprovação ou falha atual interrompe o fluxo sem reiniciar consumo nem chamar o próximo agente.
   */
  @ParameterizedTest
  @ValueSource(strings = {"FAILED", "BLOCKED", "COMPLETED"})
  void refusesFailedOrRejectedCurrentTask(String status) {
    cycle.setStage("VALIDATION");
    var task = task("technicalHomologation", status, integratedAt);
    task.setResultJson("{\"decision\":\"BLOCKED\"}");
    history.add(task);
    assertThrows(RuntimeException.class, () -> flow.advance(run));
    assertThrows(RuntimeException.class, () -> flow.advance(run));
    verifyNoInteractions(dispatch, gate);
  }

  /**
   * Conclusões anteriores ao novo conjunto geram só uma ocorrência nova, sem apagar as anteriores.
   */
  @Test
  void preservesPriorEvidenceAndDispatchesOneFreshReview() {
    cycle.setStage("VALIDATION");
    history.add(task("technicalHomologation", "COMPLETED", integratedAt.minusSeconds(1)));
    flow.advance(run);
    flow.advance(run);
    assertEquals(2, history.size());
    assertEquals("COMPLETED", history.getFirst().getStatus());
    verify(dispatch, times(1))
        .requestProductActivityExecution(
            construction.getId(), product.getId(), "technicalHomologation", null, cycle.getId());
  }
}
