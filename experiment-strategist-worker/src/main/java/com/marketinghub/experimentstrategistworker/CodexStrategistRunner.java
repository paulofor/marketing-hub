package com.marketinghub.experimentstrategistworker;

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

/** Responsabilidade: executar pesquisa Codex somente leitura com saida estruturada. */
@Component
public class CodexStrategistRunner {
  private final WorkerProperties properties;
  private final ObjectMapper json;
  private final CodexTelemetryReporter telemetry;

  /** Configura o executor e o parser JSON. */
  @Autowired
  public CodexStrategistRunner(
      WorkerProperties properties, ObjectMapper json, CodexTelemetryReporter telemetry) {
    this.properties = properties;
    this.json = json;
    this.telemetry = telemetry;
  }

  /** Mantém construção direta dos testes de comando. */
  public CodexStrategistRunner(WorkerProperties properties, ObjectMapper json) {
    this(properties, json, null);
  }

  /** Executa a pesquisa efemera e devolve o parecer auditavel. */
  public Map<String, Object> run(StrategistJob job) throws IOException, InterruptedException {
    Path output = Files.createTempFile("experiment-strategist-", ".json");
    Path log = Files.createTempFile("experiment-strategist-", ".log");
    Path schema = materialize("prompts/experiment-strategist/v1/research-schema.json", ".json");
    try {
      Process process =
          new ProcessBuilder(command(output, schema))
              .redirectErrorStream(true)
              .redirectOutput(log.toFile())
              .start();
      process.getOutputStream().write(prompt(job).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      CodexTelemetryReporter.Session session =
          telemetry == null ? null : telemetry.monitor(job.id(), process, log);
      try {
        if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
          process.destroyForcibly();
          throw new IllegalStateException(
              "Timeout do Codex do Estrategista apos "
                  + properties.getCodexTimeout().toMinutes()
                  + " minutos.");
        }
        if (process.exitValue() != 0)
          throw new IllegalStateException(
              "Codex encerrou com codigo " + process.exitValue() + ": " + Files.readString(log));
        String raw = Files.readString(output);
        JsonNode result = json.readTree(raw);
        validate(result);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alternativesJson", json.writeValueAsString(result.get("alternatives")));
        payload.put("recommendationJson", json.writeValueAsString(result.get("recommendation")));
        payload.put("publicSourcesJson", json.writeValueAsString(result.get("sources")));
        payload.put("rawModelResponse", raw);
        payload.put(
            "modelName", hasText(properties.getModel()) ? properties.getModel() : "codex-default");
        payload.put("estimatedCost", null);
        if (session != null) session.success();
        return payload;
      } finally {
        if (session != null) session.close();
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(log);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o comando com busca publica, sandbox somente leitura e schema versionado. */
  List<String> command(Path output, Path schema) {
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
    command.add(schema.toString());
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

  /** Resolve o prompt com evidencias e biblioteca comportamental versionadas. */
  private String prompt(StrategistJob job) throws IOException {
    return read("prompts/experiment-strategist/v1/research.md")
        .replace("{{EVIDENCE_SNAPSHOT}}", text(job.evidenceSnapshot()))
        .replace("{{BEHAVIORAL_MEMORY}}", "Incluida no snapshot de evidencias.")
        .replace("{{BEHAVIORAL_SCIENCE_LIBRARY}}", read("behavioral-science/v1/library.md"))
        .replace("{{RESEARCH_QUESTION}}", text(job.researchQuestion()));
  }

  /** Rejeita parecer sem exatamente tres caminhos ou sem fontes e recomendacao. */
  private void validate(JsonNode result) {
    if (!result.has("alternatives")
        || result.get("alternatives").size() != 3
        || !result.has("sources")
        || result.get("sources").isEmpty()
        || !result.hasNonNull("recommendation")
        || !result.hasNonNull("diagnosis"))
      throw new IllegalArgumentException("Resposta Codex fora do contrato estrategico v1.");
  }

  /** Materializa um recurso do classpath em arquivo temporario. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("strategist-resource-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Le integralmente um recurso versionado. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Verifica se ha texto configurado. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Normaliza texto ausente no prompt. */
  private String text(String value) {
    return hasText(value) ? value : "nao informado";
  }
}
