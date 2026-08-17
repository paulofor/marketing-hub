package com.marketinghub.metaadapproverworker;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar replay visual em Codex isolado sem provider, gasto ou publicação. */
@Component
@ConditionalOnProperty(
    name = "meta-ad-approver.execution-role",
    havingValue = "review",
    matchIfMissing = true)
public class TemisVisualLearningRunner {
  private static final String PROMPT = "prompts/visual-learning/v1/consolidate.md";
  private static final String SCHEMA = "prompts/visual-learning/v1/consolidate-schema.json";
  private final MetaAdApproverProperties properties;
  private final ObjectMapper objectMapper;

  /** Inicializa o consolidador com configuração e serialização auditável. */
  public TemisVisualLearningRunner(MetaAdApproverProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /** Executa uma sessão nova e devolve o contrato completo ao backend. */
  public Map<String, Object> run(TemisVisualLearningJob job)
      throws IOException, InterruptedException {
    Path output = Files.createTempFile("temis-visual-learning-", ".json");
    Path processOutput = Files.createTempFile("temis-visual-learning-process-", ".log");
    Path schema = materialize(SCHEMA, "temis-visual-learning-schema-", ".json");
    String request = prompt(job);
    try {
      Process process =
          new ProcessBuilder(command(output, schema))
              .redirectErrorStream(true)
              .redirectOutput(processOutput.toFile())
              .start();
      process.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        process.waitFor(10, TimeUnit.SECONDS);
        throw new IllegalStateException("Timeout da consolidação visual de Têmis");
      }
      String processLog = Files.readString(processOutput, StandardCharsets.UTF_8);
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
            "Codex consolidador encerrou com código " + process.exitValue() + ": " + processLog);
      }
      String raw = Files.readString(output, StandardCharsets.UTF_8);
      JsonNode result = objectMapper.readTree(raw);
      validate(result, job);
      Map<String, Object> callback =
          objectMapper.convertValue(
              result, new com.fasterxml.jackson.core.type.TypeReference<>() {});
      callback = new LinkedHashMap<>(callback);
      callback.put("producerExecutionId", job.producerExecutionId());
      callback.put("requestJson", request);
      callback.put("responseJson", raw);
      return callback;
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processOutput);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o comando sem busca, MCP ou permissão de escrita. */
  private List<String> command(Path output, Path schema) {
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
            "approval_policy=\"never\""));
    if (hasText(properties.getReasoningEffort())) {
      command.addAll(
          List.of(
              "--config", "model_reasoning_effort=\"" + properties.getReasoningEffort() + "\""));
    }
    if (hasText(properties.getModel())) command.addAll(List.of("--model", properties.getModel()));
    return command;
  }

  /** Resolve o prompt versionado com a amostra já congelada pelo backend. */
  private String prompt(TemisVisualLearningJob job) throws IOException {
    return read(PROMPT)
        .replace("{{CONTEXT_KEY}}", job.contextKey())
        .replace("{{BASELINE_VERSION}}", job.baselineVersion())
        .replace("{{CANDIDATE_VERSION}}", job.candidateVersion())
        .replace("{{INPUT}}", objectMapper.writeValueAsString(job.input()));
  }

  /** Bloqueia efeitos externos e divergência dos quinze IDs congelados. */
  private void validate(JsonNode result, TemisVisualLearningJob job) {
    if (result.path("externalProviderCalled").asBoolean(true)
        || result.path("spendingAuthorized").asBoolean(true)
        || result.path("publicationPerformed").asBoolean(true)) {
      throw new IllegalArgumentException("Replay visual declarou efeito externo proibido");
    }
    JsonNode assessments = result.path("caseAssessments");
    JsonNode cases = objectMapper.valueToTree(job.input()).path("cases");
    if (!assessments.isArray()
        || assessments.size() != 15
        || !cases.isArray()
        || cases.size() != 15) {
      throw new IllegalArgumentException("Replay visual deve preservar quinze casos");
    }
    java.util.Set<Long> expected = new java.util.LinkedHashSet<>();
    cases.forEach(value -> expected.add(value.path("caseId").asLong(-1)));
    java.util.Set<Long> actual = new java.util.LinkedHashSet<>();
    assessments.forEach(value -> actual.add(value.path("caseId").asLong(-1)));
    if (!actual.equals(expected))
      throw new IllegalArgumentException("IDs do replay visual divergentes");
  }

  /** Materializa um recurso versionado em arquivo temporário. */
  private Path materialize(String resource, String prefix, String suffix) throws IOException {
    Path path = Files.createTempFile(prefix, suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso versionado. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Indica se uma configuração opcional está preenchida. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
