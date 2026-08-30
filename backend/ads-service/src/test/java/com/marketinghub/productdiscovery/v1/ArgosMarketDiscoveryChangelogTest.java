package com.marketinghub.productdiscovery.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a evolução auditável de Argos no schema físico MySQL 5.7. */
class ArgosMarketDiscoveryChangelogTest {
  private static final String FILE = "2026-08-30-argos-market-discovery-v1.yaml";
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");

  /** Exige resolução relativa do novo changelog a partir do mestre. */
  @Test
  void masterIncludesArgosMarketDiscoveryRelatively() throws Exception {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));

    assertThat(master)
        .contains("file: changesets/" + FILE + "\n      relativeToChangelogFile: true");
  }

  /** Protege colunas, retomada de DDL e versão do agente sem riscos temporais ou MySQL 1093. */
  @Test
  void addsResumableMarketDiscoveryAuditFields() throws Exception {
    String yaml = Files.readString(CHANGELOG_ROOT.resolve("changesets").resolve(FILE));

    assertThat(yaml)
        .contains(
            "type: mysql",
            "splitStatements: true",
            "stripComments: true",
            "column_name = 'research_mode'",
            "column_name = 'market_type'",
            "column_name = 'reference_sources'",
            "column_name = 'research_analysis_raw_response'",
            "column_name = 'research_analysis_model'",
            "column_name = 'research_evidence_report_json'",
            "DEFAULT ''VALIDATE_MARKET''",
            "DEFAULT ''UNSPECIFIED''",
            "SET current_version = GREATEST(COALESCE(current_version, 0), 4)")
        .doesNotContain("TIMESTAMP NOT NULL")
        .doesNotContain("UPDATE product_discovery_cycle")
        .doesNotContain("DELETE FROM product_discovery_cycle");
  }

  /** Mantém aplicação, retomada, rollback e reaplicação reais na matriz MySQL 5.7. */
  @Test
  void keepsPhysicalMysql57MatrixVersioned() throws Exception {
    String baseline =
        Files.readString(
            Path.of(
                "src/test/resources/liquibase-mysql57/product-discovery-bpm-audit-baseline.sql"));
    String script =
        Files.readString(Path.of("scripts/validate-product-discovery-bpm-audit-mysql57.sh"));

    assertThat(baseline).contains("current_version INT NOT NULL DEFAULT 3");
    assertThat(script)
        .contains(FILE)
        .contains("retomada idempotente da descoberta ampla")
        .contains("rollback isolado da descoberta ampla")
        .contains("reaplicação da descoberta ampla após rollback")
        .contains("\"6:4\"")
        .contains("\"0:3\"");
  }
}
