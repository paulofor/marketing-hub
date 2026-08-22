package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger schema, semente e inclusão MySQL 5.7 do catálogo de processos. */
class BusinessProcessChangelogTest {
  /** Exige datas DATETIME, primeiro processo e include relativo. */
  @Test
  void declaresCompatibleVersionedCatalogAndLandingSeed() throws Exception {
    String change =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-14-business-process-catalog.yaml"));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(change)
        .contains("dbms:\n            type: mysql")
        .contains("created_at DATETIME NOT NULL")
        .contains("'landing-page-generation'")
        .doesNotContain("TIMESTAMP NOT NULL");
    assertThat(master)
        .contains(
            "file: changesets/2026-08-14-business-process-catalog.yaml\n      relativeToChangelogFile: true");
  }

  /** Exige classificação, pais explícitos e consolidação sem apagar o histórico. */
  @Test
  void declaresExclusiveResponsibilityBoundariesAndChainV5() throws Exception {
    String change =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-22-business-process-responsibility-boundaries.yaml"));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(change)
        .contains("process_type VARCHAR(20) NOT NULL DEFAULT 'VALUE_PROCESS'")
        .contains("parent_process_code VARCHAR(100) NULL")
        .contains("'SUBPROCESS'")
        .contains("\"subprocessCode\":\"creative-production-approval\"")
        .contains("\"subprocessCode\":\"experiment-homologation-activation\"")
        .contains("\"subprocessCode\":\"operacao-otimizacao-experimento\"")
        .contains("WHERE process_code = 'product-manufacturing-approval'")
        .contains("version_number = 5")
        .doesNotContain("TIMESTAMP NOT NULL")
        .doesNotContain(
            "UPDATE business_process_definition\n              SET status = 'RETIRED'\n              WHERE id IN (SELECT");
    assertThat(master)
        .contains(
            "file: changesets/2026-08-22-business-process-responsibility-boundaries.yaml\n      relativeToChangelogFile: true");
  }
}
