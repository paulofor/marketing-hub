package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Comprova a migração de responsabilidade no MySQL 5.7 isolado, preservando histórico e fluxo. */
@EnabledIfEnvironmentVariable(named = "VEGA377_MYSQL", matches = "local")
class PdeHarnessResponsibilityMysql57Test {
  /** Executa Liquibase, repete após perda do ledger e conserva identidades e tarefas anteriores. */
  @Test
  void migratesOnlyCurrentMetadataAndCanBeReapplied() throws Exception {
    String host = System.getenv().getOrDefault("VEGA377_MYSQL_HOST", "127.0.0.1");
    assertThat(host).isIn("127.0.0.1", "sandbox-docker");
    try (var connection =
        DriverManager.getConnection(
            "jdbc:mysql://"
                + host
                + ":33377/harness_qa?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
            "root",
            "harness-local-root")) {
      assertThat(connection.getMetaData().getDatabaseProductVersion()).startsWith("5.7.");
      try (var statement = connection.createStatement()) {
        for (String table :
            new String[] {
              "DATABASECHANGELOGLOCK",
              "DATABASECHANGELOG",
              "agent_task",
              "business_process_activity_definition",
              "business_process_definition"
            }) {
          statement.execute("DROP TABLE IF EXISTS " + table);
        }
        statement.execute(
            "CREATE TABLE business_process_definition (id BIGINT PRIMARY KEY, process_code VARCHAR(100), version_number INT, status VARCHAR(30), diagram_json LONGTEXT)");
        statement.execute(
            "CREATE TABLE business_process_activity_definition (id BIGINT PRIMARY KEY, process_definition_id BIGINT, activity_id VARCHAR(100), owner_name VARCHAR(160), definition_json LONGTEXT)");
        statement.execute(
            "CREATE TABLE agent_task (id BIGINT PRIMARY KEY, status VARCHAR(30), assigned_agent_id BIGINT, source_reference VARCHAR(100))");
        statement.execute("INSERT INTO agent_task VALUES (377, 'BLOCKED', 2, 'experiment:92')");
      }
      String node =
          "{\"id\":\"technicalHomologation\",\"type\":\"TASK\",\"owner\":\"Harness\",\"responsibleAgentKeys\":[\"customer-agent\"],\"executionMode\":\"DETERMINISTIC\"}";
      String diagram =
          "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\"},"
              + node
              + "],\"flows\":[{\"from\":\"start\",\"to\":\"technicalHomologation\"}]}";
      for (int version : new int[] {7, 8}) {
        try (var insert =
            connection.prepareStatement(
                "INSERT INTO business_process_definition VALUES (?, 'pde-construction-approval', ?, ?, ?)")) {
          insert.setLong(1, version == 8 ? 70 : 69);
          insert.setInt(2, version);
          insert.setString(3, version == 8 ? "PUBLISHED" : "RETIRED");
          insert.setString(4, diagram);
          insert.executeUpdate();
        }
        try (var insert =
            connection.prepareStatement(
                "INSERT INTO business_process_activity_definition VALUES (?, ?, 'technicalHomologation', 'Harness', ?)")) {
          insert.setLong(1, version == 8 ? 705 : 695);
          insert.setLong(2, version == 8 ? 70 : 69);
          insert.setString(3, node);
          insert.executeUpdate();
        }
      }
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var migration =
          new Liquibase(
              "db/changelog/changesets/2026-09-10-pde-harness-responsibility-v1.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {
        migration.update("");
        String first =
            value(connection, "SELECT diagram_json FROM business_process_definition WHERE id=70");
        var json = new ObjectMapper();
        var migrated = json.readTree(first);
        assertThat(migrated.path("nodes").get(1).path("owner").asText()).isEqualTo("Psique");
        assertThat(migrated.path("nodes").get(1).path("controlDescription").asText())
            .contains("harness", "testes automáticos");
        assertThat(migrated.path("flows")).isEqualTo(json.readTree(diagram).path("flows"));
        assertThat(migrated.path("nodes").get(1).path("responsibleAgentKeys").get(0).asText())
            .isEqualTo("customer-agent");
        assertThat(
                value(
                    connection,
                    "SELECT owner_name FROM business_process_activity_definition WHERE id=705"))
            .isEqualTo("Psique");
        assertThat(
                json.readTree(
                    value(
                        connection,
                        "SELECT definition_json FROM business_process_activity_definition WHERE id=705")))
            .isEqualTo(migrated.path("nodes").get(1));
        assertThat(
                value(
                    connection, "SELECT diagram_json FROM business_process_definition WHERE id=69"))
            .isEqualTo(diagram);
        assertThat(
                value(
                    connection,
                    "SELECT owner_name FROM business_process_activity_definition WHERE id=695"))
            .isEqualTo("Harness");
        assertThat(
                value(
                    connection,
                    "SELECT CONCAT(status, ':', assigned_agent_id, ':', source_reference) FROM agent_task WHERE id=377"))
            .isEqualTo("BLOCKED:2:experiment:92");
        migration.update("");
        try (var statement = connection.createStatement()) {
          statement.executeUpdate("DELETE FROM DATABASECHANGELOG");
        }
        migration.update("");
        assertThat(
                value(
                    connection, "SELECT diagram_json FROM business_process_definition WHERE id=70"))
            .isEqualTo(first);
        assertThat(value(connection, "SELECT COUNT(*) FROM business_process_activity_definition"))
            .isEqualTo("2");
      }
    }
  }

  /** Lê uma única célula das tabelas sintéticas da homologação. */
  private String value(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      return result.getString(1);
    }
  }
}
