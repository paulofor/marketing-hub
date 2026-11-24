package com.marketinghub.pde.vega.consultor.v1;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.marketinghub.pde.vega.consultor.v1.service.MusaConsultorService;
import com.marketinghub.pde.vega.style.v1.VegaStyleLocalApplication;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;

/** Valida o contrato de acesso com HTTP, controller, transações e MySQL reais isolados. */
@SpringBootTest(
    classes = MusaHarnessAccessMysql57Test.LocalConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.config.location=optional:classpath:vega380/no-production.properties",
      "spring.liquibase.change-log=classpath:vega-style/successor-changelog.yaml",
      "integrations.pde-platform.internal-token=vega-local-internal-only",
      "spring.mvc.problemdetails.enabled=true",
      "logging.level.root=WARN"
    })
@ActiveProfiles("vega-style-local")
@EnabledIfEnvironmentVariable(
    named = "VEGA_TEST_DB_HOST",
    matches = "127\\.0\\.0\\.1|sandbox-docker")
class MusaHarnessAccessMysql57Test {
  /** Importa somente a aplicação local com repositories oficiais. */
  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @org.springframework.context.annotation.Import(VegaStyleLocalApplication.class)
  static class LocalConfiguration {}

  @Autowired JdbcTemplate jdbc;
  @Autowired MusaConsultorService service;
  @LocalServerPort int port;
  final ObjectMapper json = new ObjectMapper();
  final HttpClient client = HttpClient.newHttpClient();
  static final String BASE = "/api/pde/vega/consultor/v1";

