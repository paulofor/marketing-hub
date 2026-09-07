package com.marketinghub.customeragentworker;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: executar uma análise observacional delimitada e retornar somente seu JSON
 * final.
 */
@Component
public class CodexObservationAnalyzer {
  private final String executable;
  private final String model;
  private final String reasoningEffort;
  private final String schemaPath;
  private final Duration timeout;

  /** Inicializa o executor com modelo, raciocínio máximo, schema e limite operacional. */
  public CodexObservationAnalyzer(
      @Value("${CUSTOMER_AGENT_CODEX_EXECUTABLE:codex}") String executable,
      @Value("${CUSTOMER_AGENT_MODEL:gpt-5.6-sol}") String model,
      @Value("${CUSTOMER_AGENT_REASONING_EFFORT:max}") String reasoningEffort,
      @Value(
              "${CUSTOMER_AGENT_OBSERVATION_SCHEMA:/app/prompts/customer-agent/v3/digital-observation-schema.json}")
          String schemaPath,
      @Value("${CUSTOMER_AGENT_MODEL_TIMEOUT:PT40M}") Duration timeout) {
    this.executable = executable;
    this.model = model;
    this.reasoningEffort = PsiqueReasoningPolicy.requireMaximum(reasoningEffort);
    this.schemaPath = schemaPath;
    this.timeout = timeout;
  }

  /** Executa o modelo sem sessão persistente, ferramentas ou configuração herdada do usuário. */
  public String analyze(String prompt, Path workDirectory) throws Exception {
    Path result = workDirectory.resolve("model-output.json");
    Path diagnostic = workDirectory.resolve("codex-execution.log");
    Process process =
        new ProcessBuilder(command(prompt, result))
            .redirectErrorStream(true)
            .redirectOutput(diagnostic.toFile())
            .start();
    if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
      process.destroyForcibly();
      process.waitFor(10, TimeUnit.SECONDS);
      throw new IllegalStateException(
          "Timeout da análise observacional após " + timeout.toMinutes() + " minutos.");
    }
    String diagnostics = Files.readString(diagnostic, StandardCharsets.UTF_8);
    if (process.exitValue() != 0) {
      throw new IllegalStateException("Codex falhou: " + diagnostics);
    }
    if (!Files.isRegularFile(result) || Files.size(result) == 0) {
      throw new IllegalStateException("Codex concluiu sem produzir o JSON observacional.");
    }
    return Files.readString(result, StandardCharsets.UTF_8);
  }

  /** Monta o comando efêmero com raciocínio máximo explícito e sem configuração herdada. */
  List<String> command(String prompt, Path result) {
    return List.of(
        executable,
        "--search",
        "exec",
        "--sandbox",
        "read-only",
        "--model",
        model,
        "--config",
        PsiqueReasoningPolicy.codexConfiguration(reasoningEffort),
        "--skip-git-repo-check",
        "--ephemeral",
        "--ignore-user-config",
        "--color",
        "never",
        "--output-schema",
        schemaPath,
        "--output-last-message",
        result.toString(),
        prompt);
  }
}
