package com.marketinghub.pde.vega.style.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.vega.style.v1.service.VegaStyleService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
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

/** Comprova controles da jornada contextual em transações e MySQL reais, sem serviços pagos. */
@SpringBootTest(
    classes = VegaStyleOperationalControlsMysql57Test.LocalConfiguration.class,
    properties = {
      "spring.config.location=optional:classpath:vega380/no-production.properties",
      "spring.liquibase.change-log=classpath:vega-style/successor-changelog.yaml",
      "integrations.pde-platform.internal-token=vega-local-internal-only",
      "logging.level.root=WARN",
      "spring.test.mockmvc.print=none"
    })
@AutoConfigureMockMvc
@ActiveProfiles("vega-style-local")
@EnabledIfEnvironmentVariable(
    named = "VEGA_TEST_DB_HOST",
    matches = "127\\.0\\.0\\.1|sandbox-docker")
class VegaStyleOperationalControlsMysql57Test {
  /** Carrega somente a jornada contextual e a persistência real da fixture isolada. */
  @Configuration(proxyBeanMethods = false)
  @Profile("vega-style-local")
  @Import(VegaStyleLocalApplication.class)
  static class LocalConfiguration {}

  private static final String BASE = "/api/pde/vega/style/v1";
  private static final String MISSION = "/access/missions/estilo-dia-1";
  private static final String VERSION = VegaStyleService.NEXT_VERSION;
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final List<Map<String, Object>> OBSERVATIONS = new CopyOnWriteArrayList<>();
  private static String databaseVersion;
  @Autowired private MockMvc http;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ProductRepository products;

