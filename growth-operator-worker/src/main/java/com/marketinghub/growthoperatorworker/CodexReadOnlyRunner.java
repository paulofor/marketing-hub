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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar o Codex com sandbox somente leitura e saida JSON validada. */
@Component
public class CodexReadOnlyRunner {
  private final WorkerProperties properties;
  private final ObjectMapper objectMapper;

  public CodexReadOnlyRunner(WorkerProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /** Executa um diagnostico efemero sem permitir escrita no repositorio. */
  public Map<String, Object> run(GrowthOperatorJob job) throws IOException, InterruptedException {
    Path output = Files.createTempFile("growth-operator-", ".json");
    try {
      List<String> command = buildCommand(output);
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      process.getOutputStream().write(buildPrompt(job).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      String processLog =
          new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new IllegalStateException(
            "Codex encerrou com codigo " + exitCode + ": " + processLog);
      }
      String rawResponse = Files.readString(output);
      JsonNode result = objectMapper.readTree(rawResponse);
      validateResult(result);
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("alternativesJson", objectMapper.writeValueAsString(result.get("alternatives")));
      payload.put("diagnosisJson", objectMapper.writeValueAsString(result.get("diagnosis")));
      payload.put("rawModelResponse", rawResponse);
      payload.put("recommendedDecision", result.get("decision").asText());
      payload.put("recommendedAction", result.get("recommendedAction").asText());
      payload.put("dailyReport", result.get("dailyReport").asText());
      payload.put(
          "model", hasText(properties.getModel()) ? properties.getModel() : "codex-default");
      payload.put("inputTokens", null);
      payload.put("outputTokens", null);
      payload.put("estimatedCost", null);
      return payload;
    } finally {
      Files.deleteIfExists(output);
    }
  }

  /** Monta o comando com sandbox read-only, sessao efemera e schema versionado. */
  List<String> buildCommand(Path output) throws IOException {
    List<String> command = new ArrayList<>();
    command.add(properties.getCodexCommand());
    command.add("--search");
    command.add("exec");
    command.add("-");
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
    if (hasText(properties.getModel())) {
      command.add("--model");
      command.add(properties.getModel());
    }
    return command;
  }

  /** Resolve o prompt versionado com o contexto congelado pelo backend. */
  private String buildPrompt(GrowthOperatorJob job) throws IOException {
    String template = readResource("prompts/growth-operator/v1/diagnosis.md");
    return template
        .replace("{{OBJECTIVE}}", text(job.objective()))
        .replace("{{BLOCKER}}", text(job.blocker()))
        .replace("{{EVIDENCE_SNAPSHOT}}", text(job.evidenceSnapshot()))
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
