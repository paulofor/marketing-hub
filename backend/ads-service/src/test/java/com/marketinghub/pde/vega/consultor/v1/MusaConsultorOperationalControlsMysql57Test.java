package com.marketinghub.pde.vega.consultor.v1;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.vega.consultor.v1.service.MusaConsultorService;
import com.marketinghub.pde.vega.style.v1.VegaStyleLocalApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.web.server.ResponseStatusException;

/** Comprova os controles contínuos em MySQL real e emite observações sem credenciais. */
@SpringBootTest(
    classes = MusaConsultorOperationalControlsMysql57Test.LocalConfiguration.class,
    properties = {
      "spring.config.location=optional:classpath:vega380/no-production.properties",
      "spring.liquibase.change-log=classpath:vega-style/successor-changelog.yaml",
      "integrations.pde-platform.internal-token=vega-local-internal-only",
      "logging.level.root=WARN"
    })
@ActiveProfiles("vega-style-local")
@EnabledIfEnvironmentVariable(
    named = "VEGA_TEST_DB_HOST",
    matches = "127\\.0\\.0\\.1|sandbox-docker")
@lombok.extern.slf4j.Slf4j
class MusaConsultorOperationalControlsMysql57Test {
  /** Mantém somente a aplicação e os cadastros locais já existentes. */
  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @org.springframework.context.annotation.Import(VegaStyleLocalApplication.class)
  static class LocalConfiguration {}

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final List<Map<String, Object>> RESULTS = new CopyOnWriteArrayList<>();
  @Autowired private MusaConsultorService service;
  @Autowired private JdbcTemplate jdbc;

