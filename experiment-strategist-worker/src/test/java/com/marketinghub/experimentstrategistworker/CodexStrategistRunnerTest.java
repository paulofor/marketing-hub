package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato de isolamento do executor Estrategista. */
class CodexStrategistRunnerTest {
  /** Confirma busca publica, sandbox somente leitura e schema versionado. */
  @Test
  void buildsReadOnlyResearchCommand() {
    WorkerProperties properties = new WorkerProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/marketing-hub");
    properties.setModel("gpt-5.6-sol");
    CodexStrategistRunner runner = new CodexStrategistRunner(properties, new ObjectMapper());

    var command = runner.command(Path.of("/tmp/output.json"), Path.of("/tmp/schema.json"));

    assertThat(command).contains("--search", "--sandbox", "read-only", "--output-schema");
    assertThat(command).doesNotContain("danger-full-access");
    assertThat(properties.getCodexTimeout().toMinutes()).isEqualTo(40);
  }
}
