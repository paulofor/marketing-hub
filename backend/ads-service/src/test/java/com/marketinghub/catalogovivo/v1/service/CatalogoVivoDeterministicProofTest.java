package com.marketinghub.catalogovivo.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jdbc.catalogovivo.CatalogoVivoRepository;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: limitar a execução sem modelo ao contrato explícito da prova criativa,
 * preservando as demais atividades.
 */
class CatalogoVivoDeterministicProofTest {
  /**
   * Identificador, atividade, agente e ocorrência precisam corresponder antes de aceitar o
   * callback.
   */
  @Test
  void permitsOnlyExplicitCreativeCaptureAndPreservesModelRequirementElsewhere() {
    var service =
        new CatalogoVivoService(
            mock(CatalogoVivoRepository.class),
            mock(BusinessProcessDefinitionRepository.class),
            mock(BusinessProcessActivityDefinitionRepository.class),
            mock(ExperimentRepository.class),
            new ObjectMapper());
    var process = new BusinessProcessDefinition();
    process.setProcessCode(CatalogoVivoService.PROCESS);
    var task = new AgentTask();
    task.setProcessDefinition(process);
    task.setProcessActivityId("creative");
    task.setSourceReference("experiment:106");
    task.setAssignedAgent(Agent.builder().agentKey("landing-generator").build());
    task.setExecutionModelCode("OPALA_COMMERCIAL_UI_PROOF_V1");
    task.setExecutionPrompt(
        "{\"contractVersion\":\"OPALA_COMMERCIAL_UI_PROOF_V1\",\"experimentId\":106}");
    assertThatCode(() -> service.requireModelAudit(task, "DETERMINISTIC", true))
        .doesNotThrowAnyException();
    task.setProcessActivityId("economics");
    assertThatThrownBy(() -> service.requireModelAudit(task, "DETERMINISTIC", true))
        .hasMessageContaining("auditoria do modelo");
    task.setProcessActivityId("creative");
    task.setSourceReference("experiment:922");
    assertThatThrownBy(() -> service.requireModelAudit(task, "DETERMINISTIC", true))
        .hasMessageContaining("auditoria do modelo");
    task.setSourceReference("experiment:106");
    task.setExecutionModelCode("arbitrary-renderer");
    assertThatThrownBy(() -> service.requireModelAudit(task, "DETERMINISTIC", true))
        .hasMessageContaining("auditoria do modelo");
    assertThatCode(() -> service.requireModelAudit(task, "MODEL", true)).doesNotThrowAnyException();
  }
}
