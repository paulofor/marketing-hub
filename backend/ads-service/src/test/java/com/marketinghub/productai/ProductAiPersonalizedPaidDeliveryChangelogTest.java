package com.marketinghub.productai;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import jakarta.persistence.Column;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o subtipo pago e a reconciliação comercial de Mira no MySQL 5.7. */
class ProductAiPersonalizedPaidDeliveryChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String CHANGESET =
      "changesets/2026-09-29-product-ai-personalized-paid-delivery-v1.yaml";

  /** Confirma que o mestre resolve o novo changelog relativamente ao próprio arquivo. */
  @Test
  void masterIncludesPaidDeliveryReconciliationWithRelativePath() throws IOException {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));

    assertThat(master).contains("file: " + CHANGESET + "\n      relativeToChangelogFile: true");
  }

  /** Protege a extensão textual e a reparação idempotente da linhagem completa de Mira. */
  @Test
  void changesetConvertsSubtypesAndReconcilesMiraLineage() throws IOException {
    String changeset = Files.readString(CHANGELOG_ROOT.resolve(CHANGESET));

    assertThat(changeset)
        .contains("databaseChangeLog:")
        .contains("type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("MODIFY COLUMN product_ai_subtype VARCHAR(48) NULL")
        .contains("UPDATE hypothesis h\n              JOIN experiment e")
        .contains("UPDATE experiment e\n              JOIN hypothesis h")
        .contains("UPDATE deliverable_package dp")
        .contains("UPDATE deliverable d")
        .contains("AI_PERSONALIZED_PAID_DELIVERY")
        .contains("e.id = 93")
        .contains("e.product_id = 10")
        .doesNotContain("TIMESTAMP NOT NULL")
        .doesNotContain("WHERE h.id IN (SELECT")
        .doesNotContain("WHERE e.id IN (SELECT");
  }

  /** Impede que ddl-auto reconverta o subtipo extensível para ENUM nativo do MySQL. */
  @Test
  void entitiesPinProductAiSubtypeToJdbcVarchar() throws NoSuchFieldException {
    assertVarcharMapping(Experiment.class);
    assertVarcharMapping(Hypothesis.class);
  }

  /** Valida tipo JDBC e definição SQL explícita na entidade indicada. */
  private void assertVarcharMapping(Class<?> entityType) throws NoSuchFieldException {
    var field = entityType.getDeclaredField("productAiSubtype");

    assertThat(field.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.VARCHAR);
    assertThat(field.getAnnotation(Column.class).columnDefinition()).isEqualTo("VARCHAR(48)");
  }
}
