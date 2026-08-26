package com.marketinghub.pde;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a retomada do contrato público v2 da Rigel em slots sem rascunho. */
class RigelCommercialExperienceSlotRepairChangelogTest {
  private static final Path CHANGELOG =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-26-rigel-commercial-experience-v2-slot-repair.yaml");
  private static final Path SQL =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-26-rigel-commercial-experience-v2-slot-repair.sql");

  /** Deve atualizar o contrato publicado sem exigir que um slot ativo mantenha rascunho. */
  @Test
  void shouldRepairPublishedSlotWithoutDraft() throws Exception {
    String changelog = Files.readString(CHANGELOG);
    String sql = Files.readString(SQL);

    assertThat(changelog)
        .contains("dbms:")
        .contains("type: mysql")
        .contains("relativeToChangelogFile: true")
        .contains("splitStatements: true")
        .contains("stripComments: true");
    assertThat(sql)
        .contains("published_experience_json = JSON_SET")
        .contains("draft_experience_json = CASE")
        .contains("ELSE draft_experience_json")
        .contains("'$.experienceVersion', 'kit-whatsapp-pronto-pde-v2'")
        .doesNotContain("AND draft_experience_json IS NOT NULL")
        .doesNotContain("SELECT");
  }
}
