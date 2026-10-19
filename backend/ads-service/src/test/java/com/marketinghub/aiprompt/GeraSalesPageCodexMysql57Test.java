package com.marketinghub.aiprompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Confere a troca explícita do modelo preservando contratos e histórico no MySQL 5.7 local. */
@EnabledIfEnvironmentVariable(named = "WORKER_CONNECTION_MYSQL", matches = "true")
class GeraSalesPageCodexMysql57Test {
  private static final String MIGRATION = "2026-10-19-sales-page-codex-model-v1.yaml";
  private Connection connection;

  /** Monta somente tabelas sintéticas e aplica as versões reais que precedem a correção. */
  @BeforeEach
  void prepareHistoricalContracts() throws Exception {
    connection =
        DriverManager.getConnection(
            "jdbc:mysql://"
                + System.getenv().getOrDefault("AGENT_COST_DB_HOST", "127.0.0.1")
                + ":33343/usage_cost_qa?useSSL=false&allowPublicKeyRetrieval=true",
            "root",
            "synthetic-usage-cost");
    assertThat(connection.getMetaData().getDatabaseProductVersion()).startsWith("5.7.");
    sql("CREATE DATABASE IF NOT EXISTS sales_page_codex_qa");
    connection.setCatalog("sales_page_codex_qa");
    sql(
        "DROP TABLE IF EXISTS historical_execution, ai_prompt_schema_template, openai_model, DATABASECHANGELOG, DATABASECHANGELOGLOCK");
    sql(
        "CREATE TABLE openai_model(id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), code VARCHAR(128) UNIQUE, price_input_standard DECIMAL(12,5), price_output_standard DECIMAL(12,5), price_input_cached_standard DECIMAL(12,5), price_input_batch DECIMAL(12,5), price_input_cached_batch DECIMAL(12,5), price_output_batch DECIMAL(12,5), accepts_image_input BOOLEAN NOT NULL DEFAULT 0, pricing_source VARCHAR(255), last_pricing_sync_at DATETIME, created_at DATETIME, updated_at DATETIME)");
    sql(
        "INSERT INTO openai_model(code,price_input_standard,price_output_standard) VALUES ('gpt-6.1-sol',2,10)");
    sql(
        "CREATE TABLE ai_prompt_schema_template (template_key VARCHAR(191) PRIMARY KEY, pipeline_code VARCHAR(100) NOT NULL, stage_code VARCHAR(100) NOT NULL, version VARCHAR(40) NOT NULL, openai_model VARCHAR(120) NOT NULL, schema_name VARCHAR(120) NOT NULL, prompt_markdown_content LONGTEXT NOT NULL, schema_json LONGTEXT NOT NULL, active BOOLEAN NOT NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL)");
    for (String source :
        new String[] {
          "2026-07-15-gera-sales-page-template-recovery-v7.yaml",
          "2026-07-15-gera-sales-page-exp66-delivery-v8.yaml",
          "2026-07-15-gera-sales-page-exp66-delivery-v9.yaml",
          "2026-08-03-gera-sales-page-generic-templates-v10.yaml",
          "2026-08-08-gera-sales-page-public-language-v11.yaml",
          "2026-08-20-gera-sales-page-approved-library-v12.yaml"
        }) {
      migration(source).update("");
    }
    sql(
        "INSERT INTO ai_prompt_schema_template SELECT 'unrelated', 'other-pipeline', stage_code, version, openai_model, schema_name, prompt_markdown_content, schema_json, 1, created_at, updated_at FROM ai_prompt_schema_template WHERE active=1 LIMIT 1");
    sql(
        "CREATE TABLE historical_execution(experiment_id BIGINT PRIMARY KEY, template_key VARCHAR(191), prompt_snapshot LONGTEXT, schema_snapshot LONGTEXT)");
    for (int experiment : new int[] {105, 9105}) {
      sql(
          "INSERT INTO historical_execution SELECT "
              + experiment
              + ",template_key,prompt_markdown_content,schema_json FROM ai_prompt_schema_template WHERE pipeline_code='gera-sales-page-v1' AND stage_code='sales-page-offer-brief' AND active=1");
    }
  }

