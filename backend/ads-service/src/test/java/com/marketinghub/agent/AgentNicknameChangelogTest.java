package com.marketinghub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato Liquibase do apelido dos agentes. */
class AgentNicknameChangelogTest {

  private static final Path CHANGELOG =
      Path.of("src/main/resources/db/changelog/changesets/2026-08-10-agent-nickname.yaml");
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");

  /** Confirma campo obrigatorio, indice unico e backfill seguro para registros existentes. */
  @Test
  void protectsNicknameSchemaAndBackfill() throws IOException {
    String changelog = Files.readString(CHANGELOG);

    assertThat(changelog)
        .contains("ADD COLUMN nickname VARCHAR(60) NULL")
        .contains("MODIFY COLUMN nickname VARCHAR(60) NOT NULL")
        .contains("ADD UNIQUE INDEX uq_agent_nickname (nickname)")
        .contains("LEFT(COALESCE(NULLIF(TRIM(agent_key), ''), 'agente'), 48)")
        .contains("splitStatements: true")
        .contains("stripComments: true");
  }

  /** Confirma que o changelog mestre resolve o include relativamente ao proprio arquivo. */
  @Test
  void includesNicknameChangelogRelatively() throws IOException {
    String master = Files.readString(MASTER);

    assertThat(master)
        .contains(
            "file: changesets/2026-08-10-agent-nickname.yaml\n"
                + "      relativeToChangelogFile: true");
  }
}
