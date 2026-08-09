package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a publicação segura dos logs operacionais do Aprovador Meta. */
class WorkerObservabilityContractTest {

  /** Garante que health e logfile tenham rota dedicada e arquivo conhecido. */
  @Test
  void shouldExposeDedicatedReadOnlyObservabilityEndpoints() throws Exception {
    String application = Files.readString(Path.of("src/main/resources/application.yml"));
    String workflow =
        Files.readString(Path.of("../.github/workflows/meta-ad-approver-worker-ci.yml"));

    assertThat(application).contains("base-path: /ops-meta-ad-approver-observability-v1");
    assertThat(application).contains("include: health,logfile");
    assertThat(application)
        .contains("external-file: ${LOG_FILE_NAME:/tmp/meta-ad-approver-worker.log}");
    assertThat(application).contains("name: ${LOG_FILE_NAME:/tmp/meta-ad-approver-worker.log}");
    assertThat(workflow)
        .contains("http://127.0.0.1:8097/ops-meta-ad-approver-observability-v1/health");
    assertThat(workflow).doesNotContain("http://127.0.0.1:8097/actuator/health");
  }
}