  /** Restringe esta prova ao banco efêmero da sandbox. */
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String host = System.getenv("VEGA_TEST_DB_HOST");
    String port = System.getenv().getOrDefault("VEGA_TEST_DB_PORT", "18326");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host)
        || !Set.of("3306", "18326").contains(port))
      throw new IllegalArgumentException("Banco externo proibido nesta prova.");
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

  /** Remove somente as identidades sintéticas reservadas por esta classe. */
  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM vega_private_session_v1 WHERE cycle_id IN (98122,98222)");
    jdbc.update("DELETE FROM learning_sales_cycle_v1 WHERE id IN (98122,98222)");
  }

  /** Registra os resultados realmente observados; cobertura parcial não recebe status global. */
  @AfterAll
  static void receipt() throws Exception {
    Path output =
        Path.of(System.getenv().getOrDefault("MUSA_CONTROL_EVIDENCE", "target/musa-controls.json"));
    Files.createDirectories(output.toAbsolutePath().getParent());
    Files.writeString(
        output,
        JSON.writerWithDefaultPrettyPrinter()
            .writeValueAsString(
                Map.of(
                    "contractVersion",
                    "MUSA_CONTINUOUS_CONTROLS_RECEIPT_V1",
                    "generatedAt",
                    Instant.now().toString(),
                    "origin",
                    "LOCAL_MYSQL57_WITH_CONTEXT_TEST_DOUBLES",
                    "providerCalls",
                    0,
                    "results",
                    RESULTS)));
  }

  /** Geração e salvamento concorrentes mantêm uma única saída e uma única escolha. */
  @ParameterizedTest
  @ValueSource(longs = {98122, 98222})
  void concurrentSingleConsumption(long cycle) throws Exception {
    var access = access(cycle);
    var request = request("concurrent-operation-0001", 0, "Cor e criatividade");
    var generated = parallel(6, () -> service.command(access.token(), "generate", request, false));
    assertThat(generated).allMatch(value -> value.equals(generated.getFirst()));
    var saved =
        parallel(
            6,
            () ->
                service.command(
                    access.token(),
                    "save",
                    JSON.valueToTree(
                        Map.of("operationId", "concurrent-operation-0001", "revision", 1)),
                    false));
    assertThat(saved).allMatch(value -> value.equals(saved.getFirst()));
    var state = service.report(access.token()).path("execution");
    assertThat(state.path("operations").size()).isEqualTo(1);
    assertThat(state.path("choices").size()).isEqualTo(1);
    assertThat(state.path("revision").asInt()).isEqualTo(2);
    var observed =
        Map.of(
            "requests",
            6,
            "operations",
            1,
            "choices",
            1,
            "revision",
            2,
            "identicalResponses",
            true);
    record("CONCURRENT_SINGLE_CONSUMPTION", cycle, "concurrentSingleConsumption", observed);
    record("IDEMPOTENT_REQUEST", cycle, "concurrentSingleConsumption", observed);
    record("IDEMPOTENT_COMPLETION", cycle, "concurrentSingleConsumption", observed);
  }

  /** Recusa conflito de chave e revisão antiga e aplica preferência revisada à nova ocasião. */
  @ParameterizedTest
  @ValueSource(longs = {98122, 98222})
  void preservesConflictsAndRevisedPreference(long cycle) {
    var access = access(cycle);
    var initial = request("preference-operation-0001", 0, "Cor e criatividade");
    ((com.fasterxml.jackson.databind.node.ObjectNode) initial.path("answers"))
        .put("occasion", "videochamada");
    var first = service.command(access.token(), "generate", initial, false);
    service.command(
        access.token(),
        "save",
        JSON.valueToTree(Map.of("operationId", "preference-operation-0001", "revision", 1)),
        false);
    var conflict = initial.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) conflict.path("answers"))
        .put("occasion", "trabalho");
    String keyConflict =
        denied(() -> service.command(access.token(), "generate", conflict, false), 409);
    String revisionConflict =
        denied(
            () ->
                service.command(
                    access.token(),
                    "draft",
                    JSON.valueToTree(
                        Map.of("revision", 0, "answers", Map.of("occasion", "trabalho"))),
                    false),
            409);
    service.command(
        access.token(),
        "draft",
        JSON.valueToTree(
            Map.of("revision", 2, "answers", Map.of("stylePreference", "Conforto e praticidade"))),
        false);
    var next =
        service.command(
            access.token(),
            "generate",
            JSON.valueToTree(
                Map.of(
                    "operationKey",
                    "preference-operation-0002",
                    "revision",
                    3,
                    "parentChoiceId",
                    "preference-operation-0001",
                    "answers",
                    Map.of("occasion", "videochamada"))),
            false);
    assertThat(next.path("answers").path("stylePreference").asText())
        .isEqualTo("Conforto e praticidade");
    assertThat(next.path("action").asText()).isNotEqualTo(first.path("action").asText());
    assertThat(
            service
                .workspace(access.token())
                .path("choices")
                .path("preference-operation-0001")
                .path("answers")
                .path("stylePreference")
                .asText())
        .isEqualTo("Cor e criatividade");
    record(
        "IDEMPOTENCY_CONFLICT",
        cycle,
        "preservesConflictsAndRevisedPreference",
        Map.of("httpStatus", 409, "message", keyConflict));
    record(
        "REVISION_CONFLICT",
        cycle,
        "preservesConflictsAndRevisedPreference",
        Map.of("httpStatus", 409, "message", revisionConflict));
    record(
        "PREFERENCE_REVISION_EFFECT",
        cycle,
        "preservesConflictsAndRevisedPreference",
        Map.of(
            "previousPreference",
            "Cor e criatividade",
            "currentPreference",
            next.path("answers").path("stylePreference").asText(),
            "previousAction",
            first.path("action").asText(),
            "currentAction",
            next.path("action").asText(),
            "originalPreserved",
            true));
  }

  /** A reserva sob lock não emite mais acessos que o limite mesmo em concorrência. */
  @ParameterizedTest
  @ValueSource(longs = {98122, 98222})
  void concurrentSessionLimit(long cycle) throws Exception {
    cycle(cycle);
    var results =
        parallel(
            20,
            () -> {
              try {
                service.issue(cycle);
                return "CREATED";
              } catch (ResponseStatusException ex) {
                assertThat(ex.getStatusCode().value()).isEqualTo(409);
                assertThat(ex.getReason()).contains("Limite técnico");
                return "LIMIT_REJECTED";
              }
            });
    assertThat(Collections.frequency(results, "CREATED")).isEqualTo(18);
    assertThat(Collections.frequency(results, "LIMIT_REJECTED")).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM vega_private_session_v1 WHERE cycle_id=?",
                Integer.class,
                cycle))
        .isEqualTo(18);
    record(
        "SESSION_LIMIT",
        cycle,
        "concurrentSessionLimit",
        Map.of("requests", 20, "created", 18, "rejected", 2, "persisted", 18));
  }

  /** O teto global da identidade recusa nova geração e ainda permite recuperar a existente. */
  @ParameterizedTest
  @ValueSource(longs = {98122, 98222})
  void globalOperationLimit(long cycle) {
    var access = access(cycle);
    for (int i = 0; i < 24; i++)
      service.command(
          access.token(),
          "generate",
          request("limited-operation-" + String.format("%04d", i), i, "Cor e criatividade"),
          false);
    String message =
        denied(
            () ->
                service.command(
                    access.token(),
                    "generate",
                    request("limited-operation-0024", 24, "Cor e criatividade"),
                    false),
            409);
    assertThat(service.operation(access.token(), "limited-operation-0000").path("status").asText())
        .isEqualTo("COMPLETED");
    assertThat(service.report(access.token()).path("execution").path("operations").size())
        .isEqualTo(24);
    record(
        "ATTEMPT_LIMIT",
        cycle,
        "globalOperationLimit",
        Map.of(
            "persisted",
            24,
            "rejectedAttempt",
            25,
            "httpStatus",
            409,
            "message",
            message,
            "recoveryAvailable",
            true));
  }

  /** Inválido, expirado e revogado não leem nem alteram a sessão preservada. */
  @ParameterizedTest
  @ValueSource(longs = {98122, 98222})
  void accessBoundaries(long cycle) {
    var access = access(cycle);
    record(
        "INVALID_ACCESS_REJECTED",
        cycle,
        "accessBoundaries",
        Map.of("httpStatus", 401, "message", denied(() -> service.workspace("invalid"), 401)));
    jdbc.update(
        "UPDATE vega_private_session_v1 SET expires_at=? WHERE id=?",
        java.sql.Timestamp.from(Instant.now().minusSeconds(60)),
        access.id());
    record(
        "EXPIRED_ACCESS_REJECTED",
        cycle,
        "accessBoundaries",
        Map.of("httpStatus", 401, "message", denied(() -> service.workspace(access.token()), 401)));
    var renewed = service.renew(access.id());
    assertThat(Instant.parse(renewed.path("expiresAt").asText()))
        .isBetween(
            Instant.now().plus(89, ChronoUnit.DAYS), Instant.now().plus(91, ChronoUnit.DAYS));
    jdbc.update("UPDATE vega_private_session_v1 SET revoked=1 WHERE id=?", access.id());
    record(
        "REVOKED_ACCESS_REJECTED",
        cycle,
        "accessBoundaries",
        Map.of("httpStatus", 401, "message", denied(() -> service.workspace(access.token()), 401)));
    denied(() -> service.renew(access.id()), 401);
  }

  /** Encerramento e mudança de contexto bloqueiam mutações sem apagar a escolha salva. */
  @ParameterizedTest
  @ValueSource(longs = {98122, 98222})
  void closedAndIncompatibleContext(long cycle) {
    var access = access(cycle);
    service.command(
        access.token(),
        "generate",
        request("preserved-operation-0001", 0, "Cor e criatividade"),
        false);
    service.command(
        access.token(),
        "save",
        JSON.valueToTree(Map.of("operationId", "preserved-operation-0001", "revision", 1)),
        false);
    jdbc.update("UPDATE learning_sales_cycle_v1 SET product_id=product_id+100 WHERE id=?", cycle);
    String incompatible = denied(() -> service.workspace(access.token()), 409);
    record(
        "INCOMPATIBLE_CONTEXT_REJECTED",
        cycle,
        "closedAndIncompatibleContext",
        Map.of("httpStatus", 409, "message", incompatible));
    jdbc.update("UPDATE learning_sales_cycle_v1 SET product_id=? WHERE id=?", cycle + 1, cycle);
    service.close(cycle);
    String closed =
        denied(
            () ->
                service.command(
                    access.token(),
                    "draft",
                    JSON.valueToTree(
                        Map.of("revision", 2, "answers", Map.of("occasion", "trabalho"))),
                    false),
            401);
    denied(() -> service.issue(cycle), 409);
    var stored = stored(access.id());
    assertThat(stored.path("homologationClosed").asBoolean()).isTrue();
    assertThat(stored.path("choices").size()).isEqualTo(1);
    record(
        "CLOSED_CONTEXT_MUTATIONS_REJECTED",
        cycle,
        "closedAndIncompatibleContext",
        Map.of(
            "httpStatus", 401, "message", closed, "savedChoices", 1, "homologationClosed", true));
  }

  /** Outra identidade não lê operação ou escolha mesmo conhecendo seu identificador. */
  @ParameterizedTest
  @ValueSource(longs = {98122, 98222})
  void crossSessionIsolation(long cycle) {
    var owner = access(cycle);
    service.command(
        owner.token(),
        "generate",
        request("isolated-operation-0001", 0, "Cor e criatividade"),
        false);
    service.command(
        owner.token(),
        "save",
        JSON.valueToTree(Map.of("operationId", "isolated-operation-0001", "revision", 1)),
        false);
    var issued = service.issue(cycle);
    String stranger =
        service.login(issued.path("accessKey").asText()).path("sessionToken").asText();
    service.command(stranger, "consent", JSON.valueToTree(Map.of("accepted", true)), false);
    String message = denied(() -> service.operation(stranger, "isolated-operation-0001"), 409);
    assertThat(service.workspace(stranger).path("choices").size()).isZero();
    denied(
        () ->
            service.command(
                stranger,
                "generate",
                JSON.valueToTree(
                    Map.of(
                        "operationKey",
                        "isolated-operation-0002",
                        "revision",
                        0,
                        "parentChoiceId",
                        "isolated-operation-0001",
                        "answers",
                        Map.of("occasion", "trabalho"))),
                false),
        409);
    record(
        "CROSS_SESSION_ISOLATION",
        cycle,
        "crossSessionIsolation",
        Map.of("httpStatus", 409, "message", message, "strangerChoices", 0, "ownerChoices", 1));
  }

  /** Cria um ciclo independente sem reutilizar identidades ou autorização produtivas. */
  private void cycle(long id) {
    jdbc.update(
        "INSERT INTO learning_sales_cycle_v1 (id,product_id,chain_definition_id,chain_code,process_definition_id,experiment_id,request_key,creation_json,brief_json,inherited_learning_json,stage,status,product_version,budget_limit_brl,window_start,window_end,revision,baseline,created_at,updated_at,version_changed_at) SELECT ?,?,chain_definition_id,chain_code,process_definition_id,?,?,creation_json,brief_json,inherited_learning_json,'ADJUSTMENT','OPEN',?,0,window_start,window_end,0,0,NOW(),NOW(),NOW() FROM learning_sales_cycle_v1 WHERE id=91003",
        id,
        id + 1,
        id + 2,
        UUID.randomUUID().toString(),
        MusaConsultorService.VERSION);
  }

  /** Emite e autentica uma identidade sintética sem incluir a chave no recibo. */
  private Access access(long cycle) {
    cycle(cycle);
    var issued = service.issue(cycle);
    String token = service.login(issued.path("accessKey").asText()).path("sessionToken").asText();
    service.command(token, "consent", JSON.valueToTree(Map.of("accepted", true)), false);
    return new Access(issued.path("sessionId").asText(), token);
  }

  /** Monta entradas funcionais reais com chave e revisão controladas. */
  private JsonNode request(String key, int revision, String preference) {
    return JSON.valueToTree(
        Map.of(
            "operationKey",
            key,
            "revision",
            revision,
            "answers",
            Map.of(
                "occasion",
                "almoço",
                "existingSelection",
                "Camisa, calça, lenço e bolsa. Posso usar o lenço no pescoço ou na alça.",
                "stylePreference",
                preference)));
  }

  /** Confere o status real da recusa e retorna somente a mensagem pública. */
  private String denied(Callable<?> action, int status) {
    var failure = catchThrowable(action::call);
    assertThat(failure).isInstanceOf(ResponseStatusException.class);
    var response = (ResponseStatusException) failure;
    assertThat(response.getStatusCode().value()).isEqualTo(status);
    return response.getReason();
  }

  /** Disputa a operação real em threads independentes, preservando todas as respostas. */
  private <T> List<T> parallel(int count, Callable<T> action) throws Exception {
    try (var executor = Executors.newFixedThreadPool(Math.min(count, 6))) {
      var futures = executor.invokeAll(Collections.nCopies(count, action));
      var results = new ArrayList<T>();
      for (var future : futures) results.add(future.get(30, TimeUnit.SECONDS));
      return results;
    }
  }

  /** Lê a persistência local para comprovar preservação após a revogação. */
  private JsonNode stored(String id) {
    try {
      return JSON.readTree(
          jdbc.queryForObject(
              "SELECT input_json FROM vega_private_session_v1 WHERE id=?", String.class, id));
    } catch (Exception ex) {
      log.error("Falha ao ler prova local de preservação sessionId={}", id, ex);
      throw new AssertionError(
          "Não foi possível conferir a prova persistida da sessão sintética.", ex);
    }
  }

  /** Anexa observação somente após as asserções, sem credenciais, pessoas ou custo comercial. */
  private void record(String code, long cycle, String method, Map<String, ?> observed) {
    RESULTS.add(
        Map.of(
            "code",
            code,
            "testMethod",
            method,
            "productId",
            cycle + 1,
            "cycleId",
            cycle,
            "experimentId",
            cycle + 2,
            "status",
            "PASS",
            "observed",
            observed,
            "databaseVersion",
            jdbc.queryForObject("SELECT VERSION()", String.class)));
  }

  /** Transporta credenciais sintéticas apenas em memória durante o teste. */
  private record Access(String id, String token) {}
}
