package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleExecutionContext;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleWorkResolver;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeTechnicalHomologationReadinessProvider;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Comprova leitura e comando HTTP da tarefa #377 com service e gate reais, sem escrever em
 * produção.
 */
class PdeTechnicalHomologationActivityExecutionTest {
  /**
   * Preserva a tentativa, explica a pendência atual e recusa retentativa até haver implementação.
   */
  @Test
  void blocksRetryAndExportsTheSameTruthForTheScreen() throws Exception {
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
            List.of(new PdeTechnicalHomologationReadinessProvider(targets)));
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
    process.setDiagramJson("{\"nodes\":[" + node + "]}");
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
    historicalInstances.add(instance);
    when(processes.findById(70L)).thenReturn(Optional.of(process));
    when(products.findById(4L)).thenReturn(Optional.of(product));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(4L)).thenReturn(List.of(experiment));
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(70L)).thenReturn(selectedActivities);
    when(definitions.findByProcessDefinitionIdAndActivityId(70L, "technicalHomologation"))
        .thenReturn(Optional.of(activity));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:92"))
        .thenReturn(List.of(task));
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
    assertThat(next.responsible()).isEqualTo("Psique");
    assertThat(next.activityNumber()).isEqualTo(5);
    assertThat(next.reason()).contains("implementação");
    String output = System.getProperty("vega377.output");
    if (output != null) {
      Files.createDirectories(Path.of(output).getParent());
      Files.writeString(Path.of(output), response);
      Files.writeString(Path.of(output + ".next-work.json"), json.writeValueAsString(next));
    }
  }
}
