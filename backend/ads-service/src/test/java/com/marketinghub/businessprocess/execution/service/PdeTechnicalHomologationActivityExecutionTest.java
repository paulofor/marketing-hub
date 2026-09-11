package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleExecutionContext;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleWorkResolver;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeAgentValidationReworkReadinessProvider;
import com.marketinghub.product.service.agentvalidation.PdeTechnicalHomologationReadinessProvider;
import com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot;
import com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Comprova leitura e comando HTTP da tarefa #377 com service e gate reais, sem escrever em
 * produção.
 */
class PdeTechnicalHomologationActivityExecutionTest {
  /**
   * Preserva o bloqueio, oferece a correção configurada e mantém a homologação sujeita aos
   * pré-requisitos; concentra tarefas na origem e remove a dependência de correção concluída.
   * Exporta estados auditáveis para a homologação visual com a mesma projeção leve da prontidão.
   */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void blocksRetryAndExportsTheSameTruthForTheScreen(boolean recoveryEnabled) throws Exception {
    var json =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var definitions = mock(BusinessProcessActivityDefinitionRepository.class);
    var tasks = mock(AgentTaskRepository.class);
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var products = mock(ProductRepository.class);
    var experiments = mock(ExperimentRepository.class);
    var agentTasks = mock(AgentTaskService.class);
    var targets = mock(AgentTaskTargetContextProvider.class);
    var cycle = mock(LearningCycleExecutionContext.class);
    var providers = new ArrayList<AgentProductProcessActivityReadinessProvider>();
    providers.add(new PdeTechnicalHomologationReadinessProvider(targets));
    if (recoveryEnabled)
      providers.add(
          new PdeAgentValidationReworkReadinessProvider(
              new ProductProcessActivityPredecessorService(tasks, instances, json), tasks, json));
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
            experiments,
            agentTasks,
            json,
            List.of(),
            providers);
    ReflectionTestUtils.setField(service, "learningCycleContext", cycle);

