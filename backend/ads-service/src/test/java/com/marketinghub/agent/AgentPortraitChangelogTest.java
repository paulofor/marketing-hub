package com.marketinghub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato Liquibase da imagem de identificação dos agentes. */
class AgentPortraitChangelogTest {
  private static final Path CHANGELOG =
      Path.of("src/main/resources/db/changelog/changesets/2026-08-10-agent-portrait.yaml");
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");

  /** Confirma referência opcional ao asset e compatibilidade obrigatória com MySQL. */
  @Test
  void protectsPortraitAssetReference() throws IOException {
    String changelog = Files.readString(CHANGELOG);

    assertThat(changelog)
        .contains("type: mysql")
        .contains("ADD COLUMN portrait_asset_id BIGINT NULL")
        .contains("FOREIGN KEY (portrait_asset_id) REFERENCES asset(id)")
        .contains("splitStatements: true")
        .contains("stripComments: true");
  }

  /** Confirma resolução relativa do novo changelog no arquivo mestre. */
  @Test
  void includesPortraitChangelogRelatively() throws IOException {
    String master = Files.readString(MASTER);

    assertThat(master)
        .contains(
            "file: changesets/2026-08-10-agent-portrait.yaml\n"
                + "      relativeToChangelogFile: true");
  }
}
