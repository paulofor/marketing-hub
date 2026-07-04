package com.marketinghub.facebookads;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Valida o contrato Liquibase que mantém os motivos de parada de campanha como texto extensível.
 */
class FacebookCampaignStopReasonChangelogTest {

    private static final Path CHANGELOG_DIR = Path.of("src/main/resources/db/changelog");
    private static final Path MASTER_CHANGELOG = CHANGELOG_DIR.resolve("db.changelog-master.yaml");
    private static final String STOP_REASON_CHANGESET =
            "changesets/2026-06-12-facebook-campaign-stop-reason-varchar.yaml";

    /** Confirma que o changelog mestre inclui o reparo com caminho relativo ao próprio changelog. */
    @Test
    void masterChangelogIncludesRepairWithRelativePath() throws IOException {
        String master = Files.readString(MASTER_CHANGELOG);

        assertThat(master)
                .contains("file: " + STOP_REASON_CHANGESET)
                .containsSubsequence(
                        "file: " + STOP_REASON_CHANGESET,
                        "relativeToChangelogFile: true"
                );
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
}
