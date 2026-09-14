package com.marketinghub.product.executionprofile.v1.service;

import java.sql.DriverManager;
import liquibase.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Responsabilidade: comprovar reaplicação e rollback apenas do changelog novo no MySQL descartável.
 */
public final class ExecutionProfileMigrationCheck {
  /** Impede instanciar o utilitário de homologação local. */
  private ExecutionProfileMigrationCheck() {}

  /**
   * Valida reaplicação, rollback, recriação e integridade antes de encerrar a topologia sintética.
   */
  public static void main(String[] args) throws Exception {
    String host = System.getenv().getOrDefault("PROFILE_TEST_DB_HOST", "sandbox-docker");
    if (!java.util.Set.of("sandbox-docker", "127.0.0.1").contains(host))
      throw new IllegalArgumentException("Host não local");
    try (var connection =
        DriverManager.getConnection(
            "jdbc:mysql://"
                + host
                + ":18414/execution_profiles_local?useSSL=false&allowPublicKeyRetrieval=true",
            "root",
            "profiles-local-only")) {
      var migration =
          new Liquibase(
              ExecutionProfileLocalApplication.CHANGELOG,
              new ClassLoaderResourceAccessor(),
              new JdbcConnection(connection));
      if (args.length == 1 && "reapply".equals(args[0])) {
        migration.update(new Contexts(), new LabelExpression());
        try (var q = connection.createStatement();
            var rows =
                q.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'product_execution_%_v1'")) {
          rows.next();
          if (rows.getInt(1) != 4) throw new IllegalStateException("Reaplicação incompleta");
        }
        System.out.println("MYSQL57_REAPPLY_PASS");
        return;
      }
      if (!migration.listUnrunChangeSets(new Contexts(), new LabelExpression()).isEmpty())
        throw new IllegalStateException("Changelog não aplicado");
      migration.update(new Contexts(), new LabelExpression());
      try (var q = connection.createStatement();
          var rows =
              q.executeQuery(
                  "SELECT COUNT(*) FROM product_execution_binding_v1 b JOIN product_execution_profile_v1 p ON p.id=b.profile_id WHERE b.product_id<>p.product_id")) {
        rows.next();
        if (rows.getInt(1) != 0) throw new IllegalStateException("Produto cruzado no vínculo");
      }
      migration.rollback(4, new Contexts(), new LabelExpression());
      try (var q = connection.createStatement();
          var rows =
              q.executeQuery(
                  "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'product_execution_%_v1'")) {
        rows.next();
        if (rows.getInt(1) != 0) throw new IllegalStateException("Rollback incompleto");
      }
      System.out.println("MYSQL57_APPLY_REAPPLY_ROLLBACK_PASS");
    }
  }
}
