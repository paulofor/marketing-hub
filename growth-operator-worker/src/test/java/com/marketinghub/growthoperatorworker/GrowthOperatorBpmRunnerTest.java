package com.marketinghub.growthoperatorworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: validar o contrato somente leitura do executor BPM de Hermes. */
class GrowthOperatorBpmRunnerTest {
  /** Exige referência segregada de experimento antes de executar o modelo. */
  @Test
  void shouldRequireCanonicalExperimentReference() {
    assertThat(GrowthOperatorBpmRunner.experimentId(Map.of("sourceReference", "experiment:88")))
        .isEqualTo(88L);
    assertThatThrownBy(
            () ->
                GrowthOperatorBpmRunner.experimentId(
                    Map.of("sourceReference", "commercial-plan:2")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("experiment:<id>");
  }

  /** Mantém o Codex em sandbox somente leitura com saída estruturada e MCP local. */
  @Test
  void shouldBuildReadOnlyAuditableCommand() {
    WorkerProperties properties = properties();
    GrowthOperatorBpmRunner runner = new GrowthOperatorBpmRunner(properties, new ObjectMapper());

    var command =
        runner.buildCommand(
            Path.of("/tmp/answer.json"), Path.of("/tmp/schema.json"), Path.of("/tmp/mcp.mjs"));

    assertThat(command)
        .containsSubsequence("--sandbox", "read-only")
        .contains("--json", "--search", "--output-schema", "/tmp/schema.json")
        .doesNotContain("--dangerously-bypass-approvals-and-sandbox");
    assertThat(command)
        .contains("mcp_servers.marketing_hub_readonly.command=\"node\"")
        .anyMatch(value -> value.contains("/tmp/mcp.mjs"));
  }

  /** Preserva os últimos contadores cumulativos informados no JSONL do Codex. */
  @Test
  void shouldReadRealTokenUsage() throws Exception {
    Path log = Files.createTempFile("hermes-bpm-token-", ".log");
    try {
      Files.writeString(
          log,
          """
          {"usage":{"input_tokens":100,"cached_input_tokens":20,"output_tokens":30}}
          {"usage":{"input_tokens":180,"input_tokens_details":{"cached_tokens":45},"output_tokens":60}}
          """,
          StandardCharsets.UTF_8);

      GrowthOperatorBpmRunner.TokenUsage usage =
          new GrowthOperatorBpmRunner(properties(), new ObjectMapper()).readTokenUsage(log);

      assertThat(usage).isEqualTo(new GrowthOperatorBpmRunner.TokenUsage(180L, 45L, 60L));
    } finally {
      Files.deleteIfExists(log);
    }
  }

  /** Mantém as ferramentas observadas disponíveis para o callback mesmo após falha técnica. */
  @Test
  void shouldPreserveToolUsageInFailure() throws Exception {
    var tool = new ObjectMapper().readTree("{\"tool\":\"consultar_funil\",\"status\":200}");
    var failure =
        new GrowthOperatorBpmRunner.BpmExecutionException(
            "Falha técnica", GrowthOperatorBpmRunner.TokenUsage.empty(), List.of(tool));

    assertThat(failure.toolUsage()).containsExactly(tool);
  }

  /** Executa o fluxo local completo com processo simulado e escopo exclusivo do experimento. */
  @Test
  void shouldRunBpmContractWithExperimentEnvironmentAndTelemetry() throws Exception {
    Path fakeCodex = Files.createTempFile("fake-codex-hermes-", ".sh");
    try {
      Files.writeString(
          fakeCodex,
          """
          #!/bin/sh
          [ "$MCP_EXPERIMENT_ID" = "88" ] || exit 21
          [ -z "$MCP_COMMERCIAL_PLAN_ID" ] || exit 22
          [ "$MCP_SOURCE_EXECUTION_ID" = "bpm-task-1" ] || exit 23
          answer=""
          while [ "$#" -gt 0 ]; do
            if [ "$1" = "--output-last-message" ]; then
              shift
              answer="$1"
            fi
            shift
          done
          cat >/dev/null
          printf '%s' '{"executionStatus":"BLOCKED","activityOutcome":"Instrumentação ainda sem amostra comercial válida.","observedFacts":["Meta com zero impressões"],"inferences":[],"contradictoryEvidence":[],"evidenceGaps":["Primeira impressão"],"alternatives":[{"name":"A","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"B","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"C","benefit":"B","risk":"R","effort":"E","fit":"F"}],"selectedAlternative":"A","expectedMetric":"Primeira impressão real","continueCriteria":"Eventos íntegros","adjustCriteria":"Divergência persistente","stopCriteria":"Gasto sem evento","recommendedAction":"Aguardar exposição e preservar a configuração atual."}' > "$answer"
          printf '%s\n' '{"usage":{"input_tokens":90,"cached_input_tokens":10,"output_tokens":25}}'
          """,
          StandardCharsets.UTF_8);
      assertThat(fakeCodex.toFile().setExecutable(true)).isTrue();
      WorkerProperties properties = properties();
      properties.setCodexCommand(fakeCodex.toString());

      GrowthOperatorBpmRunner.BpmExecution execution =
          new GrowthOperatorBpmRunner(properties, new ObjectMapper())
              .run(
                  Map.of(
                      "taskId", 1,
                      "activityId", "task-1",
                      "processCode", "operacao-otimizacao-experimento",
                      "sourceReference", "experiment:88"));

      assertThat(execution.result().path("executionStatus").asText()).isEqualTo("BLOCKED");
      assertThat(execution.usage())
          .isEqualTo(new GrowthOperatorBpmRunner.TokenUsage(90L, 10L, 25L));
    } finally {
      Files.deleteIfExists(fakeCodex);
    }
  }

  /** Protege gates comerciais, três alternativas e memória ligada à ferramenta. */
  @Test
  void shouldVersionExperimentOptimizationContract() throws Exception {
    String prompt = read("prompts/bpm/v1/experiment-optimization.md");
    String schema = read("prompts/bpm/v1/experiment-optimization-schema.json");
    String mcp = read("mcp/marketing-hub-readonly.mjs");

    assertThat(prompt)
        .contains(
            "Meta → landing → checkout",
            "pelo menos 100 visitas humanas válidas",
            "p95 de carregamento abaixo de 4",
            "zero erros de recurso",
            "exatamente três alternativas",
            "NaN",
            "justInTimeMemory",
            "appliesToTool");
    assertThat(schema).contains("executionStatus", "COMPLETED", "BLOCKED", "selectedAlternative");
    assertThat(mcp)
        .contains(
            "MCP_EXPERIMENT_ID",
            "consultar_cockpit",
            "funnel/analytics",
            "experiment:${await resolvedExperimentId()}",
            "MCP_TOOL",
            "recuperar_memoria_just_in_time");
  }

  /** Lê integralmente um recurso usado pelo contrato. */
  private String read(String resource) throws Exception {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Configura os limites mínimos do comando testado. */
  private WorkerProperties properties() {
    WorkerProperties properties = new WorkerProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/repository");
    properties.setModel("gpt-5.6-sol");
    properties.setMarketingHubUrl("http://backend:8000");
    return properties;
  }
}
