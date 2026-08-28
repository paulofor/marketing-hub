package com.marketinghub.businessprocessresource;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger schema, semente e inclusão MySQL 5.7 dos recursos de processo. */
class BusinessProcessExecutionResourceChangelogTest {
  private static final Path CHANGELOG =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-20-business-process-execution-resources.yaml");
  private static final Path RESPONSIBILITY_MATRIX =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-28-agent-responsibility-matrix-v3.yaml");

  /** Exige catálogo persistido, Estúdio de Têmis e datas compatíveis com MySQL 5.7. */
  @Test
  void declaresExecutionResourceCatalogAndTemisStudio() throws Exception {
    String yaml = Files.readString(CHANGELOG);

    assertThat(yaml)
        .contains("dbms:\n            type: mysql")
        .contains("CREATE TABLE business_process_execution_resource")
        .contains("created_at DATETIME NOT NULL")
        .contains("updated_at DATETIME NOT NULL")
        .contains("'themis-image-studio'")
        .contains("'meta-ad-approver'")
        .doesNotContain("TIMESTAMP NOT NULL");
  }

  /** Confirma que a matriz corrige a propriedade histórica e cadastra os recursos canônicos. */
  @Test
  void reassignsProductionResourcesToDedaloAndApollo() throws Exception {
    String yaml = Files.readString(RESPONSIBILITY_MATRIX);

    assertThat(yaml)
        .contains(
            "WHERE resource_code = 'themis-image-studio'",
            "responsible_agent_key = 'landing-generator'",
            "'pde-visual-materialization'",
            "'video-management-service'",
            "'videomaker'");
  }

  /** Confirma resolução relativa obrigatória do novo changelog. */
  @Test
  void masterUsesRelativeInclude() throws Exception {
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(master)
        .contains(
            "file: changesets/2026-08-20-business-process-execution-resources.yaml\n"
                + "      relativeToChangelogFile: true");
  }
}
