package com.marketinghub.quartzo.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: validar SQL real, percurso tipado e preservação das definições em MySQL 5.7.
 */
class QuartzoCommercialMigrationTest {
  /**
   * Aplica, reaplica e retira somente o novo catálogo sem modificar Opala ou as cadeias anteriores.
   */
  @Test
  void migratesTypedSubprocessAndPreservesHistoricalGraphs() throws Exception {
    String opala = System.getenv("OPALA_MYSQL_URL");
    Assumptions.assumeTrue(opala != null, "Fixture física exige OPALA_MYSQL_URL local.");
    assertThat(opala).matches("jdbc:mysql://(127\\.0\\.0\\.1|sandbox-docker):18307/opala_test.*");
    try (var connection = DriverManager.getConnection(opala, "root", "cycles-root-local-only");
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE DATABASE IF NOT EXISTS quartzo_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    }
    String url = opala.replace("/opala_test", "/quartzo_test");
    migrate(url, false);
    try (var connection = DriverManager.getConnection(url, "root", "cycles-root-local-only")) {
      String history =
          scalar(
              connection,
              "SELECT diagram_json FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=7");
      assertThat(history).doesNotContain("quartzo-commercial-preparation-v1");
      String parent =
          scalar(
              connection,
              "SELECT diagram_json FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=8");
      var graph = new ObjectMapper().readTree(parent);
      var route =
          graph.path("nodes").findParents("subprocessRoutes").getFirst().path("subprocessRoutes");
      assertThat(route.size()).isEqualTo(2);
      assertThat(route.get(0).path("productTypeCode").asText()).isEqualTo("PDE");
      assertThat(route.get(1).path("productTypeCode").asText())
          .isEqualTo(QuartzoCommercialContext.TYPE);
      assertThat(route.get(1).path("subprocessVersion").asInt()).isEqualTo(1);
      assertThat(
              scalar(
                  connection,
                  "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='quartzo-commercial-preparation-v1' AND p.version_number=1"))
          .isEqualTo("8");
      assertThat(
              scalar(
                  connection,
                  "SELECT COUNT(*) FROM business_process_chain_item i JOIN business_process_chain_definition c ON c.id=i.chain_definition_id JOIN business_process_definition p ON p.id=i.process_definition_id WHERE c.version_number=17 AND p.process_code='pde-commercial-homologation-activation' AND p.version_number=8"))
          .isEqualTo("1");
      migrate(url, false);
      assertThat(
              scalar(
                  connection,
                  "SELECT COUNT(*) FROM business_process_definition WHERE process_code='quartzo-commercial-preparation-v1'"))
          .isEqualTo("1");
      migrate(url, true);
      assertThat(
              scalar(
                  connection,
                  "SELECT status FROM business_process_definition WHERE process_code='quartzo-commercial-preparation-v1'"))
          .isEqualTo("RETIRED");
      assertThat(
              scalar(
                  connection,
                  "SELECT status FROM business_process_definition WHERE process_code='opala-commercial-preparation-v1'"))
          .isEqualTo("PUBLISHED");
      assertThat(
              scalar(
                  connection,
                  "SELECT diagram_json FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=7"))
          .isEqualTo(history);
      migrate(url, false);
      assertThat(
              scalar(
                  connection,
                  "SELECT status FROM business_process_definition WHERE process_code='quartzo-commercial-preparation-v1'"))
          .isEqualTo("PUBLISHED");
    }
  }

  /** Executa Liquibase sem cache otimista entre comandos consecutivos da mesma JVM. */
  private void migrate(String url, boolean rollback) throws Exception {
    var updates =
        liquibase.command.CommandFactory.getInstance()
            .getCommandDefinition("update")
            .getPipeline()
            .stream()
            .filter(liquibase.command.core.AbstractUpdateCommandStep.class::isInstance)
            .map(liquibase.command.core.AbstractUpdateCommandStep.class::cast)
            .toList();
    updates.forEach(step -> step.setFastCheckEnabled(false));
    var database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(
                new JdbcConnection(
                    DriverManager.getConnection(url, "root", "cycles-root-local-only")));
    try (var migration =
        new Liquibase(
            "liquibase-mysql57/quartzo-commercial-preparation-test.yaml",
            new ClassLoaderResourceAccessor(),
            database)) {
      if (rollback) migration.rollback("quartzo-before-preparation", new liquibase.Contexts());
      else migration.update(new liquibase.Contexts());
    } finally {
      updates.forEach(step -> step.setFastCheckEnabled(true));
    }
  }

  /** Lê um único valor diretamente do banco para conferir o resultado real da migração. */
  private String scalar(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var rows = statement.executeQuery(sql)) {
      assertThat(rows.next()).isTrue();
      return rows.getString(1);
    }
  }
}
