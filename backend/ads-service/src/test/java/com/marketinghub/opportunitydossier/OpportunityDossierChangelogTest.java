package com.marketinghub.opportunitydossier;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato MySQL 5.7 do dossiê de oportunidade. */
class OpportunityDossierChangelogTest {
  /** Confirma campos temporais seguros, LONGTEXT e inclusão relativa no mestre. */
  @Test
  void preservesMysql57Contract() throws Exception {
    String changelog =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-11-opportunity-dossier-v1.yaml"));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));
    assertThat(changelog)
        .contains(
            "dbms:",
            "type: mysql",
            "LONGTEXT",
            "created_at DATETIME NOT NULL",
            "updated_at DATETIME NOT NULL")
        .doesNotContain("TIMESTAMP NOT NULL");
    assertThat(master)
        .contains(
            "file: changesets/2026-08-11-opportunity-dossier-v1.yaml\n      relativeToChangelogFile: true");
  }
}
