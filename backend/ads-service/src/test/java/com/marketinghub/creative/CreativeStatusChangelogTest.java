package com.marketinghub.creative;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: proteger o contrato Liquibase que mantém status de criativo extensível.
 */
class CreativeStatusChangelogTest {
    private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
    private static final Path MASTER_CHANGELOG = CHANGELOG_ROOT.resolve("db.changelog-master.yaml");
    private static final String CREATIVE_STATUS_CHANGESET =
            "changesets/2026-07-25-creative-status-varchar.yaml";

    /**
     * Confirma que o changelog mestre aplica o reparo com caminho relativo ao próprio arquivo.
     */
    @Test
    void masterChangelogIncludesCreativeStatusRepairWithRelativePath() throws IOException {
        String master = Files.readString(MASTER_CHANGELOG);

        assertThat(master)
                .contains("file: " + CREATIVE_STATUS_CHANGESET)
                .containsSubsequence(
                        "file: " + CREATIVE_STATUS_CHANGESET,
                        "relativeToChangelogFile: true"
                );
    }

    /**
     * Confirma que o reparo remove o enum físico que bloqueava a reprovação de criativos.
     */
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
}
