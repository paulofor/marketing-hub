package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a segregação e a prestação de contas dos agentes no plano. */
class CommercialPlanAgentActivityServiceTest {

  /** Consolida tarefa e ciclo financeiro exclusivamente pelo identificador do plano. */
  @Test
  void consolidatesAgentRecordsAndFinancialValuesByPlan() {
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    CommercialPlanVersionService versions = mock(CommercialPlanVersionService.class);
    CommercialPlanAgentActivityService service =
        new CommercialPlanAgentActivityService(tasks, cycles, landings, versions);
    Instant now = Instant.parse("2026-08-11T10:00:00Z");
    Agent plutus = Agent.builder().agentKey("financial-agent").nickname("Plutus").build();
    AgentTask gate = new AgentTask();
    gate.setId(8L);
    gate.setAssignedAgent(plutus);
    gate.setTitle("Aprovar orçamento");
    gate.setDescription("Analisar retorno do MUSA");
    gate.setStatus("PENDING");
    gate.setTaskKind("GATE_DECISION");
    gate.setGateCode("VIDEO_BUDGET_APPROVAL");
    gate.setGateStatus("PENDING");
    gate.setSourceReference("commercial-plan:7@v2");
    gate.setUpdatedAt(now);
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setId(3L);
    cycle.setCommercialPlanId(7L);
    cycle.setStatus("PENDING_FINANCIAL_REVIEW");
    cycle.setBudgetLimitUsd(new BigDecimal("40.00"));
    cycle.setKnownCostUsd(new BigDecimal("0.00"));
    cycle.setUpdatedAt(now);
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(7L)
            .maxBudget(new BigDecimal("300.00"))
            .actualAiCost(new BigDecimal("12.00"))
            .actualTotalCost(new BigDecimal("12.00"))
            .build();
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:7@v"))
        .thenReturn(List.of(gate));
    when(cycles.findByCommercialPlanIdOrderByUpdatedAtDesc(7L)).thenReturn(List.of(cycle));
    when(versions.current(7L))
        .thenReturn(new CommercialPlanVersionDto(2L, 7L, 2, "{}", "USER", "Atualização", now));

    CommercialPlanAgentActivityDto result = service.activity(plan);

    assertThat(result.currentVersion()).isEqualTo(2);
    assertThat(result.openTasks()).isEqualTo(1);
    assertThat(result.pendingDecisions()).isEqualTo(1);
    assertThat(result.videoBudgetLimitUsd()).isEqualByComparingTo("40.00");
    assertThat(result.entries())
        .extracting(CommercialPlanAgentActivityDto.Entry::agentNickname)
        .containsExactlyInAnyOrder("Plutus", "Plutus", "Apolo");
    assertThat(result.entries()).allMatch(entry -> entry.sourceReference() != null);
  }
}
