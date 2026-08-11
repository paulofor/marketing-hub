package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a publicação dos logs operacionais de Atena para diagnóstico. */
class WorkerObservabilityContractTest {

  /** Garante que health e logfile tenham rotas versionadas validadas após o deploy. */
  @Test
  void shouldExposeDedicatedReadOnlyObservabilityEndpoints() throws Exception {
    String application = Files.readString(Path.of("src/main/resources/application.yml"));
    String workflow =
        Files.readString(Path.of("../.github/workflows/experiment-strategist-worker-ci.yml"));

    assertThat(application).contains("base-path: /ops-experiment-strategist-observability-v1");
    assertThat(application).contains("include: health,logfile");
    assertThat(application)
        .contains("external-file: ${LOG_FILE_NAME:/tmp/experiment-strategist-worker.log}");
    assertThat(application)
        .contains("name: ${LOG_FILE_NAME:/tmp/experiment-strategist-worker.log}");
    assertThat(workflow)
        .contains("http://127.0.0.1:8096/ops-experiment-strategist-observability-v1/health");
    assertThat(workflow)
        .contains("http://127.0.0.1:8096/ops-experiment-strategist-observability-v1/logfile");
    assertThat(workflow).doesNotContain("experiment-strategist-worker-log");
  }
}
