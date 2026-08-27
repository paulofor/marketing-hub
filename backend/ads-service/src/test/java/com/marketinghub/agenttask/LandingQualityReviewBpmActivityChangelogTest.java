package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o retroativo MySQL 5.7 da atividade técnica de landing. */
class LandingQualityReviewBpmActivityChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String FILE = "2026-08-27-landing-quality-review-bpm-activity.yaml";

  /** Confirma que o mestre resolve o changelog relativamente ao próprio arquivo. */
  @Test
  void masterIncludesBackfillRelatively() throws IOException {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));
    assertThat(master)
        .contains("file: changesets/" + FILE + "\n      relativeToChangelogFile: true");
  }

  /** Exige retroativo genérico, idempotente e baseado na aprovação técnica persistida. */
  @Test
  void backfillsApprovedCorrelatedQualityReviews() throws IOException {
    String yaml = Files.readString(CHANGELOG_ROOT.resolve("changesets").resolve(FILE));
    assertThat(yaml)
        .contains("INSERT IGNORE INTO business_process_activity_instance")
        .contains("quality.autonomous_cycle_id = BINARY CONCAT('agent-task:', task.id)")
        .contains("process.process_code = 'landing-page-generation'")
        .contains("technical_activity.activity_id = BINARY 'technical'")
        .contains("AND newer_quality.status = 'CONCLUIDO'\n               AND (")
        .contains("'APPROVE_FOR_PUBLICATION'")
        .contains("quality.execution_requested_at")
        .contains("quality.completed_at")
        .contains("quality.cost_usd")
        .contains("CONVERT(quality.id_job USING utf8mb4)")
        .contains("JSON_EXTRACT(quality.model_response, '$')")
        .doesNotContain("task.id = 243")
        .doesNotContain("quality.experiment_id = 89")
        .doesNotContain("TIMESTAMP NOT NULL");
  }
}
