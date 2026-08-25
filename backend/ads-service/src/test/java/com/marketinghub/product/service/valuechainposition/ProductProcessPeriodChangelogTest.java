package com.marketinghub.product.service.valuechainposition;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a retomada idempotente da migração de períodos do produto. */
class ProductProcessPeriodChangelogTest {
  private static final Path CHANGELOG =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-25-product-process-time-cost.yaml");
  private static final Path MYSQL57_BASELINE =
      Path.of("src/test/resources/liquibase-mysql57/product-process-time-cost-baseline.sql");

  /** Separa a criação da tabela do backfill para sobreviver a interrupções após o DDL. */
  @Test
  void separatesSchemaCreationFromIdempotentBackfill() throws Exception {
    String changelog = Files.readString(CHANGELOG);
    String schemaId = "id: 2026-08-25-product-process-time-cost\n";
    String backfillId = "id: 2026-08-25-product-process-time-cost-backfill\n";
    int schemaStart = changelog.indexOf(schemaId);
    int backfillStart = changelog.indexOf(backfillId);

    assertThat(schemaStart).isGreaterThanOrEqualTo(0);
    assertThat(backfillStart).isGreaterThan(schemaStart);
    assertThat(changelog.substring(schemaStart, backfillStart))
        .contains("CREATE TABLE IF NOT EXISTS product_process_period")
        .contains("9:68a4881abf2d9f1f338200f937b3b4c5")
        .doesNotContain("INSERT INTO product_process_period");
    assertThat(changelog.substring(backfillStart))
        .contains("LEFT JOIN product_process_period existing_period")
        .contains("AND existing_period.id IS NULL")
        .contains("expectedResult: 14")
        .contains("expectedResult: 4")
        .contains("indexName: idx_product_process_period_timeline")
        .contains("foreignKeyName: fk_product_process_period_product")
        .contains("splitStatements: true")
        .contains("stripComments: true");
  }

  /** Mantém a fixture física MySQL 5.7 alinhada aos contratos mínimos da migração. */
  @Test
  void keepsMysql57RecoveryFixtureVersioned() throws Exception {
    String baseline = Files.readString(MYSQL57_BASELINE);

    assertThat(baseline)
        .contains("CREATE TABLE product")
        .contains("CREATE TABLE business_process_definition")
        .contains("CREATE TABLE business_process_chain_definition")
        .contains("CREATE TABLE business_process_chain_item")
        .contains("'Rigel', 'COMUNICACAO_E_JORNADA'")
        .doesNotContain("CREATE TABLE product_process_period");
  }
}
