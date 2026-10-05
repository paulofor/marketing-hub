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

/** Responsabilidade: preservar custos e respostas auditadas na migração real do MySQL 5.7. */
@EnabledIfEnvironmentVariable(named = "AGENT_COST_MYSQL", matches = "true")
class AgentExecutionCostMysql57Test {
  /** Aplica a migração, verifica idempotência e impede redução destrutiva da precisão. */
  @Test
  void migratesFractionalCostsWithoutErasingAuditOrAcceptingLossyRollback() throws Exception {
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
          "DROP TABLE IF EXISTS DATABASECHANGELOGLOCK, DATABASECHANGELOG, experiment_strategist_execution, financial_agent_execution");
      execute(
          connection,
          "CREATE TABLE experiment_strategist_execution(id BIGINT PRIMARY KEY, estimated_cost DECIMAL(38,2) NULL, raw_model_response LONGTEXT)");
      execute(
          connection,
          "CREATE TABLE financial_agent_execution(id BIGINT PRIMARY KEY, estimated_cost DECIMAL(12,4) NULL, raw_model_response LONGTEXT)");
      execute(
          connection,
          "INSERT INTO experiment_strategist_execution VALUES (731,0.17,'preserved-response'),(972,NULL,'unknown-preserved')");
      execute(
          connection,
          "INSERT INTO financial_agent_execution VALUES (731,0.0935,'preserved-response'),(972,NULL,'unknown-preserved')");
      var history =
          scalar(
              connection,
              "SELECT GROUP_CONCAT(CONCAT(id,':',COALESCE(estimated_cost,'unknown'),':',raw_model_response) ORDER BY id) FROM experiment_strategist_execution");
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var migration =
          new Liquibase(
              "db/changelog/changesets/2026-10-05-agent-execution-cost-precision-v1.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {
        execute(
            connection,
            "UPDATE experiment_strategist_execution SET estimated_cost=10000000000 WHERE id=731");
        assertThatThrownBy(() -> migration.update("")).hasMessageContaining("Precondition failed");
        assertThat(
                scalar(
                    connection,
                    "SELECT COLUMN_TYPE FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='financial_agent_execution' AND column_name='estimated_cost'"))
            .isEqualTo("decimal(12,4)");
        execute(
            connection,
            "UPDATE experiment_strategist_execution SET estimated_cost=0.17 WHERE id=731");
        migration.update("");
        migration.update("");
        assertThat(scalar(connection, "SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo("1");
        assertThat(
                scalar(
                    connection,
                    "SELECT GROUP_CONCAT(CONCAT(id,':',COALESCE(ROUND(estimated_cost,2),'unknown'),':',raw_model_response) ORDER BY id) FROM experiment_strategist_execution"))
            .isEqualTo(history);
        for (var table :
            new String[] {"experiment_strategist_execution", "financial_agent_execution"}) {
          assertThat(
                  scalar(
                      connection,
                      "SELECT COLUMN_TYPE FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='"
                          + table
                          + "' AND column_name='estimated_cost'"))
              .isEqualTo("decimal(18,8)");
          execute(
              connection,
              "INSERT INTO "
                  + table
                  + " VALUES (1256,0.00000250,'new-positive-cost'),(2561,0.00092800,'different-execution')");
          assertThat(scalar(connection, "SELECT estimated_cost FROM " + table + " WHERE id=1256"))
              .isEqualTo("0.00000250");
          assertThat(scalar(connection, "SELECT estimated_cost FROM " + table + " WHERE id=2561"))
              .isEqualTo("0.00092800");
          assertThat(
                  scalar(
                      connection,
                      "SELECT COUNT(*) FROM "
                          + table
                          + " WHERE id=972 AND estimated_cost IS NULL AND raw_model_response='unknown-preserved'"))
              .isEqualTo("1");
        }
        // As novas auditorias devem estar confirmadas antes de simular um rollback da migração.
        connection.commit();
        assertThatThrownBy(() -> migration.rollback(1, ""))
            .hasMessageContaining("Reducao de precisao apagaria custos auditados");
        assertThat(
                scalar(
                    connection,
                    "SELECT estimated_cost FROM experiment_strategist_execution WHERE id=1256"))
            .isEqualTo("0.00000250");
        assertThat(scalar(connection, "SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo("1");
      }
    }
  }

  /** Executa SQL somente na base sintética dedicada. */
  private void execute(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  /** Recupera uma célula para conferir dados e metadados persistidos. */
  private String scalar(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      return result.getString(1);
    }
  }
}
