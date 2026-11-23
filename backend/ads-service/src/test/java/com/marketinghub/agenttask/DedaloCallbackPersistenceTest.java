package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.product.service.agentvalidation.PdeOperationalControlEvidence;
import com.marketinghub.product.valuedelivery.v1.service.*;
import com.marketinghub.repository.jpa.agent.*;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

/** Responsabilidade: integrar worker, HTTP, hook, transação e instância BPM sem modelo externo. */
@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.agenttask.DedaloCallbackPersistenceTest-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=0"
    })
class DedaloCallbackPersistenceTest {
  @Autowired private PlatformTransactionManager manager;
  @Autowired private TestEntityManager em;
  @Autowired private AgentTaskRepository tasks;
  @Autowired private AgentRepository agents;
  @Autowired private BusinessProcessDefinitionRepository processes;
  @Autowired private BusinessProcessActivityDefinitionRepository definitions;
  @Autowired private BusinessProcessActivityInstanceRepository instances;
  @Autowired private BusinessProcessExecutionResourceRepository resources;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final AtomicBoolean reject = new AtomicBoolean();
  private final AtomicInteger posts = new AtomicInteger();
  private final AtomicBoolean dropResponse = new AtomicBoolean();
  private final AtomicInteger transientFailures = new AtomicInteger();
  private AgentTaskService service;
  private TransactionTemplate tx;
  private MockMvc mvc;
  private HttpServer http;
  private URLClassLoader loader;
  private OpenAiPricingService pricing;

  /** Resolve somente o módulo deste checkout, independentemente do diretório de execução. */
  private static Path workerClasses() {
    Path checkout = Path.of(System.getProperty("basedir", ".")).toAbsolutePath();
    while (checkout != null
        && !Files.isRegularFile(checkout.resolve("landing-generator-agent-worker/pom.xml")))
      checkout = checkout.getParent();
    if (checkout == null) throw new IllegalStateException("Checkout do executor não identificado");
    return checkout.resolve("landing-generator-agent-worker/target/classes");
  }

