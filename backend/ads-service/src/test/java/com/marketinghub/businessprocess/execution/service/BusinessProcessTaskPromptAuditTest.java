package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: garantir leitura HTTP integral da auditoria somente na tarefa e contexto
 * corretos.
 */
class BusinessProcessTaskPromptAuditTest {
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);

  /** Monta o serviço real com uma tarefa persistida extensa e fronteiras de banco isoladas. */
  private BusinessProcessActivityExecutionService service() {
    var service =
        new BusinessProcessActivityExecutionService(
            mock(BusinessProcessDefinitionRepository.class), tasks, new ObjectMapper());
    ReflectionTestUtils.setField(service, "productRepository", products);
    ReflectionTestUtils.setField(service, "experimentRepository", experiments);
    when(products.existsById(4L)).thenReturn(true);
    when(experiments.existsByIdAndProductId(92L, 4L)).thenReturn(true);
    when(experiments.existsByIdAndProductId(91L, 4L)).thenReturn(true);
    var process = new BusinessProcessDefinition();
    process.setId(70L);
    var task = new AgentTask();
    task.setId(396L);
    task.setProcessDefinition(process);
    task.setSourceReference("experiment:92");
    task.setExecutionPrompt("Aprendizado e contexto íntegros. ".repeat(40000));
    task.setExecutionAgentPrompt("Psique");
    task.setExecutionActivityPrompt("Cenário aderente");
    when(tasks.findById(396L)).thenReturn(Optional.of(task));
    return service;
  }

  /** A API conserva integralmente o conteúdo extenso e confirma a identidade auditada. */
  @Test
  void readsFullPromptOnlyFromTheSelectedTask() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service()))
            .build();
    var response =
        mvc.perform(
                get("/api/business-processes/70/products/4/tasks/396/prompt-audit")
                    .param("sourceReference", "experiment:92"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(396))
            .andExpect(jsonPath("$.sourceReference").value("experiment:92"))
            .andExpect(jsonPath("$.agentPromptPart").value("Psique"))
            .andReturn()
            .getResponse();
    assertThat(
            new ObjectMapper()
                .readTree(response.getContentAsByteArray())
                .path("promptSent")
                .asText())
        .isEqualTo("Aprendizado e contexto íntegros. ".repeat(40000));
    verify(tasks).findById(396L);
    verifyNoMoreInteractions(tasks);
  }

  /** Produto, processo, ciclo e tarefa diferentes nunca recebem o prompt de outra execução. */
  @Test
  void rejectsForeignOrMissingContext() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service()))
            .build();
    for (String path :
        new String[] {
          "/70/products/10/tasks/396", "/71/products/4/tasks/396", "/70/products/4/tasks/999"
        }) {
      mvc.perform(
              get("/api/business-processes" + path + "/prompt-audit")
                  .param("sourceReference", "experiment:92"))
          .andExpect(status().isNotFound());
    }
    mvc.perform(
            get("/api/business-processes/70/products/4/tasks/396/prompt-audit")
                .param("sourceReference", "experiment:91"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/business-processes/70/products/4/tasks/396/prompt-audit"))
        .andExpect(status().isBadRequest());
  }

  /** Clientes novos solicitam a projeção leve; clientes antigos conservam o contrato integral. */
  @Test
  void supportsLightListsWithoutBreakingPreviousClients() throws Exception {
    var service = mock(BusinessProcessActivityExecutionService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service))
            .build();
    mvc.perform(
            get("/api/business-processes/70/products/4/activity-executions")
                .param("learningCycleId", "2")
                .param("chainId", "14")
                .param("includePromptAudit", "false"))
        .andExpect(status().isOk());
    verify(service).productProcessExecutions(70L, 4L, 2L, 14L, false);
    mvc.perform(
            get("/api/business-processes/70/products/4/activity-executions")
                .param("learningCycleId", "2")
                .param("chainId", "14"))
        .andExpect(status().isOk());
    verify(service).productProcessExecutions(70L, 4L, 2L, 14L);
  }
}
