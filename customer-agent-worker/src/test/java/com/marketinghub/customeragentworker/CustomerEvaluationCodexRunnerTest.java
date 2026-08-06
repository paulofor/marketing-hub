package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: proteger o contrato estruturado do executor de avaliações do Agente Cliente.
 */
class CustomerEvaluationCodexRunnerTest {

  /** Garante entrada por stdin e separação entre diagnóstico e resposta final validada. */
  @Test
  void shouldUseStructuredOutputContract() {
    CustomerEvaluationCodexRunner runner =
        new CustomerEvaluationCodexRunner(
            "codex", "gpt-test", 40, "/workspace", new ObjectMapper(), null);

    List<String> command =
        runner.buildCommand(Path.of("/tmp/answer.json"), Path.of("/tmp/schema.json"));

    assertThat(command)
        .containsSubsequence("codex", "exec", "-")
        .containsSubsequence("--sandbox", "read-only")
        .containsSubsequence("--cd", "/workspace")
        .containsSubsequence("--output-schema", "/tmp/schema.json")
        .containsSubsequence("--output-last-message", "/tmp/answer.json")
        .containsSubsequence("--model", "gpt-test");
  }
}
