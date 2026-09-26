package com.marketinghub.creative;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger os contratos que mantêm status de criativo extensíveis. */
class CreativeStatusChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final Path MASTER_CHANGELOG = CHANGELOG_ROOT.resolve("db.changelog-master.yaml");
  private static final String CREATIVE_STATUS_CHANGESET =
      "changesets/2026-07-25-creative-status-varchar.yaml";
  private static final String CREATIVE_IMPROVEMENT_STATUS_CHANGESET =
      "changesets/2026-09-26-creative-agent-improvement-status-varchar-v1.yaml";

  /** Confirma que o changelog mestre aplica o reparo com caminho relativo ao próprio arquivo. */
  @Test
  void masterChangelogIncludesCreativeStatusRepairWithRelativePath() throws IOException {
    String master = Files.readString(MASTER_CHANGELOG);

    assertThat(master)
        .contains("file: " + CREATIVE_STATUS_CHANGESET)
        .containsSubsequence("file: " + CREATIVE_STATUS_CHANGESET, "relativeToChangelogFile: true");
  }

  /** Confirma que o reparo remove o enum físico que bloqueava a reprovação de criativos. */
  @Test
  void creativeStatusRepairConvertsStatusToVarcharOnMysql() throws IOException {
    String changeset = Files.readString(CHANGELOG_ROOT.resolve(CREATIVE_STATUS_CHANGESET));

    assertThat(changeset)
        .contains("databaseChangeLog:")
        .contains("id: 2026-07-25-creative-status-varchar")
        .contains("dbms:")
        .contains("type: mysql")
        .contains("tableName: creative")
        .contains("columnName: status")
        .contains("DATA_TYPE <> 'varchar'")
        .contains("CHARACTER_MAXIMUM_LENGTH < 20")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("ALTER TABLE creative")
        .contains("MODIFY COLUMN status VARCHAR(20) NULL;");
  }

  /** Confirma que o reparo da melhoria automática é incluído por caminho relativo. */
  @Test
  void masterChangelogIncludesCreativeImprovementStatusRepairWithRelativePath() throws IOException {
    String master = Files.readString(MASTER_CHANGELOG);

    assertThat(master)
        .contains("file: " + CREATIVE_IMPROVEMENT_STATUS_CHANGESET)
        .containsSubsequence(
            "file: " + CREATIVE_IMPROVEMENT_STATUS_CHANGESET, "relativeToChangelogFile: true");
  }

  /** Confirma que o MySQL aceita novos estados sem depender de ENUM físico. */
  @Test
  void creativeImprovementStatusRepairConvertsEnumToVarcharOnMysql() throws IOException {
    String changeset =
        Files.readString(CHANGELOG_ROOT.resolve(CREATIVE_IMPROVEMENT_STATUS_CHANGESET));

    assertThat(changeset)
        .contains("databaseChangeLog:")
        .contains("id: 2026-09-26-creative-agent-improvement-status-varchar-v1")
        .contains("dbms:")
        .contains("type: mysql")
        .contains("tableName: creative")
        .contains("columnName: agent_improvement_status")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("ALTER TABLE creative")
        .contains("MODIFY COLUMN agent_improvement_status VARCHAR(24) NULL;");
  }

  /** Confirma que o Hibernate não recria ENUM nativo ao iniciar a aplicação. */
  @Test
  void creativeImprovementStatusUsesExplicitVarcharMapping() throws NoSuchFieldException {
    Field field = Creative.class.getDeclaredField("agentImprovementStatus");
    JdbcTypeCode jdbcTypeCode = field.getAnnotation(JdbcTypeCode.class);
    Column column = field.getAnnotation(Column.class);

    assertThat(CreativeImprovementStatus.values()).contains(CreativeImprovementStatus.DELEGATED);
    assertThat(jdbcTypeCode).isNotNull();
    assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.VARCHAR);
    assertThat(column).isNotNull();
    assertThat(column.name()).isEqualTo("agent_improvement_status");
    assertThat(column.columnDefinition()).isEqualTo("VARCHAR(24)");
  }
}
