package com.marketinghub.financialagentworker;

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

/** Responsabilidade: executar o Codex financeiro em sandbox somente leitura e validar sua saida. */
@Component
public class FinancialCodexRunner {
  private final FinancialAgentProperties properties;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;

  /** Configura o executor com telemetria auditável. */
  @Autowired
  public FinancialCodexRunner(
      FinancialAgentProperties properties,
      ObjectMapper objectMapper,
      CodexTelemetryReporter telemetry) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
  }

  /** Mantém a construção direta usada por testes de contrato do comando. */
  public FinancialCodexRunner(FinancialAgentProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, null);
  }

  /** Executa uma conciliacao efemera sem permitir mutacoes financeiras ou no repositorio. */
  public Map<String, Object> run(FinancialAgentJob job) throws IOException, InterruptedException {
    Path output = Files.createTempFile("financial-agent-", ".json");
    Path processOutput = Files.createTempFile("financial-agent-process-", ".log");
    Path schema = materialize("prompts/financial-agent/v1/report-schema.json", ".json");
    try {
      Process process =
          new ProcessBuilder(buildCommand(output, schema))
              .redirectErrorStream(true)
              .redirectOutput(processOutput.toFile())
              .start();
      process.getOutputStream().write(buildPrompt(job).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      CodexTelemetryReporter.Session session =
          telemetry == null ? null : telemetry.monitor(job.id(), process, processOutput);
      try {
        if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
          process.destroyForcibly();
          process.waitFor(10, TimeUnit.SECONDS);
          throw new IllegalStateException(
              "Timeout do Codex financeiro após "
                  + properties.getCodexTimeout().toMinutes()
                  + " minutos.");
        }
        String processLog = Files.readString(processOutput, StandardCharsets.UTF_8);
        int exitCode = process.exitValue();
        if (exitCode != 0)
          throw new IllegalStateException("Codex financeiro falhou: " + processLog);
        String raw = Files.readString(output);
        JsonNode result = objectMapper.readTree(raw);
        validate(result);
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("reconciliationJson", objectMapper.writeValueAsString(result));
        payload.put("dailyReport", result.get("dailyReport").asText());
        payload.put("rawModelResponse", raw);
        payload.put("model", properties.getModel());
        payload.put("estimatedCost", null);
        if (session != null) session.success();
        return payload;
      } finally {
        if (session != null) session.close();
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processOutput);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o comando Codex preservando sandbox e repositorio somente leitura. */
  List<String> buildCommand(Path output, Path schema) {
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
                "never"));
    if (properties.getModel() != null && !properties.getModel().isBlank()) {
      command.add("--model");
      command.add(properties.getModel());
    }
    return command;
  }

  /** Resolve o prompt versionado com o snapshot congelado pelo backend. */
  private String buildPrompt(FinancialAgentJob job) throws IOException {
    return read("prompts/financial-agent/v1/report.md")
        .replace("{{PLAN_ID}}", String.valueOf(job.commercialPlanId()))
        .replace("{{FINANCIAL_SNAPSHOT}}", job.financialSnapshot());
  }

  /** Materializa recurso versionado em diretorio temporario gravavel. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("financial-agent-resource-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Le um recurso integralmente do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Rejeita relatorio que omita reconciliacao, cobertura ou bloqueios. */
  private void validate(JsonNode result) {
    if (!result.has("totals")
        || !result.has("sourceCoverage")
        || !result.has("divergences")
        || !result.hasNonNull("dailyReport")
        || !result.hasNonNull("decision")) {
      throw new IllegalArgumentException("Resposta fora do contrato financeiro v1.");
    }
  }
}