    var process = new BusinessProcessDefinition();
    process.setId(70L);
    process.setName("Protótipo, validação multiagente e aprovação do PDE");
    process.setVersionNumber(8);
    process.setStatus("PUBLISHED");
    process.setProcessCode("pde-construction-approval");
    String node =
        """
        {"id":"technicalHomologation","type":"TASK","label":"Homologar tecnicamente a versão real",
         "owner":"Psique","responsibleAgentKeys":["customer-agent"],"executionMode":"DETERMINISTIC",
         "controlDescription":"Psique executa testes automáticos com o harness, a estrutura de ferramentas, cenários e evidências. Esta etapa técnica não usa parecer de IA; os cenários de experiência vêm depois."}
        """;
    String correctionNode =
        """
        {"id":"prototypeCorrection","type":"TASK","label":"Corrigir o protótipo a partir do parecer",
         "owner":"Dédalo","responsibleAgentKeys":["landing-generator"],
         "activationMode":"ON_FUNCTIONAL_REJECTION","actionLabel":"Criar tarefa de correção",
         "remediatesActivities":["technicalHomologation"]}
        """;
    process.setDiagramJson(
        "{\"nodes\":[" + node + (recoveryEnabled ? "," + correctionNode : "") + "]}");
    var activity = new BusinessProcessActivityDefinition();
    activity.setId(705L);
    activity.setProcessDefinition(process);
    activity.setActivityId("technicalHomologation");
    activity.setName("Homologar tecnicamente a versão real");
    activity.setOwnerName("Psique");
    activity.setDefinitionJson(node);
    var product =
        Product.builder()
            .id(4L)
            .name("Método MUSA")
            .internalName("Vega")
            .slug("metodo-musa")
            .automaticExecutionEnabled(true)
            .validationDefinitionVersion("v1")
            .build();
    var experiment = new Experiment();
    experiment.setId(92L);
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.PLANNED);
    Instant created = Instant.parse("2026-09-10T13:45:53Z");
    var task = new AgentTask();
    task.setId(377L);
    task.setTitle("Homologar tecnicamente a versão real · Vega");
    task.setProcessDefinition(process);
    task.setProcessActivityId("technicalHomologation");
    task.setSourceReference("experiment:92");
    task.setStatus("BLOCKED");
    task.setExecutionError("A URL do PDE é inválida ou contém parâmetros não permitidos.");
    task.setExecutionMode("DETERMINISTIC");
    task.setExecutionModelCode("pde-agent-validation-harness-v1");
    task.setExecutionReasoningEffort("NOT_APPLICABLE");
    task.setAssignedAgent(
        Agent.builder().id(2L).agentKey("customer-agent").nickname("Psique").build());
    task.setCreatedAt(created);
    task.setUpdatedAt(created);
    var instance = new BusinessProcessActivityInstance();
    instance.setId(239L);
    instance.setActivityDefinition(activity);
    instance.setSourceReference("experiment:92");
    instance.setStatus("BLOCKED");
    instance.setBlockedReason(task.getExecutionError());
    instance.setOccurrenceNumber(1);
    instance.setCreatedAt(created);
    instance.setUpdatedAt(created);
    task.setActivityInstance(instance);
    task.setBlockerCategory("TECHNICAL_FAILURE");
    task.setBlockerAction(
        "Implemente a versão do ciclo com seus testes próprios e registre a aceitação privada.");
    var history = new ArrayList<AgentTask>(List.of(task));
    var selectedActivities = new ArrayList<BusinessProcessActivityDefinition>();
    var historicalInstances = new ArrayList<BusinessProcessActivityInstance>();
    int previousId = 701;
    for (String code : List.of("journey", "deliverables", "audiovisual", "access")) {
      var previous = new BusinessProcessActivityDefinition();
      previous.setId((long) previousId++);
      previous.setProcessDefinition(process);
      previous.setActivityId(code);
      previous.setName(code);
      previous.setOwnerName(code.equals("audiovisual") ? "Apolo" : "Dédalo");
      previous.setDefinitionJson("{}");
      selectedActivities.add(previous);
      var completed = new BusinessProcessActivityInstance();
      completed.setId(previous.getId());
      completed.setActivityDefinition(previous);
      completed.setSourceReference("experiment:92");
      completed.setStatus("COMPLETED");
      completed.setObjectiveAchieved(true);
      completed.setOccurrenceNumber(1);
      completed.setCreatedAt(created.minusSeconds(50));
      completed.setUpdatedAt(created.minusSeconds(20));
      historicalInstances.add(completed);
    }
    selectedActivities.add(activity);
    var correction = new BusinessProcessActivityDefinition();
    correction.setId(706L);
    correction.setProcessDefinition(process);
    correction.setActivityId("prototypeCorrection");
    correction.setName("Corrigir o protótipo a partir do parecer");
    correction.setOwnerName("Dédalo");
    correction.setDefinitionJson(correctionNode);
    if (recoveryEnabled) {
      selectedActivities.add(correction);
      when(definitions.findByProcessDefinitionIdAndActivityId(70L, "prototypeCorrection"))
          .thenReturn(Optional.of(correction));
    }
    historicalInstances.add(instance);
    when(processes.findById(70L)).thenReturn(Optional.of(process));
    when(products.findById(4L)).thenReturn(Optional.of(product));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(4L)).thenReturn(List.of(experiment));
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(70L)).thenReturn(selectedActivities);
    when(definitions.findByProcessDefinitionIdAndActivityId(70L, "technicalHomologation"))
        .thenReturn(Optional.of(activity));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:92")).thenReturn(history);
    when(tasks.findPdeValidationTaskSnapshots("experiment:92", "pde-construction-approval"))
        .thenAnswer(
            ignored ->
                history.stream()
                    .map(
                        value ->
                            new PdeValidationTaskSnapshot(
                                value.getId(),
                                value.getProcessDefinition().getId(),
                                value.getProcessActivityId(),
                                value.getStatus(),
                                value.getBlockerCategory(),
                                value.getBlockerAction(),
                                value.getResultJson(),
                                value.getExecutionError()))
                    .toList());
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "pde-construction-approval", "experiment:92"))
        .thenReturn(historicalInstances);
    when(cycle.source(eq(2L), eq(product), eq(process), anyBoolean())).thenReturn("experiment:92");
    when(cycle.startedAt(2L, 4L)).thenReturn(created.minusSeconds(100));
    when(targets.resolve("experiment:92", "pde-construction-approval"))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    "experiment:92",
                    92L,
                    4L,
                    "metodo-musa",
                    "Método MUSA",
                    "Vega",
                    "musa-pde-entry-v8-primeiro-ajuste-aplicavel",
                    null,
                    null,
                    null,
                    null,
                    null,
                    json.createObjectNode().put("status", "PLANNED"))));
    var mvc =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .build();
    var response =
        mvc.perform(
                get("/api/business-processes/70/products/4/activity-executions")
                    .param("learningCycleId", "2")
                    .param("chainId", "14"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    var screen = json.readTree(response);
    var group = screen.path("activities").get(4);
    assertThat(group.path("tasks").get(0).path("createdAt").asText())
        .isEqualTo("2026-09-10T13:45:53Z");
    assertThat(group.path("activityOwnerName").asText()).isEqualTo("Psique");
    assertThat(group.path("executionControl").path("description").asText())
        .contains("harness", "testes automáticos");
    assertThat(group.path("executionRequestAvailable").asBoolean()).isFalse();
    assertThat(group.path("stateReason").asText()).contains("URL executável", "implementação");
    assertThat(group.path("tasks").get(0).path("executionError").asText())
        .isEqualTo(task.getExecutionError());
    assertThat(screen.path("currentExecutionReference").asText()).isEqualTo("experiment:92");
    mvc.perform(
            post("/api/business-processes/70/products/4/activities/technicalHomologation/execution-requests")
                .param("learningCycleId", "2"))
        .andExpect(status().isConflict());
    verifyNoInteractions(agentTasks);
    verify(tasks, never()).save(any());
    verify(instances, never()).save(any());
    assertThat(task.getStatus()).isEqualTo("BLOCKED");
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setSequenceNumber(3);
    item.setProcessDefinition(process);
    chain.setItems(new ArrayList<>(List.of(item)));
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    var learningCycle = new LearningSalesCycle();
    learningCycle.setId(2L);
    learningCycle.setProductId(4L);
    learningCycle.setExperimentId(92L);
    learningCycle.setChainDefinitionId(14L);
    learningCycle.setStage("ADJUSTMENT");
    learningCycle.setStatus("OPEN");
    var next = new LearningCycleWorkResolver(chains, service).resolve(learningCycle);
    assertThat(next.responsible()).isEqualTo(recoveryEnabled ? "Dédalo" : "Psique");
    assertThat(next.activityNumber()).isEqualTo(recoveryEnabled ? 6 : 5);
    assertThat(next.reason()).contains(recoveryEnabled ? "#377" : "implementação");
    assertThat(group.path("recoveryAction").path("actionAvailable").asBoolean())
        .isEqualTo(recoveryEnabled);
    String output = System.getProperty("vega377.output");
    if (output != null && recoveryEnabled) output += ".recovery.json";
    if (output != null) {
      Files.createDirectories(Path.of(output).getParent());
      Files.writeString(Path.of(output), response);
      Files.writeString(Path.of(output + ".next-work.json"), json.writeValueAsString(next));
    }
    if (recoveryEnabled) {
      when(agentTasks.retryBlockedByHumanOrRefreshPending(
              any(CreateAgentTaskRequest.class), eq(true)))
          .thenAnswer(
              invocation -> {
                CreateAgentTaskRequest request = invocation.getArgument(0);
                assertThat(request.assignedAgentKey()).isEqualTo("landing-generator");
                assertThat(request.sourceReference()).isEqualTo("experiment:92");
                assertThat(request.processDefinitionId()).isEqualTo(70L);
                assertThat(request.processActivityId()).isEqualTo("prototypeCorrection");
                assertThat(request.description()).contains("#377", "URL");
                var createdTask = new AgentTask();
                createdTask.setId(900378L);
                createdTask.setProcessDefinition(process);
                createdTask.setProcessActivityId("prototypeCorrection");
                createdTask.setSourceReference(request.sourceReference());
                createdTask.setStatus("PENDING");
                createdTask.setTitle(request.title());
                createdTask.setDescription(request.description());
                createdTask.setCreatedAt(created.plusSeconds(100));
                createdTask.setUpdatedAt(created.plusSeconds(100));
                var dedalo = new Agent();
                dedalo.setId(7L);
                dedalo.setAgentKey("landing-generator");
                dedalo.setName("Dédalo");
                dedalo.setNickname("Dédalo");
                createdTask.setAssignedAgent(dedalo);
                history.add(createdTask);
                return json.readValue(
                    "{\"id\":900378,\"status\":\"PENDING\",\"sourceReference\":\"experiment:92\"}",
                    AgentTaskResponse.class);
              });
      var commandResult =
          mvc.perform(
                  post("/api/business-processes/70/products/4/activities/prototypeCorrection/execution-requests")
                      .param("learningCycleId", "2"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
      var afterResult =
          mvc.perform(
                  get("/api/business-processes/70/products/4/activity-executions")
                      .param("learningCycleId", "2")
                      .param("chainId", "14"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
      var after = json.readTree(afterResult);
      assertThat(after.path("currentActivityId").asText()).isEqualTo("prototypeCorrection");
      assertThat(
              after
                  .path("activities")
                  .get(4)
                  .path("recoveryAction")
                  .path("actionAvailable")
                  .asBoolean())
          .isFalse();
      assertThat(after.path("activities").get(5).path("tasks").get(0).path("taskId").asLong())
          .isEqualTo(900378L);
      mvc.perform(
              post("/api/business-processes/70/products/4/activities/prototypeCorrection/execution-requests")
                  .param("learningCycleId", "2"))
          .andExpect(status().isConflict());
      verify(agentTasks, times(1))
          .retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class), eq(true));
      assertThat(task.getStatus()).isEqualTo("BLOCKED");
      assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
      if (output != null) {
        Files.writeString(Path.of(output + ".command.json"), commandResult);
        Files.writeString(Path.of(output + ".after.json"), afterResult);
        Files.writeString(
            Path.of(output + ".after-next-work.json"),
            json.writeValueAsString(
                new LearningCycleWorkResolver(chains, service).resolve(learningCycle)));
      }
      var recoveryTask =
          history.stream()
              .filter(itemTask -> itemTask.getId().equals(900378L))
              .findFirst()
              .orElseThrow();
      for (String state : List.of("IN_PROGRESS", "BLOCKED", "COMPLETED")) {
        recoveryTask.setStatus(state);
        recoveryTask.setReceivedAt(created.plusSeconds(110));
        recoveryTask.setUpdatedAt(
            created.plusSeconds(
                "IN_PROGRESS".equals(state) ? 110 : "BLOCKED".equals(state) ? 120 : 130));
        recoveryTask.setDeliveredAt(
            "IN_PROGRESS".equals(state) ? null : recoveryTask.getUpdatedAt());
        recoveryTask.setExecutionError(
            "BLOCKED".equals(state) ? "A versão executável ainda não foi disponibilizada." : null);
        recoveryTask.setBlockerCategory("BLOCKED".equals(state) ? "FUNCTIONAL_ADJUSTMENT" : null);
        recoveryTask.setBlockerAction(
            "BLOCKED".equals(state)
                ? "Disponibilizar a versão corrigida antes de homologar."
                : null);
        if ("COMPLETED".equals(state)) {
          product.setValidationDefinitionJson(
              "{\"privatePrototypeAcceptance\":{\"prototypeVersion\":\"vega-local-v9\"}}");
          recoveryTask.setResultJson(
              """
              {"decision":"READY","correctionPlan":{"sourceTaskId":377,"previousPrototypeVersion":"vega-local-v8",
                "correctedPrototypeVersion":"vega-local-v9","nextActivityId":"technicalHomologation",
                "verification":{"technicalRevalidationRequired":true,"noExternalSideEffects":true}}}
              """);
        }
        var stateResponse =
            mvc.perform(
                    get("/api/business-processes/70/products/4/activity-executions")
                        .param("learningCycleId", "2")
                        .param("chainId", "14"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var recoveryView =
            json.readTree(stateResponse).path("activities").get(4).path("recoveryAction");
        if ("COMPLETED".equals(state)) {
          assertThat(recoveryView.isNull()).isTrue();
        } else {
          assertThat(recoveryView.path("activityId").asText()).isEqualTo("prototypeCorrection");
          assertThat(recoveryView.path("sequenceNumber").asInt()).isEqualTo(6);
          assertThat(recoveryView.path("latestTask").path("status").asText()).isEqualTo(state);
          assertThat(recoveryView.path("latestTask").path("taskId").asLong()).isEqualTo(900378L);
          assertThat(recoveryView.path("objectiveAchieved").asBoolean()).isFalse();
        }
        var originTask =
            json.readTree(stateResponse).path("activities").get(5).path("tasks").get(0);
        assertThat(originTask.path("taskId").asLong()).isEqualTo(900378L);
        assertThat(originTask.path("status").asText()).isEqualTo(state);
        assertThat(json.readTree(stateResponse).path("activities").get(4).path("tasks").toString())
            .doesNotContain("900378");
        if (output != null)
          Files.writeString(Path.of(output + "." + state + ".json"), stateResponse);
      }
    }
  }
}
