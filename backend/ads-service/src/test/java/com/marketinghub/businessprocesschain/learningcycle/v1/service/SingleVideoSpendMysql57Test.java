package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.SingleVideoSpend;
import com.marketinghub.repository.jpa.learningcycle.SingleVideoSpendRepository;
import com.marketinghub.videocontract.v1.BoundedSpendContracts.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import liquibase.*;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Responsabilidade: comprovar migração e disputa do teto em MySQL 5.7 real e isolado. */
@EnabledIfSystemProperty(named = "studio.mysql", matches = "true")
class SingleVideoSpendMysql57Test {
  /**
   * Aplica a migração duas vezes, mantém enum/histórico e serializa três executores no mesmo teto.
   */
  @Test
  void migrationConcurrencyAndRecovery() throws Exception {
    String base =
        System.getProperty(
            "studio.mysql.url", "jdbc:mysql://sandbox-docker:33317/studio_test?useSSL=false");
    java.net.URI destination = java.net.URI.create(base.substring("jdbc:".length()));
    assertThat(Set.of("sandbox-docker", "127.0.0.1", "localhost")).contains(destination.getHost());
    String url =
        "jdbc:mysql://"
            + destination.getHost()
            + ":"
            + destination.getPort()
            + "/studio_budget_contract?useSSL=false&serverTimezone=UTC";
    try (var admin = DriverManager.getConnection(base, "root", "studio_test_only");
        var sql = admin.createStatement()) {
      sql.execute("DROP DATABASE IF EXISTS studio_budget_contract");
      sql.execute("CREATE DATABASE studio_budget_contract CHARACTER SET utf8mb4");
    }
    try (var connection = DriverManager.getConnection(url, "root", "studio_test_only")) {
      try (var sql = connection.createStatement()) {
        sql.execute(
            "CREATE TABLE learning_sales_cycle_v1 (id BIGINT PRIMARY KEY, status VARCHAR(32) NOT"
                + " NULL) ENGINE=InnoDB");
        sql.execute(
            "CREATE TABLE learning_sales_cycle_event_v1 (id BIGINT PRIMARY KEY, evidence_json"
                + " LONGTEXT) ENGINE=InnoDB");
        sql.execute("CREATE TABLE video_project (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        sql.execute("CREATE TABLE video_production_cycle (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        sql.execute("CREATE TABLE agent_task (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        sql.execute("CREATE TABLE sales_video_job (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        sql.execute(
            "CREATE TABLE sales_video_script (id BIGINT PRIMARY KEY, source ENUM('MANUAL','OPENAI')"
                + " NOT NULL,status ENUM('DRAFT','READY_FOR_REVIEW','APPROVED','ARCHIVED') NOT"
                + " NULL) ENGINE=InnoDB");
        sql.execute("INSERT INTO learning_sales_cycle_v1 VALUES (7001,'OPEN'),(9001,'OPEN')");
        sql.execute(
            "INSERT INTO learning_sales_cycle_event_v1 VALUES"
                + " (7128,'original-receipt-15'),(9128,'another-product')");
        sql.execute("INSERT INTO video_project VALUES (7014),(9014)");
        sql.execute("INSERT INTO sales_video_script VALUES (1,'MANUAL','APPROVED')");
      }
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var migration =
          new Liquibase(
              "db/changelog/changesets/2026-11-05-single-video-spend-v1.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {
        migration.update(new Contexts(), new LabelExpression());
        migration.update(new Contexts(), new LabelExpression());
        try (var sql = connection.createStatement();
            var rows =
                sql.executeQuery(
                    "SELECT source,status,evidence_json FROM sales_video_script CROSS JOIN"
                        + " learning_sales_cycle_event_v1 WHERE sales_video_script.id=1 AND"
                        + " learning_sales_cycle_event_v1.id=7128")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo("MANUAL");
          assertThat(rows.getString(2)).isEqualTo("APPROVED");
          assertThat(rows.getString(3)).isEqualTo("original-receipt-15");
        }
      }
    }
    DataSource source = new DriverManagerDataSource(url, "root", "studio_test_only");
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setManagedTypes(
        PersistenceManagedTypes.of(
            SingleVideoSpend.class.getName(),
            com.marketinghub.salesvideo.VideoProductRecording.class.getName()));
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(
        Map.of("hibernate.hbm2ddl.auto", "validate", "hibernate.jdbc.time_zone", "UTC"));
    factory.afterPropertiesSet();
    var emf = Objects.requireNonNull(factory.getObject());
    try {
      var em = SharedEntityManagerCreator.createSharedEntityManager(emf);
      var repository = new JpaRepositoryFactory(em).getRepository(SingleVideoSpendRepository.class);
      var ledger =
          new SingleVideoSpendLedger(
              repository, new LearningCycleJson(new ObjectMapper().findAndRegisterModules()));
      var tx = new TransactionTemplate(new JpaTransactionManager(emf));
      tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
      var start = new CountDownLatch(1);
      var pool = Executors.newFixedThreadPool(3);
      List<Future<Lease>> calls = new ArrayList<>();
      try {
        for (String operation :
            List.of("task:iris:main", "job:apollo:CLIP_1", "task:psique:audio")) {
          calls.add(
              pool.submit(
                  () -> {
                    start.await();
                    try {
                      return tx.execute(
                          s -> {
                            em.createNativeQuery(
                                    "SELECT id FROM learning_sales_cycle_v1 WHERE id=7001 FOR"
                                        + " UPDATE")
                                .getSingleResult();
                            return ledger.reserve(
                                7128L,
                                7001L,
                                7014L,
                                operation,
                                "LOCAL_TEST",
                                "bounded",
                                "{}",
                                new BigDecimal("6"),
                                new BigDecimal("15"),
                                BigDecimal.ZERO,
                                "{}");
                          });
                    } catch (IllegalArgumentException rejected) {
                      return null;
                    }
                  }));
        }
        start.countDown();
        List<Lease> accepted = new ArrayList<>();
        for (var call : calls) {
          var result = call.get(30, TimeUnit.SECONDS);
          if (result != null) accepted.add(result);
        }
        assertThat(accepted).hasSize(2);
        tx.executeWithoutResult(
            s -> {
              em.createNativeQuery(
                      "SELECT id FROM learning_sales_cycle_v1 WHERE id=7001 FOR UPDATE")
                  .getSingleResult();
              assertThat(ledger.liability(7128L)).isEqualByComparingTo("12");
              var first = accepted.getFirst();
              String op = repository.findById(first.id()).orElseThrow().getOperationKey();
              assertThat(ledger.begin(first.id(), 7128L, 7014L, op).mayInvoke()).isTrue();
              ledger.record(first.id(), 7128L, 7014L, op, new Result(null, null, null));
              assertThat(ledger.begin(first.id(), 7128L, 7014L, op).mayInvoke()).isFalse();
              assertThat(ledger.find(7128L, op).state()).isEqualTo("UNCERTAIN");
              assertThat(ledger.liability(7128L)).isEqualByComparingTo("12");
              assertThatThrownBy(() -> ledger.begin(first.id(), 9128L, 9014L, op))
                  .hasMessageContaining("outro contexto");
            });
      } finally {
        pool.shutdownNow();
      }
      tx.executeWithoutResult(
          s -> {
            em.createNativeQuery("SELECT id FROM learning_sales_cycle_v1 WHERE id=9001 FOR UPDATE")
                .getSingleResult();
            var other =
                ledger.reserve(
                    9128L,
                    9001L,
                    9014L,
                    "task:other:main",
                    "LOCAL_TEST",
                    "bounded",
                    "{}",
                    BigDecimal.ONE,
                    BigDecimal.TEN,
                    BigDecimal.ZERO,
                    "{}");
            assertThat(other.receiptId()).isEqualTo(9128L);
            assertThat(ledger.liability(7128L)).isEqualByComparingTo("12");
            assertThat(ledger.begin(other.id(), 9128L, 9014L, "task:other:main").mayInvoke())
                .isTrue();
            ledger.record(
                other.id(),
                9128L,
                9014L,
                "task:other:main",
                new Result(200, "{\"id\":\"original\"}", "req-local"));
          });
      tx.executeWithoutResult(
          status -> {
            var row = new com.marketinghub.salesvideo.VideoProductRecording();
            row.setVideoProjectId(7014L);
            row.setTenantId("local-test");
            row.setSha256("a".repeat(64));
            row.setSourceVersion("test-source");
            row.setManifestJson("{}");
            row.setVideoBytes(new byte[] {1, 2, 3});
            row.setCreatedAt(java.time.Instant.now());
            em.persist(row);
            em.flush();
            em.clear();
            assertThat(
                    em.find(com.marketinghub.salesvideo.VideoProductRecording.class, row.getId())
                        .getVideoBytes())
                .containsExactly(1, 2, 3);
          });
      // Outra instância do consumidor recupera bytes e IDs persistidos sem chamar provedor.
      var restarted =
          new SingleVideoSpendLedger(repository, new LearningCycleJson(new ObjectMapper()));
      tx.executeWithoutResult(
          s -> {
            var result = restarted.find(9128L, "task:other:main");
            assertThat(result.rawResponse()).contains("original");
            assertThat(result.mayInvoke()).isFalse();
          });
    } finally {
      factory.destroy();
    }
  }
}
