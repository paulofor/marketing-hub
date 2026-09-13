package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.controller.ProcessRunController;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.automation.v1.service.status.*;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequestResponse;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Responsabilidade: comprovar retorno criativo, diário e deduplicação com persistência real e
 * agentes simulados.
 */
@DataJpaTest(showSql = false)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.businessprocess.automation.v1.service.ProcessRunCreativeRecoveryPersistenceTest-${random.uuid};DB_CLOSE_DELAY=0"
    })
class ProcessRunCreativeRecoveryPersistenceTest {
  @Autowired private ProcessRunRepository runs;
  @Autowired private ProcessRunEventRepository events;
  @Autowired private PlatformTransactionManager transactions;
  private final ObjectMapper json =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final Map<String, ObjectNode> groups = new LinkedHashMap<>();
  private final List<String> requested = new ArrayList<>();
  private final String reference = "product:94110@agent-validation-v1";
  private ProcessRunService service;
  private MockMvc mvc;
  private long nextTask = 941410;
  private final String root = "/api/business-processes/94164/products/94110/automation/v1";

  /** Usa o controller real; somente o executor de atividades e o catálogo são substituídos. */
  @BeforeEach
  void setup() throws Exception {
    var product =
        Product.builder()
            .id(94110L)
            .internalName("Mira QA")
            .automaticExecutionEnabled(true)
            .build();
    var products = mock(ProductRepository.class);
    when(products.findById(94110L)).thenReturn(Optional.of(product));
    when(products.findLockedById(94110L)).thenReturn(Optional.of(product));
    var context = mock(ProcessRunContext.class);
    String diagram =
        """
        {"nodes":[{"id":"route","type":"TASK"},{"id":"nonAudiovisual","type":"TASK"},
          {"id":"audiovisual","type":"TASK"},{"id":"customer","type":"TASK"},
          {"id":"commercial","type":"TASK"},{"id":"human","type":"TASK"}],
          "flows":[{"from":"route","to":"nonAudiovisual"},{"from":"nonAudiovisual","to":"audiovisual"},
            {"from":"audiovisual","to":"customer"},{"from":"customer","to":"commercial"},
            {"from":"commercial","to":"human"}]}
        """;
    when(context.graph(94164L)).thenReturn(new ProcessExecutionGraph(json.readTree(diagram)));
    when(context.inputVersion(any())).thenReturn("QA_PRIVATE_V3");
    when(context.read(any(ProcessRun.class), anyBoolean())).thenAnswer(i -> snapshot());
    when(context.read(eq(94110L), eq(94164L), any(), anyBoolean())).thenAnswer(i -> snapshot());
    for (String id :
        List.of("route", "nonAudiovisual", "audiovisual", "customer", "commercial", "human")) {
      var group =
          json.createObjectNode()
              .put("activityDefinitionId", 941640 + groups.size())
              .put("activityId", id)
              .put("activityName", id)
              .put("activityObjective", "Objetivo de QA " + id)
              .put("activityOwnerName", "human".equals(id) ? "Operador humano" : "Agente local")
              .put("sequenceNumber", groups.size() + 1)
              .put("selectedVersionActivity", true)
              .put("operationalState", "NOT_STARTED")
              .put("stateReason", "Contrato local disponível")
              .put("executionRequestAvailable", true)
              .put("stateEvidence", "LOCAL_CONTRACT");
      group.putArray("tasks");
      group
          .putObject("executionControl")
          .put("executorType", "human".equals(id) ? "HUMAN" : "AGENT")
          .put("interactionType", "human".equals(id) ? "APPROVAL" : "COMMAND")
          .put("actionLabel", "human".equals(id) ? "Registrar decisão" : "Executar atividade")
          .put("actionAvailable", true)
          .put("confirmationRequired", "human".equals(id))
          .put("confirmationToken", "human".equals(id) ? "qa-explicit-human-only" : null)
          .put(
              "availabilityReason",
              "human".equals(id) ? "Selecione a peça revisada e registre sua decisão." : "Pronto")
          .putArray("requirements");
      groups.put(id, group);
    }
    state("route", "COMPLETED", true);
    state("audiovisual", "NOT_APPLICABLE", false);
    var activities = mock(BusinessProcessActivityExecutionService.class);
    when(activities.requestProductActivityExecution(
            eq(94164L), eq(94110L), anyString(), isNull(), isNull()))
        .thenAnswer(
            inv -> {
              String id = inv.getArgument(2);
              requested.add(id);
              state(id, "PENDING", false);
              task(id, "PENDING", null);
              return new ProductProcessActivityExecutionRequestResponse(
                  94164L,
                  94110L,
                  id,
                  reference,
                  List.of(),
                  "PENDING",
                  false,
                  "Tarefa local persistida pelo executor simulado");
            });
    var navigation = mock(ProcessRunNavigation.class);
    when(navigation.parents(any()))
        .thenReturn(
            List.of(
                new ProcessRunRelationResponse(
                    94163L,
                    "Comunicação local",
                    7,
                    "creatives",
                    "Produzir criativos",
                    "/products/94110/value-chain-history/processes/94163/activities?chainId=94114#activity-creatives")));
    service =
        new ProcessRunService(
            runs,
            events,
            products,
            context,
            navigation,
            mock(ProcessRunSubprocesses.class),
            mock(ProcessRunGuidance.class),
            activities,
            json,
            transactions);
    mvc =
        MockMvcBuilders.standaloneSetup(new ProcessRunController(service, "qa-process-only", ""))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .build();
  }

