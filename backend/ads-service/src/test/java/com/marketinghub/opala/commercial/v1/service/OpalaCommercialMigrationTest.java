package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;

import java.sql.DriverManager;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar SQL MySQL 5.7, chamada do pai e preservação do histórico. */
class OpalaCommercialMigrationTest {
  /** Executa migração, reenvio, rollback e reaplicação no banco local exclusivo. */
  @Test
  void migratesAndRollsBackWithoutRewritingHistory() throws Exception {
    String url = System.getenv("OPALA_MYSQL_URL");
    Assumptions.assumeTrue(url != null, "Fixture física exige OPALA_MYSQL_URL local.");
    assertThat(url).matches("jdbc:mysql://(127\\.0\\.0\\.1|sandbox-docker):18307/opala_test.*");
    try (var connection = DriverManager.getConnection(url, "root", "cycles-root-local-only")) {
      migrate(url, false);
      check(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='opala-commercial-preparation-v1' AND status='PUBLISHED'",
          1);
      check(
          connection,
          "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='opala-commercial-preparation-v1'",
          8);
      check(
          connection,
          "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='pde-sales-delivery-learning' AND p.version_number=7 AND a.subprocess_code='opala-commercial-preparation-v1'",
          1);
      check(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE version_number=6 AND diagram_json='{\"nodes\":[],\"flows\":[]}' AND status='PUBLISHED'",
          1);
      migrate(url, false);
      check(
          connection,
          "SELECT COUNT(*) FROM business_process_chain_definition WHERE version_number=15",
          1);
      migrate(url, true);
      check(
          connection,
          "SELECT COUNT(*) FROM business_process_chain_definition WHERE version_number=14 AND status='PUBLISHED'",
          1);
      check(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='opala-commercial-preparation-v1' AND status='RETIRED'",
          1);
      migrate(url, false);
      check(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='opala-commercial-preparation-v1' AND status='PUBLISHED'",
          1);
    }
  }

  /** Desativa o cache otimista entre comandos da mesma JVM e usa novas conexões reais. */
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
    var connection = DriverManager.getConnection(url, "root", "cycles-root-local-only");
    var database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
    try (var migration =
        new Liquibase(
            "liquibase-mysql57/opala-commercial-preparation-test.yaml",
            new ClassLoaderResourceAccessor(),
            database)) {
      if (rollback) migration.rollback("opala-before-preparation", new liquibase.Contexts());
      else migration.update(new liquibase.Contexts());
    } finally {
      updates.forEach(step -> step.setFastCheckEnabled(true));
    }
  }

  /** Confere cardinalidade com SQL real, sem simular o resultado da migração. */
  private void check(java.sql.Connection connection, String sql, int expected) throws Exception {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      assertThat(result.getInt(1)).isEqualTo(expected);
    }
  }
}
