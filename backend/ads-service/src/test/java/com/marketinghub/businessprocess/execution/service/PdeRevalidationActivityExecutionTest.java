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
import com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot;
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
  /** Reabre somente Têmis e encerra o retorno condicional após um novo parecer independente. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {10L, 110L})
  void reopensIntegrityWithCompiledEvidenceAndPreservesCompletedReviews(long productId) {
    var json = new ObjectMapper();
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var definitions = mock(BusinessProcessActivityDefinitionRepository.class);
    var tasks = mock(AgentTaskRepository.class);
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var products = mock(ProductRepository.class);
    var predecessors = mock(ProductProcessActivityPredecessorService.class);
    var agentTasks = mock(AgentTaskService.class);
    when(predecessors.readiness(any(), any(), anyString()))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Provas preservadas."));
    var gate = new PdeAgentValidationReworkReadinessProvider(predecessors, tasks, json);
    org.springframework.test.util.ReflectionTestUtils.setField(
        gate,
        "operationalEvidence",
        new com.marketinghub.product.service.agentvalidation.PdeOperationalControlEvidence(
            json, new org.springframework.core.io.DefaultResourceLoader()));
    var service =
        new BusinessProcessActivityExecutionService(
            processes,
            definitions,
            tasks,
            mock(AgentTaskActivityCoverageRepository.class),
            instances,
            mock(CommercialPlanRepository.class),
            null,
            products,
            mock(ExperimentRepository.class),
            agentTasks,
            json,
            List.of(),
            List.of(gate));
    String reference = "product:" + productId + "@agent-validation-v1";
    String version = "mira-private-candidate-v3";
    var process = process(70L, 11);
    var product =
        Product.builder()
            .id(productId)
            .slug("pde-planejado-36")
            .automaticExecutionEnabled(true)
            .validationDefinitionVersion("PDE_AGENT_VALIDATION_V1")
            .validationDefinitionJson(
                "{\"privatePrototypeAcceptance\":{\"prototypeVersion\":\"" + version + "\"}}")
            .build();
    String[] codes = {
      "technicalHomologation",
      "psiqueAdherent",
      "psiqueRecovery",
      "psiqueSafety",
      "commercialIntegrityReview",
      "prototypeCorrection"
    };
    var activityList = new java.util.ArrayList<BusinessProcessActivityDefinition>();
    var history = new java.util.ArrayList<AgentTask>();
    var diagram = json.createObjectNode();
    var nodes = diagram.putArray("nodes");
    for (int index = 0; index < codes.length; index++) {
      String code = codes[index];
      String agent =
          code.equals("commercialIntegrityReview")
              ? "meta-ad-approver"
              : code.equals("prototypeCorrection") ? "landing-generator" : "customer-agent";
      var activity = activity(710L + index, process, code, agent);
      var node = nodes.addObject().put("id", code).put("type", "TASK");
      node.putArray("responsibleAgentKeys").add(agent);
      if (code.equals("prototypeCorrection")) {
        node.put("activationMode", "ON_FUNCTIONAL_REJECTION");
        node.put("responsibilityDomain", "PDE_FUNCTIONAL_REWORK");
        var targets = node.putArray("remediatesActivities");
        for (int review = 0; review < 5; review++) targets.add(codes[review]);
      }
      activity.setDefinitionJson(node.toString());
      activityList.add(activity);
      var task = task(627L + index, process, code, index < 4 ? "COMPLETED" : "BLOCKED");
      task.setSourceReference(reference);
      task.setDeliveredAt(index < 4 ? task.getCreatedAt() : null);
      task.setBlockerCategory(index < 4 ? null : "FUNCTIONAL_ADJUSTMENT");
      task.setResultJson(
          "{\"decision\":\""
              + (index < 4 ? "APPROVED" : "BLOCKED")
              + "\",\"prototypeVersion\":\""
              + version
              + "\"}");
      history.add(task);
    }
    var gateNode =
        nodes
            .addObject()
            .put("id", "agentValidationGate")
            .put("type", "TASK")
            .put("executionMode", "DETERMINISTIC")
            .put("responsibilityDomain", "PDE_AGENT_VALIDATION_GATE");
    var gateActivity = activity(716L, process, "agentValidationGate", "backend");
    gateActivity.setDefinitionJson(gateNode.toString());
    activityList.add(gateActivity);
    process.setDiagramJson(diagram.toString());
    when(processes.findById(70L)).thenReturn(Optional.of(process));
    when(products.findById(productId)).thenReturn(Optional.of(product));
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(70L)).thenReturn(activityList);
    when(definitions.findByProcessDefinitionIdAndActivityId(70L, "commercialIntegrityReview"))
        .thenReturn(Optional.of(activityList.get(4)));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "product:" + productId + "@"))
        .thenReturn(history);
    when(tasks
            .findBySourceReferenceStartingWithAndProcessDefinitionProcessCodeOrderByUpdatedAtDescIdDesc(
                "product:" + productId + "@", "pde-construction-approval"))
        .thenReturn(history);
    when(tasks.findById(631L)).thenReturn(Optional.of(history.get(4)));
    when(tasks.findPdeValidationTaskSnapshots(reference, "pde-construction-approval"))
        .thenAnswer(
            ignored ->
                history.stream()
                    .map(
                        task ->
                            new PdeValidationTaskSnapshot(
                                task.getId(),
                                process.getId(),
                                task.getProcessActivityId(),
                                task.getStatus(),
                                task.getBlockerCategory(),
                                task.getBlockerAction(),
                                task.getResultJson(),
                                task.getExecutionError()))
                    .toList());
    when(agentTasks.retryBlockedByHumanOrRefreshPending(any(), eq(true)))
        .thenReturn(mock(AgentTaskResponse.class));

    var before = service.productProcessExecutions(70L, productId);
    assertThat(before.currentActivityId()).isEqualTo("commercialIntegrityReview");
    assertThat(
            before.activities().stream()
                .filter(a -> a.executionRequestAvailable())
                .map(a -> a.activityId())
                .toList())
        .containsExactly("commercialIntegrityReview");
    assertThat(
            service
                .requestProductActivityExecution(70L, productId, "commercialIntegrityReview")
                .tasks())
        .hasSize(1);
    verify(agentTasks)
        .retryBlockedByHumanOrRefreshPending(
            argThat(request -> "commercialIntegrityReview".equals(request.processActivityId())),
            eq(true));

    var approved = task(633L, process, "commercialIntegrityReview", "COMPLETED");
    approved.setSourceReference(reference);
    approved.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"" + version + "\"}");
    history.add(approved);
    var after = service.productProcessExecutions(70L, productId);
    assertThat(after.objectiveAchieved()).isFalse();
    assertThat(after.currentActivityId()).isEqualTo("agentValidationGate");
    assertThat(
            after.activities().stream()
                .filter(
                    a ->
                        !"prototypeCorrection".equals(a.activityId())
                            && !"agentValidationGate".equals(a.activityId()))
                .allMatch(a -> a.objectiveAchieved()))
        .isTrue();
    assertThat(
            after.activities().stream()
                .filter(a -> "prototypeCorrection".equals(a.activityId()))
                .findFirst()
                .orElseThrow()
                .operationalState())
        .isEqualTo("RECORDED");
    assertThat(history.get(4).getStatus()).isEqualTo("BLOCKED");
    assertThat(history.get(5).getStatus()).isEqualTo("BLOCKED");
  }

  /** Exercita o serviço da tela junto ao gate real de retrabalho e preserva a prova anterior. */
  @ParameterizedTest
  @CsvSource({
    "7,COMPLETED,mira-private-v1,349,true",
    "7,COMPLETED,mira-private-v2,349,true",
    "8,COMPLETED,mira-private-v1,349,true",
    "8,COMPLETED,mira-private-v2,349,true",
    "8,COMPLETED,mira-private-v2,355,false",
    "8,BLOCKED,mira-private-v2,581,true",
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
    if (oldTaskId == 581L) {
      old.setBlockerCategory("TECHNICAL_FAILURE");
      old.setExecutionError(
          "com.marketinghub.customeragentworker.PdeAgentValidationHarnessRunner$HarnessException: "
              + "O harness instalado não possui cenários próprios para este produto. "
              + "Implemente-os antes da homologação; não reutilize outro PDE.");
    } else {
      old.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"" + prototype + "\"}");
    }
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
    when(tasks.findPdeValidationTaskSnapshots(
            "product:10@agent-validation-v1", "pde-construction-approval"))
        .thenReturn(
            history.stream()
                .map(
                    task ->
                        new PdeValidationTaskSnapshot(
                            task.getId(),
                            task.getProcessDefinition().getId(),
                            task.getProcessActivityId(),
                            task.getStatus(),
                            task.getBlockerCategory(),
                            task.getBlockerAction(),
                            task.getResultJson(),
                            task.getExecutionError()))
                .toList());
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
      if (oldTaskId == 581L) {
        verify(agentTasks)
            .retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class), eq(true));
        assertThat(
                screen.activities().stream()
                    .filter(a -> a.activityId().equals("prototypeCorrection"))
                    .findFirst()
                    .orElseThrow()
                    .executionRequestAvailable())
            .isFalse();
      }
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
