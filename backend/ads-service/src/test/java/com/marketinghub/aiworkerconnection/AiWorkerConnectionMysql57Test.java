package com.marketinghub.aiworkerconnection;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Valida a migração incremental de conexão em MySQL 5.7 efêmero, sem dados de produção. */
@EnabledIfEnvironmentVariable(named = "WORKER_CONNECTION_MYSQL", matches = "true")
class AiWorkerConnectionMysql57Test {
  /** Confere aplicação, idempotência, tipos temporais, rollback e reaplicação. */
  @Test
  void migratesSafelyOnMysql57() throws Exception {
    String host = System.getenv().getOrDefault("AGENT_COST_DB_HOST", "127.0.0.1");
    try (var connection =
        DriverManager.getConnection(
            "jdbc:mysql://"
                + host
                + ":33343/usage_cost_qa?useSSL=false&allowPublicKeyRetrieval=true",
            "root",
            "synthetic-usage-cost")) {
      assertThat(connection.getMetaData().getDatabaseProductVersion()).startsWith("5.7.");
      try (var statement = connection.createStatement()) {
        statement.execute("CREATE DATABASE IF NOT EXISTS worker_connection_qa");
        statement.execute("USE worker_connection_qa");
        statement.execute(
            "DROP TABLE IF EXISTS ai_worker_codex_connection, DATABASECHANGELOG, DATABASECHANGELOGLOCK");
      }
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var migration =
          new Liquibase(
              "db/changelog/changesets/2026-10-19-ai-worker-codex-connection-v1.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {
        migration.update("");
        migration.update("");
        try (var statement = connection.createStatement();
            var rows =
                statement.executeQuery(
                    "SELECT id, status, session_authenticated FROM ai_worker_codex_connection")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getLong(1)).isEqualTo(1);
          assertThat(rows.getString(2)).isEqualTo("DISCONNECTED");
          assertThat(rows.getBoolean(3)).isFalse();
          assertThat(rows.next()).isFalse();
        }
        try (var statement = connection.createStatement();
            var rows =
                statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ai_worker_codex_connection' AND column_name IN ('requested_at','expires_at','completed_at','checked_at') AND data_type='datetime'")) {
          rows.next();
          assertThat(rows.getInt(1)).isEqualTo(4);
        }
        migration.rollback(1, "");
        migration.update("");
      }
    }
  }
}
