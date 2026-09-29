package com.marketinghub.facebookads;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Valida o contrato MySQL 5.7 da confirmação idempotente de conversões PDE. */
class FacebookPixelConversionDeliveryChangelogTest {
  private static final Path CHANGELOG_DIR = Path.of("src/main/resources/db/changelog");
  private static final String CHANGESET = "changesets/2026-09-29-pde-meta-pixel-conversion-v1.yaml";

  /** Confirma que o mestre resolve o novo changelog relativamente ao próprio arquivo. */
  @Test
  void masterIncludesPdePixelDeliveryWithRelativePath() throws IOException {
    String master = Files.readString(CHANGELOG_DIR.resolve("db.changelog-master.yaml"));

    assertThat(master).containsSubsequence("file: " + CHANGESET, "relativeToChangelogFile: true");
  }

  /** Confirma unicidade, temporalidade e SQL explícito compatível com MySQL 5.7. */
  @Test
  void changesetCreatesIdempotentDeliveryLedger() throws IOException {
    String changeset = Files.readString(CHANGELOG_DIR.resolve(CHANGESET));

    assertThat(changeset)
        .contains("databaseChangeLog:")
        .contains("type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("CREATE TABLE facebook_pixel_conversion_delivery")
        .contains("recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP")
        .contains("UNIQUE KEY uk_facebook_pixel_conversion_source")
        .doesNotContain("TIMESTAMP NOT NULL");
  }
}