  /** Conecta o consumidor real ao MVC por HTTP e usa o interceptor transacional produtivo. */
  @BeforeEach
  void setup() throws Exception {
    assertThat(Files.isDirectory(workerClasses()))
        .as("Compile landing-generator-agent-worker antes da homologação integrada do callback")
        .isTrue();
    tx = new TransactionTemplate(manager);
    pricing = mock(OpenAiPricingService.class);
    when(pricing.estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn(Optional.of(new java.math.BigDecimal("0.12345678")));
    var target =
        new AgentTaskService(
            tasks,
            instances,
            agents,
            processes,
            definitions,
            resources,
            json,
            pricing,
            MarketStrategicContextProvider.empty(),
            AgentTaskTargetContextProvider.empty());
    var values =
        new ProductValueDeliveryService(
            mock(ProductRepository.class), tasks, mock(PdeOperationalControlEvidence.class), json);
    AgentTaskCompletionHook hook =
        new AgentTaskCompletionHook() {
          /** Limita o efeito às atividades tipadas reais. */
          public boolean supports(AgentTask task) {
            return values.supports(task);
          }

          /**
           * Simula contrato recusado antes da correção sem substituir o hook real no caminho
           * válido.
           */
          public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
            if (reject.get())
              throw new AgentTaskResultRejectedException(
                  new IllegalArgumentException("fixture semântica"));
            return values.apply(task, request);
          }
        };
    ReflectionTestUtils.setField(target, "completionHooks", List.of(hook));
    var factory = new ProxyFactory(target);
    factory.setProxyTargetClass(true);
    factory.addAdvice(
        new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
    service = (AgentTaskService) factory.getProxy();
    mvc =
        MockMvcBuilders.standaloneSetup(
                new InternalAgentTaskExecutionController(service, null),
                new AgentTaskController(service, null))
            .setControllerAdvice(new AgentTaskResultRejectionAdvice())
            .build();
    http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    http.createContext(
        "/",
        exchange -> {
          try {
            var builder =
                "GET".equals(exchange.getRequestMethod())
                    ? get(exchange.getRequestURI().toString())
                    : post(exchange.getRequestURI().toString());
            if ("POST".equals(exchange.getRequestMethod())) posts.incrementAndGet();
            builder
                .contentType(MediaType.APPLICATION_JSON)
                .content(exchange.getRequestBody().readAllBytes());
            if (transientFailures.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
              exchange.sendResponseHeaders(503, -1);
              return;
            }
            String claim = exchange.getRequestHeaders().getFirst("X-Dedalo-Claim");
            if (claim != null) builder.header("X-Dedalo-Claim", claim);
            var response = mvc.perform(builder).andReturn().getResponse();
            if (dropResponse.compareAndSet(true, false)) return;
            byte[] bytes = response.getContentAsByteArray();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(
                response.getStatus(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) exchange.getResponseBody().write(bytes);
          } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                .error("Falha no transporte HTTP sintético", ex);
            exchange.sendResponseHeaders(500, -1);
          } finally {
            exchange.close();
          }
        });
    http.setExecutor(Executors.newCachedThreadPool());
    http.start();
    loader =
        new URLClassLoader(
            new URL[] {workerClasses().toUri().toURL()}, getClass().getClassLoader());
  }

  /** Encerra rede e classloader locais sem tocar filas ou dados externos. */
  @AfterEach
  void close() throws Exception {
    if (http != null) {
      http.stop(0);
      ((ExecutorService) http.getExecutor()).shutdownNow();
    }
    if (loader != null) loader.close();
  }

  /** Reproduz o prefixo original em dois contextos e confirma aceite e contabilização únicos. */
  @ParameterizedTest
  @ValueSource(longs = {9004, 9904})
  void acceptsOriginalPrefixThroughRealWorkerHttpAndPersistence(long productId, @TempDir Path dir)
      throws Exception {
    var fixture = fixture(productId);
    var callback = callback(fixture, "READY");
    preserve(dir, fixture, callback);
    consume(dir);
    assertAccepted(fixture, callback);
    assertThat(Files.exists(dir.resolve("pending.json"))).isFalse();
    // Resposta perdida ou concorrente: o mesmo envelope não duplica tokens, custos ou aceite.
    var pool = Executors.newFixedThreadPool(2);
    try {
      var first =
          pool.submit(
              () ->
                  service.completeClaimedProcessTask("landing-generator", fixture.id(), callback));
      var second =
          pool.submit(
              () ->
                  service.completeClaimedProcessTask("landing-generator", fixture.id(), callback));
      first.get(15, TimeUnit.SECONDS);
      second.get(15, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }
    assertAccepted(fixture, callback);
    verify(pricing, times(1))
        .estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong());
  }

  /** O campo canônico e o literal legado antes válido atravessam o mesmo caminho integrado. */
  @ParameterizedTest
  @ValueSource(strings = {"canonical", "literal"})
  void acceptsExplicitFieldAndLegacyLiteral(String format, @TempDir Path dir) throws Exception {
    var fixture = fixture("canonical".equals(format) ? 109004 : 119004);
    var original = callback(fixture, "READY");
    var result =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(original.resultJson());
    if ("canonical".equals(format)) {
      result.put("specificationReference", fixture.reference());
      result.putArray("acceptanceCriteria").add("Critério funcional da especificação atual");
    } else result.putArray("acceptanceCriteria").add(fixture.reference());
    var callback =
        new CompleteAgentTaskRequest(
            result.toString(),
            original.evidenceJson(),
            original.modelUsages(),
            original.executionAudit());
    preserve(dir, fixture, callback);
    consume(dir);
    assertAccepted(fixture, callback);
    assertThat(Files.exists(dir.resolve("pending.json"))).isFalse();
  }

  /**
   * A rejeição fica durável; replay oficial revalida o original sem modelo e libera a outbox por
   * recibo.
   */
  @Test
  void quarantinesSemanticRejectionAndRecoversOriginal(@TempDir Path dir) throws Exception {
    var fixture = fixture(19004);
    var callback = callback(fixture, "READY");
    preserve(dir, fixture, callback);
    reject.set(true);
    consume(dir);
    assertThat(read(fixture.id()).getStatus()).isEqualTo("BLOCKED");
    assertThat(read(fixture.id()).getInputTokens()).isEqualTo(120L);
    assertThat(read(fixture.id()).getResultJson()).isEqualTo(callback.resultJson());
    assertThat(instance(fixture.instanceId()).isObjectiveAchieved()).isFalse();
    assertThat(instance(fixture.instanceId()).getStatus()).isEqualTo("BLOCKED");
    assertThat(AgentTaskResultView.humanSummary(read(fixture.id())).decision())
        .isEqualTo("BLOCKED");
    assertThat(Files.readString(dir.resolve("model-result.json"))).isEqualTo(callback.resultJson());
    var view =
        service.inbox("landing-generator").stream()
            .filter(t -> t.id().equals(fixture.id()))
            .findFirst()
            .orElseThrow();
    assertThat(view.preservedResultRecoveryAvailable()).isTrue();
    writeFixture("rejected-task.json", view);
    consume(dir);
    consume(dir);
    assertThat(posts.get()).isEqualTo(1);
    reject.set(false);
    mvc.perform(post("/api/agent-tasks/" + fixture.id() + "/replay-result"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isNoContent());
    consume(dir);
    assertAccepted(fixture, callback);
    mvc.perform(post("/api/agent-tasks/" + fixture.id() + "/replay-result"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isNoContent());
    assertThat(Files.exists(dir.resolve("pending.json"))).isFalse();
    List<AgentTaskAuditLinkResponse> urls =
        tx.execute(
            ignored -> AgentTaskAuditView.accessedUrls(tasks.findById(fixture.id()).orElseThrow()));
    assertThat(urls)
        .extracting(AgentTaskAuditLinkResponse::url)
        .containsExactly("https://example.org/qa-evidence");
    verify(pricing, times(1))
        .estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong());
  }

  /** Falhas antes e depois do commit reutilizam o mesmo retorno, sem duplicar custo. */
  @ParameterizedTest
  @ValueSource(strings = {"503", "disconnect"})
  void recoversTransientTransportAndLostAcknowledgment(String mode, @TempDir Path dir)
      throws Exception {
    var fixture = fixture("503".equals(mode) ? 39004 : 49004);
    var callback = callback(fixture, "READY");
    preserve(dir, fixture, callback);
    if ("503".equals(mode)) transientFailures.set(1);
    else dropResponse.set(true);
    consume(dir);
    consume(dir);
    assertAccepted(fixture, callback);
    assertThat(Files.exists(dir.resolve("pending.json"))).isFalse();
    verify(pricing, times(1))
        .estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong());
  }

  /** Ausência, referência errada, prosa ou ambiguidades não geram aceite nem reinferência. */
  @ParameterizedTest
  @ValueSource(strings = {"missing", "wrong", "embedded", "ambiguous"})
  void rejectsInvalidReferenceThroughHttpAndKeepsOriginal(String format, @TempDir Path dir)
      throws Exception {
    var fixture =
        fixture(59004 + List.of("missing", "wrong", "embedded", "ambiguous").indexOf(format));
    var valid = callback(fixture, "READY");
    var result = json.createObjectNode().put("decision", "READY");
    switch (format) {
      case "wrong" -> result.put("specificationReference", "VALUE_SPECIFICATION:" + "0".repeat(64));
      case "embedded" ->
          result.putArray("acceptanceCriteria").add("Ver " + fixture.reference() + " no contrato");
      case "ambiguous" -> {
        result.put("specificationReference", fixture.reference());
        result.putArray("acceptanceCriteria").add("VALUE_SPECIFICATION:" + "0".repeat(64));
      }
      default -> result.put("rationale", "Sem referência");
    }
    var invalid =
        new CompleteAgentTaskRequest(
            result.toString(), valid.evidenceJson(), valid.modelUsages(), valid.executionAudit());
    preserve(dir, fixture, invalid);
    consume(dir);
    consume(dir);
    assertThat(posts.get()).isEqualTo(1);
    assertThat(read(fixture.id()).getStatus()).isEqualTo("BLOCKED");
    assertThat(read(fixture.id()).getResultJson()).isEqualTo(invalid.resultJson());
    assertThat(instance(fixture.instanceId()).isObjectiveAchieved()).isFalse();
    assertThatThrownBy(
            () -> service.completeClaimedProcessTask("landing-generator", fixture.id(), valid))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    mvc.perform(post("/api/agent-tasks/" + fixture.id() + "/replay-result"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isUnprocessableEntity());
    assertThat(read(fixture.id()).getInputTokens()).isEqualTo(120L);
    assertThat(Files.exists(dir.resolve("pending.json"))).isTrue();
  }

  /** Aceita callback tardio original, mas outra tentativa e outro claim mantêm o fencing. */
  @ParameterizedTest
  @ValueSource(strings = {"original", "newer", "foreignClaim"})
  void fencesLateAndForeignCallbacks(String mode, @TempDir Path dir) throws Exception {
    var fixture = fixture(69004 + List.of("original", "newer", "foreignClaim").indexOf(mode));
    var callback = callback(fixture, "READY");
    tx.executeWithoutResult(
        ignored -> {
          var task = tasks.findById(fixture.id()).orElseThrow();
          task.setStatus("BLOCKED");
          task.setExecutionError("DEDALO_LEASE_RECOVERY_EXHAUSTED|interrupção auditada");
          if ("newer".equals(mode)) {
            var newer =
                task(
                    task.getAssignedAgent(),
                    task.getProcessDefinition(),
                    fixture.productId(),
                    "deliverables",
                    task.getProductBigIdeaJson());
            newer.setStatus("PENDING");
            tasks.saveAndFlush(newer);
          }
        });
    preserve(dir, fixture, callback);
    if ("foreignClaim".equals(mode)) {
      var pending =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              json.readTree(Files.readString(dir.resolve("pending.json")));
      ((com.fasterxml.jackson.databind.node.ObjectNode) pending.path("task"))
          .put("executionClaimId", UUID.randomUUID().toString());
      Files.writeString(dir.resolve("pending.json"), pending.toString());
    }
    consume(dir);
    if ("original".equals(mode)) assertAccepted(fixture, callback);
    else {
      assertThat(read(fixture.id()).getStatus()).isEqualTo("BLOCKED");
      assertThat(read(fixture.id()).getInputTokens()).isNull();
      assertThat(Files.exists(dir.resolve("pending.json"))).isTrue();
    }
  }

  /** O retorno BLOCKED persiste a recusa funcional, sem transformar READY em aceite de produto. */
  @Test
  void preservesFunctionalBlockWithoutAdvancement(@TempDir Path dir) throws Exception {
    var fixture = fixture(29004);
    var callback = callback(fixture, "BLOCKED");
    var body = json.convertValue(callback, Map.class);
    body.put("error", "Falta especificação funcional suficiente");
    preserve(dir, fixture, body, "failure");
    consume(dir);
    assertThat(read(fixture.id()).getStatus()).isEqualTo("BLOCKED");
    assertThat(instance(fixture.instanceId()).isObjectiveAchieved()).isFalse();
    assertThat(Files.exists(dir.resolve("pending.json"))).isFalse();
  }

  /** Exporta somente dados sintéticos da resposta real para a validação desktop e mobile. */
  private void writeFixture(String name, Object value) throws Exception {
    Path output = Path.of("target/dedalo-callback");
    Files.createDirectories(output);
    Files.writeString(output.resolve(name), json.writeValueAsString(value));
  }

  /** Consulta a tarefa em outra transação para comprovar commit real do callback. */
  private AgentTask read(Long id) {
    return tx.execute(ignored -> tasks.findById(id).orElseThrow());
  }

  /** Consulta o aceite da atividade independente da sessão de escrita. */
  private BusinessProcessActivityInstance instance(Long id) {
    return tx.execute(ignored -> instances.findById(id).orElseThrow());
  }

  /** Confere bytes, tokens, custo e um único aceite da atividade, sem afirmar homologação. */
  private void assertAccepted(Fixture fixture, CompleteAgentTaskRequest callback) {
    var stored = read(fixture.id());
    assertThat(stored.getStatus()).isEqualTo("COMPLETED");
    assertThat(stored.getResultJson()).isEqualTo(callback.resultJson());
    assertThat(stored.getEvidenceJson()).isEqualTo(callback.evidenceJson());
    assertThat(stored.getInputTokens()).isEqualTo(120L);
    assertThat(stored.getEstimatedCostUsd()).isEqualByComparingTo("0.12345678");
    assertThat(instance(fixture.instanceId()).isObjectiveAchieved()).isTrue();
    assertThat(instance(fixture.instanceId()).getStatus()).isEqualTo("COMPLETED");
  }

  /** Materializa a outbox original sintética, sem executar o modelo para obter uma resposta. */
  private void preserve(Path dir, Fixture fixture, CompleteAgentTaskRequest callback)
      throws Exception {
    preserve(dir, fixture, json.convertValue(callback, Map.class), "result");
  }

  /** Grava o envelope persistente que o consumidor real usa no replay de transporte. */
  private void preserve(Path dir, Fixture fixture, Map<String, Object> callback, String operation)
      throws Exception {
    var task =
        Map.of(
            "taskId",
            fixture.id(),
            "sourceReference",
            "experiment:" + fixture.productId(),
            "processCode",
            "pde-construction-approval",
            "activityId",
            "deliverables",
            "executionClaimId",
            fixture.claim());
    Files.writeString(
        dir.resolve("pending.json"),
        json.writeValueAsString(
            Map.of(
                "task",
                task,
                "audit",
                audit(),
                "modelStarted",
                true,
                "operation",
                operation,
                "callback",
                callback)));
    Files.writeString(dir.resolve("model-result.json"), (String) callback.get("resultJson"));
  }

  /** Executa o consumidor compilado com um comando impossível, provando zero nova inferência. */
  private void consume(Path dir) throws Exception {
    Class<?> props =
        loader.loadClass("com.marketinghub.landinggeneratoragent.LandingGeneratorAgentProperties");
    Object p = props.getConstructor().newInstance();
    props
        .getMethod("setBackendUrl", String.class)
        .invoke(p, "http://127.0.0.1:" + http.getAddress().getPort());
    props.getMethod("setBpmStateDirectory", String.class).invoke(p, dir.toString());
    props.getMethod("setCodexCommand", String.class).invoke(p, "must-never-run-model");
    Class<?> control =
        loader.loadClass("com.marketinghub.landinggeneratoragent.AutomaticExecutionControl");
    Class<?> telemetry =
        loader.loadClass("com.marketinghub.landinggeneratoragent.CodexTelemetryReporter");
    Object consumer =
        loader
            .loadClass("com.marketinghub.landinggeneratoragent.PdeConstructionBpmTaskConsumer")
            .getConstructor(props, ObjectMapper.class, control, telemetry)
            .newInstance(p, json, null, null);
    consumer.getClass().getMethod("processOne").invoke(consumer);
  }

  /** Produz o formato exato do prefixo observado, preservando espaços e Unicode do original. */
  private CompleteAgentTaskRequest callback(Fixture fixture, String decision) throws Exception {
    String result =
        "{ \"decision\":\""
            + decision
            + "\", \"acceptanceCriteria\":[\"specificationReference="
            + fixture.reference()
            + "\"], \"scope\":\"PROTOTYPE_SLICE\", \"humanSummary\":\"Especificação preservada; implementação e testes continuam pendentes.\" }";
    return new CompleteAgentTaskRequest(
        result,
        "{ \"source\":\"experiment:" + fixture.productId() + "\" }",
        List.of(new AgentTaskModelUsageRequest("fixture-model", "STANDARD", 120L, 80L, 40L)),
        audit());
  }

  /** Fornece auditoria composta imutável e sem URLs ou credenciais externas. */
  private AgentTaskExecutionAuditRequest audit() {
    return new AgentTaskExecutionAuditRequest(
        "MODEL",
        "fixture-model",
        "max",
        "agente\n\natividade",
        "agente",
        "atividade",
        List.of(
            new AgentTaskAccessedUrlRequest(
                "https://example.org/qa-evidence",
                "Prova sintética",
                "HTTP",
                Instant.parse("2026-11-23T00:00:00Z"))));
  }

  /** Persiste base, especificação anterior, claim e atividade no mesmo contexto sintético. */
  private Fixture fixture(long productId) {
    return tx.execute(
        ignored -> {
          var agent =
              agents
                  .findByAgentKey("landing-generator")
                  .orElseGet(
                      () -> {
                        var theme = new AgentTheme();
                        theme.setName("QA callback " + productId);
                        em.persist(theme);
                        var created = new Agent();
                        created.setTheme(theme);
                        created.setAgentKey("landing-generator");
                        created.setName("Dédalo QA " + productId);
                        created.setNickname("Dédalo");
                        created.setStatus("ACTIVE");
                        created.setCurrentVersion(1);
                        created.setExecutionMode("WORKER");
                        created.setAutomaticExecutionEnabled(false);
                        em.persist(created);
                        return created;
                      });
          var process = new BusinessProcessDefinition();
          process.setProcessCode("pde-construction-approval");
          process.setName("Construção QA " + productId);
          process.setPurpose("Teste");
          process.setOwnerName("Dédalo");
          process.setTriggerDescription("Entrada");
          process.setOutcomeDescription("Especificação");
          process.setVersionNumber((int) productId);
          process.setStatus("PUBLISHED");
          process.setProcessType("VALUE_PROCESS");
          process.setDiagramJson(
              "{\"productValueDeliveryContract\":\"TURMALINA_VALUE_DELIVERY_V1\",\"nodes\":[],\"flows\":[]}");
          process.setCreatedAt(Instant.now());
          em.persist(process);
          var base =
              json.createObjectNode()
                  .put("applicable", true)
                  .put("productId", productId)
                  .put("sourceReference", "experiment:" + productId)
                  .put("blueprintVersion", "fixture-v1");
          base.putObject("bigIdea").put("id", productId);
          base.putObject("requirements").put("promise", "Valor pronto");
          var spec =
              json.createObjectNode()
                  .put("contractVersion", ProductValueContract.VERSION)
                  .put("baselineSha256", ProductValueContract.hash(base))
                  .put("scope", "PROTOTYPE_SLICE");
          var story =
              spec.putArray("stories")
                  .addObject()
                  .put("id", "ready-value")
                  .put("customerOutcome", "Valor")
                  .put("action", "BUILD")
                  .put("implementationNotes", "Implementar depois da especificação");
          story.putArray("sourceRefs").add("promise");
          var states = story.putObject("screenStates");
          for (String state : List.of("entry", "result", "resume", "empty", "error"))
            states.put(state, "Ação verificável " + state);
          story
              .putArray("criteria")
              .addObject()
              .put("id", "ready")
              .put("given", "Entrada válida")
              .put("when", "Processar")
              .put("then", "Resultado pronto")
              .put("testMethod", "syntheticReadyValue");
          var idea = json.createObjectNode().set("valueDeliveryBaseline", base).toString();
          var previous = task(agent, process, productId, "journey", idea);
          previous.setStatus("COMPLETED");
          previous.setResultJson(
              json.createObjectNode().set("valueSpecification", spec).toString());
          em.persist(previous);
          var definition = new BusinessProcessActivityDefinition();
          definition.setProcessDefinition(process);
          definition.setActivityId("deliverables");
          definition.setName("Componentes");
          definition.setOwnerName("Dédalo");
          definition.setDefinitionJson("{}");
          definition.setCreatedAt(Instant.now());
          em.persist(definition);
          var instance = new BusinessProcessActivityInstance();
          instance.setActivityDefinition(definition);
          instance.setSourceReference("experiment:" + productId);
          instance.setOccurrenceNumber(1);
          instance.setStatus("IN_PROGRESS");
          instance.setEnteredAt(Instant.now());
          instance.setCostCoverage("NOT_REPORTED");
          instance.setEvidenceQuality("DIRECT");
          instance.setCreatedAt(Instant.now());
          instance.setUpdatedAt(Instant.now());
          em.persist(instance);
          var task = task(agent, process, productId, "deliverables", idea);
          task.setStatus("IN_PROGRESS");
          task.setActivityInstance(instance);
          task.setExecutionClaimId(UUID.randomUUID().toString());
          task.setExecutionClaimPhase("START_AUTHORIZED");
          task.setExecutionMode("MODEL");
          task.setExecutionModelCode("fixture-model");
          task.setExecutionReasoningEffort("max");
          task.setExecutionPrompt(audit().promptSent());
          task.setExecutionAgentPrompt(audit().agentPromptPart());
          task.setExecutionActivityPrompt(audit().activityPromptPart());
          em.persistAndFlush(task);
          return new Fixture(
              task.getId(),
              instance.getId(),
              productId,
              task.getExecutionClaimId(),
              "VALUE_SPECIFICATION:" + ProductValueContract.hash(spec));
        });
  }

  /** Preenche apenas campos obrigatórios de uma tarefa isolada de teste. */
  private AgentTask task(
      Agent agent,
      BusinessProcessDefinition process,
      long productId,
      String activity,
      String idea) {
    var task = new AgentTask();
    task.setAssignedAgent(agent);
    task.setProcessDefinition(process);
    task.setSourceReference("experiment:" + productId);
    task.setProcessActivityId(activity);
    task.setProcessActivityName(activity);
    task.setRequestedByType("HUMAN");
    task.setRequestedByName("QA");
    task.setTitle("Callback local " + productId);
    task.setDescription("Sem modelo externo");
    task.setPriority("NORMAL");
    task.setTaskKind("WORK");
    task.setProductBigIdeaJson(idea);
    task.setCreatedAt(Instant.now());
    task.setUpdatedAt(Instant.now());
    return task;
  }

  /** Mantém as identidades da prova segregadas por caso. */
  private record Fixture(
      Long id, Long instanceId, long productId, String claim, String reference) {}
}
