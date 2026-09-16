package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pde.PdeProductionSlotStatus;
import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Responsabilidade: comprovar a migração conservadora da candidata comercial Vega v12. */
@EnabledIfEnvironmentVariable(named = "PDE_CONTRACT_MYSQL", matches = "local")
class VegaV12CandidateMysql57Test {

  private static final String CANDIDATE_CHANGELOG = "2026-09-16-vega-v12-commercial-candidate.yaml";
  private static final String STATUS_REPAIR_CHANGELOG =
      "2026-09-16-pde-production-slot-status-repair.yaml";

  /** Valida criação, idempotência, isolamento da v7 e preservação de uma candidata já promovida. */
  @Test
  void createsV12CandidateWithoutPublishingOrOverwritingEvidence() throws Exception {
    createFixture();

    migrate(CANDIDATE_CHANGELOG, false);
    try (var connection = connection()) {
      assertThat(value(connection, "SELECT status FROM pde_production_slot WHERE slot_code='v8'"))
          .isEmpty();
    }
    migrate(STATUS_REPAIR_CHANGELOG, false);
    assertCandidate();
    migrate(STATUS_REPAIR_CHANGELOG, false);
    assertCandidate();

    try (var connection = connection();
        var statement = connection.createStatement()) {
      statement.execute(
          "UPDATE pde_production_slot SET status='READY', published_experience_json='snapshot-homologado', validation_status='OK' WHERE slot_code='v8'");
      statement.execute(
          "DELETE FROM DATABASECHANGELOG WHERE ID='2026-09-16-vega-v12-commercial-candidate-001'");
    }
    migrate(CANDIDATE_CHANGELOG, false);
    migrate(STATUS_REPAIR_CHANGELOG, false);
    try (var connection = connection()) {
      assertThat(value(connection, "SELECT status FROM pde_production_slot WHERE slot_code='v8'"))
          .isEqualTo("READY");
      assertThat(
              value(
                  connection,
                  "SELECT published_experience_json FROM pde_production_slot WHERE slot_code='v8'"))
          .isEqualTo("snapshot-homologado");
      assertThat(
              value(
                  connection,
                  "SELECT experience_version FROM pde_production_slot WHERE slot_code='v7'"))
          .isEqualTo("musa-pde-entry-v7-espelho-antes-de-sair");
    }
  }

  /** Cria somente as tabelas e o histórico necessários para reproduzir o caso real no MySQL 5.7. */
  private void createFixture() throws Exception {
    try (var connection = connection();
        var statement = connection.createStatement()) {
      statement.execute(
          "DROP TABLE IF EXISTS DATABASECHANGELOGLOCK, DATABASECHANGELOG, pde_production_slot, experiment");
      statement.execute("SET GLOBAL sql_mode=''");
      statement.execute("SET SESSION sql_mode=''");
      statement.execute(
          "CREATE TABLE experiment (id BIGINT PRIMARY KEY, product_id BIGINT NOT NULL, status VARCHAR(32) NOT NULL, unit_price_brl DECIMAL(10,2), commercial_checkout_url VARCHAR(512), updated_at DATETIME NOT NULL)");
      statement.execute(
          "INSERT INTO experiment VALUES (92,4,'PLANNED',67.00,NULL,'2026-09-16 00:00:00')");
      statement.execute(
          "CREATE TABLE pde_production_slot (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, slot_code VARCHAR(64) NOT NULL, product_slug VARCHAR(191) NOT NULL, domain VARCHAR(191) NOT NULL, public_url VARCHAR(512) NOT NULL, backend_url VARCHAR(512), experience_version VARCHAR(120) NOT NULL, layout_key VARCHAR(80) NOT NULL, target_environment VARCHAR(64) NOT NULL, status ENUM('PLANNED','READY','ACTIVE','PAUSED','RETIRED') NOT NULL, source_experiment_id BIGINT, notes LONGTEXT, draft_experience_json LONGTEXT, published_experience_json LONGTEXT, published_by VARCHAR(191), published_at DATETIME, validation_status VARCHAR(32), created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, UNIQUE KEY uk_slot (product_slug,slot_code), UNIQUE KEY uk_domain (domain))");
      statement.execute(
          "INSERT INTO pde_production_slot (slot_code,product_slug,domain,public_url,experience_version,layout_key,target_environment,status,source_experiment_id,notes,draft_experience_json,published_experience_json,published_by,published_at,validation_status,created_at,updated_at) VALUES ('v7','metodo-musa-7-dias','v7.clubemusa.com.br','https://v7.clubemusa.com.br','musa-pde-entry-v7-espelho-antes-de-sair','espelho-antes-de-sair','production-v7','ACTIVE',90,'histórico v7','v7-draft','v7-publicado','human','2026-07-31 00:00:00','OK','2026-07-31 00:00:00','2026-07-31 00:00:00')");
    }
  }

