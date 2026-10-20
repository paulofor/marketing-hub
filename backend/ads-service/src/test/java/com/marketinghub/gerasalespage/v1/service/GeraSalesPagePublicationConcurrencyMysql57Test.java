package com.marketinghub.gerasalespage.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reproduz a promoção de locks de FK e comprova a publicação concorrente em MySQL 5.7 isolado. */
@EnabledIfEnvironmentVariable(
    named = "MIRA_CONTROLS_DB_HOST",
    matches = "127\\.0\\.0\\.1|sandbox-docker")
class GeraSalesPagePublicationConcurrencyMysql57Test {
  private static final Logger log =
      LoggerFactory.getLogger(GeraSalesPagePublicationConcurrencyMysql57Test.class);

  /** Conecta somente ao schema efêmero permitido pelos testes privados e pelo CI. */
  private Connection connection() throws SQLException {
    String host = System.getenv("MIRA_CONTROLS_DB_HOST");
    String port = System.getenv().getOrDefault("MIRA_CONTROLS_DB_PORT", "57183");
    String schema = System.getenv().getOrDefault("MIRA_CONTROLS_DB_SCHEMA", "mira_controls_local");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host)
        || !Set.of("57183", "3306").contains(port)
        || !"mira_controls_local".equals(schema))
      throw new IllegalArgumentException("Banco externo proibido na regressão de concorrência.");
    return DriverManager.getConnection(
        "jdbc:mysql://" + host + ":" + port + "/" + schema + "?useSSL=false&serverTimezone=UTC",
        System.getenv().getOrDefault("MIRA_CONTROLS_DB_USER", "root"),
        System.getenv().getOrDefault("MIRA_CONTROLS_DB_PASSWORD", "local-locks-only"));
  }

  /**
   * Demonstra que duas gravações filhas seguidas de atualização do pai reproduzem o erro original.
   */
  @Test
  void previousOrderDeadlocksBeforeParentUpdate() throws Exception {
    fixture(false, 7, 105);
  }

  /** Preserva página e janela em duas identidades usando o guardião real antes dos filhos. */
  @ParameterizedTest
  @CsvSource({"7,105", "307,9105"})
  void publicationAndWindowCommitWithoutDeadlock(long productId, long experimentId)
      throws Exception {
    fixture(true, productId, experimentId);
  }

  /** Executa as duas transações e elimina apenas as tabelas sintéticas desta execução. */
  private void fixture(boolean guarded, long productId, long experimentId) throws Exception {
    String prefix = "gsp_" + UUID.randomUUID().toString().replace("-", "");
    String productsTable = prefix + "_p",
        experimentsTable = prefix + "_e",
        children = prefix + "_c";
    var pool = Executors.newFixedThreadPool(2);
    var bothInserted = new CyclicBarrier(2);
    try (var setup = connection();
        var sql = setup.createStatement()) {
      assertThat(setup.getMetaData().getDatabaseProductVersion()).startsWith("5.7.");
      sql.execute("CREATE TABLE " + productsTable + " (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
      sql.execute(
          "CREATE TABLE "
              + experimentsTable
              + " (id BIGINT PRIMARY KEY, product_id BIGINT NOT NULL, window_value INT NOT NULL DEFAULT 0, page_value INT NOT NULL DEFAULT 0, FOREIGN KEY(product_id) REFERENCES "
              + productsTable
              + "(id)) ENGINE=InnoDB");
      sql.execute(
          "CREATE TABLE "
              + children
              + " (id BIGINT AUTO_INCREMENT PRIMARY KEY, experiment_id BIGINT NOT NULL, FOREIGN KEY(experiment_id) REFERENCES "
              + experimentsTable
              + "(id)) ENGINE=InnoDB");
      sql.executeUpdate("INSERT INTO " + productsTable + " VALUES (" + productId + ")");
      sql.executeUpdate(
          "INSERT INTO "
              + experimentsTable
              + "(id, product_id) VALUES ("
              + experimentId
              + ","
              + productId
              + ")");
      List<Future<Boolean>> calls = new java.util.ArrayList<>();
      for (String field : List.of("window_value", "page_value"))
        calls.add(
            pool.submit(
                () -> {
                  try (var tx = connection()) {
                    tx.setAutoCommit(false);
                    if (guarded)
                      guard(tx, productsTable, experimentsTable, productId, experimentId)
                          .preparePublication(experimentId);
                    try (var statement = tx.createStatement()) {
                      statement.executeUpdate(
                          "INSERT INTO "
                              + children
                              + "(experiment_id) VALUES ("
                              + experimentId
                              + ")");
                      if (!guarded) bothInserted.await(10, TimeUnit.SECONDS);
                      statement.executeUpdate(
                          "UPDATE "
                              + experimentsTable
                              + " SET "
                              + field
                              + "=1 WHERE id="
                              + experimentId);
                    }
                    tx.commit();
                    return true;
                  } catch (SQLException ex) {
                    log.info(
                        "Regressão MySQL de publicação guarded={} experimentId={} field={}",
                        guarded,
                        experimentId,
                        field,
                        ex);
                    if (!guarded && ex.getErrorCode() == 1213) return false;
                    throw ex;
                  }
                }));
      int success = 0;
      for (var call : calls) if (call.get(20, TimeUnit.SECONDS)) success++;
      assertThat(success).isEqualTo(guarded ? 2 : 1);
      try (var rows =
          sql.executeQuery("SELECT window_value, page_value FROM " + experimentsTable)) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getInt(1) + rows.getInt(2)).isEqualTo(guarded ? 2 : 1);
      }
    } finally {
      pool.shutdownNow();
      try (var cleanup = connection();
          var sql = cleanup.createStatement()) {
        sql.execute("DROP TABLE IF EXISTS " + children);
        sql.execute("DROP TABLE IF EXISTS " + experimentsTable);
        sql.execute("DROP TABLE IF EXISTS " + productsTable);
      }
    }
  }

  /**
   * Adapta os repositories do guardião real a locks SQL efetivos sem carregar a aplicação
   * produtiva.
   */
  private GeraSalesPagePublicationAuditService guard(
      Connection tx,
      String productsTable,
      String experimentsTable,
      long productId,
      long experimentId)
      throws Exception {
    var experiments = mock(ExperimentRepository.class);
    var products = mock(ProductRepository.class);
    when(experiments.findProductIdForPublication(experimentId)).thenReturn(Optional.of(productId));
    when(products.findLockedById(anyLong()))
        .thenAnswer(
            call -> {
              try (var sql = tx.createStatement();
                  var rows =
                      sql.executeQuery(
                          "SELECT id FROM "
                              + productsTable
                              + " WHERE id="
                              + productId
                              + " FOR UPDATE")) {
                assertThat(rows.next()).isTrue();
              }
              return Optional.of(Product.builder().id(productId).build());
            });
    when(experiments.findForSalesPageRecovery(experimentId))
        .thenAnswer(
            call -> {
              try (var sql = tx.createStatement();
                  var rows =
                      sql.executeQuery(
                          "SELECT id FROM "
                              + experimentsTable
                              + " WHERE id="
                              + experimentId
                              + " FOR UPDATE")) {
                assertThat(rows.next()).isTrue();
              }
              return Optional.of(Experiment.builder().id(experimentId).build());
            });
    return new GeraSalesPagePublicationAuditService(
        experiments, products, null, null, null, null, null, null, null, new ObjectMapper());
  }

  /** Reproduz a leitura antiga que permanece invisível após a espera pelo lock do pai. */
  @Test
  void repeatableReadRejectsSecondIdenticalCallback() throws Exception {
    callbackFixture(Connection.TRANSACTION_REPEATABLE_READ, 7, 105, 1);
  }

  /** Confirma uma única publicação e custo sob callbacks duplicados em duas identidades. */
  @ParameterizedTest
  @CsvSource({"7,105", "307,9105"})
  void duplicateCallbacksPreserveSnapshotAndCost(long productId, long experimentId)
      throws Exception {
    var transaction =
        java.util.Arrays.stream(GeraSalesPageStageService.class.getMethods())
            .filter(method -> method.getName().equals("receiveResult"))
            .findFirst()
            .orElseThrow()
            .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
    assertThat(transaction.isolation())
        .isEqualTo(org.springframework.transaction.annotation.Isolation.READ_COMMITTED);
    callbackFixture(transaction.isolation().value(), productId, experimentId, 2);
    for (String methodName :
        List.of("preparePublication", "snapshotPublication", "listPublications", "republish")) {
      var annotation =
          java.util.Arrays.stream(GeraSalesPagePublicationAuditService.class.getMethods())
              .filter(method -> method.getName().equals(methodName))
              .findFirst()
              .orElseThrow()
              .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
      assertThat(annotation.isolation())
          .as(methodName)
          .isEqualTo(org.springframework.transaction.annotation.Isolation.READ_COMMITTED);
    }
  }

  /** Mantém leitura inicial simultânea e confere o snapshot após o guardião real de publicação. */
  private void callbackFixture(
      int isolation, long productId, long experimentId, int expectedSuccess) throws Exception {
    String prefix = "cb_" + UUID.randomUUID().toString().replace("-", "");
    String productsTable = prefix + "_p",
        experimentsTable = prefix + "_e",
        auditTable = prefix + "_a";
    var pool = Executors.newFixedThreadPool(2);
    var initialRead = new CyclicBarrier(2);
    try (var setup = connection();
        var sql = setup.createStatement()) {
      sql.execute("CREATE TABLE " + productsTable + "(id BIGINT PRIMARY KEY) ENGINE=InnoDB");
      sql.execute(
          "CREATE TABLE "
              + experimentsTable
              + "(id BIGINT PRIMARY KEY, product_id BIGINT, FOREIGN KEY(product_id) REFERENCES "
              + productsTable
              + "(id)) ENGINE=InnoDB");
      sql.execute(
          "CREATE TABLE "
              + auditTable
              + "(job VARCHAR(64) PRIMARY KEY, experiment_id BIGINT, cost_usd DECIMAL(18,8), html_hash CHAR(64), FOREIGN KEY(experiment_id) REFERENCES "
              + experimentsTable
              + "(id)) ENGINE=InnoDB");
      sql.execute("INSERT INTO " + productsTable + " VALUES(" + productId + ")");
      sql.execute(
          "INSERT INTO " + experimentsTable + " VALUES(" + experimentId + "," + productId + ")");
      List<Future<Boolean>> calls = new java.util.ArrayList<>();
      for (int i = 0; i < 2; i++)
        calls.add(
            pool.submit(
                () -> {
                  try (var tx = connection()) {
                    tx.setTransactionIsolation(isolation);
                    tx.setAutoCommit(false);
                    try (var statement = tx.createStatement()) {
                      try (var rows =
                          statement.executeQuery("SELECT COUNT(*) FROM " + auditTable)) {
                        rows.next();
                      }
                      initialRead.await(10, TimeUnit.SECONDS);
                      guard(tx, productsTable, experimentsTable, productId, experimentId)
                          .preparePublication(experimentId);
                      boolean exists;
                      try (var rows =
                          statement.executeQuery(
                              "SELECT COUNT(*) FROM " + auditTable + " WHERE job='same-job'")) {
                        rows.next();
                        exists = rows.getInt(1) > 0;
                      }
                      if (!exists)
                        statement.execute(
                            "INSERT INTO "
                                + auditTable
                                + " VALUES('same-job',"
                                + experimentId
                                + ",0.156902,'"
                                + "b".repeat(64)
                                + "')");
                    }
                    tx.commit();
                    return true;
                  } catch (SQLException ex) {
                    log.info(
                        "Regressão de callback duplicado experimentId={} isolation={}",
                        experimentId,
                        isolation,
                        ex);
                    if (isolation == Connection.TRANSACTION_REPEATABLE_READ
                        && ex.getErrorCode() == 1062) return false;
                    throw ex;
                  }
                }));
      int successful = 0;
      for (var call : calls) if (call.get(20, TimeUnit.SECONDS)) successful++;
      assertThat(successful).isEqualTo(expectedSuccess);
      try (var rows =
          sql.executeQuery("SELECT COUNT(*),SUM(cost_usd),MIN(html_hash) FROM " + auditTable)) {
        rows.next();
        assertThat(rows.getInt(1)).isEqualTo(1);
        assertThat(rows.getBigDecimal(2)).isEqualByComparingTo("0.156902");
        assertThat(rows.getString(3)).isEqualTo("b".repeat(64));
      }
    } finally {
      pool.shutdownNow();
      try (var cleanup = connection();
          var sql = cleanup.createStatement()) {
        sql.execute("DROP TABLE IF EXISTS " + auditTable);
        sql.execute("DROP TABLE IF EXISTS " + experimentsTable);
        sql.execute("DROP TABLE IF EXISTS " + productsTable);
      }
    }
  }
}
