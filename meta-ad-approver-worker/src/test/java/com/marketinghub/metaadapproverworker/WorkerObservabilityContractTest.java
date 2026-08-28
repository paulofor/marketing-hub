package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a publicação segura dos logs operacionais do Aprovador Meta. */
class WorkerObservabilityContractTest {

  /** Garante observabilidade dedicada e credencial visual isolada no executor de Íris. */
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
    assertThat(workflow).contains("Provision OpenAI credential for Íris");
    assertThat(workflow).contains("test -f /run/secrets/openai_api_key");
    assertThat(workflow).contains("test -r /run/secrets/openai_api_key");
    assertThat(workflow).contains("test -s /run/secrets/openai_api_key");
    assertThat(workflow).doesNotContain("http://127.0.0.1:8097/actuator/health");
  }
}
