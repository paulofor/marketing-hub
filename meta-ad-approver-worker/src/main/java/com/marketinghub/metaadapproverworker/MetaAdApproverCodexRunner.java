package com.marketinghub.metaadapproverworker;

import com.fasterxml.jackson.core.JsonProcessingException;
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

/** Responsabilidade: executar uma revisão no Codex isolado, com MCP e saída validada. */
@Component
public class MetaAdApproverCodexRunner {
  private final MetaAdApproverProperties properties;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;

  /** Configura o executor com telemetria auditável. */
  @Autowired
  public MetaAdApproverCodexRunner(
      MetaAdApproverProperties properties,
      ObjectMapper objectMapper,
      CodexTelemetryReporter telemetry) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
  }

  /** Mantém construção direta nos testes de contrato. */
  public MetaAdApproverCodexRunner(MetaAdApproverProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, null);
  }

  /** Executa o Codex em sandbox somente leitura e converte o parecer para o callback vigente. */
  public Map<String, Object> run(MetaAdReviewJob job) throws IOException, InterruptedException {
    Path output = Files.createTempFile("meta-ad-approver-", ".json");
    Path processOutput = Files.createTempFile("meta-ad-approver-process-", ".log");
    Path mcpServer = materializeMcp();
    Path schema =
        materialize("prompts/meta-ad-approver/v1/review-schema.json", "meta-ad-schema-", ".json");
    try {
      ProcessBuilder builder =
          new ProcessBuilder(buildCommand(output, schema, mcpServer))
              .redirectErrorStream(true)
              .redirectOutput(processOutput.toFile());
      builder.environment().put("MCP_MARKETING_HUB_URL", properties.getMarketingHubUrl());
      builder.environment().put("MCP_CREATIVE_ID", job.creativeId().toString());
      builder.environment().put("MCP_EXPERIMENT_ID", job.experimentId().toString());
      Process process = builder.start();
      String request = buildPrompt(job);
      process.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      CodexTelemetryReporter.Session session =
          telemetry == null ? null : telemetry.monitor(job.creativeId(), process, processOutput);
      try {
        if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
          process.destroyForcibly();
          process.waitFor(10, TimeUnit.SECONDS);
          throw new IllegalStateException("Timeout do Codex do Aprovador Meta");
        }
        String processLog = Files.readString(processOutput, StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
          throw new IllegalStateException(
              "Codex encerrou com código " + process.exitValue() + ": " + processLog);
        }
        String rawResponse = Files.readString(output, StandardCharsets.UTF_8);
        JsonNode result = objectMapper.readTree(rawResponse);
        validate(result);
        Map<String, Object> payload = callback(result);
        payload.put("model", properties.getModel());
        payload.put("requestJson", request);
        payload.put("responseJson", rawResponse);
        payload.put("inputTokens", null);
        payload.put("outputTokens", null);
        payload.put("costUsd", null);
        if (session != null) session.success();
        return payload;
      } finally {
        if (session != null) session.close();
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processOutput);
      deleteMcpRuntime(mcpServer);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o comando impondo sandbox read-only e MCP próprio do agente. */
  List<String> buildCommand(Path output, Path schema, Path mcpServer) {
    List<String> command = new ArrayList<>();
    command.addAll(
        List.of(
            properties.getCodexCommand(),
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
            "mcp_servers.meta_ad_approver.command=\"node\"",
            "--config",
            "mcp_servers.meta_ad_approver.args=[\"" + mcpServer.toAbsolutePath() + "\"]"));
    if (hasText(properties.getReasoningEffort())) {
      command.addAll(
          List.of(
              "--config", "model_reasoning_effort=\"" + properties.getReasoningEffort() + "\""));
    }
    if (hasText(properties.getModel())) command.addAll(List.of("--model", properties.getModel()));
    return command;
  }

  /** Resolve o prompt versionado com o snapshot congelado pelo backend. */
  private String buildPrompt(MetaAdReviewJob job) throws IOException {
    return read("prompts/meta-ad-approver/v1/review.md")
        .replace("{{CREATIVE_ID}}", job.creativeId().toString())
        .replace("{{EXPERIMENT_ID}}", job.experimentId().toString())
        .replace("{{CONTEXT}}", objectMapper.writeValueAsString(job.context()));
  }

  /** Converte o JSON do Codex para o contrato persistido sem abrir gates localmente. */
  private Map<String, Object> callback(JsonNode result) throws JsonProcessingException {
    Map<String, Object> payload = new LinkedHashMap<>();
    result
        .fields()
        .forEachRemaining(
            entry -> {
              if (!List.of("issues", "recommendations").contains(entry.getKey())) {
                payload.put(
                    entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class));
              }
            });
    payload.put("issuesJson", result.path("issues").toString());
    payload.put("recommendationsJson", result.path("recommendations").toString());
    return payload;
  }

  /** Bloqueia aprovação que não satisfaça os critérios objetivos mínimos. */
  private void validate(JsonNode value) {
    String decision = value.path("decision").asText();
    if (decision.isBlank() || value.path("summary").asText().isBlank()) {
      throw new IllegalArgumentException("Parecer Codex incompleto");
    }
    if ("APPROVED".equals(decision)) {
      for (String score :
          List.of(
              "attentionScore", "clarityScore", "desireScore", "credibilityScore", "actionScore")) {
        if (value.path(score).asInt(-1) < 80)
          throw new IllegalArgumentException("Aprovação com nota inferior a 80");
      }
    } else if (value.path("revisedImagePrompt").asText().isBlank()
        || value.path("mandatoryVisualRequirements").isEmpty()
        || value.path("visualAcceptanceCriteria").isEmpty()) {
      throw new IllegalArgumentException("Correção sem contrato visual completo");
    }
  }

  /** Materializa recurso somente em diretório temporário gravável. */
  private Path materialize(String resource, String prefix, String suffix) throws IOException {
    Path path = Files.createTempFile(prefix, suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Materializa o MCP junto a um vínculo somente leitura para as dependências do navegador. */
  Path materializeMcp() throws IOException {
    Path directory = Files.createTempDirectory("meta-ad-approver-mcp-");
    Path server = directory.resolve("meta-ad-approver.mjs");
    Files.writeString(server, read("mcp/meta-ad-approver.mjs"));
    Files.createSymbolicLink(directory.resolve("node_modules"), Path.of("/app/node_modules"));
    return server;
  }

  /** Remove apenas o runtime temporário criado para uma execução do MCP. */
  private void deleteMcpRuntime(Path server) throws IOException {
    Path directory = server.getParent();
    Files.deleteIfExists(server);
    Files.deleteIfExists(directory.resolve("node_modules"));
    Files.deleteIfExists(directory);
  }

  /** Lê um recurso versionado integralmente. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Indica se uma configuração possui conteúdo. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
