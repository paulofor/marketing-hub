package com.marketinghub.growthoperatorworker;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar o Codex com sandbox somente leitura e saida JSON validada. */
@Component
public class CodexReadOnlyRunner {
  private final WorkerProperties properties;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;

  /** Configura o executor com telemetria auditável. */
  @Autowired
  public CodexReadOnlyRunner(
      WorkerProperties properties, ObjectMapper objectMapper, CodexTelemetryReporter telemetry) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
  }

  /** Mantém construção direta dos testes de comando. */
  public CodexReadOnlyRunner(WorkerProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, null);
  }

  /** Executa diagnóstico efêmero com memória correlacionada e repositório somente leitura. */
  public Map<String, Object> run(GrowthOperatorJob job) throws IOException, InterruptedException {
    Path output = Files.createTempFile("growth-operator-", ".json");
    Path processOutput = Files.createTempFile("growth-operator-process-", ".log");
    Path mcpServer = materializeMcpServer();
    try {
      List<String> command = buildCommand(output, mcpServer);
      ProcessBuilder processBuilder =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(processOutput.toFile());
      processBuilder
          .environment()
          .put("MCP_COMMERCIAL_PLAN_ID", String.valueOf(job.commercialPlanId()));
      processBuilder
          .environment()
          .put("MCP_SOURCE_EXECUTION_ID", "growth-operator-execution-" + job.id());
      processBuilder.environment().put("MCP_MARKETING_HUB_URL", properties.getMarketingHubUrl());
      Process process = processBuilder.start();
      process.getOutputStream().write(buildPrompt(job).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      CodexTelemetryReporter.Session session =
          telemetry == null ? null : telemetry.monitor(job.id(), process, processOutput);
      try {
        if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
          process.destroyForcibly();
          process.waitFor(10, TimeUnit.SECONDS);
          throw new IllegalStateException(
              "Timeout do Codex do Operador após "
                  + properties.getCodexTimeout().toMinutes()
                  + " minutos.");
        }
        String processLog = Files.readString(processOutput, StandardCharsets.UTF_8);
        int exitCode = process.exitValue();
        if (exitCode != 0) {
          throw new IllegalStateException(
              "Codex encerrou com codigo " + exitCode + ": " + processLog);
        }
        String rawResponse = Files.readString(output);
        JsonNode result = objectMapper.readTree(rawResponse);
        validateResult(result);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "alternativesJson", objectMapper.writeValueAsString(result.get("alternatives")));
        payload.put("diagnosisJson", objectMapper.writeValueAsString(result.get("diagnosis")));
        payload.put("rawModelResponse", rawResponse);
        payload.put("toolUsageJson", extractToolUsage(processLog));
        payload.put("recommendedDecision", result.get("decision").asText());
        payload.put("recommendedAction", result.get("recommendedAction").asText());
        payload.put("dailyReport", result.get("dailyReport").asText());
        payload.put(
            "model", hasText(properties.getModel()) ? properties.getModel() : "codex-default");
        payload.put("inputTokens", null);
        payload.put("outputTokens", null);
        payload.put("estimatedCost", null);
        if (session != null) session.success();
        return payload;
      } finally {
        if (session != null) session.close();
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processOutput);
      Files.deleteIfExists(mcpServer);
    }
  }

  /** Extrai das linhas de auditoria MCP quais ferramentas fundamentaram a execucao. */
  private String extractToolUsage(String processLog) throws IOException {
    List<JsonNode> calls = new ArrayList<>();
    for (String line : processLog.lines().toList()) {
      if (!line.startsWith("{") || !line.contains("\"tool\"")) {
        continue;
      }
      try {
        JsonNode candidate = objectMapper.readTree(line);
        if (candidate.hasNonNull("tool") && candidate.hasNonNull("status")) {
          calls.add(candidate);
        }
      } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
        // Linhas normais do Codex nao fazem parte da auditoria estruturada do MCP.
      }
    }
    return objectMapper.writeValueAsString(calls);
  }

  /** Monta o comando com sandbox read-only, sessao efemera e schema versionado. */
  List<String> buildCommand(Path output) throws IOException {
    return buildCommand(output, materializeMcpServer());
  }

  /** Monta o comando apontando para um servidor MCP local explicitamente informado. */
  List<String> buildCommand(Path output, Path mcpServer) throws IOException {
    List<String> command = new ArrayList<>();
    command.add(properties.getCodexCommand());
    command.add("--search");
    command.add("exec");
    command.add("-");
    command.add("--skip-git-repo-check");
    command.add("--sandbox");
    command.add("read-only");
    command.add("--cd");
    command.add(properties.getRepositoryPath());
    command.add("--output-schema");
    command.add(materializeSchema().toString());
    command.add("--output-last-message");
    command.add(output.toString());
    command.add("--color");
    command.add("never");
    command.add("--config");
    command.add("mcp_servers.marketing_hub_readonly.command=\"node\"");
    command.add("--config");
    command.add("mcp_servers.marketing_hub_readonly.args=[\"" + mcpServer.toAbsolutePath() + "\"]");
    if (hasText(properties.getReasoningEffort())) {
      command.add("--config");
      command.add("model_reasoning_effort=\"" + properties.getReasoningEffort() + "\"");
    }
    if (hasText(properties.getModel())) {
      command.add("--model");
      command.add(properties.getModel());
    }
    return command;
  }

  /** Materializa o catalogo MCP somente leitura em diretorio temporario gravavel. */
  private Path materializeMcpServer() throws IOException {
    Path server = Files.createTempFile("marketing-hub-readonly-mcp-", ".mjs");
    Files.writeString(server, readResource("mcp/marketing-hub-readonly.mjs"));
    server.toFile().deleteOnExit();
    return server;
  }

  /** Resolve o prompt versionado com o contexto congelado pelo backend. */
  private String buildPrompt(GrowthOperatorJob job) throws IOException {
    String template = readResource("prompts/growth-operator/v1/diagnosis.md");
    return template
        .replace("{{OBJECTIVE}}", text(job.objective()))
        .replace("{{BLOCKER}}", text(job.blocker()))
        .replace("{{EVIDENCE_SNAPSHOT}}", text(job.evidenceSnapshot()))
        .replace("{{PLAN_ID}}", String.valueOf(job.commercialPlanId()))
        .replace("{{MARKETING_HUB_URL}}", text(properties.getMarketingHubUrl()));
  }

  /** Materializa o schema do classpath fora do repositorio para uso pelo CLI. */
  private Path materializeSchema() throws IOException {
    Path schema = Files.createTempFile("growth-operator-schema-", ".json");
    Files.writeString(schema, readResource("prompts/growth-operator/v1/diagnosis-schema.json"));
    schema.toFile().deleteOnExit();
    return schema;
  }

  /** Le um recurso versionado integralmente. */
  private String readResource(String path) throws IOException {
    try (var input = new ClassPathResource(path).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Rejeita qualquer resposta que nao preserve o contrato minimo de decisao. */
  private void validateResult(JsonNode result) {
    if (!result.has("alternatives")
        || result.get("alternatives").size() != 3
        || !result.hasNonNull("diagnosis")
        || !result.get("diagnosis").hasNonNull("decisionAudit")
        || !result.hasNonNull("decision")
        || !result.hasNonNull("recommendedAction")
        || !result.hasNonNull("dailyReport")) {
      throw new IllegalArgumentException("Resposta Codex fora do contrato de diagnostico v1.");
    }
  }

  /** Indica se um texto possui conteudo. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Normaliza valores ausentes no prompt. */
  private String text(String value) {
    return hasText(value) ? value : "nao informado";
  }
}
