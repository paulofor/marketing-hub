package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Valida a rota operacional e a porta exclusiva do worker no host compartilhado. */
class WorkerObservabilityContractTest {
  /** Deve manter health e logfile na rota versionada usada pelo MCP. */
  @Test
  void shouldExposeVersionedObservability() throws Exception {
    String application = Files.readString(Path.of("src/main/resources/application.yml"));
    String workflow =
        Files.readString(Path.of("../.github/workflows/landing-generator-agent-worker-ci.yml"));
    assertThat(application).contains("port: 8100");
    assertThat(application).contains("base-path: /ops-landing-generator-observability-v1");
    assertThat(application)
        .contains("name: ${LOG_FILE_NAME:/tmp/landing-generator-agent-worker.log}");
    assertThat(workflow)
        .contains("http://127.0.0.1:8100/ops-landing-generator-observability-v1/health");
  }
}
