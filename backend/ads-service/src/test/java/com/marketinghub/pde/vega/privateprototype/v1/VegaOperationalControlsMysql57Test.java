package com.marketinghub.pde.vega.privateprototype.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Comprova limites, concorrência, replay e recusas com controllers e MySQL 5.7 reais. */
@SpringBootTest(
    classes = VegaOperationalControlsMysql57Test.LocalConfiguration.class,
    properties = {
      "spring.config.location=optional:classpath:vega380/no-production.properties",
      "spring.liquibase.change-log=classpath:vega380/changelog.yaml",
      "integrations.pde-platform.internal-token=vega-local-internal-only",
      "logging.level.root=WARN"
    })
@AutoConfigureMockMvc
@ActiveProfiles("vega380-local")
@EnabledIfEnvironmentVariable(
    named = "MIRA_CONTROLS_DB_HOST",
    matches = "127\\.0\\.0\\.1|sandbox-docker")
class VegaOperationalControlsMysql57Test {
  /** Reutiliza apenas o domínio real do protótipo e os cadastros externos simulados. */
  @Configuration(proxyBeanMethods = false)
  @Profile("vega380-local")
  @Import(VegaPrivateLocalApplication.class)
  static class LocalConfiguration {}

  private static final String BASE = "/api/pde/vega/private/v1";
  private static final String QUEUE = "/internal/adjustment/stage-executions";
  private static final String VERSION = "musa-pde-entry-v14-primeiro-ajuste-aplicavel";
  private static final String MODEL = "vega-deterministic-fixture-v1";
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final List<Map<String, Object>> OBSERVATIONS = new CopyOnWriteArrayList<>();
  private static String databaseVersion;
  @Autowired private MockMvc http;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ProductRepository products;

