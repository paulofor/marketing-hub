package com.marketinghub.salesvideo;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

/** Protege os estados evolutivos da análise de referências contra ENUM físico no MySQL. */
class VideoReferenceStatusChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final Path MASTER_CHANGELOG = CHANGELOG_ROOT.resolve("db.changelog-master.yaml");
  private static final String STATUS_CHANGESET =
      "changesets/2026-09-26-video-reference-status-varchar-v1.yaml";

  /** Confirma que o changelog mestre inclui o reparo pelo caminho relativo obrigatório. */
  @Test
  void masterIncludesStatusRepairWithRelativePath() throws IOException {
    String master = Files.readString(MASTER_CHANGELOG);

    assertThat(master)
        .contains("file: " + STATUS_CHANGESET)
        .containsSubsequence("file: " + STATUS_CHANGESET, "relativeToChangelogFile: true");
  }

  /** Confirma que os dois status viram VARCHAR e que rollback conhece todos os estados atuais. */
  @Test
  void changesetConvertsBothStatusColumnsToVarchar() throws IOException {
    String changeset = Files.readString(CHANGELOG_ROOT.resolve(STATUS_CHANGESET));

    assertThat(changeset)
        .contains("databaseChangeLog:")
        .contains("dbms:")
        .contains("type: mysql")
        .contains("tableName: video_reference")
        .contains("tableName: video_reference_analysis_execution")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("MODIFY COLUMN status VARCHAR(64) NOT NULL DEFAULT 'QUEUED'")
        .contains("MODIFY COLUMN status VARCHAR(32) NOT NULL")
        .contains("'FAILED'")
        .contains("'BUDGET_BLOCKED'");
  }

  /** Confirma que o Hibernate não pode recriar ENUM nativo nos dois campos corrigidos. */
  @Test
  void entitiesUseExplicitVarcharMappings() throws NoSuchFieldException {
    assertVarchar(VideoReference.class.getDeclaredField("status"), "VARCHAR(64)");
    assertVarchar(VideoReferenceAnalysisExecution.class.getDeclaredField("status"), "VARCHAR(32)");
  }

  /** Verifica o tipo JDBC e a definição física explícita de um campo de status. */
  private void assertVarchar(Field field, String definition) {
    JdbcTypeCode jdbcTypeCode = field.getAnnotation(JdbcTypeCode.class);
    Column column = field.getAnnotation(Column.class);

    assertThat(jdbcTypeCode).isNotNull();
    assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.VARCHAR);
    assertThat(column).isNotNull();
    assertThat(column.columnDefinition()).isEqualTo(definition);
  }
}
