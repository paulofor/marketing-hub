package com.marketinghub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Protege o histórico auditável das versões operacionais v4, v5 e v6 do agente Argos. */
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

  /** Exige uma versão v6 reversível que preserve a v5 e vincule os contratos estritos. */
  @Test
  void shouldPersistArgosVersionSixWithStrictStageContracts() throws Exception {
    Path moduleRoot = Path.of("").toAbsolutePath();
    String changelog =
        Files.readString(
            moduleRoot.resolve(
                "src/main/resources/db/changelog/changesets/2026-09-24-argos-agent-version-v6-strict-contracts.yaml"));
    String master =
        Files.readString(
            moduleRoot.resolve("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(changelog)
        .contains(
            "logicalFilePath: db/changelog/changesets/2026-09-24-argos-agent-version-v6-strict-contracts.yaml",
            "dbms:",
            "type: mysql",
            "splitStatements: true",
            "stripComments: true",
            "LEFT JOIN agent_version av",
            "av.version_number = 6",
            "a.current_version IN (4, 5)",
            "av.id IS NULL",
            "STRICT_OUTPUT_BY_ACTIVITY_V1",
            "gap-deepening-schema.json",
            "startupContractValidation",
            "structuredFailurePreservation",
            "previousCurrentVersion",
            "JSON_UNQUOTE(JSON_EXTRACT(av.contract_snapshot, '$.sourceChangeSet'))")
        .doesNotContain("TIMESTAMP NOT NULL", "CURRENT_TIMESTAMP(6)");
    assertThat(changelog.split(";")).allSatisfy(this::assertNoMysql1093Pattern);
    assertThat(master)
        .contains(
            "file: changesets/2026-09-24-argos-agent-version-v6-strict-contracts.yaml\n"
                + "      relativeToChangelogFile: true");
  }

  /** Valida o risco 1093 dentro de cada comando sem misturar SQLs independentes. */
  private void assertNoMysql1093Pattern(String statement) {
    java.util.regex.Matcher target =
        java.util.regex.Pattern.compile("(?is)^\\s*(?:UPDATE|DELETE\\s+FROM)\\s+([a-z0-9_]+)")
            .matcher(statement);
    if (!target.find()) return;
    String table = java.util.regex.Pattern.quote(target.group(1));
    assertThat(statement).doesNotMatch("(?is).*SELECT.+FROM\\s+`?" + table + "`?(?:\\s|$).*");
  }
}
