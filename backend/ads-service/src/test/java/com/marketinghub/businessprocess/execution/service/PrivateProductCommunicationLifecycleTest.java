package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentTheme;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.automation.v1.service.*;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.communication.v1.*;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: comprovar o comando de comunicação privada, fila e callbacks com H2 e executor
 * simulado.
 */
@DataJpaTest(showSql = false)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.businessprocess.execution.service.PrivateProductCommunicationLifecycleTest-${random.uuid};DB_CLOSE_DELAY=0"
    })
class PrivateProductCommunicationLifecycleTest {
  @Autowired private TestEntityManager em;
  @Autowired private ProcessRunRepository runs;
  @Autowired private ProcessRunEventRepository events;
  @Autowired private org.springframework.transaction.PlatformTransactionManager transactions;
  @Autowired private AgentTaskRepository tasks;
  @Autowired private BusinessProcessActivityInstanceRepository instances;
  @Autowired private BusinessProcessDefinitionRepository processes;
  @Autowired private BusinessProcessActivityDefinitionRepository definitions;
  @Autowired private AgentRepository agents;
  @Autowired private AgentTaskActivityCoverageRepository coverage;
  @Autowired private BusinessProcessExecutionResourceRepository resources;
  private final ObjectMapper json =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  /**
   * Um clique cria trabalho, a fila o reserva, falhas preservam histórico e a retentativa pode
   * concluir.
   */
  @Test
  void followsPrivateProductCommunicationWithoutExperiment() throws Exception {
    Agent dedalo = agent();
    BusinessProcessDefinition process = process();
    Product product =
        Product.builder()
            .id(900004L)
            .name("Produto sintético")
            .internalName("Mira QA")
            .validationDefinitionVersion("PDE_AGENT_VALIDATED_V1")
            .automaticExecutionEnabled(true)
            .build();
    var products = mock(ProductRepository.class);
    var experiments = mock(ExperimentRepository.class);
    when(products.findById(product.getId())).thenReturn(Optional.of(product));
    when(products.existsById(product.getId())).thenReturn(true);
    var materialization = mock(CommunicationMaterializationContextProvider.class);
    when(materialization.resolve("product:900004@agent-validation-v1"))
        .thenReturn(
            Optional.of(
                java.util.Map.of(
                    "availability",
                    "AVAILABLE",
                    "inputReadiness",
                    "READY",
                    "mode",
                    "PRODUCT_PRIVATE",
                    "marketStrategicContract",
                    java.util.Map.of(
                        "availability",
                        "AVAILABLE",
                        "contractVersion",
                        "MARKET_STRATEGY_V3",
                        "contentHash",
                        "a".repeat(64)))));
    var targets = mock(AgentTaskTargetContextProvider.class);
    var taskService =
        new AgentTaskService(
            tasks,
            instances,
            agents,
            processes,
            definitions,
            resources,
            json,
            mock(OpenAiPricingService.class),
            MarketStrategicContextProvider.empty(),
            targets);
    var service =
        new BusinessProcessActivityExecutionService(
            processes,
            definitions,
            tasks,
            coverage,
            instances,
            mock(CommercialPlanRepository.class),
            null,
            products,
            experiments,
            taskService,
            json,
            List.of(),
            List.of(
                new IrisProductProcessActivityReadinessProvider(
                    MarketStrategicContextProvider.empty(), materialization)));
    ReflectionTestUtils.setField(
        taskService, "communicationMaterializationContextProvider", materialization);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new BusinessProcessActivityExecutionController(service),
                new InternalAgentTaskExecutionController(
                    taskService, mock(AgentTaskVisualEvidenceService.class)))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .build();
    AgentTask original = originalFailure(dedalo, process);
    String base = "/api/business-processes/" + process.getId() + "/products/" + product.getId();
    String command = base + "/activities/communicationContract/execution-requests";
    String worker = "/api/internal/agent-tasks/communication-director/stage-executions";
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var chain = new BusinessProcessChainDefinition();
    chain.setId(900014L);
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    chain.getItems().add(item);
    when(chains.findById(chain.getId())).thenReturn(Optional.of(chain));
    when(products.findLockedById(product.getId())).thenReturn(Optional.of(product));
    var runContext =
        new ProcessRunContext(
            service, processes, chains, mock(LearningSalesCycleRepository.class), products, json);
    var runService =
        new ProcessRunService(
            runs,
            events,
            products,
            runContext,
            new ProcessRunNavigation(processes, chains, runs, json),
            new ProcessRunSubprocesses(definitions, instances, json),
            mock(ProcessRunGuidance.class),
            service,
            json,
            transactions);
    var scope = new ProcessRunCommand(chain.getId(), null, "product:900004@agent-validation-v1");
    var ready = runService.status(product.getId(), process.getId(), scope);
    assertThat(ready.canStart()).isTrue();
    var historyReady = service.productProcessExecutions(process.getId(), product.getId());
    var run = runService.start(product.getId(), process.getId(), scope);
    assertThat(runService.start(product.getId(), process.getId(), scope).id()).isEqualTo(run.id());
    runService.reconcile(run.id());
    long firstId =
        tasks
            .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
                process.getId(), scope.sourceReference())
            .stream()
            .filter(t -> "communicationContract".equals(t.getProcessActivityId()))
            .mapToLong(AgentTask::getId)
            .max()
            .orElseThrow();
    var waiting = runService.status(product.getId(), process.getId(), scope);
    assertThat(tasks.findById(firstId).orElseThrow().getStatus()).isEqualTo("PENDING");
    assertThat(tasks.findById(firstId).orElseThrow().getActivityInstance()).isNotNull();
    mvc.perform(post(command)).andExpect(status().isConflict());
    var pending =
        mvc.perform(
                get(worker + "/pending")
                    .param("processCode", process.getProcessCode())
                    .param("activityId", "communicationContract"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(json.readTree(pending.getContentAsString()).get(0).path("taskId").asLong())
        .isEqualTo(firstId);
    assertThat(tasks.findById(firstId).orElseThrow().getStatus()).isEqualTo("IN_PROGRESS");
    mvc.perform(
            get(worker + "/pending")
                .param("processCode", process.getProcessCode())
                .param("activityId", "communicationContract"))
        .andExpect(status().isOk());
    String audit =
        "\"executionAudit\":{\"modelCode\":\"local-double-v1\",\"executionMode\":\"DETERMINISTIC\",\"reasoningEffort\":\"NOT_APPLICABLE\",\"promptSent\":\"Contrato local de correção\",\"agentPromptPart\":\"Executor simulado\",\"activityPromptPart\":\"Corrigir causa registrada\"}";
    mvc.perform(
            post(worker + "/" + firstId + "/failure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"error\":\"Protótipo local ainda indisponível\",\"resultJson\":\"{\\\"decision\\\":\\\"BLOCKED\\\"}\",\"evidenceJson\":\"{\\\"synthetic\\\":true}\","
                        + audit
                        + "}"))
        .andExpect(status().isNoContent());
    assertThat(tasks.findById(firstId).orElseThrow().getStatus()).isEqualTo("BLOCKED");
    long secondId = create(mvc, command);
    assertThat(secondId).isNotEqualTo(firstId);
    mvc.perform(
            get(worker + "/pending")
                .param("processCode", process.getProcessCode())
                .param("activityId", "communicationContract"))
        .andExpect(status().isOk());
    mvc.perform(
            post(worker + "/" + secondId + "/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"resultJson\":\"{\\\"decision\\\":\\\"READY\\\",\\\"synthetic\\\":true}\",\"evidenceJson\":\"{\\\"synthetic\\\":true}\","
                        + audit
                        + "}"))
        .andExpect(status().isNoContent());
    verify(experiments, never()).findById(anyLong());
    em.flush();
    em.clear();
    var completed = tasks.findById(secondId).orElseThrow();
    assertThat(completed.getStatus()).isEqualTo("COMPLETED");
    assertThat(completed.getActivityInstance().isObjectiveAchieved()).isTrue();
    assertThat(completed.getSourceReference()).isEqualTo("product:900004@agent-validation-v1");
    assertThat(tasks.findById(firstId).orElseThrow().getStatus()).isEqualTo("BLOCKED");
    assertThat(tasks.findById(original.getId()).orElseThrow().getExecutionError())
        .isEqualTo("URL executável ausente");
    var completedRun = runService.reconcile(run.id());
    assertThat(completedRun.status()).isEqualTo("COMPLETED");
    assertThat(completedRun.sourceReference()).isEqualTo(scope.sourceReference());
    assertThat(completedRun.learningCycleId()).isNull();
    String output = System.getProperty("mira.lifecycle.output");
    if (output != null)
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output),
          json.writeValueAsString(
              java.util.Map.of(
                  "productId",
                  product.getId(),
                  "processId",
                  process.getId(),
                  "chainId",
                  chain.getId(),
                  "historyReady",
                  historyReady,
                  "automationReady",
                  ready,
                  "automationWaiting",
                  waiting,
                  "historyComplete",
                  service.productProcessExecutions(process.getId(), product.getId()),
                  "automationComplete",
                  completedRun)));
    var progress =
        mvc.perform(
                get(base + "/execution-progress")
                    .param("sourceReference", "product:900004@agent-validation-v1"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(progress)
        .contains("COMPLETED", "BLOCKED")
        .doesNotContain("prompt", "evidence", "resultJson");
  }

  /** Executa o POST oficial e devolve o identificador efetivamente persistido. */
  private long create(MockMvc mvc, String command) throws Exception {
    var response = mvc.perform(post(command)).andExpect(status().isOk()).andReturn().getResponse();
    return json.readTree(response.getContentAsString()).path("tasks").get(0).path("id").asLong();
  }

  /** Persiste o agente de teste sem configurar modelo ou integração externa. */
  private Agent agent() {
    var theme = new AgentTheme();
    theme.setName("Homologação local");
    em.persist(theme);
    var agent = new Agent();
    agent.setTheme(theme);
    agent.setName("Íris local");
    agent.setNickname("Íris");
    agent.setAgentKey("communication-director");
    agent.setStatus("ACTIVE");
    agent.setCurrentVersion(1);
    agent.setExecutionMode("WORKER");
    agent.setAutomaticExecutionEnabled(true);
    return em.persist(agent);
  }

  /** Persiste o contrato mínimo canônico de recuperação da homologação. */
  private BusinessProcessDefinition process() {
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-communication-sales-journey");
    process.setName("PDE local");
    process.setPurpose("Testar recuperação");
    process.setOwnerName("Íris");
    process.setTriggerDescription("Falha técnica");
    process.setOutcomeDescription("Correção auditada");
    process.setVersionNumber(8);
    process.setStatus("PUBLISHED");
    process.setProcessType("VALUE_PROCESS");
    process.setExecutionScope("PRODUCT");
    String correction =
        "{\"id\":\"communicationContract\",\"type\":\"TASK\",\"label\":\"Corrigir protótipo\",\"owner\":\"Íris\",\"responsibleAgentKeys\":[\"communication-director\"],\"actionLabel\":\"Criar tarefa de correção\",\"remediatesActivities\":[\"technicalHomologation\"]}";
    process.setDiagramJson("{\"nodes\":[" + correction + "],\"flows\":[]}");
    process.setCreatedAt(Instant.parse("2026-09-10T10:00:00Z"));
    em.persist(process);
    var definition = new BusinessProcessActivityDefinition();
    definition.setProcessDefinition(process);
    definition.setActivityId("communicationContract");
    definition.setName("Corrigir protótipo");
    definition.setOwnerName("Íris");
    definition.setDefinitionJson(correction);
    definition.setCreatedAt(process.getCreatedAt());
    em.persist(definition);
    return process;
  }

  /** Preserva a falha histórica que justifica a execução condicional de correção. */
  private AgentTask originalFailure(Agent agent, BusinessProcessDefinition process) {
    var task = new AgentTask();
    task.setAssignedAgent(agent);
    task.setRequestedByType("HUMAN");
    task.setRequestedByName("QA local");
    task.setTitle("Homologação original");
    task.setDescription("Teste isolado");
    task.setPriority("HIGH");
    task.setStatus("BLOCKED");
    task.setSourceReference("product:900004@agent-validation-v1");
    task.setProcessDefinition(process);
    task.setProcessActivityId("technicalHomologation");
    task.setTaskKind("WORK");
    task.setExecutionError("URL executável ausente");
    task.setBlockerCategory("TECHNICAL_FAILURE");
    task.setCreatedAt(Instant.parse("2026-09-10T10:01:00Z"));
    task.setUpdatedAt(task.getCreatedAt());
    return em.persist(task);
  }
}
