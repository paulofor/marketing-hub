package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: materializar o HTML integral após a decisão estratégica de Dédalo. */
@Component
public class LandingHtmlCodexGenerator {
  private final LandingGeneratorAgentProperties properties;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;

  /** Inicializa a geração final com configuração, JSON e telemetria auditável. */
  public LandingHtmlCodexGenerator(
      LandingGeneratorAgentProperties properties,
      ObjectMapper objectMapper,
      CodexTelemetryReporter telemetry) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
  }

  /** Executa uma interação dedicada e limitada ao documento HTML completo. */
  public GeneratedHtml generate(LandingAgentJob job, JsonNode decision)
      throws IOException, InterruptedException {
    Path output = Files.createTempFile("landing-html-", ".json");
    Path log = Files.createTempFile("landing-html-process-", ".log");
    Path schema = materialize("prompts/landing-generator/v1/html-schema.json", "html-schema-");
    String request =
        read("prompts/landing-generator/v1/html.md")
            .replace("{{CONTEXT}}", objectMapper.writeValueAsString(job.context()))
            .replace("{{DECISION}}", objectMapper.writeValueAsString(decision));
    try {
      Process process =
          new ProcessBuilder(command(output, schema))
              .redirectErrorStream(true)
              .redirectOutput(log.toFile())
              .start();
      process.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      try (CodexTelemetryReporter.Session session = telemetry.monitor(job, process, log)) {
        if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
          process.destroyForcibly();
          throw new CodexActivityTimeoutException("Timeout ao materializar o HTML integral");
        }
        if (process.exitValue() != 0)
          throw new IllegalStateException(
              "Codex encerrou a materialização com código "
                  + process.exitValue()
                  + ": "
                  + Files.readString(log));
        JsonNode response = objectMapper.readTree(Files.readString(output));
        String html = response.path("generatedHtml").asText();
        if (html.length() < 500)
          throw new IllegalArgumentException("Materialização não devolveu HTML completo");
        CodexTelemetryReporter.TokenUsage usage = session.tokenUsage();
        session.success();
        return new GeneratedHtml(html, usage);
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(log);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o comando estrito da interação de materialização. */
  List<String> command(Path output, Path schema) {
    List<String> command =
        new ArrayList<>(
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
                "--json",
                "--config",
                "approval_policy=\"never\""));
    command.addAll(
        List.of(
            "--config", "model_reasoning_effort=\"" + properties.requiredReasoningEffort() + "\""));
    command.addAll(List.of("--model", properties.getModel()));
    return command;
  }

  /** Materializa o schema versionado em arquivo temporário. */
  private Path materialize(String resource, String prefix) throws IOException {
    Path path = Files.createTempFile(prefix, ".json");
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso versionado. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Transporta o artefato completo e o consumo real da interação dedicada. */
  public record GeneratedHtml(String html, CodexTelemetryReporter.TokenUsage usage) {}
}
