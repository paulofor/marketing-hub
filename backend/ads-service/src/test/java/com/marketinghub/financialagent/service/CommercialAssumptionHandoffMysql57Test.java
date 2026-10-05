package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Responsabilidade: preservar histórico e impedir duplicação da proposta no MySQL 5.7. */
@EnabledIfEnvironmentVariable(named = "AGENT_COST_MYSQL", matches = "true")
class CommercialAssumptionHandoffMysql57Test {
  /** Aplica o vínculo incremental, preserva nulos e comprova a proteção de unicidade. */
  @Test
  void migrationPreservesHistoryAndDeduplicatesProposals() throws Exception {
    var host = System.getenv().getOrDefault("AGENT_COST_DB_HOST", "127.0.0.1");
    assertThat(host).isIn("127.0.0.1", "sandbox-docker");
    try (var connection =
        DriverManager.getConnection(
            "jdbc:mysql://"
                + host
                + ":33343/usage_cost_qa?useSSL=false&allowPublicKeyRetrieval=true",
            "root",
            "synthetic-usage-cost")) {
      assertThat(connection.getMetaData().getDatabaseProductVersion()).startsWith("5.7.");
      execute(
          connection,
          "DROP TABLE IF EXISTS DATABASECHANGELOGLOCK, DATABASECHANGELOG, financial_agent_execution");
      execute(
          connection,
          "CREATE TABLE financial_agent_execution(id BIGINT PRIMARY KEY, estimated_cost DECIMAL(18,8) NULL, raw_model_response LONGTEXT)");
      execute(
          connection,
          "INSERT INTO financial_agent_execution VALUES (421,0.03124440,'preserved'),(822,NULL,'unknown')");
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var migration =
          new Liquibase(
              "db/changelog/changesets/2026-10-05-commercial-assumption-handoff-v1.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {
        migration.update("");
        migration.update("");
        assertThat(scalar(connection, "SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo("1");
        assertThat(
                scalar(
                    connection,
                    "SELECT COUNT(*) FROM financial_agent_execution WHERE strategist_execution_id IS NULL"))
            .isEqualTo("2");
        assertThat(
                scalar(
                    connection,
                    "SELECT estimated_cost FROM financial_agent_execution WHERE id=421"))
            .isEqualTo("0.03124440");
        assertThat(
                scalar(
                    connection,
                    "SELECT raw_model_response FROM financial_agent_execution WHERE id=822"))
            .isEqualTo("unknown");
        execute(
            connection,
            "INSERT INTO financial_agent_execution VALUES (1237,NULL,'proposal-one',903),(2248,NULL,'proposal-two',1702)");
        assertThatThrownBy(
                () ->
                    execute(
                        connection,
                        "INSERT INTO financial_agent_execution VALUES (3259,NULL,'duplicate',903)"))
            .hasMessageContaining("Duplicate entry");
        connection.commit();
        assertThatThrownBy(() -> migration.rollback(1, ""))
            .hasMessageContaining("Preservar vinculacao");
        assertThat(
                scalar(
                    connection,
                    "SELECT COUNT(*) FROM financial_agent_execution WHERE strategist_execution_id=903"))
            .isEqualTo("1");
        assertThat(scalar(connection, "SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo("1");
      }
    }
  }

  /** Executa SQL somente na base efêmera sintética. */
  private void execute(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  /** Consulta dados ou metadados para comprovar a migração. */
  private String scalar(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      return result.getString(1);
    }
  }
}
