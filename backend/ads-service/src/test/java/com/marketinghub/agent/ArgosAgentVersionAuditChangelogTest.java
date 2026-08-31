package com.marketinghub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Protege o histórico auditável da versão autônoma v4 do agente Argos. */
class ArgosAgentVersionAuditChangelogTest {

  /** Exige contrato MySQL 5.7 idempotente sem alterar o changeset já publicado do Argos. */
  @Test
  void shouldPersistArgosVersionFourWithIdempotentHistory() throws Exception {
    Path moduleRoot = Path.of("").toAbsolutePath();
    String changelog =
        Files.readString(
            moduleRoot.resolve(
                "src/main/resources/db/changelog/changesets/2026-08-31-argos-agent-version-v4-audit.yaml"));
    String master =
        Files.readString(
            moduleRoot.resolve("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(changelog)
        .contains(
            "logicalFilePath: db/changelog/changesets/2026-08-31-argos-agent-version-v4-audit.yaml",
            "dbms:",
            "type: mysql",
            "splitStatements: true",
            "stripComments: true",
            "LEFT JOIN agent_version av",
            "av.version_number = 4",
            "av.id IS NULL",
            "sourceChangeSet",
            "previousCurrentVersion",
            "JSON_UNQUOTE(JSON_EXTRACT(av.contract_snapshot, '$.sourceChangeSet'))",
            "JSON_UNQUOTE(JSON_EXTRACT(av.contract_snapshot, '$.previousCurrentVersion'))",
            "marketDiscoveryVersion",
            "META_AD_LIBRARY")
        .doesNotContain("TIMESTAMP NOT NULL", "CURRENT_TIMESTAMP(6)");
    assertThat(master)
        .contains(
            "file: changesets/2026-08-31-argos-agent-version-v4-audit.yaml\n"
                + "      relativeToChangelogFile: true");
  }
}
