package com.marketinghub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o versionamento e as fronteiras persistidas de Atena, Hermes e Têmis. */
class AgentResponsibilityBoundariesChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String CHANGELOG =
      "changesets/2026-08-28-agent-responsibility-boundaries-v2.yaml";

  /** Confirma que o mestre resolve o novo contrato por caminho relativo obrigatório. */
  @Test
  void shouldIncludeResponsibilityBoundariesRelatively() throws Exception {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));

    assertThat(master)
        .contains(
            "file: " + CHANGELOG + System.lineSeparator() + "      relativeToChangelogFile: true");
  }

  /** Impede reutilizar números históricos quando os agentes já possuem versões superiores. */
  @Test
  void shouldCreateNextAgentVersionsDynamically() throws Exception {
    String changelog = Files.readString(CHANGELOG_ROOT.resolve(CHANGELOG));

    assertThat(occurrences(changelog, "current_version = current_version + 1")).isEqualTo(3);
    assertThat(occurrences(changelog, "SELECT a.id, a.current_version,")).isEqualTo(3);
    assertThat(changelog)
        .doesNotContain("GREATEST(current_version, 2)", "SELECT a.id, 2,")
        .contains(
            "ATENA_DEFINES_MARKET_STRATEGY",
            "HERMES_OPERATES_GROWTH",
            "TEMIS_TRANSLATES_STRATEGY_INTO_COMMUNICATION");
  }

  /** Confirma que a cadeia publicada referencia o processo com responsabilidades separadas. */
  @Test
  void shouldPublishCommunicationAndValueChainVersions() throws Exception {
    String changelog = Files.readString(CHANGELOG_ROOT.resolve(CHANGELOG));

    assertThat(changelog)
        .contains(
            "'pde-communication-sales-journey'",
            "responsibleAgentKeys",
            "\"meta-ad-approver\"",
            "\"growth-operator\"",
            "version_number = 5",
            "version_number = 7");
  }

  /** Conta ocorrências literais sem depender de expressões regulares. */
  private int occurrences(String content, String expected) {
    return (content.length() - content.replace(expected, "").length()) / expected.length();
  }
}
