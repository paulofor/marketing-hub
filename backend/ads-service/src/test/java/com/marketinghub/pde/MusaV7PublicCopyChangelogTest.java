package com.marketinghub.pde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a copy pública do slot MUSA v7 contra vazamento técnico. */
class MusaV7PublicCopyChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final Path MASTER_CHANGELOG = CHANGELOG_ROOT.resolve("db.changelog-master.yaml");
  private static final String CLEANUP_CHANGESET =
      "changesets/2026-07-31-musa-v7-public-copy-cleanup.yaml";
  private static final String ORIGINAL_V7_CHANGESET =
      "changesets/2026-07-31-musa-v7-scientific-seven-signals.yaml";

  /** Confirma que o changelog mestre aplica a limpeza com caminho relativo ao próprio arquivo. */
  @Test
  void masterChangelogIncludesMusaV7PublicCopyCleanupWithRelativePath() throws IOException {
    String master = Files.readString(MASTER_CHANGELOG);

    assertThat(master)
        .contains("file: " + CLEANUP_CHANGESET)
        .containsSubsequence("file: " + CLEANUP_CHANGESET, "relativeToChangelogFile: true");
  }

  /** Confirma que o contrato inicial da v7 preserva o seed histórico já aplicado. */
  @Test
  void originalMusaV7ChangelogKeepsAppliedSeedChecksumStable() throws IOException {
    String changeset = Files.readString(CHANGELOG_ROOT.resolve(ORIGINAL_V7_CHANGESET));

    assertThat(changeset).contains("\"videoKicker\": \"PDE v7 científico\"");
  }

  /** Confirma que o reparo corrige contratos já persistidos no Marketing Hub. */
  @Test
  void cleanupChangesetReplacesTechnicalPublicCopyInPersistedContracts() throws IOException {
    String changeset = Files.readString(CHANGELOG_ROOT.resolve(CLEANUP_CHANGESET));

    assertThat(changeset)
        .contains("id: 2026-07-31-musa-v7-public-copy-cleanup-001")
        .contains("dbms:")
        .contains("type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("UPDATE pde_production_slot")
        .contains("WHERE slot_code = 'v7'")
        .contains("draft_experience_json = REPLACE")
        .contains("published_experience_json = REPLACE")
        .contains("'A jornada começa pelo idioma silencioso da roupa.'")
        .contains("'Cada dia trabalha um sinal de presença.'")
        .contains("'A promessa é ciência traduzida em microação.'")
        .contains("'Método MUSA em 7 dias'");
  }
}
