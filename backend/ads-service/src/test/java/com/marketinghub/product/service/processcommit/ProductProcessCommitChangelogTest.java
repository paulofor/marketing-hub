package com.marketinghub.product.service.processcommit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato MySQL 5.7 da tabela de commits por produto e processo. */
class ProductProcessCommitChangelogTest {
  private static final Path CHANGELOG =
      Path.of("src/main/resources/db/changelog/changesets/2026-08-26-product-process-commits.yaml");
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");

  /** Mantém chaves, deduplicação e temporalidade compatíveis com MySQL 5.7. */
  @Test
  void createsAuditableProductProcessCommitTable() throws Exception {
    String changelog = Files.readString(CHANGELOG);

    assertThat(changelog)
        .contains("dbms:")
        .contains("type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("CREATE TABLE IF NOT EXISTS product_process_commit")
        .contains("process_definition_id BIGINT NOT NULL")
        .contains("commit_sha VARCHAR(64)")
        .contains("recorded_at DATETIME NOT NULL")
        .contains("UNIQUE KEY uk_product_process_commit")
        .contains("FOREIGN KEY (product_id) REFERENCES product(id)")
        .contains("FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id)")
        .doesNotContain("TIMESTAMP NOT NULL");
  }

  /** Garante que o include relativo novo usa resolução pelo changelog mestre. */
  @Test
  void includesChangelogRelativeToMaster() throws Exception {
    String master = Files.readString(MASTER);
    String include = "file: changesets/2026-08-26-product-process-commits.yaml";
    int includeIndex = master.indexOf(include);

    assertThat(includeIndex).isGreaterThanOrEqualTo(0);
    assertThat(master.substring(includeIndex, includeIndex + include.length() + 50))
        .contains("relativeToChangelogFile: true");
  }
}
