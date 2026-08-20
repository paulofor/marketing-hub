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
            "codex", "gpt-test", 40, "/workspace", "read-only", new ObjectMapper(), null);

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

  /** Permite execução sem sandbox interna somente quando o ambiente externo já está isolado. */
  @Test
  void shouldUseExternallySandboxedModeWhenExplicitlyConfigured() {
    CustomerEvaluationCodexRunner runner =
        new CustomerEvaluationCodexRunner(
            "codex", "gpt-test", 40, "/workspace", "danger-full-access", new ObjectMapper(), null);

    List<String> command =
        runner.buildCommand(Path.of("/tmp/answer.json"), Path.of("/tmp/schema.json"));

    assertThat(command)
        .contains("--dangerously-bypass-approvals-and-sandbox")
        .doesNotContain("--sandbox", "read-only");
  }

  /** Anexa a evidência visual validada ao Codex sem expor acesso interno. */
  @Test
  void shouldAttachVisualEvidenceToStructuredEvaluation() {
    CustomerEvaluationCodexRunner runner =
        new CustomerEvaluationCodexRunner(
            "codex", "gpt-test", 40, "/workspace", "read-only", new ObjectMapper(), null);
    Path image = Path.of("/tmp/approved-product.png");

    List<String> command =
        runner.buildCommand(
            Path.of("/tmp/answer.json"),
            Path.of("/tmp/schema.json"),
            Path.of("/tmp/customer-agent.mjs"),
            List.of(image));

    assertThat(command).containsSubsequence("--image", image.toString());
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
        .contains("evidência visual primária")
        .contains("o CTA interno deve orientar as clientes dela")
        .contains("não exija fotografia exclusiva da compradora")
        .contains("CTA de captura de tela, salvar, compartilhar ou responder")
        .contains("não reprove a peça isolada por não exibir todo o pacote")
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

  /** Protege a versão afetiva e social como evolução explícita do simulador. */
  @Test
  void shouldRequireAffectiveSocialAndBoundedRationalityInBehavioralVersionTwo() throws Exception {
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/customer-agent/behavioral-v2/evaluation.md"));
    String schema =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/customer-agent/behavioral-v2/evaluation-schema.json"));
    String core =
        Files.readString(Path.of("src/main/resources/prompts/psique/behavioral-core-v2.md"));

    assertThat(prompt)
        .contains("{{PSIQUE_BEHAVIORAL_CORE_V2}}")
        .contains("primeiro impulso afetivo")
        .contains("pertencimento, admiração, valor relacional e amor");
    assertThat(schema)
        .contains(
            "affectiveImpulse",
            "motivationalDynamics",
            "noveltyFamiliarity",
            "relationalValue",
            "postHocRationalization",
            "FOUNDATIONAL");
    assertThat(core)
        .contains("evitar esforço")
        .contains("surpresa")
        .contains("amada")
        .contains("Não recomende explorar vergonha");
  }

  /**
   * Rejeita uma distribuição probabilística que aparenta precisão sem fechar o universo de ações.
   */
  @Test
  void shouldRejectBehavioralProbabilitiesThatDoNotSumOneHundred() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    CustomerEvaluationCodexRunner runner =
        new CustomerEvaluationCodexRunner(
            "codex", "gpt-test", 40, "/workspace", "read-only", mapper, null);
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

  /** Rejeita v2 sem necessidade relacional estrutural, mesmo com probabilidades coerentes. */
  @Test
  void shouldRejectBehavioralVersionTwoWithoutFoundationalRelationalNeed() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    CustomerEvaluationCodexRunner runner =
        new CustomerEvaluationCodexRunner(
            "codex", "gpt-test", 40, "/workspace", "read-only", mapper, null);
    var result =
        mapper.readTree(
            """
            {
              "decision":"AJUSTAR","assessment":"teste","hypotheses":[],"sources":[],
              "initialState":{},"mentalTransitions":[],"memoryRecall":{},
              "baselineComparison":{},
              "actionProbabilities":{"ignore":20,"explore":20,"startAction":20,"abandon":20,"checkout":10,"purchase":10},
              "affectiveImpulse":{},"motivationalDynamics":{},"noveltyFamiliarity":{},
              "relationalValue":{"foundationalNeed":"OPTIONAL"},
              "postHocRationalization":{},"ethicalBoundary":{}
            }
            """);

    assertThatThrownBy(() -> runner.validateBehavioral(result, "BEHAVIORAL_V2"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("comportamental v2");
  }
}
