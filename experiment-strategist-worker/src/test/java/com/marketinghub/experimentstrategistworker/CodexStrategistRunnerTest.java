package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato de isolamento do executor Estrategista. */
class CodexStrategistRunnerTest {
  /** Comprova processo local → uso JSONL → callback, sem inferência nem perda da resposta bruta. */
  @Test
  void reportsAuditableUsageFromCompletedLocalProcess() throws Exception {
    WorkerProperties properties = new WorkerProperties();
    properties.setCodexCommand(
        Path.of("src/test/resources/research/cost-codex.mjs").toAbsolutePath().toString());
    properties.setRepositoryPath(Path.of(".").toAbsolutePath().toString());
    properties.setBackendUrl("http://127.0.0.1:1");
    properties.setModel("gpt-5.6-sol");
    var runner = new CodexStrategistRunner(properties, new ObjectMapper());
    for (long id : new long[] {301L, 809L}) {
      var callback =
          runner.run(
              new StrategistJob(
                  id, id + 100L, "RUNNING", "READ_ONLY_RESEARCH", "Reavaliar prova local", "{}"));
      assertThat(callback)
          .containsEntry("inputTokens", 100L)
          .containsEntry("cachedInputTokens", 20L)
          .containsEntry("outputTokens", 30L)
          .containsEntry("effectiveServiceTier", "STANDARD");
      assertThat(callback.get("rawModelResponse").toString()).contains("MARKET_STRATEGY_V2");
      assertThat(callback.get("estimatedCost")).isNull();
    }
  }

  /** Conserva ausência ou contradição como desconhecida e não soma eventos cumulativos. */
  @Test
  void refusesIncompleteUsageAndReadsLatestTotal() throws Exception {
    Path output = Files.createTempFile("atena-cost-", ".jsonl");
    try {
      var runner = new CodexStrategistRunner(new WorkerProperties(), new ObjectMapper());
      for (String usage :
          new String[] {
            "{}",
            "{\"input_tokens\":12,\"output_tokens\":7}",
            "{\"input_tokens\":12,\"cached_input_tokens\":13,\"output_tokens\":7}"
          }) {
        Files.writeString(output, "{\"usage\":" + usage + "}\n");
        assertThat(runner.readTokenUsage(output)).isNull();
      }
      Files.writeString(
          output,
          "{\"usage\":{\"input_tokens\":100,\"cached_input_tokens\":20,\"output_tokens\":30}}\n"
              + "{\"usage\":{\"input_tokens\":140,\"cached_input_tokens\":40,\"output_tokens\":50}}\n");
      assertThat(runner.readTokenUsage(output))
          .isEqualTo(new CodexStrategistRunner.TokenUsage(140, 40, 50));
    } finally {
      Files.deleteIfExists(output);
    }
  }

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
    assertThat(command)
        .contains(
            "mcp_servers.experiment_strategist.env_vars=[\"MCP_BACKEND_URL\",\"MCP_EXECUTION_ID\"]");
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

  /** Confirma base de navegador compartilhada, pesquisa externa e procedência no artefato. */
  @Test
  void packagesAuditableBrowserResearch() throws Exception {
    String dockerfile = Files.readString(Path.of("Dockerfile"));
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/experiment-strategist/v2/research.md"));
    String schema =
        Files.readString(
            Path.of("src/main/resources/prompts/experiment-strategist/v2/research-schema.json"));

    assertThat(dockerfile)
        .contains("FROM mcr.microsoft.com/playwright:v1.54.2-noble@sha256:")
        .contains("FROM eclipse-temurin:21-jre-noble AS java-runtime")
        .contains("COPY --from=java-runtime /opt/java/openjdk /opt/java/openjdk")
        .contains("node --version | grep -Eq '^v2[0-9]\\.'")
        .contains("npm cache clean --force")
        .doesNotContain("playwright-core install", "apt-get", "chmod -R a+rX /ms-playwright")
        .contains("COPY --from=build /build/src/main/resources/browser /app/browser");
    assertThat(prompt)
        .contains("consultar_paginas_publicas")
        .doesNotContain("node /app/browser/public-research.mjs")
        .contains("duas classes independentes de evidência")
        .contains("mapa comparativo dos concorrentes")
        .contains("linguagem literal pública de clientes")
        .contains("snapshots PAGE, SOURCE e DEVICE")
        .contains("Hermes não pode redefinir sua estratégia")
        .contains("Dédalo materializa o produto; Íris materializa")
        .contains("landing e comunicação")
        .doesNotContain("Dédalo materializa produto, landing e comunicação");
    assertThat(schema)
        .contains("marketIntelligence", "customerLanguage", "competitors", "customerEffort")
        .contains(
            "evidenceClass",
            "portfolioAssessment",
            "winnerProductId",
            "operatorBoundary",
            "positioning",
            "marketStrategicContract");
    assertThat(schema).contains("behavioralAssessment", "AGGREGATE_ONLY_NO_INDIVIDUAL_PROFILING");
  }

  /** Confirma que constantes booleanas mantêm o tipo exigido pelo Structured Outputs. */
  @Test
  void declaresTypeForBooleanConstantInStrictSchema() throws Exception {
    var schema =
        new ObjectMapper()
            .readTree(
                Path.of("src/main/resources/prompts/experiment-strategist/v2/research-schema.json")
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
            Path.of("src/main/resources/prompts/experiment-strategist/v2/research.md"));
    var schema =
        new ObjectMapper()
            .readTree(
                Path.of("src/main/resources/prompts/experiment-strategist/v2/research-schema.json")
                    .toFile());

    assertThat(prompt)
        .contains("Sem venda e entrega, não declare vencedor")
        .contains("não inicia, pausa, avança ou encerra experimento");
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

  /** Exige a autoria única de Atena e a fronteira imutável consumida por Hermes. */
  @Test
  void requiresVersionedMarketStrategicContract() throws Exception {
    var schema =
        new ObjectMapper()
            .readTree(
                Path.of("src/main/resources/prompts/experiment-strategist/v2/research-schema.json")
                    .toFile());
    var contract = schema.path("properties").path("marketStrategicContract");

    assertThat(schema.path("required"))
        .anyMatch(value -> value.asText().equals("marketStrategicContract"));
    assertThat(contract.path("properties").path("contractVersion").path("const").asText())
        .isEqualTo("MARKET_STRATEGY_V2");
    assertThat(contract.path("properties").path("operatorBoundary").path("const").asText())
        .isEqualTo("ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH");
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

  /** Preserva a separação entre estratégia, produto, comunicação e audiovisual no núcleo v1. */
  @Test
  void assignsProductToDedaloAndCommunicationToIris() throws Exception {
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/experiment-strategist/v1/agent-core.md"));

    assertThat(prompt)
        .contains("produto pertence a Dédalo, comunicação a Íris")
        .contains("audiovisual a")
        .contains("Apolo")
        .doesNotContain("produto, landing e comunicação pertencem a Dédalo");
  }
}
