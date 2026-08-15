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
}
