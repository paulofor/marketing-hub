package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
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
        .containsSubsequence("codex", "--search", "exec", "-")
        .containsSubsequence("--sandbox", "read-only")
        .containsSubsequence("--cd", "/workspace")
        .containsSubsequence("--output-schema", "/tmp/schema.json")
        .containsSubsequence("--output-last-message", "/tmp/answer.json")
        .containsSubsequence("--model", "gpt-test");
  }

  /** Confirma pesquisa pública auditável e navegador somente leitura no contrato da avaliação. */
  @Test
  void shouldRequireExternalSourcesAndBrowserInspection() throws Exception {
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/customer-agent/v1/evaluation.md"));
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/customer-agent/v1/evaluation-schema.json"));

    assertThat(prompt)
        .contains("node /app/browser/public-research.mjs")
        .contains("padrões sociais/econômicos")
        .contains("nunca como prova de venda");
    assertThat(schema).contains("sources", "collectionMethod", "learning");
  }

  /** Protege os componentes comportamentais e a comparação explícita com o baseline. */
  @Test
  void shouldVersionBehavioralSimulationAndRequireProbabilityDistribution() throws Exception {
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/customer-agent/behavioral-v1/evaluation.md"));
    String schema =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/customer-agent/behavioral-v1/evaluation-schema.json"));

    assertThat(prompt)
        .contains("estado anterior à exposição")
        .contains("Consuma o ativo progressivamente")
        .contains("BASELINE_V1_JSON");
    assertThat(schema)
        .contains("initialState")
        .contains("actionProbabilities")
        .contains("memoryRecall")
        .contains("baselineComparison");
  }

  /**
   * Rejeita uma distribuição probabilística que aparenta precisão sem fechar o universo de ações.
   */
  @Test
  void shouldRejectBehavioralProbabilitiesThatDoNotSumOneHundred() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    CustomerEvaluationCodexRunner runner =
        new CustomerEvaluationCodexRunner("codex", "gpt-test", 40, "/workspace", mapper, null);
    var result =
        mapper.readTree(
            """
            {
              "decision":"AJUSTAR","assessment":"teste","hypotheses":[],"sources":[],
              "initialState":{},"mentalTransitions":[],"memoryRecall":{},
              "baselineComparison":{},
              "actionProbabilities":{"ignore":20,"explore":20,"startAction":20,"abandon":20,"checkout":20,"purchase":20}
            }
            """);

    assertThatThrownBy(() -> runner.validateBehavioral(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("soma recebida=120");
  }
}
