package com.marketinghub.geralanding;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o armazenamento integral dos prompts auditáveis do GeraLanding. */
class GeraLandingPromptContentChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String CHANGESET =
      "changesets/2026-08-10-gera-landing-prompt-content-longtext.yaml";

  /** Confirma que a entidade e o changelog preservam prompts maiores que o limite de TINYTEXT. */
  @Test
  void shouldPersistPromptContentAsLongText() throws Exception {
    Column column =
        GeraLandingStageExecution.class
            .getDeclaredField("promptContent")
            .getAnnotation(Column.class);
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));
    String changeset = Files.readString(CHANGELOG_ROOT.resolve(CHANGESET));

    assertThat(column.columnDefinition()).isEqualTo("LONGTEXT");
    assertThat(master).contains("file: " + CHANGESET + "\n      relativeToChangelogFile: true");
    assertThat(changeset)
        .contains("columnName: prompt_content")
        .contains("newDataType: LONGTEXT")
        .contains("type: mysql");
  }
}
