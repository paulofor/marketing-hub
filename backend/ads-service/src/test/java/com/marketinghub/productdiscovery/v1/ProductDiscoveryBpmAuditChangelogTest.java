package com.marketinghub.productdiscovery.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o retroativo MySQL 5.7 das execuções PDE no histórico BPM. */
class ProductDiscoveryBpmAuditChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String FILE = "2026-08-28-product-discovery-bpm-audit.yaml";

  /** Confirma que o mestre resolve o changelog relativamente ao próprio arquivo. */
  @Test
  void masterIncludesProductDiscoveryAuditRelatively() throws Exception {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));

    assertThat(master)
        .contains("file: changesets/" + FILE + "\n      relativeToChangelogFile: true");
  }

  /** Exige correlação idempotente, status fiel e ausência de timestamps inventados. */
  @Test
  void backfillsRealCyclesIntoActivityInstancesAndTasks() throws Exception {
    String yaml = Files.readString(CHANGELOG_ROOT.resolve("changesets").resolve(FILE));

    assertThat(yaml)
        .contains("INSERT IGNORE INTO business_process_activity_instance")
        .contains("INSERT INTO agent_task")
        .contains("CONCAT('product-discovery-cycle:', cycle.id)")
        .contains("activity.activity_id = BINARY 'inspiration'")
        .contains("newer.effective_at <= cycle.created_at")
        .contains("LEFT JOIN agent_task existing_task")
        .contains("WHERE existing_task.id IS NULL")
        .contains("WHEN 'RESEARCHING' THEN 'IN_PROGRESS'")
        .contains("WHEN 'COMPLETED' THEN 'COMPLETED'")
        .contains("WHEN 'FAILED' THEN 'BLOCKED'")
        .contains("'BACKFILLED_FROM_CYCLE'")
        .contains("'product-discovery-bpm-audit/v1'")
        .contains("NULL,\n                CASE WHEN cycle.status = 'COMPLETED'")
        .doesNotContain("TIMESTAMP NOT NULL")
        .doesNotContain("UPDATE agent_task")
        .doesNotContain("DELETE FROM agent_task WHERE")
        .doesNotContain("DELETE FROM business_process_activity_instance WHERE");
  }

  /** Exige que rollback remova somente os registros identificados pelo próprio retroativo. */
  @Test
  void rollbackUsesJoinedTargetsAndBackfillMarker() throws Exception {
    String yaml = Files.readString(CHANGELOG_ROOT.resolve("changesets").resolve(FILE));

    assertThat(yaml)
        .contains("DELETE instance\n              FROM business_process_activity_instance instance")
        .contains("DELETE task\n              FROM agent_task task")
        .contains("JSON_EXTRACT(task.evidence_json, '$.backfillSource')")
        .contains("instance.evidence_quality = 'BACKFILLED_FROM_CYCLE'");
    assertThat(yaml.indexOf("DELETE task")).isLessThan(yaml.indexOf("DELETE instance"));
  }

  /** Confirma que a matriz física de aplicação, idempotência e rollback está versionada. */
  @Test
  void keepsPhysicalMysql57ValidationMatrixVersioned() throws Exception {
    Path moduleRoot = Path.of("");
    String mysqlDockerfile =
        Files.readString(moduleRoot.resolve("Dockerfile.mysql57-product-discovery-bpm-audit"));
    String compose =
        Files.readString(
            moduleRoot.resolve("docker-compose.product-discovery-bpm-audit-mysql57.yml"));
    String script =
        Files.readString(
            moduleRoot.resolve("scripts/validate-product-discovery-bpm-audit-mysql57.sh"));
    String workflow =
        Files.readString(moduleRoot.resolve("../../.github/workflows/liquibase-mysql57.yml"));

    assertThat(mysqlDockerfile).contains("FROM mysql:5.7");
    assertThat(compose).contains("dockerfile: Dockerfile.mysql57-product-discovery-bpm-audit");
    assertThat(script)
        .contains("reaplicação idempotente")
        .contains("audit_liquibase_command \"rollbackCount 1\"")
        .contains("reaplicação após rollback")
        .contains("\"6:6:3\"");
    assertThat(workflow)
        .contains("product-discovery-bpm-audit-mysql57")
        .contains("validate-product-discovery-bpm-audit-mysql57.sh");
  }
}
