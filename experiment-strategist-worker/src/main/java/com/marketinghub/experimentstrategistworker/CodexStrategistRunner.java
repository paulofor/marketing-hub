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
  private static final long ACTIVITY_POLL_SECONDS = 15L;
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
    boolean clarityAvailable = isClarityApiTokenAvailable();
    boolean assumptions = "COMMERCIAL_ASSUMPTIONS_PROPOSAL".equals(job.authorityMode());
    Path schema =
        materialize(
            assumptions
                ? "prompts/experiment-strategist/v1/commercial-assumptions-schema.json"
                : "prompts/experiment-strategist/v1/research-schema.json",
            ".json");
    Path mcp = materialize("mcp/experiment-strategist.mjs", ".mjs");
    Path clarityMcp = materialize("mcp/clarity-aggregate.mjs", ".mjs");
    try {
      ProcessBuilder builder =
          new ProcessBuilder(command(output, schema, mcp, clarityMcp, clarityAvailable));
      builder.redirectErrorStream(true).redirectOutput(log.toFile());
      builder.environment().put("MCP_BACKEND_URL", properties.getBackendUrl());
      builder.environment().put("MCP_EXECUTION_ID", job.id().toString());
      if (clarityAvailable)
        builder.environment().put("CLARITY_API_TOKEN_FILE", properties.getClarityApiTokenFile());
      Process process = builder.start();
      process
          .getOutputStream()
          .write(prompt(job, clarityAvailable).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      CodexTelemetryReporter.Session session =
          telemetry == null ? null : telemetry.monitor(job.id(), process, log);
      try {
        if (!waitWhileActive(process, log)) {
          process.destroyForcibly();
          process.waitFor(10, TimeUnit.SECONDS);
          throw new CodexActivityTimeoutException(
              "Timeout do Codex do Estrategista sem atividade comprovada.");
        }
        if (process.exitValue() != 0)
          throw new IllegalStateException(
              "Codex encerrou com codigo " + process.exitValue() + ": " + Files.readString(log));
        String raw = Files.readString(output);
        JsonNode result = json.readTree(raw);
        validate(result, assumptions);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alternativesJson", json.writeValueAsString(result.get("alternatives")));
        Map<String, Object> recommendation = new LinkedHashMap<>();
        recommendation.put("diagnosis", result.get("diagnosis"));
        if (!assumptions)
          recommendation.put("behavioralAssessment", result.get("behavioralAssessment"));
        recommendation.put(
            assumptions ? "proposedAssumptions" : "marketIntelligence",
            assumptions ? result.get("proposedAssumptions") : result.get("marketIntelligence"));
        recommendation.put(
            assumptions ? "evidenceQuality" : "portfolioAssessment",
            assumptions ? result.get("evidenceQuality") : result.get("portfolioAssessment"));
        recommendation.put("recommendation", result.get("recommendation"));
        payload.put("recommendationJson", json.writeValueAsString(recommendation));
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
      Files.deleteIfExists(mcp);
      Files.deleteIfExists(clarityMcp);
    }
  }

  /** Aguarda progresso observável e estende a janela até o teto absoluto de três ciclos. */
  boolean waitWhileActive(Process process, Path log) throws IOException, InterruptedException {
    long idleLimit = properties.getCodexTimeout().toMillis();
    long hardLimit = Math.multiplyExact(idleLimit, 3L);
    long startedAt = System.currentTimeMillis();
    long lastActivityAt = startedAt;
    long observedSize = Files.size(log);
    while (System.currentTimeMillis() - startedAt < hardLimit) {
      if (process.waitFor(ACTIVITY_POLL_SECONDS, TimeUnit.SECONDS)) return true;
      long currentSize = Files.size(log);
      if (currentSize != observedSize) {
        observedSize = currentSize;
        lastActivityAt = System.currentTimeMillis();
      }
      if (System.currentTimeMillis() - lastActivityAt >= idleLimit) return false;
    }
    return false;
  }

  /** Monta o comando com busca publica, sandbox somente leitura e schema versionado. */
  List<String> command(Path output, Path schema) {
    return command(output, schema, Path.of("experiment-strategist.mjs"));
  }

  /** Monta o comando com o MCP exclusivo e versionado do Estrategista. */
  List<String> command(Path output, Path schema, Path mcp) {
    return command(output, schema, mcp, Path.of("clarity-aggregate.mjs"), false);
  }

  /** Monta o comando com o MCP interno e o adaptador agregado opcional do Clarity. */
  List<String> command(
      Path output, Path schema, Path mcp, Path clarityMcp, boolean clarityAvailable) {
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
    command.add("--config");
    command.add("mcp_servers.experiment_strategist.command=\"node\"");
    command.add("--config");
    command.add("mcp_servers.experiment_strategist.args=[\"" + mcp.toAbsolutePath() + "\"]");
    if (clarityAvailable) {
      command.add("--config");
      command.add("mcp_servers.clarity_aggregate.command=\"node\"");
      command.add("--config");
      command.add("mcp_servers.clarity_aggregate.args=[\"" + clarityMcp.toAbsolutePath() + "\"]");
    }
    if (hasText(properties.getModel())) {
      command.add("--model");
      command.add(properties.getModel());
    }
    return command;
  }

  /** Resolve o prompt com evidencias e biblioteca comportamental versionadas. */
  private String prompt(StrategistJob job, boolean clarityAvailable) throws IOException {
    boolean assumptions = "COMMERCIAL_ASSUMPTIONS_PROPOSAL".equals(job.authorityMode());
    return read(assumptions
            ? "prompts/experiment-strategist/v1/commercial-assumptions.md"
            : "prompts/experiment-strategist/v1/research.md")
        .replace("{{EVIDENCE_SNAPSHOT}}", text(job.evidenceSnapshot()))
        .replace("{{BEHAVIORAL_MEMORY}}", "Incluida no snapshot de evidencias.")
        .replace("{{BEHAVIORAL_SCIENCE_LIBRARY}}", read("behavioral-science/v1/library.md"))
        .replace(
            "{{CLARITY_CAPABILITY}}",
            clarityAvailable
                ? "DISPONIVEL: consulte somente snapshots agregados por PAGE, SOURCE e DEVICE."
                : "INDISPONIVEL: declare a lacuna e use somente o funil interno; não invente dados.")
        .replace("{{RESEARCH_QUESTION}}", text(job.researchQuestion()));
  }

  /** Confirma que o arquivo secreto do Clarity existe, é legível e não está vazio. */
  private boolean isClarityApiTokenAvailable() throws IOException {
    if (!hasText(properties.getClarityApiTokenFile())) return false;
    Path tokenFile = Path.of(properties.getClarityApiTokenFile());
    return Files.isRegularFile(tokenFile)
        && Files.isReadable(tokenFile)
        && Files.size(tokenFile) > 0;
  }

  /** Rejeita parecer sem portfólio, inteligência de mercado, três caminhos ou recomendação. */
  private void validate(JsonNode result, boolean assumptions) {
    if (assumptions) {
      if (!result.has("alternatives")
          || result.get("alternatives").size() != 3
          || !result.has("proposedAssumptions")
          || !result.hasNonNull("evidenceQuality")
          || !result.hasNonNull("recommendation")
          || !result.hasNonNull("diagnosis"))
        throw new IllegalArgumentException(
            "Proposta de premissas fora do contrato estratégico v1.");
      return;
    }
    if (!result.has("alternatives")
        || result.get("alternatives").size() != 3
        || !result.has("sources")
        || result.get("sources").size() < 2
        || !result.hasNonNull("marketIntelligence")
        || !result.hasNonNull("behavioralAssessment")
        || !result.hasNonNull("portfolioAssessment")
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
