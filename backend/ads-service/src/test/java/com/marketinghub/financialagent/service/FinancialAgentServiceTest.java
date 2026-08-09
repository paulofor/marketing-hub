package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a conciliacao honesta das fontes financeiras do planejamento. */
class FinancialAgentServiceTest {
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
}
