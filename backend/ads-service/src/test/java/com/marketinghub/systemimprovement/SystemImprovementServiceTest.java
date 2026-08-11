package com.marketinghub.systemimprovement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.systemimprovement.SystemImprovementRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar autoria, data e validação do cadastro de melhorias dos agentes. */
class SystemImprovementServiceTest {

  /** Persiste a sugestão com a identidade do agente e a data controlada pelo backend. */
  @Test
  void createsAuditableImprovementForRegisteredAgent() {
    SystemImprovementRepository repository = mock(SystemImprovementRepository.class);
    AgentRepository agentRepository = mock(AgentRepository.class);
    Agent agent = new Agent();
    agent.setId(7L);
    agent.setAgentKey("landing-generator");
    agent.setNickname("Dédalo");
    when(agentRepository.findByAgentKey("landing-generator")).thenReturn(Optional.of(agent));
    when(repository.save(any(SystemImprovement.class)))
        .thenAnswer(
            invocation -> {
              SystemImprovement saved = invocation.getArgument(0);
              saved.setId(19L);
              return saved;
            });
    Instant now = Instant.parse("2026-08-11T12:00:00Z");
    SystemImprovementService service =
        new SystemImprovementService(repository, agentRepository, Clock.fixed(now, ZoneOffset.UTC));

    var response =
        service.create(
            new CreateSystemImprovementRequest(
                "landing-generator",
                "Melhorar prova visual",
                "Criar comparação antes e depois legível no celular.",
                "experimento 88"));

    assertThat(response.id()).isEqualTo(19L);
    assertThat(response.agentNickname()).isEqualTo("Dédalo");
    assertThat(response.requestedAt()).isEqualTo(now);
    assertThat(response.status()).isEqualTo("SUGGESTED");
  }

  /** Impede que uma identidade inexistente atribua uma sugestão a um agente. */
  @Test
  void rejectsUnknownAgent() {
    AgentRepository agentRepository = mock(AgentRepository.class);
    when(agentRepository.findByAgentKey("unknown")).thenReturn(Optional.empty());
    SystemImprovementService service =
        new SystemImprovementService(
            mock(SystemImprovementRepository.class), agentRepository, Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.create(
                    new CreateSystemImprovementRequest("unknown", "Título", "Descrição", null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Agente solicitante não encontrado");
  }
}
