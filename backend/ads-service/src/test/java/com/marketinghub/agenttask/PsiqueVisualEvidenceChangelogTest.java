package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o schema MySQL 5.7 das provas visuais privadas de Psique. */
class PsiqueVisualEvidenceChangelogTest {
  private static final String FILE = "2026-08-29-psique-task-visual-evidence-v1.yaml";
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");
  private static final Path CHANGELOG =
      Path.of("src/main/resources/db/changelog/changesets/" + FILE);

  /** Exige include relativo no mestre para manter a resolução estável no Liquibase. */
  @Test
  void includesVisualEvidenceChangelogRelatively() throws Exception {
    assertThat(Files.readString(MASTER))
        .contains("file: changesets/" + FILE + "\n      relativeToChangelogFile: true");
  }

  /** Mantém tabela segregada, vínculo de tarefa e tempos compatíveis com MySQL 5.7. */
  @Test
  void createsPrivateVisualEvidenceMetadataWithoutTemporalRisk() throws Exception {
    String changelog = Files.readString(CHANGELOG);

    assertThat(changelog)
        .contains(
            "type: mysql",
            "splitStatements: true",
            "stripComments: true",
            "CREATE TABLE IF NOT EXISTS agent_task_visual_evidence",
            "capture_session_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL",
            "evidence_key VARCHAR(160) CHARACTER SET ascii COLLATE ascii_bin NOT NULL",
            "UNIQUE KEY uk_task_visual_evidence_session_key",
            "FOREIGN KEY (agent_task_id) REFERENCES agent_task(id) ON DELETE CASCADE",
            "captured_at DATETIME NOT NULL",
            "created_at DATETIME NOT NULL")
        .doesNotContain("TIMESTAMP NOT NULL", "UPDATE ", "DELETE FROM agent_task_visual_evidence");
  }
}
