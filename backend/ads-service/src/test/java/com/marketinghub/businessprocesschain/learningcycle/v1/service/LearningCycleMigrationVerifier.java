package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import java.sql.DriverManager;
import java.util.Set;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/** Responsabilidade: comprovar aplicação, idempotência, rollback e reaplicação no MySQL local. */
public final class LearningCycleMigrationVerifier {
  /** Impede instâncias de um verificador de linha de comando. */
  private LearningCycleMigrationVerifier() {}

  /** Executa aplicação ou reversão por marcos identificados, somente no banco da homologação. */
  public static void main(String[] args) throws Exception {
    if (args.length != 1 || !Set.of("verify-and-rollback", "update-and-verify").contains(args[0]))
      throw new IllegalArgumentException("Informe a fase explícita de homologação.");
    try (var connection = localConnection();
        var migration = migration(connection)) {
      if ("update-and-verify".equals(args[0])) {
        migration.update(new Contexts(), new LabelExpression());
        assertCount(
            connection,
            "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '2026-09-08-learning-sales-cycles-v1-%'",
            4);
        assertCount(
            connection,
            "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='value-chain-learning-sales-cycle'",
            48);
        assertCount(
            connection,
            "SELECT COUNT(*) FROM business_process_definition WHERE process_code='value-chain-learning-sales-cycle' AND status='PUBLISHED' AND version_number=4",
            1);
        assertCount(
            connection,
            "SELECT COUNT(*) FROM business_process_definition WHERE process_code='value-chain-learning-sales-cycle' AND status='RETIRED' AND version_number IN (1,2,3)",
            3);
        verifyOrganization(connection);
        System.out.println("PASS MySQL 5.7: aplicação física e catálogo sem duplicação.");
        return;
      }
      try (var statement = connection.createStatement()) {
        statement.executeUpdate("UPDATE learning_sales_cycle_v1 SET current_instance_id=NULL");
        statement.executeUpdate("DELETE FROM learning_cycle_decision_proposal_v1");
        statement.executeUpdate("DELETE FROM learning_sales_cycle_event_v1");
        statement.executeUpdate("DELETE FROM learning_sales_cycle_v1 ORDER BY id DESC");
        statement.executeUpdate("DELETE FROM business_process_activity_instance");
      }
      // Confirma a limpeza da fixture antes de o Liquibase gerenciar a própria transação.
      if (!connection.getAutoCommit()) connection.commit();
      migration.validate();
      migration.update(new Contexts(), new LabelExpression());
      assertCount(connection, "SELECT COUNT(*) FROM business_process_activity_instance", 0);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='value-chain-learning-sales-cycle' AND version_number=1",
          1);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='value-chain-learning-sales-cycle'",
          48);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='learning_sales_cycle_v1' AND COLUMN_NAME IN ('created_at','updated_at','version_changed_at','window_start','window_end') AND DATA_TYPE='datetime' AND DATETIME_PRECISION=6 AND IS_NULLABLE='NO'",
          5);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='learning_sales_cycle_v1'",
          7);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND DATETIME_PRECISION=6 AND ((TABLE_NAME IN ('agent_task','facebook_ads_campaign') AND COLUMN_NAME='created_at') OR (TABLE_NAME='business_process_activity_instance' AND COLUMN_NAME IN ('entered_at','exited_at','created_at','updated_at')))",
          6);
      verifyOrganization(connection);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('product_process_run_v1','product_process_run_event_v1')",
          2);
      rollbackThrough(migration, "2026-09-12-product-process-automation-v1");
      assertCount(
          connection,
          "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('product_process_run_v1','product_process_run_event_v1')",
          0);
      verifyOrganization(connection);
      rollbackThrough(migration, "2026-09-09-learning-cycle-decision-atena-v4-bpm");
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='value-chain-learning-sales-cycle' AND version_number=3 AND status='PUBLISHED'",
          1);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='value-chain-learning-sales-cycle' AND version_number=4",
          0);
      rollbackThrough(migration, "2026-09-09-learning-cycle-decision-atena-v1-table");
      assertCount(
          connection,
          "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='learning_cycle_decision_proposal_v1'",
          0);
      rollbackThrough(migration, "2026-09-09-pde-sales-flow-v6");
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='pde-sales-delivery-learning' AND version_number=6 AND status='RETIRED'",
          1);
      rollbackThrough(migration, "2026-09-09-learning-cycle-automatic-measurement-v3");
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='value-chain-learning-sales-cycle' AND version_number=2 AND status='PUBLISHED'",
          1);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='value-chain-learning-sales-cycle' AND version_number=3",
          0);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_chain_definition WHERE version_number=13 AND status='PUBLISHED'",
          1);
      rollbackThrough(migration, "2026-09-08-pde-learning-cycle-organization-v1");
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_chain_definition WHERE version_number=12 AND status='PUBLISHED'",
          1);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_chain_item i JOIN business_process_chain_definition c ON c.id=i.chain_definition_id WHERE c.version_number=13",
          6);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='pde-sales-delivery-learning' AND version_number=5 AND status='RETIRED'",
          1);
      // A reaplicação ocorre em outra JVM para não reutilizar o estado do executor de rollback.
      rollbackThrough(migration, "2026-09-08-learning-sales-cycles-v1-cycle-table");
      assertCount(
          connection,
          "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('learning_sales_cycle_v1','learning_sales_cycle_event_v1')",
          0);
      assertCount(
          connection,
          "SELECT COUNT(*) FROM business_process_definition WHERE process_code='pde-construction-approval'",
          1);
    }
    System.out.println(
        "PASS MySQL 5.7: schema, 7 FKs, DATETIME, BPM v4 com Atena, rollback e preservação histórica.");
  }

  /** Reverte até o ID único aplicado, incluindo migrações posteriores sem contar posições fixas. */
  static void rollbackThrough(Liquibase migration, String changeSetId) throws Exception {
    var applied = migration.getDatabase().getRanChangeSetList();
    int target = -1;
    for (int index = 0; index < applied.size(); index++) {
      if (!changeSetId.equals(applied.get(index).getId())) continue;
      if (target >= 0) throw new IllegalStateException("Marco de rollback ambíguo: " + changeSetId);
      target = index;
    }
    if (target < 0)
      throw new IllegalStateException("Marco de rollback não aplicado: " + changeSetId);
    int count = applied.size() - target;
    System.out.println("Rollback até " + changeSetId + ": " + count + " changesets aplicados.");
    migration.rollback(count, new Contexts(), new LabelExpression());
  }

  /** Confere hierarquia, chamada, retornos, idempotência e preservação da definição anterior. */
  private static void verifyOrganization(java.sql.Connection connection) throws Exception {
    assertCount(
        connection,
        "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='value-chain-learning-sales-cycle' AND p.version_number=4 AND a.activity_id='DECISION' AND JSON_UNQUOTE(JSON_EXTRACT(a.definition_json,'$.responsibleAgentKeys[0]'))='experiment-strategist' AND JSON_EXTRACT(a.definition_json,'$.humanApprovalRequired')=true",
        1);
    assertCount(
        connection,
        "SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='learning_cycle_decision_proposal_v1'",
        4);

    assertCount(
        connection,
        "SELECT COUNT(*) FROM business_process_definition WHERE process_code='value-chain-learning-sales-cycle' AND version_number=4 AND status='PUBLISHED' AND JSON_UNQUOTE(JSON_EXTRACT(diagram_json,'$.schemaVersion'))='LEARNING_SALES_CYCLE_V4'",
        1);
    assertCount(
        connection,
        "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='value-chain-learning-sales-cycle' AND p.version_number=3 AND a.activity_id='MEASUREMENT' AND a.owner_name LIKE 'Marketing Hub%conciliação automática%'",
        1);
    assertCount(
        connection,
        "SELECT COUNT(*) FROM business_process_chain_definition WHERE version_number=14 AND status='PUBLISHED'",
        1);
    assertCount(
        connection,
        "SELECT COUNT(*) FROM business_process_chain_item i JOIN business_process_chain_definition c ON c.id=i.chain_definition_id WHERE c.version_number=13",
        6);
    assertCount(
        connection,
        "SELECT COUNT(*) FROM business_process_definition p JOIN business_process_chain_item i ON i.process_definition_id=p.id JOIN business_process_chain_definition c ON c.id=i.chain_definition_id WHERE c.version_number=13 AND i.sequence_number=6 AND p.process_code='pde-sales-delivery-learning' AND p.version_number=5",
        1);
    assertCount(
        connection,
        "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='pde-sales-delivery-learning' AND p.version_number=5 AND a.activity_id='learningCycle' AND a.subprocess_code='value-chain-learning-sales-cycle'",
        1);
    assertCount(
        connection,
        "SELECT JSON_LENGTH(diagram_json,'$.learningCycleReturns') FROM business_process_definition WHERE process_code='pde-sales-delivery-learning' AND version_number=5",
        5);
    assertCount(
        connection,
        "SELECT JSON_LENGTH(diagram_json,'$.nodes') FROM business_process_definition WHERE process_code='pde-sales-delivery-learning' AND version_number=4",
        6);
    assertCount(
        connection,
        "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='pde-sales-delivery-learning' AND p.version_number=5",
        4);
  }

  /** Abre uma conexão exclusiva da fixture sem aceitar destino produtivo. */
  private static java.sql.Connection localConnection() throws Exception {
    String host = System.getenv().getOrDefault("LEARNING_CYCLES_DB_HOST", "127.0.0.1");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Host externo proibido na fixture.");
    return DriverManager.getConnection(
        "jdbc:mysql://"
            + host
            + ":18307/learning_cycles_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        "cycles_local",
        "cycles-local-only");
  }

  /** Instancia o Liquibase com a migração versionada desta funcionalidade. */
  private static Liquibase migration(java.sql.Connection connection) throws Exception {
    return new Liquibase(
        "learningcycle/changelog.yaml",
        new ClassLoaderResourceAccessor(),
        new JdbcConnection(connection));
  }

  /** Verifica uma propriedade do schema real sem confiar apenas no texto SQL. */
  private static void assertCount(java.sql.Connection connection, String sql, long expected)
      throws Exception {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      long actual = result.next() ? result.getLong(1) : -1;
      if (actual != expected)
        throw new AssertionError(
            "Contrato físico divergente: esperado="
                + expected
                + " atual="
                + actual
                + " SQL="
                + sql);
    }
  }
}