  /** ADJUST reabre produção uma vez e todas as revisões são refeitas antes da decisão humana. */
  @Test
  void returnsToProductionAndWaitsForExplicitHumanDecision() throws Exception {
    var run = start();
    tick(run.id());
    callback("nonAudiovisual", "COMPLETED", null);
    tick(run.id());
    callback(
        "customer",
        "BLOCKED",
        "{\"decision\":\"ADJUST\",\"requiredChanges\":[\"Identificar aplicativo e ampliar prova\"]}");
    state("nonAudiovisual", "NOT_STARTED", false);
    var blockedHistory = snapshot();
    var corrected = tick(run.id());
    assertThat(corrected.status()).isEqualTo("WAITING_ACTIVITY");
    assertThat(requested).containsExactly("nonAudiovisual", "customer", "nonAudiovisual");
    for (int i = 0; i < 3; i++) tick(run.id());
    assertThat(requested).hasSize(3);
    callback("nonAudiovisual", "COMPLETED", null);
    tick(run.id());
    callback("customer", "COMPLETED", "{\"decision\":\"APPROVED\",\"requiredChanges\":[]}");
    tick(run.id());
    callback("commercial", "COMPLETED", "{\"decision\":\"APPROVED\",\"requiredChanges\":[]}");
    var waiting = tick(run.id());
    assertThat(waiting.status()).isEqualTo("WAITING_HUMAN");
    assertThat(waiting.completedActivities()).isEqualTo(4);
    assertThat(waiting.omittedActivities()).isEqualTo(1);
    assertThat(waiting.remainingActivities()).isEqualTo(1);
    assertThat(requested).doesNotContain("human");
    var humanHistory = snapshot();
    state("human", "COMPLETED", true); // Decisão explícita da fixture humana, fora do executor.
    var completed = tick(run.id());
    assertThat(completed.status()).isEqualTo("COMPLETED");
    assertThat(completed.completedActivities()).isEqualTo(5);
    assertThat(completed.sourceReference()).isEqualTo(reference);
    assertThat(completed.learningCycleId()).isNull();
    assertThat(service.history(94110L, 94164L, run.id(), null))
        .anySatisfy(
            event -> {
              assertThat(event.eventType()).isEqualTo("RECOVERY_REQUESTED");
              assertThat(event.details().path("correctionInputHash").asText()).hasSize(64);
            });
    assertThat(groups.get("customer").path("tasks")).hasSize(2);
    assertThat(groups.get("customer").path("tasks").get(0).path("status").asText())
        .isEqualTo("BLOCKED");
    String output = System.getProperty("creative.lifecycle.output");
    if (output != null)
      Files.writeString(
          Path.of(output),
          json.writeValueAsString(
              Map.of(
                  "productId",
                  94110L,
                  "processId",
                  94164L,
                  "chainId",
                  94114L,
                  "historyCorrection",
                  blockedHistory,
                  "automationCorrection",
                  corrected,
                  "historyHuman",
                  humanHistory,
                  "automationHuman",
                  waiting,
                  "historyComplete",
                  snapshot(),
                  "automationComplete",
                  completed)));
  }

