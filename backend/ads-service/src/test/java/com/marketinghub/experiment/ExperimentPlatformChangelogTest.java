package com.marketinghub.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a extensibilidade do canal de aquisição entre Java e MySQL. */
class ExperimentPlatformChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String PLATFORM_CHANGELOG =
      "changesets/2026-08-22-experiment-platform-varchar.yaml";

  /** Confirma que o changelog mestre aplica a migração por caminho relativo. */
  @Test
  void shouldIncludePlatformMigrationFromMasterChangelog() throws Exception {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));

    assertThat(master)
        .contains("file: " + PLATFORM_CHANGELOG)
        .containsSubsequence("file: " + PLATFORM_CHANGELOG, "relativeToChangelogFile: true");
  }

  /** Confirma que novos canais Java não ficam limitados pelo antigo ENUM físico do MySQL. */
  @Test
  void shouldPersistExperimentPlatformAsExtensibleVarchar() throws Exception {
    String changelog = Files.readString(CHANGELOG_ROOT.resolve(PLATFORM_CHANGELOG));
    String entity =
        Files.readString(Path.of("src/main/java/com/marketinghub/experiment/Experiment.java"));

    assertThat(ExperimentPlatform.values())
        .containsExactly(ExperimentPlatform.FACEBOOK, ExperimentPlatform.DIRECT_ONE_TO_ONE);
    assertThat(changelog)
        .contains("ALTER TABLE experiment")
        .contains("MODIFY COLUMN platform VARCHAR(40) NULL")
        .doesNotContain("ENUM('FACEBOOK')");
    assertThat(entity)
        .containsSubsequence(
            "@Enumerated(EnumType.STRING)",
            "@JdbcTypeCode(SqlTypes.VARCHAR)",
            "@Column(name = \"platform\", length = 40)",
            "private ExperimentPlatform platform;");
  }
}