  /** Usa o schema efêmero já provisionado pelo CI e recusa qualquer banco externo. */
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String host = System.getenv("MIRA_CONTROLS_DB_HOST");
    String port = System.getenv().getOrDefault("MIRA_CONTROLS_DB_PORT", "3306");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host)
        || !Set.of("3306", "18316").contains(port))
      throw new IllegalArgumentException("A prova exige o MySQL efêmero local.");
    properties.add(
        "spring.datasource.url",
        () ->
            "jdbc:mysql://"
                + host
                + ":"
                + port
                + "/vega_queue_local?useSSL=false&serverTimezone=UTC");
    properties.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("MIRA_CONTROLS_DB_USER", "vega"));
    properties.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("MIRA_CONTROLS_DB_PASSWORD", "vega-local-only"));
  }

  /** Cria dois contextos isolados da mesma implementação sem consultar cadastros publicados. */
  @BeforeEach
  void prepare() {
    cleanup();
    databaseVersion = jdbc.queryForObject("SELECT VERSION()", String.class);
    for (long id : List.of(91004L, 97004L)) {
      when(products.findById(id))
          .thenReturn(
              Optional.of(
                  Product.builder()
                      .id(id)
                      .slug("metodo-musa-7-dias")
                      .internalName("Fixture operacional " + id)
                      .pdeExperienceJson("{}")
                      .build()));
      jdbc.update(
          "INSERT INTO learning_sales_cycle_v1 SELECT ?,?,chain_definition_id,chain_code,process_definition_id,?,NULL,?,creation_json,brief_json,inherited_learning_json,stage,'OPEN',?,budget_limit_brl,window_start,window_end,NULL,NULL,NULL,NULL,revision,baseline,created_at,updated_at,version_changed_at,NULL FROM learning_sales_cycle_v1 WHERE id=91002",
          cycle(id),
          id,
          id + 81000,
          UUID.randomUUID().toString(),
          VERSION);
    }
  }

  /** Remove somente as linhas sintéticas desta prova, sem apagar fixtures de outros testes. */
  @AfterEach
  void cleanup() {
    jdbc.update(
        "DELETE e FROM vega_adjustment_execution_v1 e JOIN vega_private_session_v1 s ON s.id=e.session_id WHERE s.cycle_id IN (171004,177004)");
    jdbc.update("DELETE FROM vega_private_session_v1 WHERE cycle_id IN (171004,177004)");
    jdbc.update("DELETE FROM learning_sales_cycle_v1 WHERE id IN (171004,177004)");
  }

  /** Comprova a reserva de capacidade concorrente e a recusa da décima nona sessão. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void concurrentSessionCreationEnforcesCapacity(long productId) throws Exception {
    List<Integer> statuses;
    try (var executor = Executors.newFixedThreadPool(8)) {
      var futures = new ArrayList<Future<Integer>>();
      for (int index = 0; index < 24; index++)
        futures.add(executor.submit(() -> createResponse(productId).getResponse().getStatus()));
      statuses = new ArrayList<>();
      for (var future : futures) statuses.add(future.get(30, TimeUnit.SECONDS));
    }
    assertThat(Collections.frequency(statuses, 200)).isEqualTo(18);
    assertThat(Collections.frequency(statuses, 409)).isEqualTo(6);
    assertThat(countSessions(productId)).isEqualTo(18);
    record(
        "SESSION_LIMIT",
        productId,
        "concurrentSessionCreationEnforcesCapacity",
        Map.of(
            "requests",
            24,
            "concurrentThreads",
            8,
            "accepted",
            18,
            "rejected409",
            6,
            "persistedSessions",
            countSessions(productId)));
  }

  /** Cliques e reservas simultâneos mantêm uma execução e apenas um executor com lease válida. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void concurrentRequestsConsumeOnce(long productId) throws Exception {
    var session = create(productId);
    post("/start", Map.of(), token(session), 200);
    Set<Long> ids = new HashSet<>();
    try (var executor = Executors.newFixedThreadPool(8)) {
      var futures = new ArrayList<Future<JsonNode>>();
      for (int index = 0; index < 8; index++)
        futures.add(executor.submit(() -> post("/generate", input(), token(session), 200)));
      for (var future : futures)
        ids.add(future.get(30, TimeUnit.SECONDS).path("executionId").asLong());
    }
    assertThat(ids).hasSize(1).doesNotContain(0L);
    assertThat(countAttempts(session)).isEqualTo(1);
    long repeated = post("/generate", input(), token(session), 200).path("executionId").asLong();
    assertThat(ids).contains(repeated);
    var claims = new ArrayList<Integer>();
    try (var executor = Executors.newFixedThreadPool(8)) {
      var futures = new ArrayList<Future<Integer>>();
      for (int index = 0; index < 8; index++)
        futures.add(
            executor.submit(
                () ->
                    http.perform(
                            MockMvcRequestBuilders.post(BASE + QUEUE + "/" + repeated + "/claim")
                                .header("X-PDE-Internal-Token", "vega-local-internal-only")
                                .contentType("application/json")
                                .content("{}"))
                        .andReturn()
                        .getResponse()
                        .getStatus()));
      for (var future : futures) claims.add(future.get(30, TimeUnit.SECONDS));
    }
    assertThat(Collections.frequency(claims, 200)).isEqualTo(1);
    assertThat(Collections.frequency(claims, 409)).isEqualTo(7);
    assertThat(countAttempts(session)).isEqualTo(1);
    for (String code : List.of("CONCURRENT_SINGLE_CONSUMPTION", "IDEMPOTENT_REQUEST"))
      record(
          code,
          productId,
          "concurrentRequestsConsumeOnce",
          Map.of(
              "concurrentRequests",
              8,
              "distinctExecutionIds",
              ids.size(),
              "persistedAttempts",
              countAttempts(session),
              "repeatedRequestReusedExecution",
              true,
              "concurrentClaimRequests",
              8,
              "acceptedClaims",
              1,
              "rejectedClaims409",
              7));
  }

  /**
   * O replay conserva resultado, horário e custo, mesmo recebendo callback posterior divergente.
   */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void duplicateCallbackPreservesResultAndCost(long productId) throws Exception {
    var session = create(productId);
    long execution = generate(session);
    claimAndAudit(execution);
    var result = result(execution, "COMPLETED");
    var first = post(QUEUE + "/" + execution + "/result", result, null, 200);
    var stored =
        jdbc.queryForMap(
            "SELECT status,result_json,cost_usd,finished_at,response_json FROM vega_adjustment_execution_v1 WHERE id=?",
            execution);
    var replay = post(QUEUE + "/" + execution + "/result", result, null, 200);
    var divergent =
        post(QUEUE + "/" + execution + "/result", result(execution, "FAILED"), null, 200);
    var expected = (com.fasterxml.jackson.databind.node.ObjectNode) first.deepCopy();
    var replayBusiness = (com.fasterxml.jackson.databind.node.ObjectNode) replay.deepCopy();
    var divergentBusiness = (com.fasterxml.jackson.databind.node.ObjectNode) divergent.deepCopy();
    expected.remove("finishedAt");
    replayBusiness.remove("finishedAt");
    divergentBusiness.remove("finishedAt");
    assertThat(replayBusiness).isEqualTo(expected);
    assertThat(divergentBusiness).isEqualTo(expected);
    assertThat(
            jdbc.queryForMap(
                "SELECT status,result_json,cost_usd,finished_at,response_json FROM vega_adjustment_execution_v1 WHERE id=?",
                execution))
        .isEqualTo(stored);
    assertThat(first.path("status").asText()).isEqualTo("COMPLETED");
    assertThat(first.path("costUsd").decimalValue().signum()).isZero();
    assertThat(countAttempts(session)).isEqualTo(1);
    record(
        "IDEMPOTENT_CALLBACK",
        productId,
        "duplicateCallbackPreservesResultAndCost",
        Map.of(
            "callbacks",
            3,
            "persistedAttempts",
            1,
            "sameResult",
            true,
            "sameFinishedAt",
            true,
            "costUsd",
            first.path("costUsd").decimalValue(),
            "divergentCallbackDidNotOverwrite",
            true));
  }

  /** O teto cumulativo continua consumido após falha e revogação, sem permitir nova tentativa. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void attemptsRemainBoundedAfterFailuresAndRevocation(long productId) throws Exception {
    var sessions = new ArrayList<JsonNode>();
    for (int index = 0; index < 18; index++) {
      var session = create(productId);
      sessions.add(session);
      post("/start", Map.of(), token(session), 200);
      for (int attempt = 0; attempt < 2; attempt++) {
        long execution =
            post("/generate", input(), token(session), 200).path("executionId").asLong();
        claimAndAudit(execution);
        assertThat(
                post(QUEUE + "/" + execution + "/result", result(execution, "FAILED"), null, 200)
                    .path("status")
                    .asText())
            .isEqualTo("FAILED");
      }
      post("/generate", input(), token(session), 409);
      assertThat(countAttempts(session)).isEqualTo(2);
    }
    revoke(sessions.getFirst());
    assertThat(createResponse(productId).getResponse().getStatus()).isEqualTo(409);
    int attempts =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM vega_adjustment_execution_v1 e JOIN vega_private_session_v1 s ON s.id=e.session_id WHERE s.cycle_id=?",
            Integer.class,
            cycle(productId));
    assertThat(attempts).isEqualTo(36);
    record(
        "ATTEMPT_LIMIT",
        productId,
        "attemptsRemainBoundedAfterFailuresAndRevocation",
        Map.of(
            "sessions",
            18,
            "attemptsPerSession",
            2,
            "persistedAttempts",
            attempts,
            "thirdAttemptsRejected409",
            18,
            "revocationDidNotRefundCapacity",
            true,
            "newSessionStatusAfterRevocation",
            409));
  }

  /**
   * O acesso vencido é recusado na leitura, geração e reserva, preservando a tentativa anterior.
   */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void expiredAccessCannotReadGenerateOrClaim(long productId) throws Exception {
    var session = create(productId);
    long execution = generate(session);
    assertThat(
            http.perform(
                    MockMvcRequestBuilders.get(BASE + "/session")
                        .header("X-Vega-Session", "invalid-synthetic-token"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
    post("/generate", input(), "invalid-synthetic-token", 401);
    assertThat(countSessions(productId)).isEqualTo(1);
    assertThat(countAttempts(session)).isEqualTo(1);
    record(
        "INVALID_ACCESS_REJECTED",
        productId,
        "expiredAccessCannotReadGenerateOrClaim",
        Map.of(
            "readStatus",
            401,
            "generateStatus",
            401,
            "persistedSessions",
            1,
            "persistedAttempts",
            1));
    jdbc.update(
        "UPDATE vega_private_session_v1 SET expires_at=DATE_SUB(NOW(),INTERVAL 1 DAY) WHERE id=?",
        session.path("id").asText());
    assertThat(
            http.perform(
                    MockMvcRequestBuilders.get(BASE + "/session")
                        .header("X-Vega-Session", token(session)))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
    post("/generate", input(), token(session), 401);
    post(QUEUE + "/" + execution + "/claim", Map.of(), null, 401);
    assertPendingExcludes(execution);
    assertThat(countAttempts(session)).isEqualTo(1);
    record(
        "EXPIRED_ACCESS_REJECTED",
        productId,
        "expiredAccessCannotReadGenerateOrClaim",
        Map.of(
            "readStatus",
            401,
            "generateStatus",
            401,
            "claimStatus",
            401,
            "absentFromPending",
            true,
            "persistedAttempts",
            1));
  }

  /** Uma sessão válida nunca recebe cartão ou entrada da outra, mesmo usando seu ID na URL. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void anotherSessionCannotReadOwnedResult(long productId) throws Exception {
    var owner = create(productId);
    var other = create(productId);
    long execution = generate(owner);
    claimAndAudit(execution);
    post(QUEUE + "/" + execution + "/result", result(execution, "COMPLETED"), null, 200);
    var response =
        http.perform(
                MockMvcRequestBuilders.get(BASE + "/session?sessionId=" + owner.path("id").asText())
                    .header("X-Vega-Session", token(other)))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    var isolated =
        JSON.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    assertThat(isolated.path("id")).isEqualTo(other.path("id"));
    assertThat(isolated.path("card").isMissingNode() || isolated.path("card").isNull()).isTrue();
    assertThat(isolated.path("input").isEmpty()).isTrue();
    assertThat(countAttempts(other)).isZero();
    var owned =
        http.perform(
                MockMvcRequestBuilders.get(BASE + "/session")
                    .header("X-Vega-Session", token(owner)))
            .andReturn()
            .getResponse();
    assertThat(
            JSON.readTree(owned.getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("card")
                .path("cardId")
                .asText())
        .isEqualTo(String.valueOf(execution));
    record(
        "CROSS_SESSION_ISOLATION",
        productId,
        "anotherSessionCannotReadOwnedResult",
        Map.of(
            "ownerCardAvailable",
            true,
            "foreignQueryReturnsOwnSession",
            true,
            "foreignCardExposed",
            false,
            "foreignSessionAttempts",
            0));
  }

  /** A revogação impede leitura e novos comandos sem apagar nem duplicar a execução auditada. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void revokedAccessCannotReadGenerateOrClaim(long productId) throws Exception {
    var session = create(productId);
    long execution = generate(session);
    revoke(session);
    assertThat(
            http.perform(
                    MockMvcRequestBuilders.get(BASE + "/session")
                        .header("X-Vega-Session", token(session)))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
    post("/generate", input(), token(session), 401);
    post(QUEUE + "/" + execution + "/claim", Map.of(), null, 401);
    assertPendingExcludes(execution);
    assertThat(countAttempts(session)).isEqualTo(1);
    record(
        "REVOKED_ACCESS_REJECTED",
        productId,
        "revokedAccessCannotReadGenerateOrClaim",
        Map.of(
            "readStatus",
            401,
            "generateStatus",
            401,
            "claimStatus",
            401,
            "absentFromPending",
            true,
            "persistedAttempts",
            1));
  }

  /** Ciclo encerrado ou versão incompatível nunca autoriza reserva nem envio ao provedor. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void incompatibleContextCannotClaimOrSendRequest(long productId) throws Exception {
    var session = create(productId);
    long execution = generate(session);
    String before =
        jdbc.queryForObject(
            "SELECT input_json FROM vega_adjustment_execution_v1 WHERE id=?",
            String.class,
            execution);
    jdbc.update("UPDATE learning_sales_cycle_v1 SET status='CLOSED' WHERE id=?", cycle(productId));
    post(QUEUE + "/" + execution + "/claim", Map.of(), null, 409);
    assertPendingExcludes(execution);
    jdbc.update(
        "UPDATE learning_sales_cycle_v1 SET status='OPEN',product_version='incompatible-version' WHERE id=?",
        cycle(productId));
    post(QUEUE + "/" + execution + "/claim", Map.of(), null, 409);
    assertPendingExcludes(execution);
    jdbc.update(
        "UPDATE learning_sales_cycle_v1 SET product_version=? WHERE id=?",
        VERSION,
        cycle(productId));
    post(QUEUE + "/" + execution + "/claim", Map.of(), null, 200);
    jdbc.update("UPDATE learning_sales_cycle_v1 SET status='CLOSED' WHERE id=?", cycle(productId));
    post(
        QUEUE + "/" + execution + "/request",
        Map.of("model", MODEL, "request", Map.of("provider", "DETERMINISTIC_FIXTURE")),
        null,
        409);
    assertThat(
            jdbc.queryForObject(
                "SELECT input_json FROM vega_adjustment_execution_v1 WHERE id=?",
                String.class,
                execution))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT request_json FROM vega_adjustment_execution_v1 WHERE id=?",
                String.class,
                execution))
        .isNull();
    record(
        "INCOMPATIBLE_CONTEXT_REJECTED",
        productId,
        "incompatibleContextCannotClaimOrSendRequest",
        Map.of(
            "closedCycleClaimStatus",
            409,
            "wrongVersionClaimStatus",
            409,
            "requestAfterClosingStatus",
            409,
            "absentFromPending",
            true,
            "inputPreserved",
            true,
            "providerRequestPersisted",
            false));
  }

  /** Persiste somente observações aprovadas e sanitizadas das duas identidades testadas. */
  @AfterAll
  static void writeReceipt() throws Exception {
    String output = System.getProperty("vega.controls.output");
    if (output == null) return;
    assertThat(OBSERVATIONS).hasSize(20);
    var result =
        Map.of(
            "contractVersion",
            "PDE_PRIVATE_EXECUTION_CONTROLS_RUN_V1",
            "databaseVersion",
            databaseVersion,
            "prototypeVersion",
            VERSION,
            "generatedAt",
            Instant.now().toString(),
            "providerCalls",
            0,
            "commercialSideEffects",
            false,
            "observations",
            OBSERVATIONS);
    Files.writeString(
        Path.of(output), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result));
  }

  /** Resolve o ciclo exclusivo de cada produto sintético. */
  private static long cycle(long productId) {
    return productId + 80000;
  }

  /** Mantém credenciais efêmeras somente na memória da chamada HTTP local. */
  private String token(JsonNode session) {
    return session.path("sessionToken").asText();
  }

  /** Fornece a mesma entrada válida em chamadas concorrentes e retentativas. */
  private Map<String, String> input() {
    return Map.of(
        "occasion",
        "Almoço informal",
        "existingSelection",
        "Camisa branca já disponível",
        "optionalNote",
        "");
  }

  /** Solicita uma sessão pelo controller canônico, sem acesso direto à persistência funcional. */
  private MvcResult createResponse(long productId) throws Exception {
    return http.perform(
            MockMvcRequestBuilders.post(BASE + "/internal/sessions")
                .header("X-PDE-Internal-Token", "vega-local-internal-only")
                .contentType("application/json")
                .content(
                    JSON.writeValueAsString(
                        Map.of(
                            "cycleId",
                            cycle(productId),
                            "prototypeVersion",
                            VERSION,
                            "origin",
                            "AGENT_VALIDATION"))))
        .andReturn();
  }

  /** Exige que a sessão criada pertença à identidade e versão fornecidas. */
  private JsonNode create(long productId) throws Exception {
    var response = createResponse(productId);
    assertThat(response.getResponse().getStatus()).isEqualTo(200);
    var session =
        JSON.readTree(
            response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    assertThat(session.path("productId").asLong()).isEqualTo(productId);
    assertThat(session.path("prototypeVersion").asText()).isEqualTo(VERSION);
    return session;
  }

  /** Executa somente o contrato HTTP informado e confere seu status real. */
  private JsonNode post(String path, Object body, String session, int expectedStatus)
      throws Exception {
    var request =
        MockMvcRequestBuilders.post(BASE + path)
            .contentType("application/json")
            .content(JSON.writeValueAsString(body));
    request.header(
        session == null ? "X-PDE-Internal-Token" : "X-Vega-Session",
        session == null ? "vega-local-internal-only" : session);
    var response = http.perform(request).andReturn().getResponse();
    assertThat(response.getStatus()).as(path).isEqualTo(expectedStatus);
    return response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8).isBlank()
        ? JSON.nullNode()
        : JSON.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** Inicia explicitamente a experiência antes de reservar sua primeira geração. */
  private long generate(JsonNode session) throws Exception {
    post("/start", Map.of(), token(session), 200);
    return post("/generate", input(), token(session), 200).path("executionId").asLong();
  }

  /** Reserva a pendência e registra um request de fixture sem chamada externa. */
  private void claimAndAudit(long execution) throws Exception {
    post(QUEUE + "/" + execution + "/claim", Map.of(), null, 200);
    post(
        QUEUE + "/" + execution + "/request",
        Map.of("model", MODEL, "request", Map.of("provider", "DETERMINISTIC_FIXTURE")),
        null,
        200);
  }

  /** Monta somente o retorno determinístico permitido para a sessão sintética. */
  private Map<String, Object> result(long execution, String status) {
    var value = new LinkedHashMap<String, Object>();
    value.put("status", status);
    value.put("model", MODEL);
    value.put("inputTokens", 0);
    value.put("outputTokens", 0);
    value.put("costUsd", 0);
    value.put("rawResponse", Map.of("synthetic", true, "status", status));
    value.put("error", "FAILED".equals(status) ? "Falha sintética local" : null);
    value.put(
        "card",
        "COMPLETED".equals(status)
            ? Map.of(
                "cardId",
                String.valueOf(execution),
                "action",
                "Alinhe o caimento com conforto",
                "application",
                "Acomode o tecido das peças que já possui",
                "occasion",
                "Almoço informal",
                "selfAssessmentPrompt",
                "O movimento ficou confortável?",
                "usesOnlyAvailableItems",
                true)
            : null);
    return value;
  }

  /** Revoga pelo endpoint oficial, preservando o histórico existente. */
  private void revoke(JsonNode session) throws Exception {
    assertThat(
            http.perform(
                    MockMvcRequestBuilders.delete(
                            BASE + "/internal/sessions/" + session.path("id").asText())
                        .header("X-PDE-Internal-Token", "vega-local-internal-only"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(200);
  }

  /** Confere a ausência real da execução inválida na fila canônica. */
  private void assertPendingExcludes(long execution) throws Exception {
    var response =
        http.perform(
                MockMvcRequestBuilders.get(BASE + QUEUE + "/pending?mode=FIXTURE")
                    .header("X-PDE-Internal-Token", "vega-local-internal-only"))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    for (var row :
        JSON.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)))
      assertThat(row.path("id").asLong()).isNotEqualTo(execution);
  }

  /** Conta capacidade reservada com a mesma fonte SQL usada pela homologação. */
  private int countSessions(long productId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM vega_private_session_v1 WHERE cycle_id=?",
        Integer.class,
        cycle(productId));
  }

  /** Conta tentativas persistidas para impedir consumo e replay duplicados. */
  private int countAttempts(JsonNode session) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM vega_adjustment_execution_v1 WHERE session_id=?",
        Integer.class,
        session.path("id").asText());
  }

  /** Registra somente fatos já conferidos, sem armazenar credenciais ou alegações de mercado. */
  private void record(String code, long productId, String method, Map<String, Object> observed) {
    OBSERVATIONS.add(
        Map.of(
            "code",
            code,
            "productId",
            productId,
            "cycleId",
            cycle(productId),
            "experimentId",
            productId + 81000,
            "testMethod",
            method,
            "status",
            "PASS",
            "observed",
            observed));
  }
}
