package com.marketinghub.facebookads.resumption;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;

/** Valida migração real MySQL 5.7, reaplicação e mapeamento JPA com dados segregados. */
class FacebookCampaignResumptionMysqlTest {
  /** Aplica somente a migração nova e persiste a entidade real, sem acessar banco produtivo. */
  @Test
  void migrationAndJpaAgreeAndReapplyWithoutLosingHistory() throws Exception {
    String url = System.getenv("VEGA91_MYSQL_URL");
    assumeTrue(
        url != null && url.contains("resumption_test"),
        "Fixture física executada somente no schema local segregado");
    try (Connection connection =
        DriverManager.getConnection(url, "root", "local-resumption-only")) {
      try (Statement st = connection.createStatement()) {
        st.execute(
            "CREATE TABLE IF NOT EXISTS experiment (id BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB");
        st.execute("INSERT IGNORE INTO experiment(id) VALUES (910091)");
      }
      Liquibase liquibase =
          new Liquibase(
              "db/changelog/changesets/2026-09-20-facebook-campaign-resumption-v1.yaml",
              new ClassLoaderResourceAccessor(),
              new JdbcConnection(connection));
      liquibase.update(new liquibase.Contexts());
      liquibase.update(new liquibase.Contexts());
      Liquibase stopPolicyLiquibase =
          new Liquibase(
              "db/changelog/changesets/2026-09-22-facebook-campaign-resumption-stop-policy-v2.yaml",
              new ClassLoaderResourceAccessor(),
              new JdbcConnection(connection));
      stopPolicyLiquibase.update(new liquibase.Contexts());
      stopPolicyLiquibase.update(new liquibase.Contexts());
      try (Statement st = connection.createStatement();
          ResultSet rs =
              st.executeQuery(
                  "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '2026-09-20-%'")) {
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(2);
      }
    }
    var registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.connection.url", url)
            .applySetting("hibernate.connection.username", "root")
            .applySetting("hibernate.connection.password", "local-resumption-only")
            .applySetting("hibernate.hbm2ddl.auto", "validate")
            .build();
    try (var factory =
            new MetadataSources(registry)
                .addAnnotatedClass(FacebookCampaignResumption.class)
                .buildMetadata()
                .buildSessionFactory();
        var session = factory.openSession()) {
      var tx = session.beginTransaction();
      FacebookCampaignResumption r = new FacebookCampaignResumption();
      r.setExperimentId(910091L);
      r.setCampaignId("synthetic-campaign");
      r.setAdSetId("synthetic-adset");
      r.setStatus("PENDING");
      r.setTotalLimit(new BigDecimal("150"));
      r.setDailyBudget(new BigDecimal("20"));
      r.setStartDate(LocalDate.now());
      r.setEndDate(LocalDate.now().plusDays(6));
      r.setZeroResultSpendLimit(new BigDecimal("50"));
      r.setZeroPurchaseSpendLimit(new BigDecimal("50"));
      r.setPurchaseStopCount(5);
      r.setReason("Homologação local sem mídia real");
      r.setDestinationUrl("https://example.invalid");
      r.setRequestedAt(Instant.now());
      session.persist(r);
      tx.commit();
      var first =
          java.util.concurrent.CompletableFuture.supplyAsync(() -> reserve(factory, r.getId()));
      var second =
          java.util.concurrent.CompletableFuture.supplyAsync(() -> reserve(factory, r.getId()));
      assertThat(
              first.get(15, java.util.concurrent.TimeUnit.SECONDS)
                  + second.get(15, java.util.concurrent.TimeUnit.SECONDS))
          .isEqualTo(1);
      session.clear();
      assertThat(session.find(FacebookCampaignResumption.class, r.getId()).getTotalLimit())
          .isEqualByComparingTo("150");
      assertThat(session.find(FacebookCampaignResumption.class, r.getId()).getDailyBudget())
          .isEqualByComparingTo("20");
      assertThat(session.find(FacebookCampaignResumption.class, r.getId()).getPurchaseStopCount())
          .isEqualTo(5);
    } finally {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  /** Comprova exclusão entre duas reservas reais do mesmo registro MySQL. */
  private int reserve(org.hibernate.SessionFactory factory, Long id) {
    try (var session = factory.openSession()) {
      var tx = session.beginTransaction();
      var row =
          session.find(
              FacebookCampaignResumption.class,
              id,
              jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
      int claimed = "PENDING".equals(row.getStatus()) ? 1 : 0;
      if (claimed == 1) row.setStatus("RUNNING");
      tx.commit();
      return claimed;
    }
  }
}
