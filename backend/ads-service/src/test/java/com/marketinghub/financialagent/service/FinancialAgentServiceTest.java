package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.agenttask.FailAgentTaskRequest;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: proteger a conciliacao honesta das fontes financeiras do planejamento. */
class FinancialAgentServiceTest {
  /** Abre projeção vinculada à versão oficial e à mesa sem alterar valores realizados. */
  @Test
  void deveEnfileirarProjecaoDeReceitaNaMesaDePlutus() {
    FinancialAgentExecutionRepository repository = mock(FinancialAgentExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    CommercialPlanVersionService versionService = mock(CommercialPlanVersionService.class);
    AgentTaskService taskService = mock(AgentTaskService.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(9L);
    plan.setName("MUSA v7");
    plan.setActualRevenue(BigDecimal.ZERO);
    plan.setOfferPriceBrl(new BigDecimal("97.00"));
    plan.setVariableCostPerSaleBrl(new BigDecimal("12.00"));
    plan.setExpectedMonthlyTraffic(800);
    plan.setExpectedConversionRatePercent(new BigDecimal("1.50"));
    plan.setExpectedCacBrl(new BigDecimal("28.00"));
    plan.setExpectedRefundRatePercent(new BigDecimal("5.00"));
    plan.setFixedOperationalCostBrl(new BigDecimal("40.00"));
    when(planService.getPlan(9L)).thenReturn(plan);
    when(versionService.current(9L))
        .thenReturn(new CommercialPlanVersionDto(3L, 9L, 4, "{}", "USER", "contexto", null));
    when(taskService.createByHuman(any()))
        .thenReturn(
            new AgentTaskResponse(
                55L,
                4L,
                "financial-agent",
                "Plutus",
                "HUMAN",
                null,
                null,
                "Plano Comercial",
                "Projetar",
                "Cenários",
                "HIGH",
                "PENDING",
                "commercial-plan:9@v4:revenue-projection",
                "WORK",
                null,
                null,
                null,
                null,
                null,
                null));
    when(repository.save(any(FinancialAgentExecution.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    FinancialAgentService service =
        new FinancialAgentService(
            repository,
            planService,
            new ObjectMapper().registerModule(new JavaTimeModule()),
            mock(StudioCostLedgerService.class),
            versionService,
            taskService);

    FinancialAgentExecutionResponse response =
        service.startRevenueProjection(
            9L, new StartRevenueProjectionRequest("Decidir teto inicial"));

    assertThat(response.authorityMode()).isEqualTo("READ_ONLY_REVENUE_PROJECTION");
    assertThat(response.commercialPlanVersion()).isEqualTo(4);
    assertThat(response.agentTaskId()).isEqualTo(55L);
    assertThat(response.financialSnapshot()).contains("\"approvedRevenueBrl\":0");
    assertThat(response.financialSnapshot()).contains("\"offerPriceBrl\":97.00");
    assertThat(response.financialSnapshot()).contains("\"expectedMonthlyTraffic\":800");
  }

  /** Entrega ao MCP o mesmo snapshot imutavel associado a execucao reservada. */
  @Test
  void deveExporSnapshotCongeladoAoMcp() {
    FinancialAgentExecutionRepository repository = mock(FinancialAgentExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(2L);
    FinancialAgentExecution execution = new FinancialAgentExecution();
    execution.setId(8L);
    execution.setCommercialPlan(plan);
    execution.setStatus(FinancialAgentExecutionStatus.RUNNING);
    execution.setFinancialSnapshot("{\"approvedRevenueBrl\":0}");
    when(repository.findById(8L)).thenReturn(Optional.of(execution));
    FinancialAgentService service =
        new FinancialAgentService(
            repository,
            mock(CommercialPlanService.class),
            new ObjectMapper(),
            mock(StudioCostLedgerService.class));

    FinancialAgentExecutionResponse response = service.getExecution(8L);

    assertThat(response.id()).isEqualTo(8L);
    assertThat(response.financialSnapshot()).contains("approvedRevenueBrl");
  }

  /** Confirma que custos sao separados e lacunas nao viram zeros confirmados. */
  @Test
  void deveCongelarCustosReceitaECoberturaDasFontes() throws Exception {
    FinancialAgentExecutionRepository repository = mock(FinancialAgentExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(2L);
    plan.setName("Agenda Cheia");
    plan.setActualCampaignCost(new BigDecimal("60.00"));
    plan.setActualAiCost(new BigDecimal("4.00"));
    plan.setActualTotalCost(new BigDecimal("70.00"));
    plan.setActualRevenue(BigDecimal.ZERO);
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.save(any(FinancialAgentExecution.class)))
        .thenAnswer(
            invocation -> {
              FinancialAgentExecution execution = invocation.getArgument(0);
              execution.setId(1L);
              return execution;
            });
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    StudioCostLedgerService studioCostLedgerService = mock(StudioCostLedgerService.class);
    when(studioCostLedgerService.totalKnownCostUsd(2L)).thenReturn(BigDecimal.ZERO);
    when(studioCostLedgerService.coverage(2L))
        .thenReturn(
            Map.of(
                "status", "NO_ATTEMPTS_RECORDED",
                "knownCostAttempts", 0,
                "totalAttempts", 0,
                "unknownCostAttempts", 0,
                "imageAttempts", 0,
                "videoAttempts", 0));
    when(studioCostLedgerService.totalUnassignedCostUsd()).thenReturn(new BigDecimal("4.80"));
    when(studioCostLedgerService.unassignedCoverage())
        .thenReturn(
            Map.of(
                "status", "PARTIAL",
                "knownCostAttempts", 4,
                "totalAttempts", 12,
                "unknownCostAttempts", 8,
                "imageAttempts", 0,
                "videoAttempts", 12));
    when(studioCostLedgerService.providerEfficiency(2L))
        .thenReturn(
            List.of(
                Map.of(
                    "provider",
                    "RUNWAY",
                    "approvedAssets",
                    2,
                    "commercialApprovalRatePercent",
                    new BigDecimal("50.00"),
                    "knownCostPerApprovedAssetUsd",
                    new BigDecimal("1.20"),
                    "decisionCoverage",
                    "READY_FOR_COMPARISON")));
    FinancialAgentService service =
        new FinancialAgentService(repository, planService, objectMapper, studioCostLedgerService);

    FinancialAgentExecutionResponse response = service.start(2L);
    var snapshot = objectMapper.readTree(response.financialSnapshot());

    assertThat(response.status()).isEqualTo(FinancialAgentExecutionStatus.PENDING);
    assertThat(snapshot.get("campaignCostBrl").decimalValue()).isEqualByComparingTo("60.00");
    assertThat(snapshot.get("otherAttributedCostBrl").decimalValue()).isEqualByComparingTo("6.00");
    assertThat(snapshot.get("refundsBrl").isNull()).isTrue();
    assertThat(snapshot.at("/sourceCoverage/refunds").asText())
        .isEqualTo("NOT_YET_AVAILABLE_AS_SEPARATE_SOURCE");
    assertThat(snapshot.at("/studioCostCoverage/status").asText())
        .isEqualTo("NO_ATTEMPTS_RECORDED");
    assertThat(snapshot.get("studioUnassignedKnownCostUsd").decimalValue())
        .isEqualByComparingTo("4.80");
    assertThat(snapshot.at("/studioUnassignedCostCoverage/totalAttempts").asInt()).isEqualTo(12);
    assertThat(snapshot.get("studioCostInterpretation").asText())
        .contains("nao comprova custo real zero");
    assertThat(snapshot.at("/studioProviderEfficiency/0/provider").asText()).isEqualTo("RUNWAY");
    assertThat(snapshot.at("/studioProviderEfficiency/0/approvedAssets").asInt()).isEqualTo(2);
  }

  /** Conclui a tarefa correlacionada com resultado, evidência, prompt e tokens reais. */
  @Test
  void deveAuditarConclusaoNaTarefaDePlutus() throws Exception {
    FinancialAgentExecutionRepository repository = mock(FinancialAgentExecutionRepository.class);
    AgentTaskService taskService = mock(AgentTaskService.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(4L);
    FinancialAgentExecution execution = runningExecution(27L, 253L, plan);
    when(repository.findById(27L)).thenReturn(Optional.of(execution));
    when(repository.save(any(FinancialAgentExecution.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    FinancialAgentService service =
        new FinancialAgentService(
            repository,
            mock(CommercialPlanService.class),
            objectMapper,
            mock(StudioCostLedgerService.class),
            null,
            taskService);

    FinancialAgentExecutionResponse response =
        service.complete(
            27L,
            new CompleteFinancialAgentRequest(
                "{\"decision\":\"BLOCKED_BY_MISSING_SOURCE\"}",
                "Sem tráfego real para estimar CAC.",
                "{\"decision\":\"BLOCKED_BY_MISSING_SOURCE\"}",
                "gpt-5.6-sol",
                null,
                "Núcleo de Plutus.\n\nReconcilie as métricas.",
                "Núcleo de Plutus.",
                "Reconcilie as métricas.",
                "high",
                "default",
                "STANDARD",
                "Flex não anunciado pelo catálogo Codex OAuth.",
                100L,
                20L,
                30L));

    ArgumentCaptor<CompleteAgentTaskRequest> callback =
        ArgumentCaptor.forClass(CompleteAgentTaskRequest.class);
    verify(taskService)
        .completeClaimedProcessTask(eq("financial-agent"), eq(253L), callback.capture());
    assertThat(response.status()).isEqualTo(FinancialAgentExecutionStatus.COMPLETED);
    assertThat(callback.getValue().resultJson()).contains("BLOCKED_BY_MISSING_SOURCE");
    assertThat(
            objectMapper.readTree(callback.getValue().evidenceJson()).get("artifactType").asText())
        .isEqualTo("FINANCIAL_AGENT_EXECUTION");
    assertThat(callback.getValue().evidenceJson())
        .contains("financial_agent_execution:27:raw_model_response")
        .doesNotContain("\\\"decision\\\"");
    assertThat(callback.getValue().modelUsages()).hasSize(1);
    assertThat(callback.getValue().modelUsages().getFirst().serviceTier()).isEqualTo("STANDARD");
    assertThat(callback.getValue().modelUsages().getFirst().inputTokens()).isEqualTo(100L);
    assertThat(callback.getValue().executionAudit().promptSent())
        .isEqualTo("Núcleo de Plutus.\n\nReconcilie as métricas.");
    assertThat(callback.getValue().executionAudit().agentPromptPart())
        .isEqualTo("Núcleo de Plutus.");
    assertThat(callback.getValue().executionAudit().activityPromptPart())
        .isEqualTo("Reconcilie as métricas.");
  }

  /**
   * Bloqueia a tarefa correlacionada preservando causa, execução e ausência de efeitos externos.
   */
  @Test
  void deveAuditarFalhaNaTarefaDePlutus() throws Exception {
    FinancialAgentExecutionRepository repository = mock(FinancialAgentExecutionRepository.class);
    AgentTaskService taskService = mock(AgentTaskService.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(4L);
    FinancialAgentExecution execution = runningExecution(28L, 254L, plan);
    when(repository.findById(28L)).thenReturn(Optional.of(execution));
    when(repository.save(any(FinancialAgentExecution.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    FinancialAgentService service =
        new FinancialAgentService(
            repository,
            mock(CommercialPlanService.class),
            objectMapper,
            mock(StudioCostLedgerService.class),
            null,
            taskService);

    FinancialAgentExecutionResponse response =
        service.fail(28L, new FailFinancialAgentRequest("MCP indisponível"));

    ArgumentCaptor<FailAgentTaskRequest> callback =
        ArgumentCaptor.forClass(FailAgentTaskRequest.class);
    verify(taskService).failClaimedProcessTask(eq("financial-agent"), eq(254L), callback.capture());
    assertThat(response.status()).isEqualTo(FinancialAgentExecutionStatus.FAILED);
    assertThat(callback.getValue().error()).isEqualTo("MCP indisponível");
    assertThat(
            objectMapper
                .readTree(callback.getValue().evidenceJson())
                .get("externalSideEffects")
                .asBoolean())
        .isFalse();
    assertThat(callback.getValue().resultJson()).isNull();
    assertThat(callback.getValue().blockerGuidance().category()).isEqualTo("TECHNICAL_FAILURE");
    assertThat(callback.getValue().blockerGuidance().recommendedAction())
        .contains("MCP indisponível");
  }

  /** Cria uma execução financeira em processamento para os testes de callback. */
  private FinancialAgentExecution runningExecution(
      Long executionId, Long taskId, CommercialPlan plan) {
    FinancialAgentExecution execution = new FinancialAgentExecution();
    execution.setId(executionId);
    execution.setAgentTaskId(taskId);
    execution.setCommercialPlan(plan);
    execution.setCommercialPlanVersion(3);
    execution.setAuthorityMode("READ_ONLY_REVENUE_PROJECTION");
    execution.setFinancialSnapshot("{\"planId\":4}");
    execution.setStatus(FinancialAgentExecutionStatus.RUNNING);
    return execution;
  }
}
