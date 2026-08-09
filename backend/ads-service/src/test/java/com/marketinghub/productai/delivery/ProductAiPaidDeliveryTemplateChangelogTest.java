package com.marketinghub.productai.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato Liquibase da entrega paga personalizada. */
class ProductAiPaidDeliveryTemplateChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String CHANGESET =
      "changesets/2026-08-09-product-ai-paid-delivery-template-v2.yaml";

  /** Garante que o changelog mestre carregue a recuperação do template por caminho relativo. */
  @Test
  void masterIncludesPaidDeliveryTemplateRecovery() throws IOException {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));

    assertThat(master).contains("file: " + CHANGESET + "\n      relativeToChangelogFile: true");
  }

  /** Garante modelo vigente, ativação idempotente e schema funcional mínimo da entrega. */
  @Test
  void recoveryCreatesActiveVersionedContract() throws IOException {
    String changeset = Files.readString(CHANGELOG_ROOT.resolve(CHANGESET));

    assertThat(changeset)
        .contains("preConditions:")
        .contains("type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("product-ai.personalizedsample.v1.paid-delivery.v2")
        .contains("'gpt-5.6-sol'")
        .contains("ON DUPLICATE KEY UPDATE")
        .contains("active = TRUE")
        .contains("personalizedDiagnostic")
        .contains("qualityChecklist");
  }
}
