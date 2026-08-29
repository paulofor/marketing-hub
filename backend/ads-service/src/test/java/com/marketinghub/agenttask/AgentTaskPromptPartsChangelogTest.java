package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a persistência MySQL 5.7 das partes explícitas dos prompts. */
class AgentTaskPromptPartsChangelogTest {
  private static final String FILE = "2026-08-29-agent-task-prompt-parts-v1.yaml";
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");
  private static final Path CHANGELOG =
      Path.of("src/main/resources/db/changelog/changesets/" + FILE);

  /** Exige include relativo no mestre para manter a resolução estável no Liquibase. */
  @Test
  void includesPromptPartsChangelogRelatively() throws Exception {
    assertThat(Files.readString(MASTER))
        .contains("file: changesets/" + FILE + "\n      relativeToChangelogFile: true");
  }

  /** Exige partes separadas em tarefas e execuções técnicas sem risco temporal ou MySQL 1093. */
  @Test
  void addsLongTextPromptPartsToBothAuditSources() throws Exception {
    String changelog = Files.readString(CHANGELOG);

    assertThat(changelog)
        .contains(
            "type: mysql",
            "splitStatements: true",
            "stripComments: true",
            "execution_agent_prompt LONGTEXT NULL",
            "execution_activity_prompt LONGTEXT NULL",
            "agent_prompt_part LONGTEXT NULL",
            "activity_prompt_part LONGTEXT NULL")
        .doesNotContain("TIMESTAMP NOT NULL", "UPDATE ", "DELETE ");
  }
}
