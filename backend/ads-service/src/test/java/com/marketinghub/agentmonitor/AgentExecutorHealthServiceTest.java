package com.marketinghub.agentmonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a classificação central da prontidão dos executores dos agentes. */
class AgentExecutorHealthServiceTest {
  /** Aprova somente quando versão, backend e autenticação estão comprovados. */
  @Test
  void shouldReportReadyOnlyWithAllThreeSignals() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    Agent agent =
        Agent.builder().agentKey("landing-generator").currentVersion(2).nickname("Dédalo").build();
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(agent));
    when(checks.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AgentExecutorHealthService service =
        new AgentExecutorHealthService(
            agents, checks, Clock.fixed(Instant.parse("2026-08-12T12:00:00Z"), ZoneOffset.UTC));

    AgentExecutorHealthResponse result =
        service.report(
            new AgentExecutorHealthReportRequest(
                "landing-generator", 2, "abc123", true, true, "Executor pronto."));

    assertThat(result.status()).isEqualTo("READY");
    assertThat(result.versionCurrent()).isTrue();
    assertThat(result.backendAccessible()).isTrue();
    assertThat(result.codexAuthenticated()).isTrue();
  }

  /** Bloqueia uma imagem antiga mesmo quando rede e autenticação funcionam. */
  @Test
  void shouldBlockOutdatedExecutorVersion() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    Agent agent =
        Agent.builder().agentKey("videomaker").currentVersion(2).nickname("Apolo").build();
    when(agents.findByAgentKey("videomaker")).thenReturn(Optional.of(agent));
    when(checks.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AgentExecutorHealthService service = new AgentExecutorHealthService(agents, checks);

    AgentExecutorHealthResponse result =
        service.report(
            new AgentExecutorHealthReportRequest(
                "videomaker", 1, "old", true, true, "Imagem anterior."));

    assertThat(result.status()).isEqualTo("BLOCKED");
    assertThat(result.versionCurrent()).isFalse();
  }

  /** Invalida uma prova antiga para não manter um falso estado saudável. */
  @Test
  void shouldExpireStaleHealthProof() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    Agent agent =
        Agent.builder().agentKey("meta-ad-approver").currentVersion(1).nickname("Têmis").build();
    AgentExecutorHealthCheck old =
        new AgentExecutorHealthCheck(
            agent,
            1,
            "abc",
            true,
            true,
            "READY",
            "Executor pronto.",
            Instant.parse("2026-08-12T11:40:00Z"));
    when(checks.findTopByAgentAgentKeyOrderByCheckedAtDesc("meta-ad-approver"))
        .thenReturn(Optional.of(old));
    AgentExecutorHealthService service =
        new AgentExecutorHealthService(
            agents, checks, Clock.fixed(Instant.parse("2026-08-12T12:00:00Z"), ZoneOffset.UTC));

    AgentExecutorHealthResponse result = service.current(agent);

    assertThat(result.status()).isEqualTo("UNKNOWN");
    assertThat(result.detail()).contains("vencida");
  }
}
