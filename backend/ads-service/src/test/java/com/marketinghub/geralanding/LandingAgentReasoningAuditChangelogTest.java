package com.marketinghub.geralanding;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a auditoria do esforço de raciocínio das execuções do Dédalo. */
class LandingAgentReasoningAuditChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String CHANGESET =
      "changesets/2026-08-27-landing-agent-reasoning-audit.yaml";

  /** Confirma a coluna opcional e compatível com MySQL 5.7 usada na auditoria técnica. */
  @Test
  void shouldPersistConfiguredReasoningEffort() throws Exception {
    Column column =
        GeraLandingStageExecution.class
            .getDeclaredField("executionReasoningEffort")
            .getAnnotation(Column.class);
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));
    String changeset = Files.readString(CHANGELOG_ROOT.resolve(CHANGESET));

    assertThat(column.name()).isEqualTo("execution_reasoning_effort");
    assertThat(column.length()).isEqualTo(32);
    assertThat(master).contains("file: " + CHANGESET + "\n      relativeToChangelogFile: true");
    assertThat(changeset)
        .contains("onFail: MARK_RAN")
        .contains("type: mysql")
        .contains("name: execution_reasoning_effort")
        .contains("type: VARCHAR(32)");
  }
}
