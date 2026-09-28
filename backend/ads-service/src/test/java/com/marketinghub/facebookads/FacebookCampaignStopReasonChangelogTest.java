package com.marketinghub.facebookads;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

/**
 * Valida o contrato Liquibase que mantém os motivos de parada de campanha como texto extensível.
 */
class FacebookCampaignStopReasonChangelogTest {

  private static final Path CHANGELOG_DIR = Path.of("src/main/resources/db/changelog");
  private static final Path MASTER_CHANGELOG = CHANGELOG_DIR.resolve("db.changelog-master.yaml");
  private static final String STOP_REASON_CHANGESET =
      "changesets/2026-06-12-facebook-campaign-stop-reason-varchar.yaml";
  private static final String STOP_REASON_REPAIR_V2 =
      "changesets/2026-09-28-facebook-stop-reason-varchar-repair-v2.yaml";

  /** Confirma que o changelog mestre inclui o reparo com caminho relativo ao próprio changelog. */
  @Test
  void masterChangelogIncludesRepairWithRelativePath() throws IOException {
    String master = Files.readString(MASTER_CHANGELOG);

    assertThat(master)
        .contains("file: " + STOP_REASON_CHANGESET)
        .containsSubsequence("file: " + STOP_REASON_CHANGESET, "relativeToChangelogFile: true")
        .contains("file: " + STOP_REASON_REPAIR_V2)
        .containsSubsequence("file: " + STOP_REASON_REPAIR_V2, "relativeToChangelogFile: true");
  }

  /** Confirma que o reparo converte stop_reason para VARCHAR e roda apenas em MySQL. */
  @Test
  void repairChangesetConvertsStopReasonToVarcharOnMysql() throws IOException {
    String changeset = Files.readString(CHANGELOG_DIR.resolve(STOP_REASON_CHANGESET));

    assertThat(changeset)
        .contains("databaseChangeLog:")
        .contains("id: 2026-07-04-facebook-campaign-stop-reason-varchar-repair")
        .contains("dbms:")
        .contains("type: mysql")
        .contains("tableName: facebook_ads_campaign")
        .contains("columnName: stop_reason")
        .contains("data_type = 'varchar'")
        .contains("character_maximum_length >= 100")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("ALTER TABLE facebook_ads_campaign")
        .contains("MODIFY stop_reason VARCHAR(100) NULL;");
  }

  /** Confirma que o reparo novo converge as duas colunas alteradas pelo Hibernate. */
  @Test
  void latestRepairConvertsCampaignAndEvaluationStopReasonsToVarchar() throws IOException {
    String changeset = Files.readString(CHANGELOG_DIR.resolve(STOP_REASON_REPAIR_V2));

    assertThat(changeset)
        .contains("id: 2026-09-28-facebook-ads-campaign-stop-reason-varchar-repair-v2")
        .contains("id: 2026-09-28-campaign-strategy-evaluation-stop-reason-varchar-repair-v2")
        .contains("tableName: facebook_ads_campaign")
        .contains("tableName: campaign_strategy_evaluation")
        .contains("data_type = 'varchar'")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("ALTER TABLE facebook_ads_campaign")
        .contains("ALTER TABLE campaign_strategy_evaluation")
        .contains("MODIFY stop_reason VARCHAR(100) NULL;");
  }

  /** Impede que ddl-auto=update reconverta os motivos extensíveis para ENUM nativo do MySQL. */
  @Test
  void entitiesPinStopReasonToJdbcVarchar() throws NoSuchFieldException {
    assertVarcharMapping(FacebookAdsCampaign.class);
    assertVarcharMapping(CampaignStrategyEvaluation.class);
  }

  /** Valida o tipo JDBC e a definição SQL explícita de uma entidade com motivo de parada. */
  private void assertVarcharMapping(Class<?> entityType) throws NoSuchFieldException {
    var field = entityType.getDeclaredField("stopReason");

    assertThat(field.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.VARCHAR);
    assertThat(field.getAnnotation(Column.class).columnDefinition()).isEqualTo("VARCHAR(100)");
  }
}