  /** Libera a conexão do banco descartável após cada cenário. */
  @AfterEach
  void closeDatabase() throws Exception {
    if (connection != null) connection.close();
  }

  /** Valida sete contratos reais, duas identidades, reaplicação e rollback sem apagar provas. */
  @Test
  void versionsAllStagesWithoutChangingContentOrPastExecutions() throws Exception {
    String history = historicalSnapshot();
    String executions =
        value(
            "SELECT GROUP_CONCAT(CONCAT(experiment_id,':',template_key,':',SHA2(prompt_snapshot,256),':',SHA2(schema_snapshot,256)) ORDER BY experiment_id) FROM historical_execution");
    var migration = migration(MIGRATION);
    migration.update("");
    migration.update("");
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template WHERE pipeline_code='gera-sales-page-v1' AND active=1 AND openai_model='gpt-6.1-sol'"))
        .isEqualTo("7");
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template source JOIN ai_prompt_schema_template target ON target.template_key=CONCAT(source.template_key,':codex1') WHERE target.active=1 AND source.active=0 AND BINARY source.prompt_markdown_content=BINARY target.prompt_markdown_content AND BINARY source.schema_json=BINARY target.schema_json"))
        .isEqualTo("7");
    assertThat(historicalSnapshot()).isEqualTo(history);
    assertThat(
            value(
                "SELECT GROUP_CONCAT(CONCAT(experiment_id,':',template_key,':',SHA2(prompt_snapshot,256),':',SHA2(schema_snapshot,256)) ORDER BY experiment_id) FROM historical_execution"))
        .isEqualTo(executions);
    assertThat(value("SELECT active FROM ai_prompt_schema_template WHERE template_key='unrelated'"))
        .isEqualTo("1");
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template WHERE template_key LIKE '%:codex1'"))
        .isEqualTo("7");
    migration.rollback(1, "");
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template WHERE pipeline_code='gera-sales-page-v1' AND active=1 AND openai_model='gpt-5.5'"))
        .isEqualTo("7");
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template WHERE template_key LIKE '%:codex1'"))
        .isEqualTo("7");
    migration.update("");
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template WHERE active=1 AND openai_model='gpt-6.1-sol'"))
        .isEqualTo("7");
    assertThat(historicalSnapshot()).isEqualTo(history);
  }

  /** Rejeita fonte incompleta, modelo sem preço e colisão antes de desativar qualquer contrato. */
  @Test
  void rejectsIncompleteSourcesAndConflictingTargetsAtomically() throws Exception {
    var migration = migration(MIGRATION);
    sql("DELETE FROM openai_model");
    assertThatThrownBy(() -> migration.update("")).isInstanceOf(LiquibaseException.class);
    sql(
        "INSERT INTO openai_model(code,price_input_standard,price_output_standard) VALUES ('gpt-6.1-sol',2,10)");
    sql(
        "UPDATE ai_prompt_schema_template SET active=0 WHERE pipeline_code='gera-sales-page-v1' AND stage_code='sales-page-wireframe'");
    assertThatThrownBy(() -> migration.update("")).isInstanceOf(LiquibaseException.class);
    sql(
        "UPDATE ai_prompt_schema_template SET active=1 WHERE template_key='gera-sales-page-v1:sales-page-wireframe:v7'");
    sql(
        "INSERT INTO ai_prompt_schema_template SELECT CONCAT(template_key,':codex1'),pipeline_code,stage_code,CONCAT(version,'-codex1'),'conflicting-model',schema_name,prompt_markdown_content,schema_json,0,created_at,updated_at FROM ai_prompt_schema_template WHERE pipeline_code='gera-sales-page-v1' AND stage_code='sales-page-offer-brief' AND active=1");
    assertThatThrownBy(() -> migration.update("")).isInstanceOf(LiquibaseException.class);
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template WHERE pipeline_code='gera-sales-page-v1' AND active=1 AND openai_model='gpt-5.5'"))
        .isEqualTo("7");
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template WHERE active=1 AND openai_model='gpt-6.1-sol'"))
        .isEqualTo("0");
  }

