package com.marketinghub.customeragentworker;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar e validar a resposta estruturada do Codex para uma avaliação. */
@Component
public class CustomerEvaluationCodexRunner {
  private final String executable;
  private final String model;
  private final long timeoutMinutes;
  private final String repositoryPath;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;

  /** Configura o executor somente leitura e sua telemetria auditável. */
  public CustomerEvaluationCodexRunner(
      @Value("${CUSTOMER_AGENT_CODEX_EXECUTABLE:codex}") String executable,
      @Value("${CUSTOMER_AGENT_MODEL:gpt-5.6-sol}") String model,
      @Value("${CUSTOMER_AGENT_EVALUATION_TIMEOUT_MINUTES:40}") long timeoutMinutes,
      @Value("${CUSTOMER_AGENT_REPOSITORY_PATH:/workspace}") String repositoryPath,
      ObjectMapper objectMapper,
      CodexTelemetryReporter telemetry) {
    this.executable = executable;
    this.model = model;
    this.timeoutMinutes = timeoutMinutes;
    this.repositoryPath = repositoryPath;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
  }

  /** Executa a avaliação por stdin e retorna somente uma resposta compatível com o schema. */
  Map<String, String> run(long evaluationId, Map<?, ?> job)
      throws IOException, InterruptedException {
    Path answer = Files.createTempFile("customer-agent-evaluation-answer-", ".json");
    Path processLog = Files.createTempFile("customer-agent-evaluation-process-", ".log");
    Path schema = materialize("prompts/customer-agent/v1/evaluation-schema.json", ".json");
    try {
      Process process =
          new ProcessBuilder(buildCommand(answer, schema))
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      process.getOutputStream().write(buildPrompt(job).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      try (CodexTelemetryReporter.Session session =
          telemetry.monitor(evaluationId, process, processLog)) {
        if (!process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
          process.destroyForcibly();
          process.waitFor(10, TimeUnit.SECONDS);
          throw new IllegalStateException("Timeout do Codex após " + timeoutMinutes + " minutos.");
        }
        String diagnostic = Files.readString(processLog, StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
          throw new IllegalStateException("Codex falhou: " + diagnostic);
        }
        String raw = Files.readString(answer, StandardCharsets.UTF_8);
        JsonNode result = objectMapper.readTree(raw);
        validate(result);
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        payload.put("assessment", result.get("assessment").asText());
        payload.put("hypothesisJson", objectMapper.writeValueAsString(result.get("hypotheses")));
        payload.put("rawModelResponse", raw);
        payload.put("model", model);
        session.success();
        return payload;
      }
    } finally {
      Files.deleteIfExists(answer);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o comando com arquivo final, schema versionado e sandbox somente leitura. */
  List<String> buildCommand(Path answer, Path schema) {
    List<String> command =
        new ArrayList<>(
            List.of(
                executable,
                "--search",
                "exec",
                "-",
                "--skip-git-repo-check",
                "--sandbox",
                "read-only",
                "--cd",
                repositoryPath,
                "--output-schema",
                schema.toString(),
                "--output-last-message",
                answer.toString(),
                "--color",
                "never"));
    if (model != null && !model.isBlank()) {
      command.add("--model");
      command.add(model);
    }
    return command;
  }

  /** Resolve o prompt versionado usando o contexto congelado e pesquisa pública auditável. */
  private String buildPrompt(Map<?, ?> job) throws IOException {
    return read("prompts/customer-agent/v1/evaluation.md")
        .replace("{{PERSONA_JSON}}", String.valueOf(job.get("persona")))
        .replace("{{ASSET_REFERENCE}}", String.valueOf(job.get("assetReference")));
  }

  /** Materializa um recurso do classpath para consumo seguro pelo processo Codex. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("customer-agent-resource-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso versionado do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Rejeita qualquer resposta sem decisão, parecer e hipóteses estruturadas. */
  private void validate(JsonNode result) {
    if (!result.hasNonNull("decision")
        || !result.hasNonNull("assessment")
        || !result.has("hypotheses")
        || !result.get("hypotheses").isArray()
        || !result.has("sources")
        || !result.get("sources").isArray()) {
      throw new IllegalArgumentException("Resposta fora do contrato do Agente Cliente v1.");
    }
  }
}
