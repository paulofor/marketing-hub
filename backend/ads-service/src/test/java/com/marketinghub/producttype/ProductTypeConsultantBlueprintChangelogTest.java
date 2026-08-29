package com.marketinghub.producttype;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger as migrações e a homologação física dos tipos de consultoria. */
class ProductTypeConsultantBlueprintChangelogTest {
  private static final String FILE = "2026-08-29-product-type-consultant-blueprints-v1.yaml";
  private static final String RESEARCH_FILE =
      "2026-08-29-product-type-consultant-research-enrichment-v2.yaml";
  private static final String SANDBOX_COMPOSE_PROJECT =
      "aihub-195ed4dd-a386-4676-9cf8-0e87e6dd05e0-a970d7a3fd";
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");
  private static final Path CHANGELOG =
      Path.of("src/main/resources/db/changelog/changesets").resolve(FILE);
  private static final Path RESEARCH_CHANGELOG =
      Path.of("src/main/resources/db/changelog/changesets").resolve(RESEARCH_FILE);

  /** Exige includes relativos e mantém o enriquecimento depois da criação das bases. */
  @Test
  void includesConsultantBlueprintMigrationRelatively() throws Exception {
    String master = Files.readString(MASTER);
    int catalog = master.indexOf("2026-08-23-product-type-catalog.yaml");
    int internalIdentity = master.indexOf("2026-08-23-product-type-internal-identity.yaml");
    int include = master.indexOf(FILE);
    int researchInclude = master.indexOf(RESEARCH_FILE);

    assertThat(include).isGreaterThanOrEqualTo(0);
    assertThat(catalog).isBetween(0, internalIdentity - 1);
    assertThat(internalIdentity).isBetween(0, include - 1);
    assertThat(researchInclude).isGreaterThan(include);
    assertThat(master.substring(include, Math.min(master.length(), include + 180)))
        .contains("relativeToChangelogFile: true");
    assertThat(master.substring(researchInclude, Math.min(master.length(), researchInclude + 180)))
        .contains("relativeToChangelogFile: true");
  }

  /** Mantém os treze campos, as identidades e os SDKs canônicos no incremento. */
  @Test
  void seedsCompletePwaAndWhatsappConsultantBlueprints() throws Exception {
    String changelog = Files.readString(CHANGELOG);

    assertThat(changelog)
        .contains(
            "AI_SANDBOX_CONVERSATIONAL_PRODUCT",
            "Consultor WhatsApp com IA",
            "Fluorita",
            "consultant-whatsapp-v1",
            "AI_PWA_CONSULTANT_PRODUCT",
            "Consultor PWA com IA",
            "Turmalina",
            "consultant-pwa-v1",
            "pde-platform/pde-harness-sdk",
            "pde-platform/frontend/src/consultant-sdk/v1");
    assertThat(changelog).containsOnlyOnce("ADD COLUMN blueprint_version");
    assertThat(changelog).doesNotContain("TIMESTAMP NOT NULL", "CURRENT_TIMESTAMP(6)");
  }

  /** Impede o padrão 1093 e exige SQL compatível com o contrato MySQL 5.7. */
  @Test
  void keepsMysql57SafetyContract() throws Exception {
    String changelog = Files.readString(CHANGELOG);
    String researchChangelog = Files.readString(RESEARCH_CHANGELOG);

    assertThat(changelog).contains("type: mysql", "splitStatements: true", "stripComments: true");
    assertThat(changelog.split(";")).allSatisfy(this::assertNoMysql1093Pattern);
    assertThat(researchChangelog)
        .contains("type: mysql", "splitStatements: true", "stripComments: true");
    assertThat(researchChangelog.split(";")).allSatisfy(this::assertNoMysql1093Pattern);
  }

  /** Mantém microvalor, confiança, permissão e recorrência nas duas bases versionadas. */
  @Test
  void enrichesConsultantsWithResearchBackedContracts() throws Exception {
    String changelog = Files.readString(RESEARCH_CHANGELOG);

    assertThat(changelog)
        .contains(
            "consultant-whatsapp-v2",
            "consultant-pwa-v2",
            "momento concreto",
            "microvalor operacional",
            "origem da conversa",
            "proveniência de cada dado",
            "alto impacto sem confirmação",
            "instalação é oferecida somente após valor",
            "retorno D1, D7 e D30");
    assertThat(changelog).doesNotContain("TIMESTAMP NOT NULL", "CURRENT_TIMESTAMP(6)");
  }

  /** Garante que o PR execute a fixture física, a reaplicação e as consultas de preservação. */
  @Test
  void keepsDedicatedMysql57PhysicalValidation() throws Exception {
    Path moduleRoot = Path.of("").toAbsolutePath();
    String baseline =
        Files.readString(
            moduleRoot.resolve(
                "src/test/resources/liquibase-mysql57/product-type-consultants-v1-baseline.sql"));
    String compose =
        Files.readString(
            moduleRoot.resolve("docker-compose.product-type-consultants-v1-mysql57.yml"));
    String runner =
        Files.readString(
            moduleRoot.resolve("scripts/validate-product-type-consultants-v1-mysql57.sh"));
    String workflow =
        Files.readString(moduleRoot.resolve("../../.github/workflows/liquibase-mysql57.yml"));

    assertThat(baseline)
        .contains(
            "AI_SANDBOX_CONVERSATIONAL_PRODUCT",
            "Fluorita",
            "INSERT INTO product (id, product_type, product_type_id)");
    assertThat(compose).contains(FILE, RESEARCH_FILE, "mysql57-product-type-consultants-v1");
    assertThat(runner)
        .contains(
            "DELETE FROM DATABASECHANGELOG",
            SANDBOX_COMPOSE_PROJECT,
            "colunas da base de construção",
            "contratos enriquecidos pela pesquisa",
            "confiança do WhatsApp e valor antes da instalação PWA",
            "produto histórico preservado",
            "apelidos sem duplicação após retomada");
    assertThat(workflow)
        .contains(
            "validate-product-type-consultants-v1:",
            "validate-product-type-consultants-v1-mysql57.sh");
  }

  /** Valida o risco 1093 em cada comando sem misturar instruções SQL independentes. */
  private void assertNoMysql1093Pattern(String statement) {
    java.util.regex.Matcher target =
        java.util.regex.Pattern.compile("(?is)^\\s*(?:UPDATE|DELETE\\s+FROM)\\s+([a-z0-9_]+)")
            .matcher(statement);
    if (!target.find()) {
      return;
    }
    String table = java.util.regex.Pattern.quote(target.group(1));
    assertThat(statement).doesNotMatch("(?is).*SELECT.+FROM\\s+`?" + table + "`?(?:\\s|$).*");
  }
}
