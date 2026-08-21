package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
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

  /**
   * Confirma que o adaptador Clarity só entra quando existe credencial e nunca vai na linha de
   * comando.
   */
  @Test
  void enablesAggregateClarityWithoutExposingTokenInCommand() {
    WorkerProperties properties = new WorkerProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/marketing-hub");
    CodexStrategistRunner runner = new CodexStrategistRunner(properties, new ObjectMapper());

    var command =
        runner.command(
            Path.of("/tmp/output.json"),
            Path.of("/tmp/schema.json"),
            Path.of("/tmp/internal.mjs"),
            Path.of("/tmp/clarity.mjs"),
            true);

    assertThat(command)
        .contains("mcp_servers.clarity_aggregate.command=\"node\"")
        .contains("mcp_servers.clarity_aggregate.args=[\"/tmp/clarity.mjs\"]")
        .noneMatch(value -> value.contains("api-token"));
  }

  /** Confirma que navegador, pesquisa externa e procedência entram no artefato versionado. */
  @Test
  void packagesAuditableBrowserResearch() throws Exception {
    String dockerfile = Files.readString(Path.of("Dockerfile"));
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/experiment-strategist/v1/research.md"));
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/experiment-strategist/v1/research-schema.json"));

    assertThat(dockerfile)
        .contains("FROM node:20-bookworm-slim AS node-runtime")
        .contains("FROM eclipse-temurin:21-jre-noble")
        .contains("ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm")
        .contains("node --version | grep -Eq '^v2[0-9]\\.'")
        .contains("npx playwright-core install --with-deps chromium")
        .contains("COPY --from=build /build/src/main/resources/browser /app/browser");
    assertThat(prompt)
        .contains("node /app/browser/public-research.mjs")
        .contains("duas classes independentes de evidência")
        .contains("mapa comparativo dos concorrentes")
        .contains("linguagem literal pública de clientes")
        .contains("o mercado oferece X, mas o cliente ainda precisa fazer Y")
        .contains("snapshots: PAGE, SOURCE e DEVICE")
        .contains("Nunca solicite gravação");
    assertThat(schema)
        .contains("marketIntelligence", "customerLanguage", "competitors", "customerEffort")
        .contains(
            "evidenceClass",
            "statementType",
            "portfolioAssessment",
            "winnerProductId",
            "operatorBoundary",
            "positioning",
            "memoryOutcome");
    assertThat(schema).contains("behavioralAssessment", "AGGREGATE_ONLY_NO_INDIVIDUAL_PROFILING");
  }

  /** Confirma que constantes booleanas mantêm o tipo exigido pelo Structured Outputs. */
  @Test
  void declaresTypeForBooleanConstantInStrictSchema() throws Exception {
    var schema =
        new ObjectMapper()
            .readTree(
                Path.of("src/main/resources/prompts/experiment-strategist/v1/research-schema.json")
                    .toFile());

    var approval =
        schema
            .path("properties")
            .path("recommendation")
            .path("properties")
            .path("requiresHumanApproval");

    assertThat(approval.path("type").asText()).isEqualTo("boolean");
    assertThat(approval.path("const").asBoolean()).isTrue();
  }

  /** Impede declarar vencedor sem venda e entrega e preserva a fronteira com o Operador. */
  @Test
  void requiresAuditablePortfolioAssessment() throws Exception {
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/experiment-strategist/v1/research.md"));
    var schema =
        new ObjectMapper()
            .readTree(
                Path.of("src/main/resources/prompts/experiment-strategist/v1/research-schema.json")
                    .toFile());

    assertThat(prompt)
        .contains("Sem venda aprovada e entrega satisfatória")
        .contains("não inicie, pause, avance ou encerre experimento");
    assertThat(schema.path("required"))
        .anyMatch(value -> value.asText().equals("portfolioAssessment"));
    assertThat(
            schema
                .path("properties")
                .path("portfolioAssessment")
                .path("properties")
                .path("operatorBoundary")
                .path("const")
                .asText())
        .isEqualTo("STRATEGIST_RECOMMENDS_OPERATOR_EXECUTES");
  }

  /** Protege a proposta estratégica que antecede a validação financeira de Plutus. */
  @Test
  void packagesCommercialAssumptionProposal() throws Exception {
    String prompt =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/experiment-strategist/v1/commercial-assumptions.md"));
    String schema =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/experiment-strategist/v1/commercial-assumptions-schema.json"));

    assertThat(prompt).contains("três alternativas", "Plutus", "não comprova disposição de pagar");
    assertThat(schema)
        .contains("proposedAssumptions", "offerPriceBrl", "expectedConversionRatePercent");
  }
}
