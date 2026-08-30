package com.marketinghub.pde;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a quantidade exata da entrega comercial congelada do Rigel. */
class RigelExactDeliveryContractChangelogTest {
  private static final Path CHANGELOG =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-30-rigel-exact-delivery-contract-v1.yaml");
  private static final Path SQL =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-30-rigel-exact-delivery-contract-v1.sql");
  private static final Path MASTER =
      Path.of("src/main/resources/db/changelog/db.changelog-master.yaml");

  /** Exige MySQL, include relativo e o contrato SQL idempotente para produto e slot. */
  @Test
  void shouldPersistExactCommercialCountsWithoutTargetTableSubquery() throws Exception {
    String changelog = Files.readString(CHANGELOG);
    String sql = Files.readString(SQL);
    String master = Files.readString(MASTER);

    assertThat(changelog)
        .contains("dbms:")
        .contains("type: mysql")
        .contains("relativeToChangelogFile: true")
        .contains("splitStatements: true")
        .contains("stripComments: true");
    assertThat(master)
        .contains(
            "file: changesets/2026-08-30-rigel-exact-delivery-contract-v1.yaml\n"
                + "      relativeToChangelogFile: true");
    assertThat(sql)
        .contains("15 respostas personalizadas")
        .contains("8 perguntas de qualificação")
        .contains("4 follow-ups manuais")
        .contains("'$.missions[4].deliveryContract.sections[0].minItems', 15")
        .contains("'$.missions[4].deliveryContract.sections[0].maxItems', 15")
        .contains("'$.missions[4].deliveryContract.sections[1].minItems', 8")
        .contains("'$.missions[4].deliveryContract.sections[1].maxItems', 8")
        .contains("'$.missions[4].deliveryContract.sections[2].minItems', 4")
        .contains("'$.missions[4].deliveryContract.sections[2].maxItems', 4")
        .contains("draft_experience_json = CASE")
        .contains("published_experience_json = JSON_SET")
        .contains("ELSE draft_experience_json")
        .doesNotContain("NOT EXISTS (SELECT")
        .doesNotContain("IN (SELECT")
        .doesNotContain("SET pde_experience_json = (SELECT");
  }
}
