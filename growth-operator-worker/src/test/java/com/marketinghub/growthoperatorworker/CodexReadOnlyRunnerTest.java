package com.marketinghub.growthoperatorworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar os limites de seguranca do comando Codex. */
class CodexReadOnlyRunnerTest {
  /** Confirma sandbox read-only, pesquisa web e repositorio explicitamente delimitado. */
  @Test
  void shouldForceReadOnlyEphemeralExecution() throws Exception {
    WorkerProperties properties = new WorkerProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/repository");
    CodexReadOnlyRunner runner = new CodexReadOnlyRunner(properties, new ObjectMapper());

    var command = runner.buildCommand(Path.of("/tmp/result.json"));

    assertThat(command).containsSubsequence("--sandbox", "read-only");
    assertThat(command).contains("--search", "--cd", "/workspace/repository");
    assertThat(command).doesNotContain("--dangerously-bypass-approvals-and-sandbox");
  }
}
