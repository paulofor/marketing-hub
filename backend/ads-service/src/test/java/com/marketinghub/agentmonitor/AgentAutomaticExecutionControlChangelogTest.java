package com.marketinghub.agentmonitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir regressões no changelog MySQL 5.7 do controle PLAY/STOP. */
class AgentAutomaticExecutionControlChangelogTest {
  private static final Path CHANGELOG =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-20-agent-automatic-execution-control.yaml");
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");

  /** Valida include relativo, MySQL, SQL e campos temporais compatíveis com o cânone. */
  @Test
  void shouldKeepMysql57ContractAndRelativeInclude() throws Exception {
    String changelog = Files.readString(CHANGELOG);
    String master = Files.readString(MASTER);

    assertThat(changelog)
        .contains("type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("changed_at DATETIME NOT NULL")
        .doesNotContain("TIMESTAMP NOT NULL");
    assertThat(master)
        .containsSubsequence(
            "file: changesets/2026-08-20-agent-automatic-execution-control.yaml",
            "relativeToChangelogFile: true");
  }
}
