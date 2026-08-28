package com.marketinghub.growthoperatorworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
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
    assertThat(command).contains("model_reasoning_effort=\"high\"");
    assertThat(command).doesNotContain("--dangerously-bypass-approvals-and-sandbox");
  }

  /** Confirma o limite operacional padrão de quarenta minutos. */
  @Test
  void shouldLimitCodexExecutionToFortyMinutes() {
    WorkerProperties properties = new WorkerProperties();

    assertThat(properties.getCodexTimeout()).isEqualTo(Duration.ofMinutes(40));
  }

  /** Confirma que o prompt exige operar a estratégia imutável produzida por Atena. */
  @Test
  void shouldRequireExperimentStrategicContractInDiagnosisPrompt() throws Exception {
    String prompt =
        new String(
            new ClassPathResource("prompts/growth-operator/v2/diagnosis.md")
                .getInputStream()
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);

    assertThat(prompt)
        .contains(
            "marketStrategicContract",
            "Atena é a única autora",
            "não pode",
            "redefinir",
            "revisionRequired=true");
  }

  /** Bloqueia sem abrir o Codex quando Atena ainda não produziu estratégia operável. */
  @Test
  void shouldBlockWithoutAtenaContractAtZeroModelCost() throws Exception {
    WorkerProperties properties = new WorkerProperties();
    properties.setCodexCommand("comando-que-nao-deve-ser-executado");
    CodexReadOnlyRunner runner = new CodexReadOnlyRunner(properties, new ObjectMapper());
    GrowthOperatorJob job =
        new GrowthOperatorJob(
            77L,
            4L,
            1,
            "READ_ONLY_DIAGNOSIS",
            "Gerar vendas",
            "Instrumentação",
            """
            {"marketStrategicContract":{"availability":"MISSING","reason":"Atena não executada"}}
            """);

    Map<String, Object> result = runner.run(job);

    assertThat(result)
        .containsEntry("recommendedDecision", "ADJUST")
        .containsEntry("model", "market-strategy-contract-gate-v2")
        .containsEntry("inputTokens", 0L)
        .containsEntry("outputTokens", 0L);
    assertThat(result.get("recommendedAction").toString())
        .contains("Atena", "antes de Hermes operar");
    var raw = new ObjectMapper().readTree(result.get("rawModelResponse").toString());
    assertThat(raw.path("strategicContractAssessment").path("revisionRequired").asBoolean())
        .isTrue();
    assertThat(raw.path("diagnosis").path("decisionAudit").path("observedFacts")).hasSize(1);
    assertThat(raw.path("dailyReport").asText()).contains("fronteira estratégica");
  }

  /** Libera o modelo somente com versão, hash e status estratégico íntegros. */
  @Test
  void shouldReleaseReadyAtenaContract() throws Exception {
    CodexReadOnlyRunner runner =
        new CodexReadOnlyRunner(new WorkerProperties(), new ObjectMapper());
    GrowthOperatorJob job =
        new GrowthOperatorJob(
            78L,
            4L,
            1,
            "READ_ONLY_DIAGNOSIS",
            "Gerar vendas",
            "Instrumentação",
            """
            {
              "marketStrategicContract":{
                "availability":"AVAILABLE",
                "contractVersion":"MARKET_STRATEGY_V2",
                "contentHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "contract":{
                  "contractVersion":"MARKET_STRATEGY_V2",
                  "status":"READY_FOR_OPERATION",
                  "operatorBoundary":"ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH"
                }
              }
            }
            """);

    assertThat(runner.strategicContractGate(job)).isEmpty();
  }

  /** Rejeita diagnóstico que altere o hash estratégico ao devolver a decisão. */
  @Test
  void shouldRejectDifferentAtenaHashInDiagnosis() throws Exception {
    CodexReadOnlyRunner runner =
        new CodexReadOnlyRunner(new WorkerProperties(), new ObjectMapper());
    var result =
        new ObjectMapper()
            .readTree(
                """
                {
                  "strategicContractAssessment":{"availability":"AVAILABLE","contractVersion":"MARKET_STRATEGY_V2","contentHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","strategyPreserved":true,"revisionRequired":false},
                  "alternatives":[{},{},{}],
                  "diagnosis":{"decisionAudit":{}},
                  "decision":"ADJUST",
                  "recommendedAction":"Corrigir instrumentação",
                  "dailyReport":"Instrumentação pendente"
                }
                """);

    assertThatThrownBy(
            () ->
                runner.validateResult(
                    result, "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
        .hasMessageContaining("diagnóstico v2");
  }

  /** Confirma autocorreção auditável e uso seguro da memória ligada à ferramenta. */
  @Test
  void shouldRequireAuditableReasoningProtocol() throws Exception {
    String prompt =
        new String(
            new ClassPathResource("prompts/growth-operator/v2/diagnosis.md")
                .getInputStream()
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);
    String schema =
        new String(
            new ClassPathResource("prompts/growth-operator/v2/diagnosis-schema.json")
                .getInputStream()
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);

    assertThat(prompt)
        .contains(
            "decomposição, verificação e correção",
            "procure evidência",
            "contraditória",
            "Não exponha cadeia de pensamento",
            "tente refutar a alternativa",
            "justInTimeMemory",
            "appliesToTool");
    assertThat(schema)
        .contains(
            "decisionAudit",
            "observedFacts",
            "contradictoryEvidence",
            "changeDecisionIf",
            "confidence",
            "strategicContractAssessment");
  }
}
