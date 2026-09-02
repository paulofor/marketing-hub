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
    String argosCycle =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-11-opportunity-dossier-argos-cycle.yaml"));
    String privateValidation =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-09-02-atena-private-validation-handoff-v1.yaml"));
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
            "file: changesets/2026-08-11-opportunity-dossier-v1.yaml\n      relativeToChangelogFile: true",
            "file: changesets/2026-08-11-opportunity-dossier-argos-cycle.yaml\n      relativeToChangelogFile: true",
            "file: changesets/2026-09-02-atena-private-validation-handoff-v1.yaml\n      relativeToChangelogFile: true");
    assertThat(argosCycle)
        .contains(
            "dbms:",
            "type: mysql",
            "product_discovery_cycle_id BIGINT NULL",
            "LEFT JOIN product_discovery_cycle",
            "LEFT JOIN agent_task")
        .doesNotContain("TIMESTAMP NOT NULL", "NOT EXISTS (SELECT");
    assertThat(privateValidation)
        .contains(
            "dbms:",
            "type: mysql",
            "READY_FOR_PRIVATE_VALIDATION",
            "version_number = 6",
            "version_number = 10",
            "SET status = 'PUBLISHED'",
            "published_at = COALESCE(published_at, UTC_TIMESTAMP())",
            "splitStatements: true",
            "stripComments: true")
        .doesNotContain("TIMESTAMP NOT NULL", "UPDATE opportunity_dossier");
  }
}
