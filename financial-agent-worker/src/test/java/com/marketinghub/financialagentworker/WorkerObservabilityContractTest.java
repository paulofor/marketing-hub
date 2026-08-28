package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Protege o contrato operacional que permite ao MCP diagnosticar o Agente Financeiro. */
class WorkerObservabilityContractTest {

  /** Garante que o worker grave em arquivo e publique a rota canônica de leitura. */
  @Test
  void shouldExposeCanonicalLogfileEndpoint() throws IOException {
    String application = Files.readString(Path.of("src/main/resources/application.yml"));
    String compose = Files.readString(Path.of("docker-compose.yml"));

    assertThat(application).contains("base-path: /ops-financial-agent-observability-v1");
    assertThat(application).contains("logfile: financial-agent-worker-log");
    assertThat(application).contains("name: ${LOG_FILE_NAME:/tmp/financial-agent-worker.log}");
    assertThat(compose).contains("${FINANCIAL_AGENT_WORKER_PORT:-8095}:8095");
  }

  /** Garante que a prontidão publique a versão vigente e a build imutável do deploy. */
  @Test
  void shouldReportCurrentAgentVersionAndDeploymentBuild() throws IOException {
    String compose = Files.readString(Path.of("docker-compose.yml"));
    String workflow =
        Files.readString(Path.of("../.github/workflows/financial-agent-worker-ci.yml"));

    assertThat(compose).contains("AGENT_HEALTH_VERSION: \"4\"");
    assertThat(compose).contains("AGENT_BUILD_REFERENCE: ${AGENT_BUILD_REFERENCE:-local}");
    assertThat(workflow).contains("AGENT_BUILD_REFERENCE='${GITHUB_SHA}'");
  }
}