  /** Falha da nova produção não pode induzir outra tarefa paga durante polling repetido. */
  @Test
  void correctionFailureStopsWithoutRepeatingSameInput() throws Exception {
    var run = start();
    tick(run.id());
    callback("nonAudiovisual", "COMPLETED", null);
    tick(run.id());
    callback(
        "customer", "BLOCKED", "{\"decision\":\"ADJUST\",\"requiredChanges\":[\"Ampliar prova\"]}");
    state("nonAudiovisual", "NOT_STARTED", false);
    tick(run.id());
    callback("nonAudiovisual", "BLOCKED", "{\"decision\":\"BLOCKED\"}");
    for (int i = 0; i < 3; i++) assertThat(tick(run.id()).status()).isEqualTo("BLOCKED");
    assertThat(requested).containsExactly("nonAudiovisual", "customer", "nonAudiovisual");
    assertThat(service.history(94110L, 94164L, run.id(), null))
        .anyMatch(e -> "NO_PROGRESS".equals(e.eventType()));
  }

  /** Inicia pelo endpoint de processo e confirma a identidade persistida. */
  private ProcessRunResponse start() throws Exception {
    return json.readValue(
        mvc.perform(
                post(root)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(new ProcessRunCommand(94114L, null, reference))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
        ProcessRunResponse.class);
  }

  /** O worker simulado pede somente conciliação; o backend escolhe a atividade. */
  private ProcessRunResponse tick(long id) throws Exception {
    return json.readValue(
        mvc.perform(
                post("/api/internal/business-processes/automation/v1/stage-executions/"
                        + id
                        + "/reconcile")
                    .header("X-Process-Worker-Token", "qa-process-only"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
        ProcessRunResponse.class);
  }

  /** Preserva cada tentativa no contrato simulado, inclusive a reprovação anterior. */
  private void task(String id, String status, String result) {
    var group = groups.get(id);
    var tasks = (com.fasterxml.jackson.databind.node.ArrayNode) group.path("tasks");
    var created =
        tasks
            .addObject()
            .put("taskId", ++nextTask)
            .put("processDefinitionId", 94164L)
            .put("sourceReference", reference)
            .put("status", status)
            .put("comments", result)
            .put("title", "QA " + id)
            .put("assignedAgentNickname", id)
            .put("costEstimationStatus", "NO_EXECUTIONS");
    created.putArray("visualEvidence");
    created.putArray("accessedUrls");
    group
        .put("taskCount", tasks.size())
        .put("activityInstanceId", nextTask)
        .put("occurrenceNumber", tasks.size());
  }

  /** Aplica o retorno do executor sem decidir a próxima atividade do processo. */
  private void callback(String id, String status, String result) {
    var tasks = groups.get(id).path("tasks");
    ((ObjectNode) tasks.get(tasks.size() - 1)).put("status", status).put("comments", result);
    state(id, status, "COMPLETED".equals(status));
  }

  /**
   * Simula somente a prontidão do domínio, cuja implementação possui testes de contrato próprios.
   */
  private void state(String id, String state, boolean achieved) {
    groups
        .get(id)
        .put("operationalState", "PENDING".equals(state) ? "IN_PROGRESS" : state)
        .put("objectiveAchieved", achieved)
        .put("executionRequestAvailable", !achieved);
  }

  /** Expõe a projeção de QA com custos segregados, omissão explícita e todas as atividades. */
  private ProductProcessActivityExecutionHistoryResponse snapshot() {
    int completed =
        (int) groups.values().stream().filter(g -> g.path("objectiveAchieved").asBoolean()).count();
    var data =
        json.createObjectNode()
            .put("productId", 94110L)
            .put("productName", "Mira QA")
            .put("productInternalName", "Mira QA")
            .put("selectedProcessDefinitionId", 94164L)
            .put("processCode", "creative-production-approval")
            .put("processName", "Criativos locais")
            .put("selectedProcessVersionNumber", 8)
            .put("selectedProcessStatus", "PUBLISHED")
            .put("currentExecutionReference", reference)
            .put("operationalState", completed == 5 ? "COMPLETED" : "IN_PROGRESS")
            .put("objectiveAchieved", completed == 5)
            .put("selectedActivityCount", 6)
            .put("completedActivityCount", completed)
            .put("remainingActivityCount", 5 - completed)
            .put("activityCount", 6)
            .put("costCoverage", "NO_EXECUTIONS")
            .put("knownEstimatedCostUsd", 0);
    data.set("activities", json.valueToTree(groups.values()));
    return json.convertValue(data, ProductProcessActivityExecutionHistoryResponse.class);
  }
}
