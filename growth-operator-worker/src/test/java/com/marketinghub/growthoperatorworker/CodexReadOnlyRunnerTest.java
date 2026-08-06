package com.marketinghub.growthoperatorworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: validar os limites de seguranca do comando Codex. */
class CodexReadOnlyRunnerTest {
  /** Confirma sandbox read-only, pesquisa web e montagem externa aceita explicitamente. */
  @Test
  void shouldForceReadOnlyEphemeralExecution() throws Exception {
    WorkerProperties properties = new WorkerProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/repository");
    CodexReadOnlyRunner runner = new CodexReadOnlyRunner(properties, new ObjectMapper());

    var command = runner.buildCommand(Path.of("/tmp/result.json"));

    assertThat(command).containsSubsequence("--sandbox", "read-only");
    assertThat(command).contains("--search", "--cd", "/workspace/repository");
    assertThat(command).containsSubsequence("exec", "-", "--skip-git-repo-check");
    assertThat(command)
        .contains("mcp_servers.marketing_hub_readonly.command=\"node\"")
        .anyMatch(value -> value.startsWith("mcp_servers.marketing_hub_readonly.args="));
    assertThat(command).doesNotContain("--dangerously-bypass-approvals-and-sandbox");
  }

  /** Confirma que o prompt exige comparar eventos com o contrato estrategico do experimento. */
  @Test
  void shouldRequireExperimentStrategicContractInDiagnosisPrompt() throws Exception {
    String prompt =
        new String(
            new ClassPathResource("prompts/growth-operator/v1/diagnosis.md")
                .getInputStream()
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);

    assertThat(prompt)
        .contains(
            "experimentStrategicContract",
            "objetivo, hipótese, métrica/meta e critérios de continuar, ajustar e parar",
            "retorne ADJUST");
  }
}
