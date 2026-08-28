package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a reconciliação MySQL 5.7 entre Plutus e sua tarefa auditável. */
class FinancialAgentTaskAuditChangelogTest {
  private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
  private static final String FILE =
      "changesets/2026-08-28-financial-agent-task-audit-reconciliation.yaml";

  /** Confirma que o mestre resolve o retroativo relativamente ao próprio changelog. */
  @Test
  void deveIncluirRetroativoFinanceiroPorCaminhoRelativo() throws Exception {
    String master = Files.readString(CHANGELOG_ROOT.resolve("db.changelog-master.yaml"));

    assertThat(master)
        .contains("file: " + FILE + System.lineSeparator() + "      relativeToChangelogFile: true");
  }

  /** Exige atualização por JOIN, evidência estruturada e ausência do erro MySQL 1093. */
  @Test
  void deveReconciliarAuditoriaSemSubconsultaDaTabelaAlvo() throws Exception {
    String yaml = Files.readString(CHANGELOG_ROOT.resolve(FILE));

    assertThat(yaml)
        .contains("type: mysql")
        .contains("splitStatements: true")
        .contains("stripComments: true")
        .contains("UPDATE agent_task task\n              JOIN financial_agent_execution execution")
        .contains("task.received_at = COALESCE(task.received_at, execution.started_at)")
        .contains("task.result_json = COALESCE")
        .contains("task.evidence_json = COALESCE")
        .contains("'financial-agent-task-audit/v1'")
        .contains("SHA2(execution.financial_snapshot, 256)")
        .contains("'externalSideEffects', JSON_EXTRACT('false', '$')")
        .doesNotContain("TIMESTAMP NOT NULL")
        .doesNotContain("SELECT 1 FROM agent_task")
        .doesNotContain("SELECT\n                    FROM agent_task");
  }

  /** Confirma que aplicação física e reaplicação idempotente estão versionadas. */
  @Test
  void deveManterMatrizFisicaMysql57Versionada() throws Exception {
    Path moduleRoot = Path.of("");
    String dockerfile =
        Files.readString(moduleRoot.resolve("Dockerfile.mysql57-financial-agent-task-audit"));
    String compose =
        Files.readString(
            moduleRoot.resolve("docker-compose.financial-agent-task-audit-mysql57.yml"));
    String script =
        Files.readString(
            moduleRoot.resolve("scripts/validate-financial-agent-task-audit-mysql57.sh"));
    String workflow =
        Files.readString(moduleRoot.resolve("../../.github/workflows/liquibase-mysql57.yml"));

    assertThat(dockerfile).contains("FROM mysql:5.7");
    assertThat(compose).contains("dockerfile: Dockerfile.mysql57-financial-agent-task-audit");
    assertThat(script)
        .contains("conclusão auditada do Plutus")
        .contains("tokens históricos não inventados")
        .contains("reaplicação idempotente");
    assertThat(workflow)
        .contains("validate-financial-agent-task-audit")
        .contains("validate-financial-agent-task-audit-mysql57.sh");
  }
}