  /** Restringe toda a execução ao schema e à engine efêmeros da sandbox. */
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String host = System.getenv("VEGA_TEST_DB_HOST");
    String port = System.getenv().getOrDefault("VEGA_TEST_DB_PORT", "18326");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host)
        || !Set.of("3306", "18326").contains(port))
      throw new IllegalArgumentException("A prova exige banco local.");
    properties.add(
        "spring.datasource.url",
        () -> "jdbc:mysql://" + host + ":" + port + "/vega_local?useSSL=false&serverTimezone=UTC");
    properties.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("VEGA_TEST_DB_USER", "vega"));
    properties.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("VEGA_TEST_DB_PASSWORD", "vega-local-only"));
  }

  /** Mantém duas identidades próprias, com ciclos reais e somente cadastro externo simulado. */
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
                      .internalName("Fixture contextual " + id)
                      .build()));
      jdbc.update(
          "INSERT INTO learning_sales_cycle_v1 SELECT ?,?,chain_definition_id,chain_code,process_definition_id,?,NULL,?,creation_json,brief_json,inherited_learning_json,stage,'OPEN',?,budget_limit_brl,window_start,window_end,NULL,NULL,NULL,NULL,revision,baseline,created_at,updated_at,version_changed_at,NULL FROM learning_sales_cycle_v1 WHERE id=91003",
          cycle(id),
          id,
          id + 81000,
          UUID.randomUUID().toString(),
          VERSION);
    }
  }

  /** Remove exclusivamente dados sintéticos criados por esta classe. */
  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM vega_private_session_v1 WHERE cycle_id IN (171004,177004)");
    jdbc.update("DELETE FROM learning_sales_cycle_v1 WHERE id IN (171004,177004)");
  }

  /** A disputa pelo ciclo não cria capacidade acima das dezoito sessões contratadas. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void concurrentSessionsRespectLimit(long productId) throws Exception {
    List<Integer> statuses =
        parallel(24, () -> createResponse(productId, VERSION).getResponse().getStatus());
    assertThat(Collections.frequency(statuses, 200)).isEqualTo(18);
    assertThat(Collections.frequency(statuses, 409)).isEqualTo(6);
    assertThat(countSessions(productId)).isEqualTo(18);
    jdbc.update("UPDATE vega_private_session_v1 SET revoked=1 WHERE cycle_id=?", cycle(productId));
    assertThat(createResponse(productId, VERSION).getResponse().getStatus()).isEqualTo(409);
    record(
        "SESSION_LIMIT",
        productId,
        "concurrentSessionsRespectLimit",
        Map.of(
            "requests",
            24,
            "accepted",
            18,
            "rejected409",
            6,
            "persistedSessions",
            18,
            "revocationDoesNotRestoreCapacity",
            true));
  }

  /** Gerações simultâneas com a mesma chave persistem uma resposta e uma única operação. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void concurrentGenerationAndReplayConsumeOnce(long productId) throws Exception {
    JsonNode session = started(productId);
    Map<String, Object> request = generation("operation-shared-0001", VERSION);
    List<JsonNode> results =
        parallel(8, () -> post(MISSION + "/ai-guidance", request, token(session), 200));
    assertThat(new HashSet<>(results)).hasSize(1);
    String before = persisted(session);
    assertThat(post(MISSION + "/ai-guidance", request, token(session), 200))
        .isEqualTo(results.getFirst());
    var divergent = new LinkedHashMap<>(request);
    divergent.put(
        "answers",
        Map.of(
            "occasion",
            "videochamada",
            "existingSelection",
            pieces(),
            "stylePreference",
            "Discrição"));
    post(MISSION + "/ai-guidance", divergent, token(session), 409);
    assertThat(persisted(session)).isEqualTo(before);
    assertThat(report(session).path("operations").size()).isEqualTo(1);
    for (String code : List.of("CONCURRENT_SINGLE_CONSUMPTION", "IDEMPOTENT_REQUEST"))
      record(
          code,
          productId,
          "concurrentGenerationAndReplayConsumeOnce",
          Map.of(
              "concurrentRequests",
              8,
              "distinctResults",
              1,
              "persistedOperations",
              1,
              "divergentInputRejected",
              true,
              "replayPreservesState",
              true));
  }

  /** Duas operações por missão são o teto cumulativo, inclusive para entradas bloqueadas. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void distinctOperationsRespectLimit(long productId) throws Exception {
    JsonNode session = started(productId);
    var codes =
        parallel(
            8,
            () -> {
              String key = "operation-" + UUID.randomUUID();
              return send(MISSION + "/ai-guidance", generation(key, VERSION), token(session))
                  .getResponse()
                  .getStatus();
            });
    assertThat(Collections.frequency(codes, 200)).isEqualTo(2);
    assertThat(Collections.frequency(codes, 409)).isEqualTo(6);
    assertThat(report(session).path("operations").size()).isEqualTo(2);
    String before = persisted(session);
    post(
        MISSION + "/ai-guidance", generation("operation-third-0003", VERSION), token(session), 409);
    assertThat(persisted(session)).isEqualTo(before);
    record(
        "ATTEMPT_LIMIT",
        productId,
        "distinctOperationsRespectLimit",
        Map.of(
            "concurrentRequests",
            8,
            "accepted",
            2,
            "rejected409",
            6,
            "persistedOperations",
            2,
            "extraRequestPreservesState",
            true));
  }

  /** Conclusões concorrentes não duplicam missão, progresso, eventos nem processamento. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void completionIsIdempotent(long productId) throws Exception {
    JsonNode session = started(productId);
    post(
        MISSION + "/ai-guidance",
        generation("operation-complete-0001", VERSION),
        token(session),
        200);
    post(MISSION + "/interactions", Map.of("answers", answers()), token(session), 204);
    var codes =
        parallel(
            8,
            () -> send(MISSION + "/complete", Map.of(), token(session)).getResponse().getStatus());
    assertThat(Collections.frequency(codes, 204)).isEqualTo(8);
    String before = persisted(session);
    post(MISSION + "/complete", Map.of(), token(session), 204);
    assertThat(persisted(session)).isEqualTo(before);
    JsonNode workspace = get("/access/workspace", token(session), 200);
    assertThat(workspace.path("completedMissionIds").size()).isEqualTo(1);
    assertThat(workspace.path("progressPercent").asInt()).isEqualTo(14);
    assertThat(report(session).path("operations").size()).isEqualTo(1);
    record(
        "IDEMPOTENT_COMPLETION",
        productId,
        "completionIsIdempotent",
        Map.of(
            "concurrentCompletions",
            8,
            "completedMissions",
            1,
            "progressPercent",
            14,
            "persistedOperations",
            1,
            "reopenAndRepeatPreserveState",
            true));
  }

  /** Credenciais inválidas, vencidas e revogadas não leem nem alteram o agregado. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void invalidExpiredAndRevokedAccessCannotMutate(long productId) throws Exception {
    for (String mode : List.of("INVALID", "EXPIRED", "REVOKED")) {
      JsonNode session = started(productId);
      post(
          MISSION + "/ai-guidance",
          generation("operation-access-0001", VERSION),
          token(session),
          200);
      String access = token(session);
      if (mode.equals("INVALID")) access = "0".repeat(64);
      else
        post(
            "/internal/fixtures/" + (mode.equals("EXPIRED") ? "expire-session" : "revoke"),
            Map.of(),
            access,
            200);
      String before = persisted(session);
      get("/access/workspace", access, 401);
      get("/access/ai-guidance/operation-access-0001", access, 401);
      for (String suffix :
          List.of(
              "/start",
              MISSION + "/complete",
              MISSION + "/interactions",
              MISSION + "/ai-guidance",
              "/events")) {
        Object body =
            suffix.endsWith("ai-guidance")
                ? generation("operation-denied-0002", VERSION)
                : suffix.endsWith("interactions")
                    ? Map.of("answers", answers())
                    : suffix.equals("/events")
                        ? Map.of("eventType", "VALUE_MOMENT", "requestId", "operation-access-0001")
                        : Map.of();
        post(suffix, body, access, 401);
      }
      assertThat(persisted(session)).isEqualTo(before);
      record(
          mode + "_ACCESS_REJECTED",
          productId,
          "invalidExpiredAndRevokedAccessCannotMutate",
          Map.of(
              "readRequestsRejected",
              2,
              "mutationRequestsRejected",
              5,
              "persistedStateUnchanged",
              true));
    }
  }

  /** Contexto encerrado ou incompatível recusa novas mutações e preserva resultado histórico. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void closedOrIncompatibleContextCannotMutate(long productId) throws Exception {
    for (String mode : List.of("CLOSED", "INCOMPATIBLE")) {
      JsonNode session = started(productId);
      post(
          MISSION + "/ai-guidance",
          generation("operation-context-0001", VERSION),
          token(session),
          200);
      if (mode.equals("CLOSED"))
        jdbc.update(
            "UPDATE learning_sales_cycle_v1 SET status='CLOSED' WHERE id=?", cycle(productId));
      else
        jdbc.update(
            "UPDATE learning_sales_cycle_v1 SET product_version='incompatible-test-version' WHERE id=?",
            cycle(productId));
      String before = persisted(session);
      post("/start", Map.of(), token(session), 409);
      post(
          MISSION + "/ai-guidance",
          generation("operation-context-0002", VERSION),
          token(session),
          409);
      post(MISSION + "/interactions", Map.of("answers", answers()), token(session), 409);
      post(MISSION + "/complete", Map.of(), token(session), 409);
      post(
          "/events",
          Map.of("eventType", "VALUE_MOMENT", "requestId", "operation-context-0001"),
          token(session),
          409);
      post("/internal/fixtures/full-access", Map.of(), token(session), 409);
      assertThat(persisted(session)).isEqualTo(before);
      assertThat(
              get("/access/ai-guidance/operation-context-0001", token(session), 200)
                  .path("status")
                  .asText())
          .isEqualTo("COMPLETED");
      record(
          mode.equals("CLOSED")
              ? "CLOSED_CONTEXT_MUTATIONS_REJECTED"
              : "INCOMPATIBLE_CONTEXT_REJECTED",
          productId,
          "closedOrIncompatibleContextCannotMutate",
          Map.of(
              "mutationRequestsRejected",
              6,
              "historicalResultReadable",
              true,
              "persistedStateUnchanged",
              true));
      jdbc.update(
          "UPDATE learning_sales_cycle_v1 SET status='OPEN',product_version=? WHERE id=?",
          VERSION,
          cycle(productId));
    }
  }

  /** Outra sessão não recebe a orientação da titular nem altera seus registros. */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void sessionsRemainIsolated(long productId) throws Exception {
    JsonNode owner = started(productId);
    JsonNode other = started(productId);
    post(
        MISSION + "/ai-guidance",
        generation("operation-isolation-0001", VERSION),
        token(owner),
        200);
    String before = persisted(owner);
    get("/access/ai-guidance/operation-isolation-0001", token(other), 409);
    post(MISSION + "/interactions", Map.of("answers", answers()), token(other), 409);
    post(MISSION + "/complete", Map.of(), token(other), 409);
    assertThat(persisted(owner)).isEqualTo(before);
    assertThat(report(other).path("operations").isEmpty()).isTrue();
    record(
        "CROSS_SESSION_ISOLATION",
        productId,
        "sessionsRemainIsolated",
        Map.of(
            "crossSessionReadRejected",
            true,
            "crossSessionMutationsRejected",
            2,
            "ownerStateUnchanged",
            true));
  }

  /** A versão anterior conserva o comentário obrigatório e sua conclusão antes válida. */
  @Test
  void previousVersionRemainsValid() throws Exception {
    jdbc.update(
        "UPDATE learning_sales_cycle_v1 SET product_version=? WHERE id=?",
        VegaStyleService.VERSION,
        cycle(91004));
    JsonNode session =
        JSON.readTree(
            createResponse(91004, VegaStyleService.VERSION).getResponse().getContentAsString());
    post("/start", Map.of(), token(session), 200);
    post(
        MISSION + "/ai-guidance",
        generation("operation-previous-0001", VegaStyleService.VERSION),
        token(session),
        200);
    post(MISSION + "/complete", Map.of(), token(session), 409);
    var prior = new LinkedHashMap<>(answers());
    prior.put("reflection", "Registro sintético da versão anterior");
    post(MISSION + "/interactions", Map.of("answers", prior), token(session), 204);
    post(MISSION + "/complete", Map.of(), token(session), 204);
    assertThat(get("/access/workspace", token(session), 200).path("progressPercent").asInt())
        .isEqualTo(14);
  }

  /** Escreve somente observações executadas, sem credenciais ou aprovação independente. */
  @AfterAll
  static void receipt() throws Exception {
    String target = System.getenv("VEGA_OPERATIONAL_REPORT");
    if (target == null || target.isBlank()) return;
    Path path = Path.of(target);
    Files.createDirectories(path.toAbsolutePath().getParent());
    var result = new LinkedHashMap<String, Object>();
    result.put("origin", "LOCAL_MYSQL57_WITH_CONTEXT_TEST_DOUBLES");
    result.put("databaseVersion", databaseVersion);
    result.put("generatedAt", Instant.now().toString());
    result.put("prototypeVersion", VERSION);
    result.put("providerCalls", 0);
    result.put("commercialSideEffects", false);
    result.put("observations", OBSERVATIONS);
    Files.writeString(path, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result));
  }

  /** Deriva o identificador exclusivo do ciclo sintético. */
  private static long cycle(long productId) {
    return productId + 80000;
  }

  /** Informa somente peças sintéticas explicitamente disponíveis. */
  private static String pieces() {
    return "camisa, lenço e bolsa; posso usar lenço no pescoço ou na alça";
  }

  /** Monta escolhas reconhecidas pela regra contextual, com comentário opcional vazio. */
  private static Map<String, String> answers() {
    return Map.of(
        "occasion",
        "almoço",
        "existingSelection",
        pieces(),
        "stylePreference",
        "Cor e criatividade",
        "reflection",
        "");
  }

  /** Monta o contrato de uma operação sem provedor externo. */
  private static Map<String, Object> generation(String key, String version) {
    return Map.of("experienceVersion", version, "operationKey", key, "answers", answers());
  }

  /** Usa a credencial sintética apenas no transporte local. */
  private static String token(JsonNode session) {
    return session.path("sessionToken").asText();
  }

  /** Obtém a quantidade persistida sem pós-processamento da aplicação. */
  private int countSessions(long productId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM vega_private_session_v1 WHERE cycle_id=?",
        Integer.class,
        cycle(productId));
  }

  /** Compara todo o agregado funcional preservado após recusas ou replay. */
  private String persisted(JsonNode session) {
    return jdbc.queryForObject(
        "SELECT CONCAT_WS('|',state,input_json,events_json,COALESCE(CAST(consented_at AS CHAR),'')) FROM vega_private_session_v1 WHERE id=?",
        String.class,
        session.path("id").asText());
  }

  /** Solicita uma sessão pelo controller real, mantendo os cadastros no banco local. */
  private MvcResult createResponse(long productId, String version) throws Exception {
    return send(
        "/internal/sessions",
        Map.of("cycleId", cycle(productId), "prototypeVersion", version),
        null);
  }

  /** Cria e inicia a sessão pelo contrato oficial. */
  private JsonNode started(long productId) throws Exception {
    var response = createResponse(productId, VERSION);
    assertThat(response.getResponse().getStatus()).isEqualTo(200);
    JsonNode session = JSON.readTree(response.getResponse().getContentAsString());
    post("/start", Map.of(), token(session), 200);
    return session;
  }

  /** Consulta a prova persistida sem incluir a credencial no recibo. */
  private JsonNode report(JsonNode session) throws Exception {
    return get("/internal/report", token(session), 200);
  }

  /** Executa leitura autenticada pela interface real e confere o resultado. */
  private JsonNode get(String suffix, String token, int status) throws Exception {
    var response =
        http.perform(
                MockMvcRequestBuilders.get(BASE + suffix)
                    .header("X-Vega-Style-Session", token)
                    .header("X-PDE-Internal-Token", "vega-local-internal-only"))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).as(suffix).isEqualTo(status);
    return response.getContentAsString().isBlank()
        ? JSON.nullNode()
        : JSON.readTree(response.getContentAsString());
  }

  /** Executa um comando pela interface real, sem confirmar eventos comerciais. */
  private MvcResult send(String suffix, Object body, String token) throws Exception {
    var request =
        MockMvcRequestBuilders.post(BASE + suffix)
            .contentType("application/json")
            .header("X-PDE-Internal-Token", "vega-local-internal-only")
            .content(JSON.writeValueAsString(body));
    if (token != null) request.header("X-Vega-Style-Session", token);
    return http.perform(request).andReturn();
  }

  /** Confere status e extrai somente o JSON funcional do comando. */
  private JsonNode post(String suffix, Object body, String token, int status) throws Exception {
    var response = send(suffix, body, token).getResponse();
    assertThat(response.getStatus()).as(suffix).isEqualTo(status);
    return response.getContentAsString().isBlank()
        ? JSON.nullNode()
        : JSON.readTree(response.getContentAsString());
  }

  /** Disputa transações em conexões independentes com limite determinístico de espera. */
  private <T> List<T> parallel(int count, Callable<T> operation) throws Exception {
    try (var executor = Executors.newFixedThreadPool(8)) {
      var futures = new ArrayList<Future<T>>();
      for (int i = 0; i < count; i++) futures.add(executor.submit(operation));
      var results = new ArrayList<T>();
      for (var future : futures) results.add(future.get(30, TimeUnit.SECONDS));
      return results;
    }
  }

  /** Vincula a observação aprovada ao método e à identidade realmente exercitados. */
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
            "prototypeVersion",
            VERSION,
            "testMethod",
            method,
            "status",
            "PASS",
            "observed",
            observed));
  }
}
