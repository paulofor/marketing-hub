package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.service.predecessor.*;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeAgentValidationReworkReadinessProvider;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

/**
 * Comprova leitura e comando da revalidação sem reaproveitar conclusão histórica ou duplicar
 * trabalho ativo.
 */
class PdeRevalidationActivityExecutionTest {
  /** Exercita o serviço da tela junto ao gate real de retrabalho e preserva a prova anterior. */
  @ParameterizedTest
  @CsvSource({
    "7,COMPLETED,mira-private-v1,349,true",
    "7,COMPLETED,mira-private-v2,349,true",
    "8,COMPLETED,mira-private-v1,349,true",
    "8,COMPLETED,mira-private-v2,349,true",
    "8,COMPLETED,mira-private-v2,355,false",
    "8,PENDING,mira-private-v2,354,false",
    "8,IN_PROGRESS,mira-private-v2,354,false"
  })
  void reopensOnlyRetiredCompletion(
      int oldVersion, String state, String prototype, long oldTaskId, boolean expectedAvailable) {
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var definitions = mock(BusinessProcessActivityDefinitionRepository.class);
    var tasks = mock(AgentTaskRepository.class);
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var coverages = mock(AgentTaskActivityCoverageRepository.class);
    var plans = mock(CommercialPlanRepository.class);
    var products = mock(ProductRepository.class);
    var experiments = mock(ExperimentRepository.class);
    AgentTaskService agentTasks =
        mock(
            AgentTaskService.class,
            invocation ->
                invocation.getMethod().getName().startsWith("retryBlocked")
                    ? mock(AgentTaskResponse.class)
                    : RETURNS_DEFAULTS.answer(invocation));
    var predecessors = mock(ProductProcessActivityPredecessorService.class);
    when(predecessors.readiness(any(), any(), anyString()))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(true, "Predecessora concluída."));
    var gate =
        new PdeAgentValidationReworkReadinessProvider(predecessors, tasks, new ObjectMapper());
    var service =
        new BusinessProcessActivityExecutionService(
            processes,
            definitions,
            tasks,
            coverages,
            instances,
            plans,
            null,
            products,
            experiments,
            agentTasks,
            new ObjectMapper(),
            List.of(),
            List.of(gate));
    var process = process(70L, 8);
    var oldProcess = oldVersion == 8 ? process : process(69L, oldVersion);
    var technical = activity(705L, process, "technicalHomologation", "customer-agent");
    var correction = activity(706L, process, "prototypeCorrection", "landing-generator");
    var psique = activity(707L, process, "psiqueAdherent", "customer-agent");
    var oldActivity =
        oldVersion == 8
            ? technical
            : activity(695L, oldProcess, "technicalHomologation", "customer-agent");
    Product product =
        Product.builder()
            .id(10L)
            .internalName("Mira")
            .automaticExecutionEnabled(true)
            .validationDefinitionVersion("PDE_AGENT_VALIDATION_V1")
            .validationDefinitionJson(
                "{\"privatePrototypeAcceptance\":{\"prototypeVersion\":\"mira-private-v2\"}}")
            .build();
    AgentTask old = task(oldTaskId, oldProcess, "technicalHomologation", state);
    old.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"" + prototype + "\"}");
    var oldInstance = new BusinessProcessActivityInstance();
    oldInstance.setId(207L);
    oldInstance.setActivityDefinition(oldActivity);
    oldInstance.setSourceReference("product:10@agent-validation-v1");
    oldInstance.setStatus(state);
    oldInstance.setObjectiveAchieved("COMPLETED".equals(state));
    oldInstance.setOccurrenceNumber(1);
    oldInstance.setCreatedAt(old.getCreatedAt());
    oldInstance.setUpdatedAt(old.getUpdatedAt());
    old.setActivityInstance(oldInstance);
    AgentTask corrected = task(353L, process, "prototypeCorrection", "COMPLETED");
    corrected.setResultJson(
        """
      {"decision":"READY","correctionPlan":{"previousPrototypeVersion":"mira-private-v1",
       "correctedPrototypeVersion":"mira-private-v2","nextActivityId":"technicalHomologation",
       "verification":{"technicalRevalidationRequired":true,"noExternalSideEffects":true}}}
      """);
    AgentTask rejected = task(350L, process(69L, 7), "psiqueAdherent", "BLOCKED");
    rejected.setBlockerCategory("FUNCTIONAL_ADJUSTMENT");
    rejected.setResultJson("{\"decision\":\"BLOCKED\",\"prototypeVersion\":\"mira-private-v1\"}");
    List<AgentTask> history = List.of(old, rejected, corrected);
    when(processes.findById(70L)).thenReturn(Optional.of(process));
    when(products.findById(10L)).thenReturn(Optional.of(product));
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(70L))
        .thenReturn(List.of(technical, correction, psique));
    when(definitions.findByProcessDefinitionIdAndActivityId(70L, "technicalHomologation"))
        .thenReturn(Optional.of(technical));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("product:10@"))
        .thenReturn(history);
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("product:10@agent-validation-v1"))
        .thenReturn(history);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceStartingWithOrderByCreatedAtDescIdDesc(
                "pde-construction-approval", "product:10@"))
        .thenReturn(List.of(oldInstance));

    var screen = service.productProcessExecutions(70L, 10L);
    var group =
        screen.activities().stream()
            .filter(a -> a.activityId().equals("technicalHomologation"))
            .findFirst()
            .orElseThrow();
    assertThat(group.executionRequestAvailable()).isEqualTo(expectedAvailable);
    if (List.of("PENDING", "IN_PROGRESS").contains(state)) {
      assertThat(screen.currentActivityId()).isEqualTo("technicalHomologation");
    }
    if (expectedAvailable) {
      assertThat(group.objectiveAchieved()).isFalse();
      assertThat(screen.currentActivityId()).isEqualTo("technicalHomologation");
      assertThat(service.requestProductActivityExecution(70L, 10L, "technicalHomologation").tasks())
          .hasSize(1);
    } else {
      assertThatThrownBy(
              () -> service.requestProductActivityExecution(70L, 10L, "technicalHomologation"))
          .isInstanceOf(ResponseStatusException.class);
      verifyNoInteractions(agentTasks);
    }
    assertThat(old.getStatus()).isEqualTo(state);
    assertThat(oldInstance.isObjectiveAchieved()).isEqualTo("COMPLETED".equals(state));
  }

  /** Monta o processo publicado com os comandos canônicos usados pela tela. */
  private BusinessProcessDefinition process(long id, int version) {
    var process = new BusinessProcessDefinition();
    process.setId(id);
    process.setVersionNumber(version);
    process.setProcessCode("pde-construction-approval");
    process.setStatus("PUBLISHED");
    process.setName("Homologação PDE");
    process.setDiagramJson(
        """
      {"nodes":[{"id":"technicalHomologation","type":"TASK","label":"Homologar tecnicamente a versão real","responsibleAgentKeys":["customer-agent"]},
       {"id":"prototypeCorrection","type":"TASK","label":"Corrigir protótipo","responsibleAgentKeys":["landing-generator"]},
       {"id":"psiqueAdherent","type":"TASK","label":"Psique aderente","responsibleAgentKeys":["customer-agent"]}]}
      """);
    return process;
  }

  /** Associa a atividade ao processo e ao seu executor sem abreviar o contrato real. */
  private BusinessProcessActivityDefinition activity(
      long id, BusinessProcessDefinition process, String code, String agent) {
    var activity = new BusinessProcessActivityDefinition();
    activity.setId(id);
    activity.setProcessDefinition(process);
    activity.setActivityId(code);
    activity.setName(code);
    activity.setDefinitionJson("{\"responsibleAgentKeys\":[\"" + agent + "\"]}");
    return activity;
  }

  /** Cria uma tentativa auditável com ordem temporal determinística. */
  private AgentTask task(
      long id, BusinessProcessDefinition process, String activity, String status) {
    var task = new AgentTask();
    task.setId(id);
    task.setProcessDefinition(process);
    task.setProcessActivityId(activity);
    task.setProcessActivityName(activity);
    task.setSourceReference("product:10@agent-validation-v1");
    task.setStatus(status);
    task.setTitle(activity);
    task.setCreatedAt(Instant.parse("2026-09-08T00:00:00Z").plusSeconds(id));
    task.setUpdatedAt(task.getCreatedAt());
    task.setAssignedAgent(
        Agent.builder()
            .id(2L)
            .agentKey(
                "prototypeCorrection".equals(activity) ? "landing-generator" : "customer-agent")
            .nickname("Agente")
            .build());
    return task;
  }
}
