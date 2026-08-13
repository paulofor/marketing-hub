package com.marketinghub.salesvideo.autonomy.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato MySQL 5.7 dos ciclos governados de vídeo. */
class VideoProductionCycleChangelogTest {
  /** Comprova que projetos legados podem preservar plano nulo e que o include é relativo. */
  @Test
  void shouldAllowLegacyCycleWithoutCommercialPlan() throws Exception {
    String change =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-11-video-cycle-legacy-plan-fix.yaml"));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(change).contains("MODIFY COLUMN commercial_plan_id BIGINT NULL");
    assertThat(change).contains("splitStatements: true", "stripComments: true", "type: mysql");
    assertThat(master)
        .contains(
            "file: changesets/2026-08-11-video-cycle-legacy-plan-fix.yaml\n      relativeToChangelogFile: true");
  }

  /** Comprova que a reconciliação persiste o diagnóstico com SQL compatível com MySQL 5.7. */
  @Test
  void shouldPersistApolloTerminalFailureDiagnosis() throws Exception {
    String change =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-13-video-cycle-apollo-reconciliation.yaml"));
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));

    assertThat(change)
        .contains(
            "last_failed_job_id BIGINT NULL",
            "last_apollo_failure_code VARCHAR(191) NULL",
            "last_apollo_failure_detail LONGTEXT NULL",
            "last_apollo_failure_at DATETIME NULL",
            "splitStatements: true",
            "stripComments: true",
            "type: mysql");
    assertThat(master)
        .contains(
            "file: changesets/2026-08-13-video-cycle-apollo-reconciliation.yaml\n      relativeToChangelogFile: true");
  }
}
