package com.marketinghub.systemimprovement;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato MySQL 5.7 do backlog de melhorias dos agentes. */
class SystemImprovementChangelogTest {

  /** Exige autoria, data DATETIME e include relativo no changelog mestre. */
  @Test
  void declaresMysql57CompatibleSchemaAndRelativeInclude() throws Exception {
    String change =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-11-system-improvements.yaml"));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(change)
        .contains("dbms:\n            type: mysql")
        .contains("requested_by_agent_id BIGINT NOT NULL")
        .contains("requested_at DATETIME NOT NULL")
        .doesNotContain("TIMESTAMP NOT NULL");
    assertThat(master)
        .contains(
            "file: changesets/2026-08-11-system-improvements.yaml\n"
                + "      relativeToChangelogFile: true");
  }
}
