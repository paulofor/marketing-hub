package com.marketinghub.facebookads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

/** Valida em MySQL 5.7 real o reparo dos motivos de parada extensíveis. */
class FacebookCampaignStopReasonMysqlTest {

  private static final String CHANGELOG =
      "db/changelog/changesets/2026-09-28-facebook-stop-reason-varchar-repair-v2.yaml";

  /** Converte os dois ENUM legados, preserva valores e aceita o novo motivo após reaplicação. */
  @Test
  void repairConvergesLegacyEnumsAndAcceptsAuthorizedWindowReason() throws Exception {
    String url = System.getenv("VEGA91_MYSQL_URL");
    assumeTrue(
        url != null && url.contains("resumption_test"),
        "Fixture física executada somente no schema local segregado");

    try (Connection connection =
        DriverManager.getConnection(url, "root", "local-resumption-only")) {
      createLegacySchema(connection);
      Liquibase liquibase =
          new Liquibase(
              CHANGELOG, new ClassLoaderResourceAccessor(), new JdbcConnection(connection));

      liquibase.update(new Contexts());
      liquibase.update(new Contexts());

      assertVarcharColumn(connection, "facebook_ads_campaign");
      assertVarcharColumn(connection, "campaign_strategy_evaluation");
      connection.setAutoCommit(true);
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(
            "UPDATE facebook_ads_campaign "
                + "SET stop_reason = 'CAMPAIGN_AUTHORIZED_WINDOW_ENDED' WHERE id = 'campaign-91'");
        try (ResultSet result =
            statement.executeQuery(
                "SELECT stop_reason FROM facebook_ads_campaign WHERE id = 'campaign-91'")) {
          assertThat(result.next()).isTrue();
          assertThat(result.getString(1)).isEqualTo("CAMPAIGN_AUTHORIZED_WINDOW_ENDED");
        }
      }
    }
  }

  /** Reinicia o schema segregado e monta as colunas com o tipo nativo encontrado em produção. */
  private void createLegacySchema(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS DATABASECHANGELOGLOCK");
      statement.execute("DROP TABLE IF EXISTS DATABASECHANGELOG");
      statement.execute("DROP TABLE IF EXISTS campaign_strategy_evaluation");
      statement.execute("DROP TABLE IF EXISTS facebook_ads_campaign");
      statement.execute(
          "CREATE TABLE facebook_ads_campaign ("
              + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
              + "stop_reason ENUM('ADMIN_EXPERIMENT_PAUSED') NULL) ENGINE=InnoDB");
      statement.execute(
          "CREATE TABLE campaign_strategy_evaluation ("
              + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
              + "stop_reason ENUM('ADMIN_EXPERIMENT_PAUSED') NULL) ENGINE=InnoDB");
      statement.executeUpdate(
          "INSERT INTO facebook_ads_campaign(id, stop_reason) "
              + "VALUES ('campaign-91', 'ADMIN_EXPERIMENT_PAUSED')");
      statement.executeUpdate(
          "INSERT INTO campaign_strategy_evaluation(stop_reason) "
              + "VALUES ('ADMIN_EXPERIMENT_PAUSED')");
    }
  }

  /** Confirma o tipo final e a preservação do valor existente em cada tabela. */
  private void assertVarcharColumn(Connection connection, String tableName) throws Exception {
    try (var statement =
        connection.prepareStatement(
            "SELECT data_type, character_maximum_length "
                + "FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = 'stop_reason'")) {
      statement.setString(1, tableName);
      try (ResultSet result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        assertThat(result.getString("data_type")).isEqualTo("varchar");
        assertThat(result.getLong("character_maximum_length")).isEqualTo(100L);
      }
    }
    try (Statement statement = connection.createStatement();
        ResultSet result =
            statement.executeQuery("SELECT stop_reason FROM " + tableName + " LIMIT 1")) {
      assertThat(result.next()).isTrue();
      assertThat(result.getString(1)).isEqualTo("ADMIN_EXPERIMENT_PAUSED");
    }
  }
}
