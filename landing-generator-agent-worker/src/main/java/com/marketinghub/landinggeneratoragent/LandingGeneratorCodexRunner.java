package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar o Codex 5.6 Sol isolado e validar o plano de correção da landing. */
@Component
public class LandingGeneratorCodexRunner {
  private final LandingGeneratorAgentProperties properties;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;

  /** Inicializa o runner com configuração, JSON e telemetria. */
  public LandingGeneratorCodexRunner(
      LandingGeneratorAgentProperties properties,
      ObjectMapper objectMapper,
      CodexTelemetryReporter telemetry) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
  }

  /** Executa o agente em sandbox read-only e devolve auditoria integral. */
  public Map<String, Object> run(LandingAgentJob job) throws IOException, InterruptedException {
    Path output = Files.createTempFile("landing-agent-", ".json");
    Path log = Files.createTempFile("landing-agent-process-", ".log");
    Path schema =
        materialize(
            "prompts/landing-generator/v1/remediation-schema.json", "landing-schema-", ".json");
    Path mcp = materialize("mcp/landing-generator.mjs", "landing-mcp-", ".mjs");
    String request = buildPrompt(job);
    try {
      Process process =
          new ProcessBuilder(command(output, schema, mcp, job))
              .redirectErrorStream(true)
              .redirectOutput(log.toFile())
              .start();
      process.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      try (CodexTelemetryReporter.Session session = telemetry.monitor(job, process, log)) {
        if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
          process.destroyForcibly();
          throw new IllegalStateException("Timeout do Codex do Agente Gerador de Landing");
        }
        if (process.exitValue() != 0)
          throw new IllegalStateException(
              "Codex encerrou com código " + process.exitValue() + ": " + Files.readString(log));
        String raw = Files.readString(output);
        JsonNode decision = objectMapper.readTree(raw);
        validate(decision);
        session.success();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("decisionJson", raw);
        result.put("requestJson", request);
        result.put("responseJson", raw);
        result.put("model", properties.getModel());
        result.put("inputTokens", null);
        result.put("outputTokens", null);
        result.put("costUsd", null);
        return result;
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(log);
      Files.deleteIfExists(schema);
      Files.deleteIfExists(mcp);
    }
  }

  /** Monta o comando com modelo, pesquisa, MCP exclusivo e autoridade não interativa. */
  List<String> command(Path output, Path schema, Path mcp, LandingAgentJob job) {
    List<String> command =
        new ArrayList<>(
            List.of(
                properties.getCodexCommand(),
                "--search",
                "exec",
                "-",
                "--skip-git-repo-check",
                "--sandbox",
                "read-only",
                "--cd",
                properties.getRepositoryPath(),
                "--output-schema",
                schema.toString(),
                "--output-last-message",
                output.toString(),
                "--color",
                "never",
                "--config",
                "approval_policy=\"never\"",
                "--config",
                "mcp_servers.landing_generator.command=\"node\"",
                "--config",
                "mcp_servers.landing_generator.args=[\"" + mcp.toAbsolutePath() + "\"]",
                "--config",
                "mcp_servers.landing_generator.env={MCP_MARKETING_HUB_URL=\""
                    + properties.getMarketingHubUrl()
                    + "\",MCP_EXECUTION_ID=\""
                    + job.executionId()
                    + "\",MCP_EXPERIMENT_ID=\""
                    + job.experimentId()
                    + "\",PLAYWRIGHT_BROWSERS_PATH=\"/ms-playwright\"}"));
    if (properties.getReasoningEffort() != null && !properties.getReasoningEffort().isBlank())
      command.addAll(
          List.of(
              "--config", "model_reasoning_effort=\"" + properties.getReasoningEffort() + "\""));
    command.addAll(List.of("--model", properties.getModel()));
    return command;
  }

  /** Resolve o prompt versionado com o snapshot congelado. */
  private String buildPrompt(LandingAgentJob job) throws IOException {
    return read("prompts/landing-generator/v1/remediation.md")
        .replace("{{EXECUTION_ID}}", job.executionId())
        .replace("{{EXPERIMENT_ID}}", job.experimentId().toString())
        .replace("{{CONTEXT}}", objectMapper.writeValueAsString(job.context()));
  }

  /** Bloqueia planos vagos, autoaprovação e autonomia sem estratégia ou controle. */
  private void validate(JsonNode value) {
    if (!"REGENERATE_BEFORE_PUBLICATION".equals(value.path("approvalRecommendation").asText()))
      throw new IllegalArgumentException("Agente executor não pode aprovar a própria landing");
    if (value.path("recommendedRegeneration").isEmpty()
        || value.path("acceptanceCriteria").isEmpty())
      throw new IllegalArgumentException("Correção sem etapa e critérios verificáveis");
    if (value.path("score").asInt(-1) < 0)
      throw new IllegalArgumentException("Plano sem score de referência");
    if (value.path("strategyOptions").size() < 3
        || value.path("selectedStrategy").isEmpty()
        || value.path("autonomousBacklog").isEmpty())
      throw new IllegalArgumentException("Plano sem decisão autônoma comparada e executável");
    if (value.path("expectedMetrics").isEmpty()
        || value.path("stopConditions").path("continueWhen").isEmpty()
        || value.path("stopConditions").path("adjustWhen").isEmpty()
        || value.path("stopConditions").path("stopWhen").isEmpty())
      throw new IllegalArgumentException("Plano autônomo sem métricas e condições de controle");
  }

  /** Materializa um recurso versionado somente no diretório temporário. */
  private Path materialize(String resource, String prefix, String suffix) throws IOException {
    Path path = Files.createTempFile(prefix, suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê um recurso versionado integralmente. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
