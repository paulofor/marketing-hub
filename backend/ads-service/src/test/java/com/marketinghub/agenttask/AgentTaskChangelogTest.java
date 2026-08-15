package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato MySQL 5.7 da caixa de entrada dos agentes. */
class AgentTaskChangelogTest {
  private static final Path CHANGELOG =
      Path.of("src/main/resources/db/changelog/changesets/2026-08-11-agent-task-inbox.yaml");

  /** Confirma campos temporais DATETIME e vínculos com remetente e destinatário. */
  @Test
  void preservesMysql57AndAuditContract() throws IOException {
    String yaml = Files.readString(CHANGELOG);
    assertThat(yaml)
        .contains("dbms:\n            type: mysql")
        .contains("created_at DATETIME NOT NULL")
        .contains("updated_at DATETIME NOT NULL")
        .contains("fk_agent_task_assignee")
        .contains("fk_agent_task_requester");
  }

  /** Confirma resolução relativa obrigatória no changelog mestre. */
  @Test
  void masterUsesRelativeInclude() throws IOException {
    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));
    assertThat(master)
        .contains(
            "file: changesets/2026-08-11-agent-task-inbox.yaml\n"
                + "      relativeToChangelogFile: true");
  }

  /** Protege o vínculo auditável sem tornar tarefas históricas inválidas. */
  @Test
  void addsOptionalPublishedProcessBindingAndGovernedException() throws IOException {
    String yaml =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-15-agent-task-business-process.yaml"));
    assertThat(yaml)
        .contains("process_definition_id BIGINT NULL")
        .contains("process_activity_id VARCHAR(100) NULL")
        .contains("exceptional TINYINT(1) NOT NULL DEFAULT 0")
        .contains("fk_agent_task_process_definition");
  }

  /** Confirma os marcos temporais de recebimento e entrega compatíveis com MySQL 5.7. */
  @Test
  void addsCanonicalReceiptAndDeliveryTimestamps() throws IOException {
    String yaml =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-15-agent-task-delivery-timestamps.yaml"));
    assertThat(yaml)
        .contains("received_at DATETIME NULL")
        .contains("delivered_at DATETIME NULL")
        .contains("MODIFY COLUMN received_at DATETIME NOT NULL")
        .contains("SET received_at = created_at")
        .contains("SET delivered_at = updated_at");
  }
}