  /** Impede uso de banco externo ou schema produtivo nesta regressão. */
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String host = System.getenv("VEGA_TEST_DB_HOST");
    String port = System.getenv().getOrDefault("VEGA_TEST_DB_PORT", "18326");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host)
        || !Set.of("18326", "3306").contains(port))
      throw new IllegalArgumentException("Banco externo proibido");
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

  /** Limpa exclusivamente as identidades reservadas pela suíte sintética. */
  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM vega_private_session_v1 WHERE cycle_id IN (94022,95022)");
    jdbc.update("DELETE FROM learning_sales_cycle_v1 WHERE id IN (94022,95022)");
  }

  /** Reproduz o409 cumulativo e recupera no limite sem reutilizar dados nem ampliar validade. */
  @ParameterizedTest
  @ValueSource(longs = {94022, 95022})
  void recoversAtLimitWithIsolatedAttempts(long cycle) throws Exception {
    seed(cycle);
    for (int i = 0; i < 18; i++) service.issue(cycle);
    jdbc.update(
        "UPDATE vega_private_session_v1 SET input_json=JSON_SET(input_json, '$.draft.occasion', 'histórico sintético') WHERE cycle_id=?",
        cycle);
    var rejected = post("/internal/cycles/" + cycle + "/access", Map.of(), null, true);
    assertThat(rejected.statusCode()).isEqualTo(409);
    assertThat(rejected.body()).contains("Limite técnico de acessos atingido");
    var request = access(cycle, "ADHERENT", "DESKTOP_1440", "PRIMARY", "attempt-original-0001");
    var first = post("/internal/cycles/" + cycle + "/harness-access", request, null, true);
    assertThat(first.statusCode()).isEqualTo(200);
    assertThat(json.readTree(first.body()).path("reusedSession").asBoolean()).isTrue();
    String id = json.readTree(first.body()).path("sessionId").asText();
    String expires = json.readTree(first.body()).path("expiresAt").asText();
    var retry = post("/internal/cycles/" + cycle + "/harness-access", request, null, true);
    assertThat(json.readTree(retry.body()).path("sessionId").asText()).isEqualTo(id);
    assertThat(json.readTree(retry.body()).path("expiresAt").asText()).isEqualTo(expires);
    assertThat(count(cycle)).isEqualTo(18);
    String token = login(request);
    assertThat(command(token, "consent", Map.of("accepted", true)).statusCode()).isEqualTo(200);
    assertThat(
            command(token, "draft", Map.of("revision", 0, "answers", Map.of("occasion", "almoço")))
                .statusCode())
        .isEqualTo(200);
    var next = new LinkedHashMap<>(request);
    next.put("attemptId", "attempt-next-0000002");
    next.put("recoveryKey", "b".repeat(64));
    assertThat(post("/internal/cycles/" + cycle + "/harness-access", next, null, true).statusCode())
        .isEqualTo(409);
    assertThat(post("/internal/harness-access/finish", Map.of(), token, true).statusCode())
        .isEqualTo(200);
    assertThat(post("/internal/harness-access/finish", Map.of(), token, true).statusCode())
        .isEqualTo(200);
    assertThat(post("/internal/cycles/" + cycle + "/harness-access", next, null, true).statusCode())
        .isEqualTo(200);
    assertThat(get("/workspace", token).statusCode()).isEqualTo(401);
    String nextToken = login(next);
    var workspace = json.readTree(get("/workspace", nextToken).body());
    assertThat(workspace.path("draft").size()).isZero();
    assertThat(workspace.path("consented").asBoolean()).isFalse();
    assertThat(
            post("/internal/cycles/" + cycle + "/harness-access", request, null, true).statusCode())
        .isEqualTo(409);
    var stored =
        json.readTree(
            jdbc.queryForObject(
                "SELECT input_json FROM vega_private_session_v1 WHERE id=?", String.class, id));
    assertThat(
            stored
                .path("harnessAttempts")
                .path("attempt-original-0001")
                .path("draft")
                .path("occasion")
                .asText())
        .isEqualTo("almoço");
    assertThat(stored.path("draft").path("occasion").asText()).isEqualTo("histórico sintético");
    assertThat(stored.path("accessAudit").size()).isEqualTo(3);
    assertThat(count(cycle)).isEqualTo(18);
  }

  /** Concorrência e resposta perdida convergem na mesma identidade sem criar uma19ª sessão. */
  @Test
  void concurrentReservationAndRestartAreIdempotent() throws Exception {
    seed(94022);
    for (int i = 0; i < 18; i++) service.issue(94022L);
    var request = access(94022, "RECOVERY", "IPHONE_15_PRO", "PRIMARY", "attempt-concurrent-01");
    try (var pool = Executors.newFixedThreadPool(4)) {
      var futures = new ArrayList<Future<HttpResponse<String>>>();
      for (int i = 0; i < 4; i++)
        futures.add(
            pool.submit(() -> post("/internal/cycles/94022/harness-access", request, null, true)));
      var ids = new HashSet<String>();
      for (var f : futures) {
        var r = f.get(20, TimeUnit.SECONDS);
        assertThat(r.statusCode()).isEqualTo(200);
        ids.add(json.readTree(r.body()).path("sessionId").asText());
      }
      assertThat(ids).hasSize(1);
    }
    assertThat(count(94022)).isEqualTo(18);
    var wrong = new LinkedHashMap<>(request);
    wrong.put("recoveryKey", "f".repeat(64));
    assertThat(post("/internal/cycles/94022/harness-access", wrong, null, true).statusCode())
        .isEqualTo(409);
  }

  /** Contextos errados, expiração e encerramento não recebem acesso por recuperação. */
  @Test
  void failsClosedForWrongContextExpiryAndClose() throws Exception {
    seed(94022);
    seed(95022);
    var request = access(94022, "SAFETY", "PIXEL_7", "PRIMARY", "attempt-boundary-0001");
    for (String field :
        List.of("productId", "experimentId", "prototypeVersion", "sourceReference")) {
      var wrong = new LinkedHashMap<>(request);
      wrong.put(field, field.endsWith("Id") ? 1 : "wrong");
      assertThat(post("/internal/cycles/94022/harness-access", wrong, null, true).statusCode())
          .isEqualTo(409);
    }
    assertThat(post("/internal/cycles/95022/harness-access", request, null, true).statusCode())
        .isEqualTo(409);
    assertThat(post("/internal/cycles/94022/harness-access", request, null, false).statusCode())
        .isEqualTo(401);
    var r = post("/internal/cycles/94022/harness-access", request, null, true);
    assertThat(r.statusCode()).isEqualTo(200);
    String id = json.readTree(r.body()).path("sessionId").asText();
    jdbc.update(
        "UPDATE vega_private_session_v1 SET expires_at=? WHERE id=?",
        java.sql.Timestamp.from(Instant.now().minusSeconds(1)),
        id);
    assertThat(post("/internal/cycles/94022/harness-access", request, null, true).statusCode())
        .isEqualTo(401);
    service.close(94022L);
    assertThat(post("/internal/cycles/94022/harness-access", request, null, true).statusCode())
        .isEqualTo(409);
    jdbc.update("UPDATE learning_sales_cycle_v1 SET status='CLOSED' WHERE id=95022");
    assertThat(
            post(
                    "/internal/cycles/95022/harness-access",
                    access(95022, "ADHERENT", "DESKTOP_1440", "PRIMARY", "attempt-closed-00001"),
                    null,
                    true)
                .statusCode())
        .isEqualTo(409);
  }

  /** Operações históricas contam no teto24 mesmo com workspace novo e tentativa distinta. */
  @Test
  void cumulativeOperationLimitIncludesLegacyAndOtherAttempts() throws Exception {
    seed(94022);
    var issued = service.issue(94022L);
    String id = issued.path("sessionId").asText();
    var legacy = json.createObjectNode();
    for (int i = 0; i < 24; i++) legacy.put("historical-operation-" + i, "preservada");
    jdbc.update(
        "UPDATE vega_private_session_v1 SET input_json=JSON_SET(input_json, '$.operations', CAST(? AS JSON)) WHERE id=?",
        legacy.toString(),
        id);
    var request = access(94022, "ADHERENT", "DESKTOP_1440", "PRIMARY", "attempt-cumulative-01");
    assertThat(post("/internal/cycles/94022/harness-access", request, null, true).statusCode())
        .isEqualTo(200);
    String token = login(request);
    command(token, "consent", Map.of("accepted", true));
    var result =
        command(
            token,
            "generate",
            Map.of(
                "operationKey",
                "new-operation-blocked-001",
                "revision",
                0,
                "answers",
                Map.of(
                    "occasion",
                    "almoço",
                    "existingSelection",
                    "lenço e bolsa; uso na alça ou pescoço",
                    "stylePreference",
                    "Cor e criatividade")));
    assertThat(result.statusCode()).isEqualTo(409);
    assertThat(result.body()).contains("Limite técnico atingido");
    var preserved =
        json.readTree(
            jdbc.queryForObject(
                "SELECT input_json FROM vega_private_session_v1 WHERE id=?", String.class, id));
    assertThat(preserved.path("operations").size()).isEqualTo(24);
    assertThat(
            preserved
                .path("harnessAttempts")
                .path("attempt-cumulative-01")
                .path("operations")
                .size())
        .isZero();
  }

  /** A retomada usa a última operação confirmada, sem exibir cartão antigo para rascunho novo. */
  @Test
  void restoresCurrentResultAndSeparatesNewDraft() throws Exception {
    seed(94022);
    var request = access(94022, "RECOVERY", "IPHONE_15_PRO", "PRIMARY", "attempt-result-order1");
    assertThat(post("/internal/cycles/94022/harness-access", request, null, true).statusCode())
        .isEqualTo(200);
    String token = login(request);
    command(token, "consent", Map.of("accepted", true));
    var answers =
        Map.of(
            "occasion",
            "almoço",
            "existingSelection",
            "lenço e bolsa; uso na alça ou pescoço",
            "stylePreference",
            "Cor e criatividade");
    for (int i = 0; i < 2; i++) {
      var result =
          command(
              token,
              "generate",
              Map.of(
                  "operationKey",
                  i == 0 ? "zzzz-original-op-001" : "aaaa-current-op-002",
                  "revision",
                  i,
                  "answers",
                  answers));
      assertThat(result.statusCode()).isEqualTo(200);
    }
    assertThat(
            json.readTree(get("/workspace", token).body())
                .path("latestOperation")
                .path("operationId")
                .asText())
        .isEqualTo("aaaa-current-op-002");
    command(token, "draft", Map.of("revision", 2, "answers", Map.of("occasion", "nova ocasião")));
    assertThat(json.readTree(get("/workspace", token).body()).path("latestOperation").isNull())
        .isTrue();
  }

  /** Cria somente o ciclo sintético solicitado sobre a fixture de referência isolada. */
  void seed(long cycle) {
    jdbc.update(
        "INSERT INTO learning_sales_cycle_v1 (id,product_id,chain_definition_id,chain_code,process_definition_id,experiment_id,request_key,creation_json,brief_json,inherited_learning_json,stage,status,product_version,budget_limit_brl,window_start,window_end,revision,baseline,created_at,updated_at,version_changed_at) SELECT ?,?,chain_definition_id,chain_code,process_definition_id,?,?,creation_json,brief_json,inherited_learning_json,'ADJUSTMENT','OPEN',?,0,window_start,window_end,0,0,NOW(),NOW(),NOW() FROM learning_sales_cycle_v1 WHERE id=91003",
        cycle,
        cycle + 1,
        cycle + 2,
        UUID.randomUUID().toString(),
        MusaConsultorService.VERSION);
  }

  /** Monta o contrato com uma chave exclusivamente sintética. */
  Map<String, Object> access(
      long cycle, String scenario, String device, String role, String attempt) {
    return Map.of(
        "productId",
        cycle + 1,
        "experimentId",
        cycle + 2,
        "prototypeVersion",
        MusaConsultorService.VERSION,
        "sourceReference",
        "experiment:" + (cycle + 2),
        "scenarioCode",
        scenario,
        "deviceProfile",
        device,
        "role",
        role,
        "attemptId",
        attempt,
        "recoveryKey",
        "a".repeat(64));
  }

  /** Autentica o grant de teste e retorna somente ao escopo local do teste. */
  String login(Map<String, Object> request) throws Exception {
    var r = post("/login", Map.of("accessKey", request.get("recoveryKey")), null, false);
    assertThat(r.statusCode()).isEqualTo(200);
    return r.headers().firstValue("set-cookie").orElseThrow().split(";")[0].split("=")[1];
  }

  /** Envia um comando pelo controller real. */
  HttpResponse<String> command(String token, String action, Object payload) throws Exception {
    return post("/commands", Map.of("action", action, "payload", payload), token, false);
  }

  /** Conta as identidades físicas cumulativas, sem descontar tentativas antigas. */
  int count(long cycle) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM vega_private_session_v1 WHERE cycle_id=?", Integer.class, cycle);
  }

  /** Faz uma consulta HTTP local com a credencial de sessão sintética. */
  HttpResponse<String> get(String path, String token) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + BASE + path))
            .header("X-Musa-Session", token)
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  /** Mantém cabeçalhos e corpos de teste fora dos logs e respostas públicas. */
  HttpResponse<String> post(String path, Object body, String token, boolean privileged)
      throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + BASE + path))
            .header("Content-Type", "application/json");
    if (token != null) request.header("X-Musa-Session", token);
    if (privileged) request.header("X-PDE-Internal-Token", "vega-local-internal-only");
    return client.send(
        request.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
