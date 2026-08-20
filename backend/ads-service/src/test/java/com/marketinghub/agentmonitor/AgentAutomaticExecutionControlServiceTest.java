package com.marketinghub.agentmonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agentmonitor.AgentAutomaticExecutionControlEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar persistência, idempotência e compatibilidade do controle PLAY/STOP. */
@ExtendWith(MockitoExtension.class)
class AgentAutomaticExecutionControlServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-20T04:30:00Z");
  @Mock private AgentRepository agents;
  @Mock private AgentAutomaticExecutionControlEventRepository events;

  /** Confirma que STOP altera a fonte atual e cria um evento auditável. */
  @Test
  void shouldStopAutomaticExecutionAndAuditChange() {
    Agent agent = agent(true);
    when(agents.findLockedById(8L)).thenReturn(Optional.of(agent));
    AgentAutomaticExecutionControlService service = service();

    AgentAutomaticExecutionControlResponse result = service.update(8L, false, "operador");

    assertThat(result.automaticExecutionStatus()).isEqualTo("STOP");
    assertThat(agent.getAutomaticExecutionEnabled()).isFalse();
    assertThat(agent.getAutomaticExecutionChangedAt()).isEqualTo(NOW);
    assertThat(agent.getAutomaticExecutionChangedBy()).isEqualTo("operador");
    ArgumentCaptor<AgentAutomaticExecutionControlEvent> event =
        ArgumentCaptor.forClass(AgentAutomaticExecutionControlEvent.class);
    verify(events).save(event.capture());
    assertThat(event.getValue().isAutomaticExecutionEnabled()).isFalse();
    assertThat(event.getValue().getChangedAt()).isEqualTo(NOW);
  }

  /** Evita eventos repetidos quando o operador solicita o estado que já está vigente. */
  @Test
  void shouldKeepSameStateIdempotent() {
    Agent agent = agent(false);
    when(agents.findLockedById(8L)).thenReturn(Optional.of(agent));

    AgentAutomaticExecutionControlResponse result = service().update(8L, false, "operador");

    assertThat(result.automaticExecutionStatus()).isEqualTo("STOP");
    verify(agents, never()).save(any());
    verify(events, never()).save(any());
  }

  /** Mantém agentes legados sem valor explícito em PLAY durante a transição da migração. */
  @Test
  void shouldTreatLegacyNullAsPlay() {
    Agent agent = agent(null);
    when(agents.findByAgentKey("videomaker")).thenReturn(Optional.of(agent));

    AgentAutomaticExecutionControlResponse result = service().current("videomaker");

    assertThat(result.automaticExecutionEnabled()).isTrue();
    assertThat(result.automaticExecutionStatus()).isEqualTo("PLAY");
  }

  /** Cria o serviço com relógio fixo para comparar a auditoria sem flutuação temporal. */
  private AgentAutomaticExecutionControlService service() {
    return new AgentAutomaticExecutionControlService(
        agents, events, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  /** Monta um agente mínimo com identidade técnica estável. */
  private Agent agent(Boolean enabled) {
    return Agent.builder().id(8L).agentKey("videomaker").automaticExecutionEnabled(enabled).build();
  }
}
