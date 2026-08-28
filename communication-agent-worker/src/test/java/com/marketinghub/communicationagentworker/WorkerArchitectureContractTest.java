package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar empacotamento, observabilidade e isolamento operacional de Íris. */
class WorkerArchitectureContractTest {

  /** Exige porta exclusiva, health e logfile na rota conhecida pelo MCP central. */
  @Test
  void shouldExposeVersionedObservability() throws Exception {
    String application = Files.readString(Path.of("src/main/resources/application.yml"));
    String workflow =
        Files.readString(Path.of("../.github/workflows/communication-agent-worker-ci.yml"));

    assertThat(application)
        .contains(
            "port: ${SERVER_PORT:8101}",
            "base-path: /ops-communication-agent-observability-v1",
            "include: health,logfile",
            "external-file: ${LOG_FILE_NAME:/tmp/communication-agent-worker.log}");
    assertThat(workflow)
        .contains(
            "http://127.0.0.1:8101/ops-communication-agent-observability-v1/health",
            "http://127.0.0.1:8101/ops-communication-agent-observability-v1/logfile");
  }

  /** Impede banco, host network, privilégio e sessão Codex compartilhada. */
  @Test
  void shouldKeepContainerIsolatedAndReproducible() throws Exception {
    String compose = Files.readString(Path.of("docker-compose.yml"));
    String dockerfile = Files.readString(Path.of("Dockerfile"));

    assertThat(compose)
        .contains(
            "read_only: true",
            "no-new-privileges:true",
            "tmpfs:",
            "COMMUNICATION_AGENT_CODEX_HOME",
            ":/workspace/marketing-hub:ro")
        .doesNotContain("network_mode: host", "privileged: true", "/var/run/docker.sock");
    assertThat(dockerfile)
        .contains("@openai/codex@${CODEX_VERSION}", "USER operator")
        .doesNotContain("COPY .env", "auth.json");
  }

  /** Confirma que o MCP usa identidade fixa, APIs do backend e nenhuma conexão de banco. */
  @Test
  void shouldKeepMcpScopedToIrisAndCurrentTask() throws Exception {
    String mcp = Files.readString(Path.of("src/main/resources/mcp/communication-agent.mjs"));

    assertThat(mcp)
        .contains(
            "/api/internal/agent-tasks/communication-director/stage-executions/${taskId}",
            "/api/internal/agent-memory/v1/agents/communication-director",
            "MCP_TASK_ID",
            "MCP_SOURCE_REFERENCE",
            "(?::[A-Za-z0-9_-]+)*$",
            "sourceExecutionId: `agent-task-${taskId}`")
        .doesNotContain("mysql", "jdbc", "db_query", "child_process");
  }
}
