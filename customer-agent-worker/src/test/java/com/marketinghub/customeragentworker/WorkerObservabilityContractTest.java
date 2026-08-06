package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Protege o contrato operacional que permite ao MCP diagnosticar o Agente Cliente. */
class WorkerObservabilityContractTest {

  /** Garante que o worker grave em arquivo e publique a rota canônica de leitura. */
  @Test
  void shouldExposeCanonicalLogfileEndpoint() throws IOException {
    String application = Files.readString(Path.of("src/main/resources/application.yml"));
    String compose = Files.readString(Path.of("docker-compose.yml"));

    assertThat(application).contains("base-path: /ops-customer-agent-observability-v1");
    assertThat(application).contains("logfile: customer-agent-worker-log");
    assertThat(application).contains("name: ${LOG_FILE_NAME:/tmp/customer-agent-worker.log}");
    assertThat(compose).contains("${CUSTOMER_AGENT_WORKER_PORT:-8099}:8099");
  }
}
