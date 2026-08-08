package com.marketinghub.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentTheme;
import com.marketinghub.agent.AgentVersion;
import com.marketinghub.agent.dto.SaveAgentRequest;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agent.AgentVersionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar a persistência e a auditoria do contrato operacional dos agentes. */
class AgentServiceTest {

  /** Confirma que as regras administrativas entram no agente e em sua versão imutável. */
  @Test
  void createsVersionedOperatingContract() {
    AgentRepository repository = mock(AgentRepository.class);
    AgentVersionRepository versionRepository = mock(AgentVersionRepository.class);
    AgentThemeService themeService = mock(AgentThemeService.class);
    AgentTheme theme = new AgentTheme();
    theme.setId(7L);
    when(themeService.get(7L)).thenReturn(theme);
    when(repository.save(any(Agent.class)))
        .thenAnswer(
            invocation -> {
              Agent saved = invocation.getArgument(0);
              saved.setId(11L);
              return saved;
            });

    SaveAgentRequest request = new SaveAgentRequest();
    request.setName("Especialista comercial");
    request.setAgentKey("commercial-specialist");
    request.setExecutionMode("DECISION_GATE");
    request.setThemeId(7L);
    request.setResponsibilityContract("Avaliar a viabilidade comercial.");
    request.setOrchestratorPolicy("Acionar após evidências mínimas; bloquear gasto.");
    request.setAnalysisPolicy("Comparar conversão, risco e evidências.");
    request.setOfferingPolicy("Entregar parecer e próximo teste.");

    AgentService service =
        new AgentService(repository, themeService, versionRepository, new ObjectMapper());
    Agent saved = service.create(request);

    assertThat(saved.getResponsibilityContract()).isEqualTo(request.getResponsibilityContract());
    assertThat(saved.getOrchestratorPolicy()).isEqualTo(request.getOrchestratorPolicy());
    assertThat(saved.getAnalysisPolicy()).isEqualTo(request.getAnalysisPolicy());
    assertThat(saved.getOfferingPolicy()).isEqualTo(request.getOfferingPolicy());

    ArgumentCaptor<AgentVersion> version = ArgumentCaptor.forClass(AgentVersion.class);
    verify(versionRepository).save(version.capture());
    assertThat(version.getValue().getContractSnapshot())
        .contains(
            "responsibilityContract", "orchestratorPolicy", "analysisPolicy", "offeringPolicy");
  }
}
