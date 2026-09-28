package com.marketinghub.facebookads;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Valida o contrato MySQL 5.7 que persiste a janela oficial observada na Meta. */
class FacebookCampaignWindowReconciliationChangelogTest {
  private static final Path CHANGELOG_DIR = Path.of("src/main/resources/db/changelog");
  private static final String CHANGESET =
      "changesets/2026-09-28-facebook-campaign-window-reconciliation-v1.yaml";

  /** Confirma que o mestre resolve o novo changelog relativamente ao próprio arquivo. */
  @Test
  void masterIncludesWindowReconciliationWithRelativePath() throws IOException {
    String master = Files.readString(CHANGELOG_DIR.resolve("db.changelog-master.yaml"));

    assertThat(master).containsSubsequence("file: " + CHANGESET, "relativeToChangelogFile: true");
  }

  /** Confirma colunas temporais DATETIME e SQL idempotente compatível com MySQL 5.7. */
  @Test
  void changesetPersistsMetaWindowAndStatusesWithMysqlContracts() throws IOException {
    String changeset = Files.readString(CHANGELOG_DIR.resolve(CHANGESET));

    assertThat(changeset)
        .contains("databaseChangeLog:")
        .contains("type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("meta_configured_status VARCHAR(32) NULL")
        .contains("meta_effective_status VARCHAR(32) NULL")
        .contains("meta_start_time DATETIME NULL")
        .contains("meta_stop_time DATETIME NULL")
        .contains("status_last_synced_at DATETIME NULL")
        .contains("budget_remaining_minor BIGINT NULL")
        .doesNotContain("TIMESTAMP NOT NULL");
  }
}
