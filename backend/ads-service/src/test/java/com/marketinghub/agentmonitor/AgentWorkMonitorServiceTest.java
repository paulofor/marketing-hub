package com.marketinghub.agentmonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a leitura comum de paralelismo, bloqueios e decisões dos agentes. */
class AgentWorkMonitorServiceTest {
  /** Impede que bloqueio antigo de Argos prevaleça sobre a pesquisa mais recente concluída. */
  @Test
  void shouldIgnoreObsoleteBlockedTaskWhenLatestTaskIsCompleted() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    Agent argos =
        Agent.builder().id(5L).agentKey("market-radar").nickname("Argos").name("Radar").build();
    AgentTask completed = new AgentTask();
    completed.setId(20L);
    completed.setStatus("COMPLETED");
    AgentTask obsolete = new AgentTask();
    obsolete.setId(10L);
    obsolete.setStatus("BLOCKED");
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(argos));
    when(tasks.findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc("market-radar"))
        .thenReturn(List.of(completed, obsolete));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles).list().getFirst();

    assertThat(result.workStatus()).isEqualTo("IDLE");
    assertThat(result.currentWork()).isEqualTo("Sem trabalho ativo");
  }

  /** Comprova que Plutus decide enquanto Apolo aguarda o mesmo ciclo sem duplicar estado. */
  @Test
  void shouldExposeFinancialDecisionAndApolloWaitingFromSameCycle() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    Agent apolo =
        Agent.builder().id(8L).agentKey("videomaker").nickname("Apolo").name("Videomaker").build();
    Agent plutus =
        Agent.builder()
            .id(3L)
            .agentKey("financial-agent")
            .nickname("Plutus")
            .name("Financeiro")
            .build();
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setId(21L);
    cycle.setStatus("PENDING_FINANCIAL_REVIEW");
    cycle.setBudgetLimitUsd(new BigDecimal("40.00"));
    cycle.setKnownCostUsd(BigDecimal.ZERO);
    cycle.setUpdatedAt(Instant.parse("2026-08-11T04:00:00Z"));
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(apolo, plutus));
    when(cycles.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(cycle));
    AgentWorkMonitorService service = new AgentWorkMonitorService(agents, tasks, landings, cycles);

    List<AgentWorkMonitorResponse> result = service.list();

    assertThat(result)
        .extracting(AgentWorkMonitorResponse::workStatus)
        .containsExactly("WAITING", "DECISION_REQUIRED");
    assertThat(result.get(1).externalDecisionRequired()).isTrue();
    assertThat(result.get(1).sourceReference()).isEqualTo("video-production-cycle:21");
  }

  /** Impede que a falha canônica de Dédalo continue aparecendo como trabalho ativo. */
  @Test
  void shouldExposeDedaloFailureAsBlocked() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    Agent dedalo =
        Agent.builder()
            .id(7L)
            .agentKey("landing-generator")
            .nickname("Dédalo")
            .name("Landing")
            .build();
    GeraLandingStageExecution execution =
        GeraLandingStageExecution.builder()
            .experimentId(88L)
            .stageCode("landing-generation-agent-v1")
            .status("FALHA")
            .errorMessage("Timeout registrado")
            .completedAt(Instant.parse("2026-08-11T04:39:00Z"))
            .build();
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(dedalo));
    when(landings.findTopByStageCodeOrderByExecutionRequestedAtDesc("landing-generation-agent-v1"))
        .thenReturn(Optional.of(execution));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles).list().getFirst();

    assertThat(result.workStatus()).isEqualTo("BLOCKED");
    assertThat(result.difficulty()).isEqualTo("Timeout registrado");
  }
}
