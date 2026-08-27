package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a persistência normalizada da cobertura de atividades por tarefa. */
class AgentTaskActivityCoverageChangelogTest {

  /**
   * Exige tabela, vínculos, idempotência e backfill restrito às homologações compostas de landing.
   */
  @Test
  void declaresNormalizedCoverageAndSafeLandingBackfill() throws Exception {
    String changelog =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-27-agent-task-activity-coverage.yaml"));

    assertThat(changelog)
        .contains("CREATE TABLE IF NOT EXISTS agent_task_activity_coverage")
        .contains("information_schema.table_constraints")
        .contains("UNIQUE KEY uk_agent_task_activity_coverage")
        .contains("FOREIGN KEY (agent_task_id) REFERENCES agent_task(id)")
        .contains(
            "FOREIGN KEY (activity_definition_id) REFERENCES business_process_activity_definition(id)")
        .contains("INSERT IGNORE INTO agent_task_activity_coverage")
        .contains("activity.activity_id IN ('select', 'strategy', 'compose')")
        .contains("process.process_code = 'landing-page-generation'")
        .contains("task.process_activity_id = 'html'")
        .contains("task.source_reference LIKE 'commercial-plan:%:journey%'")
        .doesNotContain("TIMESTAMP NOT NULL");
  }

  /** Confirma que o changelog novo usa include relativo no mestre. */
  @Test
  void masterUsesRelativeInclude() throws Exception {
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(master)
        .contains(
            "file: changesets/2026-08-27-agent-task-activity-coverage.yaml\n"
                + "      relativeToChangelogFile: true");
  }
}
