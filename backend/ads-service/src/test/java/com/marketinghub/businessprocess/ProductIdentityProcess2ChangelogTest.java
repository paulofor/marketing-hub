package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a versão do Processo 2 e o reparo auditável da execução 32. */
class ProductIdentityProcess2ChangelogTest {
  private static final Path CHANGELOG =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-09-26-processo2-identidade-produto-v1.yaml");
  private static final Path SQL =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-09-26-processo2-identidade-produto-v1.sql");
  private static final Path REPAIR_SQL =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-09-26-processo2-identidade-produto-11-repair.sql");
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");
  private static final Path WORKFLOW = Path.of("../../.github/workflows/liquibase-mysql57.yml");
  private static final Path PHYSICAL_VALIDATOR =
      Path.of("scripts/validate-process2-product-identity-mysql57.sh");

  /** Exige o include relativo e as opções canônicas de execução SQL no MySQL 5.7. */
  @Test
  void includesMysql57ChangeLogWithCanonicalOptions() throws Exception {
    String changelog = Files.readString(CHANGELOG);
    String master = Files.readString(MASTER);

    assertThat(master)
        .contains("file: changesets/2026-09-26-processo2-identidade-produto-v1.yaml")
        .containsSubsequence(
            "file: changesets/2026-09-26-processo2-identidade-produto-v1.yaml",
            "relativeToChangelogFile: true");
    assertThat(changelog)
        .contains("type: mysql")
        .contains("relativeToChangelogFile: true")
        .contains("splitStatements: true")
        .contains("stripComments: true");
  }

  /** Exige processo v9, cadeia v23 e decisão de identidade antes da economia e construção. */
  @Test
  void versionsProcessAndChainWithProductIdentityContract() throws Exception {
    String sql = Files.readString(SQL);

    assertThat(sql)
        .contains("'$.productIdentityContractVersion', 'PRODUCT_IDENTITY_V1'")
        .contains("source.version_number=8")
        .contains("target.version_number=23")
        .contains("WHEN 'pde-commercial-plan-offer' THEN 9")
        .contains("nome interno de estrela ainda livre")
        .contains("tipo ACTIVE do catalogo");
  }

  /** Exige que o produto 11 receba Alcyone e Safira sem subconsulta na tabela alvo. */
  @Test
  void repairsExecution32ProductWithoutMysql1093Pattern() throws Exception {
    String sql = Files.readString(REPAIR_SQL);

    assertThat(sql)
        .contains("UPDATE product product_record")
        .contains("JOIN product_type_definition type_definition")
        .contains("product_record.internal_name='Alcyone'")
        .contains("type_definition.code='AI_PRODUCT'")
        .contains("'productTypeInternalName','Safira'")
        .doesNotContain("UPDATE product product_record\nSET")
        .doesNotContain("FROM product product_record WHERE");
  }

  /** Mantém a prova física MySQL 5.7 ligada ao workflow acionado pelo changelog. */
  @Test
  void wiresPhysicalMysql57ContractIntoWorkflow() throws Exception {
    String workflow = Files.readString(WORKFLOW);
    String validator = Files.readString(PHYSICAL_VALIDATOR);

    assertThat(workflow)
        .contains("validate-process2-product-identity:")
        .contains("validate-process2-product-identity-mysql57.sh");
    assertThat(validator)
        .contains("rollbackCount 1")
        .contains("identity_assert_applied")
        .contains("Validação física do Processo 2");
  }
}
