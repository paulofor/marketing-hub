package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a migração MySQL 5.7 de atividades, instâncias e tentativas. */
class BusinessProcessActivityInstanceChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String FILE = "2026-08-25-business-process-activity-instances.yaml";

  /** Confirma que o mestre resolve o novo changelog relativamente ao próprio arquivo. */
  @Test
  void masterIncludesActivityInstancesRelatively() throws IOException {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));
    assertThat(master)
        .contains("file: changesets/" + FILE + "\n      relativeToChangelogFile: true");
  }

  /** Confirma entidades, vínculos e campos temporais compatíveis com MySQL 5.7. */
  @Test
  void createsExplicitThreeLevelModel() throws IOException {
    String yaml = Files.readString(CHANGELOG_ROOT.resolve("changesets").resolve(FILE));
    assertThat(yaml)
        .contains("CREATE TABLE IF NOT EXISTS business_process_activity_definition")
        .contains("CREATE TABLE IF NOT EXISTS business_process_activity_instance")
        .contains("activity_instance_id BIGINT NULL")
        .contains("entered_at DATETIME NOT NULL")
        .contains("exited_at DATETIME NULL")
        .contains("created_at DATETIME NOT NULL")
        .contains("updated_at DATETIME NOT NULL")
        .contains("fk_agent_task_activity_instance")
        .doesNotContain("TIMESTAMP NOT NULL");
  }

  /** Protege o backfill idempotente contra a leitura 1093 da própria tabela-alvo. */
  @Test
  void backfillsWithJoinsAndPreservesLegacyTasks() throws IOException {
    String yaml = Files.readString(CHANGELOG_ROOT.resolve("changesets").resolve(FILE));
    assertThat(yaml)
        .contains("JSON_LENGTH(process.diagram_json, '$.nodes')")
        .contains("INSERT IGNORE INTO business_process_activity_definition")
        .contains("INSERT IGNORE INTO business_process_activity_instance")
        .contains("MAX(id) AS latest_task_id")
        .contains("BINARY instance.source_reference = BINARY task.source_reference")
        .contains("UPDATE agent_task task\n              JOIN business_process_activity_definition")
        .contains("WHERE task.activity_instance_id IS NULL")
        .doesNotContain("UPDATE agent_task SET activity_instance_id = (SELECT")
        .doesNotContain("process_definition_id BIGINT NOT NULL AFTER");
  }
}
