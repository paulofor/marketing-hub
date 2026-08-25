package com.marketinghub.pde;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a paridade entre o contrato MUSA v7 persistido e o fallback PDE. */
class MusaV7CanonicalProductContractChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final Path MASTER_CHANGELOG = CHANGELOG_ROOT.resolve("db.changelog-master.yaml");
  private static final Path CHANGESET =
      CHANGELOG_ROOT.resolve("changesets/2026-08-24-musa-v7-canonical-product-contract.yaml");
  private static final Path SQL =
      CHANGELOG_ROOT.resolve("changesets/2026-08-24-musa-v7-canonical-product-contract.sql");
  private static final Path PDE_FALLBACK =
      Path.of("../../pde-platform/backend/src/main/resources/contracts/musa-v7-product-v1.json");
  private static final Pattern SQL_CONTRACT =
      Pattern.compile("SET @musa_v7_canonical_contract = '(.*?)';", Pattern.DOTALL);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** Confirma que o changelog mestre inclui a correção usando resolução relativa obrigatória. */
  @Test
  void masterIncludesCanonicalContractCorrectionWithRelativePath() throws IOException {
    String master = Files.readString(MASTER_CHANGELOG);
    String file = "changesets/2026-08-24-musa-v7-canonical-product-contract.yaml";

    assertThat(master)
        .contains("file: " + file)
        .containsSubsequence("file: " + file, "relativeToChangelogFile: true");
  }

  /** Confirma as proteções obrigatórias do changelog Liquibase para MySQL 5.7. */
  @Test
  void changesetUsesMysqlPreconditionsAndSafeSqlFileOptions() throws IOException {
    String changeset = Files.readString(CHANGESET);

    assertThat(changeset)
        .contains("dbms:")
        .contains("type: mysql")
        .contains("relativeToChangelogFile: true")
        .contains("splitStatements: true")
        .contains("stripComments: true");
  }

  /** Confirma que produto e slot recebem exatamente o contrato v7 homologado pelo PDE. */
  @Test
  void persistedContractMatchesPdeFallbackSemantically() throws IOException {
    String sql = Files.readString(SQL);
    var matcher = SQL_CONTRACT.matcher(sql);
    assertThat(matcher.find()).isTrue();

    JsonNode persisted = OBJECT_MAPPER.readTree(matcher.group(1).replace("''", "'"));
    JsonNode fallback = OBJECT_MAPPER.readTree(Files.readString(PDE_FALLBACK));

    assertThat(persisted).isEqualTo(fallback);
    assertThat(persisted.path("name").asText())
        .isEqualTo("Método MUSA - Presença Elegante em 7 Dias");
    assertThat(persisted.path("missions")).hasSize(7);
    persisted
        .path("missions")
        .forEach(mission -> assertThat(mission.path("interaction").isObject()).isTrue());
  }

  /** Impede atualização baseada em subconsulta da própria tabela-alvo no MySQL 5.7. */
  @Test
  void correctionAvoidsMysqlTargetTableSubqueries() throws IOException {
    String sql = Files.readString(SQL).toLowerCase();

    assertThat(sql).doesNotContain("from product").doesNotContain("from pde_production_slot");
  }
}
