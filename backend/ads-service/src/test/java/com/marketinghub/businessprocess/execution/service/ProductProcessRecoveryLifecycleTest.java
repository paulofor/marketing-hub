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
import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleExecutionContext;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeAgentValidationReworkReadinessProvider;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
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
 * Responsabilidade: percorrer comando, fila e callbacks reais com persistência H2 e executor
 * simulado.
 */
@DataJpaTest(showSql = false)
@TestPropertySource(properties = {"spring.liquibase.enabled=false", "spring.jpa.show-sql=false"})
class ProductProcessRecoveryLifecycleTest {
  @Autowired private TestEntityManager em;
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
  void followsRealTaskLifecycleWithPersistenceAndPreservedFailure() throws Exception {
    Agent dedalo = agent();
    BusinessProcessDefinition process = process();
    Product product =
        Product.builder()
            .id(900004L)
            .name("Produto sintético")
            .internalName("Vega QA")
            .automaticExecutionEnabled(true)
            .build();
    Experiment experiment = new Experiment();
    experiment.setId(900092L);
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.PLANNED);
    var products = mock(ProductRepository.class);
    var experiments = mock(ExperimentRepository.class);
    when(products.findById(product.getId())).thenReturn(Optional.of(product));
    when(products.existsById(product.getId())).thenReturn(true);
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(product.getId()))
        .thenReturn(List.of(experiment));
    when(experiments.existsByIdAndProductId(experiment.getId(), product.getId())).thenReturn(true);
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
                new PdeAgentValidationReworkReadinessProvider(
                    new ProductProcessActivityPredecessorService(tasks, instances, json),
                    tasks,
                    json)));
    var cycle = mock(LearningCycleExecutionContext.class);
    when(cycle.source(eq(900002L), eq(product), eq(process), anyBoolean()))
        .thenReturn("experiment:900092");
    when(cycle.startedAt(900002L, product.getId()))
        .thenReturn(Instant.parse("2026-09-10T10:00:00Z"));
    ReflectionTestUtils.setField(service, "learningCycleContext", cycle);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new BusinessProcessActivityExecutionController(service),
                new InternalAgentTaskExecutionController(
                    taskService, mock(AgentTaskVisualEvidenceService.class)))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .build();
    AgentTask original = originalFailure(dedalo, process);
    String base = "/api/business-processes/" + process.getId() + "/products/" + product.getId();
    String command = base + "/activities/prototypeCorrection/execution-requests";
    String worker = "/api/internal/agent-tasks/landing-generator/stage-executions";
    long firstId = create(mvc, command);
    assertThat(tasks.findById(firstId).orElseThrow().getStatus()).isEqualTo("PENDING");
    assertThat(tasks.findById(firstId).orElseThrow().getActivityInstance()).isNotNull();
    mvc.perform(post(command).param("learningCycleId", "900002")).andExpect(status().isConflict());
    var pending =
        mvc.perform(
                get(worker + "/pending")
                    .param("processCode", process.getProcessCode())
                    .param("activityId", "prototypeCorrection"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(json.readTree(pending.getContentAsString()).get(0).path("taskId").asLong())
        .isEqualTo(firstId);
    assertThat(tasks.findById(firstId).orElseThrow().getStatus()).isEqualTo("IN_PROGRESS");
    mvc.perform(
            get(worker + "/pending")
                .param("processCode", process.getProcessCode())
                .param("activityId", "prototypeCorrection"))
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
                .param("activityId", "prototypeCorrection"))
        .andExpect(status().isOk());
    mvc.perform(
            post(worker + "/" + secondId + "/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"resultJson\":\"{\\\"decision\\\":\\\"READY\\\",\\\"synthetic\\\":true}\",\"evidenceJson\":\"{\\\"synthetic\\\":true}\","
                        + audit
                        + "}"))
        .andExpect(status().isNoContent());
    em.flush();
    em.clear();
    var completed = tasks.findById(secondId).orElseThrow();
    assertThat(completed.getStatus()).isEqualTo("COMPLETED");
    assertThat(completed.getActivityInstance().isObjectiveAchieved()).isTrue();
    assertThat(completed.getSourceReference()).isEqualTo("experiment:900092");
    assertThat(tasks.findById(firstId).orElseThrow().getStatus()).isEqualTo("BLOCKED");
    assertThat(tasks.findById(original.getId()).orElseThrow().getExecutionError())
        .isEqualTo("URL executável ausente");
    var progress =
        mvc.perform(get(base + "/execution-progress").param("sourceReference", "experiment:900092"))
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
    var response =
        mvc.perform(post(command).param("learningCycleId", "900002"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return json.readTree(response.getContentAsString()).path("tasks").get(0).path("id").asLong();
  }

  /** Persiste o agente de teste sem configurar modelo ou integração externa. */
  private Agent agent() {
    var theme = new AgentTheme();
    theme.setName("Homologação local");
    em.persist(theme);
    var agent = new Agent();
    agent.setTheme(theme);
    agent.setName("Dédalo local");
    agent.setNickname("Dédalo");
    agent.setAgentKey("landing-generator");
    agent.setStatus("ACTIVE");
    agent.setCurrentVersion(1);
    agent.setExecutionMode("WORKER");
    agent.setAutomaticExecutionEnabled(true);
    return em.persist(agent);
  }

  /** Persiste o contrato mínimo canônico de recuperação da homologação. */
  private BusinessProcessDefinition process() {
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-construction-approval");
    process.setName("PDE local");
    process.setPurpose("Testar recuperação");
    process.setOwnerName("Dédalo");
    process.setTriggerDescription("Falha técnica");
    process.setOutcomeDescription("Correção auditada");
    process.setVersionNumber(8);
    process.setStatus("PUBLISHED");
    process.setProcessType("VALUE_PROCESS");
    String correction =
        "{\"id\":\"prototypeCorrection\",\"type\":\"TASK\",\"label\":\"Corrigir protótipo\",\"owner\":\"Dédalo\",\"responsibleAgentKeys\":[\"landing-generator\"],\"activationMode\":\"ON_FUNCTIONAL_REJECTION\",\"actionLabel\":\"Criar tarefa de correção\",\"remediatesActivities\":[\"technicalHomologation\"]}";
    process.setDiagramJson("{\"nodes\":[" + correction + "],\"flows\":[]}");
    process.setCreatedAt(Instant.parse("2026-09-10T10:00:00Z"));
    em.persist(process);
    var definition = new BusinessProcessActivityDefinition();
    definition.setProcessDefinition(process);
    definition.setActivityId("prototypeCorrection");
    definition.setName("Corrigir protótipo");
    definition.setOwnerName("Dédalo");
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
    task.setSourceReference("experiment:900092");
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
