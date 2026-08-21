package com.marketinghub.growthoperatorworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
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

  /** Confirma autocorreção auditável e uso seguro da memória ligada à ferramenta. */
  @Test
  void shouldRequireAuditableReasoningProtocol() throws Exception {
    String prompt =
        new String(
            new ClassPathResource("prompts/growth-operator/v1/diagnosis.md")
                .getInputStream()
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);
    String schema =
        new String(
            new ClassPathResource("prompts/growth-operator/v1/diagnosis-schema.json")
                .getInputStream()
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);

    assertThat(prompt)
        .contains(
            "decomposição, verificação e correção",
            "procure evidência",
            "contraditória, teste as três alternativas",
            "Não exponha cadeia de pensamento",
            "Tente refutar a alternativa escolhida",
            "justInTimeMemory",
            "appliesToTool");
    assertThat(schema)
        .contains(
            "decisionAudit",
            "observedFacts",
            "contradictoryEvidence",
            "changeDecisionIf",
            "confidence");
  }
}