  /** Não reativa a versão antiga quando uma evolução posterior já substituiu uma etapa. */
  @Test
  void rollbackPreservesFutureConfiguration() throws Exception {
    var migration = migration(MIGRATION);
    migration.update("");
    sql(
        "INSERT INTO ai_prompt_schema_template SELECT 'future-offer',pipeline_code,stage_code,'v99','future-model',schema_name,prompt_markdown_content,schema_json,1,created_at,updated_at FROM ai_prompt_schema_template WHERE pipeline_code='gera-sales-page-v1' AND stage_code='sales-page-offer-brief' AND active=1");
    sql(
        "UPDATE ai_prompt_schema_template SET active=0 WHERE stage_code='sales-page-offer-brief' AND template_key LIKE '%:codex1'");
    migration.rollback(1, "");
    assertThat(
            value(
                "SELECT openai_model FROM ai_prompt_schema_template WHERE pipeline_code='gera-sales-page-v1' AND stage_code='sales-page-offer-brief' AND active=1"))
        .isEqualTo("future-model");
    assertThatThrownBy(() -> migration.update("")).isInstanceOf(LiquibaseException.class);
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template WHERE pipeline_code='gera-sales-page-v1' AND active=1"))
        .isEqualTo("7");
  }

  /** Prepara instalações novas sem sobrescrever preço cadastrado ou apagá-lo no rollback. */
  @Test
  void seedsMissingCatalogBeforeActivatingContracts() throws Exception {
    sql("DELETE FROM openai_model");
    var catalog = migration("2026-10-19-codex-model-catalog-v1.yaml");
    catalog.update("");
    catalog.update("");
    assertThat(value("SELECT COUNT(*) FROM openai_model")).isEqualTo("1");
    assertThat(
            value(
                "SELECT CONCAT(price_input_standard,':',price_output_standard) FROM openai_model WHERE code='gpt-6.1-sol'"))
        .isEqualTo("2.00000:10.00000");
    migration(MIGRATION).update("");
    assertThat(
            value(
                "SELECT COUNT(*) FROM ai_prompt_schema_template WHERE active=1 AND openai_model='gpt-6.1-sol'"))
        .isEqualTo("7");
    migration(MIGRATION).rollback(1, "");
    catalog.rollback(1, "");
    sql(
        "UPDATE openai_model SET price_input_standard=7,price_output_standard=35 WHERE code='gpt-6.1-sol'");
    catalog.update("");
    assertThat(
            value(
                "SELECT CONCAT(price_input_standard,':',price_output_standard) FROM openai_model WHERE code='gpt-6.1-sol'"))
        .isEqualTo("7.00000:35.00000");
  }

  /** Carrega o arquivo versionado real e mantém a mesma conexão sintética entre aplicações. */
  private Liquibase migration(String file) throws Exception {
    return new Liquibase(
        "db/changelog/changesets/" + file,
        new ClassLoaderResourceAccessor(),
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection)));
  }

  /** Executa uma alteração restrita às fixtures desta classe. */
  private void sql(String query) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(query);
      if (!connection.getAutoCommit()) connection.commit();
    }
  }

  /** Lê um valor determinístico para verificar a integridade da migração. */
  private String value(String query) throws Exception {
    try (var statement = connection.createStatement();
        var rows = statement.executeQuery(query)) {
      assertThat(rows.next()).isTrue();
      return rows.getString(1);
    }
  }

  /** Compara hashes históricos sem incluir os campos de ativação alterados deliberadamente. */
  private String historicalSnapshot() throws Exception {
    sql("SET SESSION group_concat_max_len=100000");
    return value(
        "SELECT GROUP_CONCAT(CONCAT(template_key,':',openai_model,':',SHA2(prompt_markdown_content,256),':',SHA2(schema_json,256),':',created_at) ORDER BY template_key) FROM ai_prompt_schema_template WHERE template_key NOT LIKE '%:codex1'");
  }
}
