package com.marketinghub.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato MySQL 5.7 da execução semanal do Plano Comercial. */
class CommercialPlanWeeklyExecutionChangelogTest {
  private static final Path CHANGELOG =
      Path.of(
          "src/main/resources/db/changelog/changesets/2026-08-11-commercial-plan-weekly-execution.yaml");

  /** Confirma auditoria de versão, agente, prazo, status e metas financeiras. */
  @Test
  void preservesWeeklyExecutionContract() throws IOException {
    String yaml = Files.readString(CHANGELOG);
    assertThat(yaml)
        .contains("dbms:\n            type: mysql")
        .contains("plan_version_number INT")
        .contains("assigned_agent_key VARCHAR(100)")
        .contains("execution_status VARCHAR(30) NOT NULL")
        .contains("due_date DATE")
        .contains("planned_cost DECIMAL(15,2)")
        .contains("planned_revenue DECIMAL(15,2)");
  }

  /** Confirma resolução relativa obrigatória no changelog mestre. */
  @Test
  void masterUsesRelativeInclude() throws IOException {
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));
    assertThat(master)
        .contains(
            "file: changesets/2026-08-11-commercial-plan-weekly-execution.yaml\n"
                + "      relativeToChangelogFile: true");
  }
}
