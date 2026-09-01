package com.marketinghub.product.service.valuechainposition;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o índice que evita varredura integral das tarefas históricas. */
class AgentTaskHistorySourceIndexChangelogTest {
  private static final Path CHANGELOG =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-09-01-agent-task-history-source-index.yaml");
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");

  /** Exige índice compatível com utf8mb4 no vínculo consultado pelo histórico sob demanda. */
  @Test
  void indexesTaskSourceReferenceForMysql57() throws Exception {
    String changelog = Files.readString(CHANGELOG);
    String master = Files.readString(MASTER);

    assertThat(changelog)
        .contains("databaseChangeLog:")
        .contains("type: mysql")
        .contains("idx_agent_task_source_reference")
        .contains("source_reference(191)")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .doesNotContain("TIMESTAMP NOT NULL");
    assertThat(master)
        .contains(
            "file: changesets/2026-09-01-agent-task-history-source-index.yaml\n"
                + "      relativeToChangelogFile: true");
  }
}