  /** Confere que a migração prepara a v12 sem declará-la publicada ou homologada. */
  private void assertCandidate() throws Exception {
    try (var connection = connection()) {
      assertThat(value(connection, "SELECT COUNT(*) FROM pde_production_slot WHERE slot_code='v8'"))
          .isEqualTo("1");
      assertThat(
              value(
                  connection,
                  "SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pde_production_slot' AND COLUMN_NAME='status'"))
          .isEqualTo("varchar(32)");
      assertThat(
              PdeProductionSlotStatus.valueOf(
                  value(connection, "SELECT status FROM pde_production_slot WHERE slot_code='v8'")))
          .isEqualTo(PdeProductionSlotStatus.CANDIDATE);
      assertThat(
              value(
                  connection,
                  "SELECT CONCAT(status,':',source_experiment_id,':',public_url) FROM pde_production_slot WHERE slot_code='v8'"))
          .isEqualTo("CANDIDATE:92:https://v8.clubemusa.com.br");
      assertThat(
              value(
                  connection,
                  "SELECT JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json,'$.commercialBinding.experimentId')) FROM pde_production_slot WHERE slot_code='v8'"))
          .isEqualTo("92");
      assertThat(
              value(
                  connection,
                  "SELECT JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json,'$.supportMaterials[0].url')) FROM pde_production_slot WHERE slot_code='v8'"))
          .startsWith("/materials/musa-v12/");
      assertThat(
              value(
                  connection,
                  "SELECT published_experience_json IS NULL FROM pde_production_slot WHERE slot_code='v8'"))
          .isEqualTo("1");
      assertThat(value(connection, "SELECT commercial_checkout_url FROM experiment WHERE id=92"))
          .isEqualTo("https://go.pepper.com.br/owm6x");
      assertThat(
              value(
                  connection,
                  "SELECT published_experience_json FROM pde_production_slot WHERE slot_code='v7'"))
          .isEqualTo("v7-publicado");
    }
  }

  /** Usa somente o banco descartável e a credencial sintética da homologação local. */
  private Connection connection() throws Exception {
    String host = System.getenv().getOrDefault("PDE_CONTRACT_MYSQL_HOST", "127.0.0.1");
    assertThat(host).isIn("127.0.0.1", "localhost", "sandbox-docker");
    String port = System.getenv().getOrDefault("PDE_CONTRACT_MYSQL_PORT", "33385");
    return DriverManager.getConnection(
        "jdbc:mysql://"
            + host
            + ":"
            + port
            + "/pde_contract_qa?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
        "root",
        "pde-contract-local-only");
  }

  /** Executa o changelog versionado no mesmo MySQL 5.7 isolado usado pelo contrato histórico. */
  private void migrate(String changelog, boolean rollback) throws Exception {
    var database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection()));
    try (var migration =
        new Liquibase(
            "db/changelog/changesets/" + changelog, new ClassLoaderResourceAccessor(), database)) {
      if (rollback) migration.rollback(1, "");
      else migration.update("");
    }
  }

  /** Obtém um valor escalar sem alterar o estado conferido. */
  private String value(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var rows = statement.executeQuery(sql)) {
      assertThat(rows.next()).isTrue();
      return rows.getString(1);
    }
  }
}
