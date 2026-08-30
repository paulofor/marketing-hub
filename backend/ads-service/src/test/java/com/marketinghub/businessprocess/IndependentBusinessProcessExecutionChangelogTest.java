package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a migração MySQL 5.7 das execuções independentes de processo. */
class IndependentBusinessProcessExecutionChangelogTest {
  private static final Path ROOT = Path.of("src/main/resources/db/changelog");
  private static final String FILE = "2026-08-30-independent-business-process-executions.yaml";

  /** Confirma que o mestre resolve o changelog relativamente ao próprio arquivo. */
  @Test
  void masterIncludesChangelogRelatively() throws IOException {
    String master = Files.readString(ROOT.resolve("db.changelog-master.yaml"));
    assertThat(master)
        .contains("file: changesets/" + FILE + "\n      relativeToChangelogFile: true");
  }

  /** Exige escopo explícito, correlação idempotente e timestamps compatíveis com MySQL 5.7. */
  @Test
  void createsIndependentExecutionContract() throws IOException {
    String yaml = Files.readString(ROOT.resolve("changesets").resolve(FILE));
    assertThat(yaml)
        .contains("execution_scope VARCHAR(32) NOT NULL DEFAULT 'PRODUCT'")
        .contains("SET execution_scope = 'INDEPENDENT'")
        .contains("CREATE TABLE IF NOT EXISTS business_process_independent_execution")
        .contains("request_key CHAR(36) NOT NULL")
        .contains("input_json LONGTEXT NOT NULL")
        .contains("created_at DATETIME NOT NULL")
        .contains("type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .doesNotContain("TIMESTAMP NOT NULL")
        .doesNotContain(
            "UPDATE business_process_definition\n              SET execution_scope = (SELECT");
  }
}
