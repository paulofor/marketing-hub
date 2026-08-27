package com.marketinghub.agentdetail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentInput;
import com.marketinghub.agent.AgentInternalFunction;
import com.marketinghub.agent.AgentOutput;
import com.marketinghub.agent.AgentTheme;
import com.marketinghub.businessprocessresource.BusinessProcessExecutionResource;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agent.AgentVersionRepository;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a consolidação do contrato atual e dos recursos de um agente. */
class AgentDetailServiceTest {

  /** Consolida identidade, governança, prompts, itens, automação e recursos do agente correto. */
  @Test
  void getsCompleteAgentDetail() {
    AgentRepository agentRepository = mock(AgentRepository.class);
    AgentVersionRepository versionRepository = mock(AgentVersionRepository.class);
    BusinessProcessExecutionResourceRepository resourceRepository =
        mock(BusinessProcessExecutionResourceRepository.class);
    Agent agent = completeAgent();
    AgentVersionRepository.CurrentVersionChange versionChange =
        mock(AgentVersionRepository.CurrentVersionChange.class);
    when(versionChange.getAgentId()).thenReturn(5L);
    when(versionChange.getChangedAt()).thenReturn(Instant.parse("2026-08-27T11:00:00Z"));
    when(agentRepository.findDetailedById(5L)).thenReturn(Optional.of(agent));
    when(versionRepository.findCurrentVersionChanges(List.of(5L)))
        .thenReturn(List.of(versionChange));
    when(resourceRepository.findAllByResponsibleAgentKeyAndActiveTrueOrderByNameAsc("market-radar"))
        .thenReturn(List.of(resource()));

    var detail =
        new AgentDetailService(agentRepository, versionRepository, resourceRepository, catalog())
            .getDetail(5L);

    assertThat(detail.nickname()).isEqualTo("Argos");
    assertThat(detail.themeName()).isEqualTo("Pesquisa de mercado");
    assertThat(detail.automaticExecutionEnabled()).isTrue();
    assertThat(detail.promptContractPath()).isEqualTo("prompts/argos/v1/research.md");
    assertThat(detail.inputs()).extracting("name").containsExactly("Briefing comercial");
    assertThat(detail.outputs()).extracting("name").containsExactly("Relatório de demanda");
    assertThat(detail.internalFunctions()).extracting("name").containsExactly("Pesquisa web");
    assertThat(detail.executionResources())
        .extracting("resourceCode")
        .containsExactly("argos-market-radar");
    assertThat(detail.harness().status()).isEqualTo("COMPLETE");
    assertThat(detail.harness().contractVersion()).isEqualTo("agent-harness-v1");
    assertThat(detail.harness().sections()).extracting("code").contains("runtime", "orchestration");
    assertThat(detail.harness().artifacts())
        .extracting("path")
        .contains("product-discovery-worker/prompts/productdiscovery.v1/plan/system.md");
    assertThat(detail.lastContractChangeAt()).isEqualTo(Instant.parse("2026-08-27T11:00:00Z"));
    verify(resourceRepository)
        .findAllByResponsibleAgentKeyAndActiveTrueOrderByNameAsc("market-radar");
  }

  /** Responde como ausente quando o identificador não pertence a um agente cadastrado. */
  @Test
  void rejectsUnknownAgent() {
    AgentRepository agentRepository = mock(AgentRepository.class);
    when(agentRepository.findDetailedById(99L)).thenReturn(Optional.empty());
    AgentDetailService service =
        new AgentDetailService(
            agentRepository,
            mock(AgentVersionRepository.class),
            mock(BusinessProcessExecutionResourceRepository.class),
            catalog());

    assertThatThrownBy(() -> service.getDetail(99L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Agente não encontrado");
  }

  /** Monta um agente com todos os grupos relevantes para a leitura administrativa. */
  private Agent completeAgent() {
    AgentTheme theme = new AgentTheme();
    theme.setId(3L);
    theme.setName("Pesquisa de mercado");
    Agent agent = new Agent();
    agent.setId(5L);
    agent.setName("Radar de mercado");
    agent.setNickname("Argos");
    agent.setAgentKey("market-radar");
    agent.setStatus("ACTIVE");
    agent.setCurrentVersion(4);
    agent.setTheme(theme);
    agent.setOwnerName("Marketing Hub");
    agent.setDescription("Descobre sinais comerciais reais.");
    agent.setBusinessObjective("Priorizar oportunidades com demanda.");
    agent.setSuccessMetrics("Oportunidades comprovadas.");
    agent.setModelName("gpt-5.6-sol");
    agent.setExecutionMode("BATCH");
    agent.setAutomaticExecutionEnabled(true);
    agent.setAutomaticExecutionChangedAt(Instant.parse("2026-08-27T10:00:00Z"));
    agent.setAutomaticExecutionChangedBy("operador");
    agent.setTriggerPolicy("Executar no ciclo PDE.");
    agent.setResponsibilityContract("Comprovar dor e demanda.");
    agent.setOrchestratorPolicy("Bloquear sem evidência.");
    agent.setAnalysisPolicy("Comparar comportamento pago.");
    agent.setOfferingPolicy("Entregar relatório rastreável.");
    agent.setAuthorityPolicy("Pesquisa somente leitura.");
    agent.setPromptContractPath("prompts/argos/v1/research.md");
    agent.setSchemaContractPath("prompts/argos/v1/research-schema.json");
    agent.setCreatedAt(Instant.parse("2026-08-01T10:00:00Z"));
    agent.setUpdatedAt(Instant.parse("2026-08-27T10:00:00Z"));

    AgentInput input = new AgentInput();
    input.setId(1L);
    input.setName("Briefing comercial");
    input.setOrderIndex(0);
    AgentOutput output = new AgentOutput();
    output.setId(2L);
    output.setName("Relatório de demanda");
    output.setOrderIndex(0);
    AgentInternalFunction function = new AgentInternalFunction();
    function.setId(3L);
    function.setName("Pesquisa web");
    function.setOrderIndex(0);
    agent.setInputs(new ArrayList<>(List.of(input)));
    agent.setOutputs(new ArrayList<>(List.of(output)));
    agent.setInternalFunctions(new ArrayList<>(List.of(function)));
    return agent;
  }

  /** Monta um recurso especializado pertencente ao agente do cenário. */
  private BusinessProcessExecutionResource resource() {
    BusinessProcessExecutionResource resource = new BusinessProcessExecutionResource();
    resource.setId(8L);
    resource.setResourceCode("argos-market-radar");
    resource.setName("Radar comercial de Argos");
    resource.setDescription("Coleta sinais de mercado.");
    resource.setResourceType("CONTAINER");
    resource.setResponsibleAgentKey("market-radar");
    resource.setExecutorReference("market-radar-worker");
    resource.setUsageInstructions("Consumir o endpoint pending do backend.");
    resource.setActive(true);
    return resource;
  }

  /** Carrega o manifesto real usado pelo endpoint de detalhe. */
  private AgentHarnessCatalog catalog() {
    return new AgentHarnessCatalog(new ObjectMapper());
  }
}
