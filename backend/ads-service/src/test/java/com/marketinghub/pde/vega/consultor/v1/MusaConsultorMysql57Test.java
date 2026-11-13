package com.marketinghub.pde.vega.consultor.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.vega.consultor.v1.service.MusaConsultorService;
import com.marketinghub.pde.vega.style.v1.VegaStyleLocalApplication;
import com.marketinghub.repository.jpa.vega.VegaPrivateSessionRepository;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Comprova preservação transacional de escolhas quando o encerramento disputa a mesma sessão. */
@SpringBootTest(
    classes = MusaConsultorMysql57Test.LocalConfiguration.class,
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
class MusaConsultorMysql57Test {
  /** Carrega somente a persistência e a jornada local, sem varrer o backend produtivo. */
  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @org.springframework.context.annotation.Import(VegaStyleLocalApplication.class)
  static class LocalConfiguration {}

  @Autowired private MusaConsultorService service;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactions;
  @SpyBean private VegaPrivateSessionRepository sessions;
  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita exclusivamente o banco efêmero da sandbox para esta prova concorrente. */
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String host = System.getenv("VEGA_TEST_DB_HOST");
    String port = System.getenv().getOrDefault("VEGA_TEST_DB_PORT", "18326");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host)
        || !Set.of("3306", "18326").contains(port))
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

  /** Remove somente os ciclos sintéticos reservados por esta classe. */
  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM vega_private_session_v1 WHERE cycle_id IN (91022,92022)");
    jdbc.update("DELETE FROM learning_sales_cycle_v1 WHERE id IN (91022,92022)");
  }

  /** Uma escolha confirmada antes da revogação permanece guardada em duas identidades distintas. */
  @ParameterizedTest
  @ValueSource(longs = {91022, 92022})
  void closingPreservesConcurrentConfirmedChoice(long cycleId) throws Exception {
    jdbc.update(
        "INSERT INTO learning_sales_cycle_v1 (id,product_id,chain_definition_id,chain_code,process_definition_id,experiment_id,request_key,creation_json,brief_json,inherited_learning_json,stage,status,product_version,budget_limit_brl,window_start,window_end,revision,baseline,created_at,updated_at,version_changed_at) SELECT ?,?,chain_definition_id,chain_code,process_definition_id,?, ?,creation_json,brief_json,inherited_learning_json,stage,'OPEN',?,0,window_start,window_end,0,0,NOW(),NOW(),NOW() FROM learning_sales_cycle_v1 WHERE id=91003",
        cycleId,
        cycleId + 1,
        cycleId + 2,
        UUID.randomUUID().toString(),
        MusaConsultorService.VERSION);
    var issued = service.issue(cycleId);
    String token = service.login(issued.path("accessKey").asText()).path("sessionToken").asText();
    service.command(token, "consent", json.valueToTree(Map.of("accepted", true)), false);
    var writerReady = new CountDownLatch(1);
    var oldUnlockedRead = new CountDownLatch(1);
    // A consulta sem lock reproduz a leitura antiga antes do commit do salvamento.
    // O encerramento corrigido usa a consulta bloqueada e não passa por esse ponto.
    doAnswer(
            call -> {
              var result =
                  entityManager
                      .createQuery(
                          "select s from VegaPrivateSession s where s.cycleId=:cycleId order by s.createdAt",
                          com.marketinghub.pde.vega.privateprototype.v1.VegaPrivateSession.class)
                      .setParameter("cycleId", cycleId)
                      .getResultList();
              oldUnlockedRead.countDown();
              return result;
            })
        .when(sessions)
        .findByCycleIdOrderByCreatedAtAsc(cycleId);
    var executor = Executors.newSingleThreadExecutor();
    try {
      var writer =
          executor.submit(
              () ->
                  new TransactionTemplate(transactions)
                      .execute(
                          status -> {
                            service.command(
                                token,
                                "generate",
                                json.valueToTree(
                                    Map.of(
                                        "operationKey",
                                        "preserved-operation-0001",
                                        "revision",
                                        0,
                                        "answers",
                                        Map.of(
                                            "occasion",
                                            "almoço",
                                            "existingSelection",
                                            "Camisa, lenço e bolsa. Posso usar o lenço no pescoço ou na alça.",
                                            "stylePreference",
                                            "Cor e criatividade"))),
                                false);
                            service.command(
                                token,
                                "save",
                                json.valueToTree(
                                    Map.of(
                                        "operationId", "preserved-operation-0001", "revision", 1)),
                                false);
                            writerReady.countDown();
                            try {
                              oldUnlockedRead.await(2, TimeUnit.SECONDS);
                            } catch (InterruptedException ex) {
                              Thread.currentThread().interrupt();
                              throw new IllegalStateException(ex);
                            }
                            return true;
                          }));
      assertThat(writerReady.await(10, TimeUnit.SECONDS)).isTrue();
      service.close(cycleId);
      assertThat(writer.get(10, TimeUnit.SECONDS)).isTrue();
      String stored =
          jdbc.queryForObject(
              "SELECT input_json FROM vega_private_session_v1 WHERE id=?",
              String.class,
              issued.path("sessionId").asText());
      var data = json.readTree(stored);
      assertThat(data.path("choices").has("preserved-operation-0001"))
          .as("A revogação deve preservar a escolha confirmada pela transação concorrente")
          .isTrue();
      assertThat(data.path("homologationClosed").asBoolean()).isTrue();
    } finally {
      executor.shutdownNow();
    }
  }
}
