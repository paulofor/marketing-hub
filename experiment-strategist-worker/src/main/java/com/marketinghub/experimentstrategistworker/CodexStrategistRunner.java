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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar pesquisa Codex somente leitura com saida estruturada. */
@Component
public class CodexStrategistRunner {
  private static final Logger log = LoggerFactory.getLogger(CodexStrategistRunner.class);
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

  /** Executa pesquisa efêmera e devolve parecer e consumo cumulativo auditáveis. */
  public Map<String, Object> run(StrategistJob job) throws IOException, InterruptedException {
    Path output = Files.createTempFile("experiment-strategist-", ".json");
    Path log = Files.createTempFile("experiment-strategist-", ".log");
    boolean clarityAvailable = isClarityApiTokenAvailable();
    boolean assumptions = "COMMERCIAL_ASSUMPTIONS_PROPOSAL".equals(job.authorityMode());
    Path schema =
        materialize(
            assumptions
                ? "prompts/experiment-strategist/v1/commercial-assumptions-schema.json"
                : "prompts/experiment-strategist/v2/research-schema.json",
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
        if (!assumptions)
          recommendation.put("marketStrategicContract", result.get("marketStrategicContract"));
        recommendation.put("recommendation", result.get("recommendation"));
        payload.put("recommendationJson", json.writeValueAsString(recommendation));
        payload.put("publicSourcesJson", json.writeValueAsString(result.get("sources")));
        payload.put("rawModelResponse", raw);
        payload.put(
            "modelName", hasText(properties.getModel()) ? properties.getModel() : "codex-default");
        payload.put("estimatedCost", null);
        payload.put("effectiveServiceTier", "STANDARD");
        TokenUsage usage = readTokenUsage(log);
        if (usage != null) {
          payload.put("inputTokens", usage.inputTokens());
          payload.put("cachedInputTokens", usage.cachedInputTokens());
          payload.put("outputTokens", usage.outputTokens());
        }
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

  /** Lê o último total completo do Codex sem somar eventos cumulativos nem inventar tokens. */
  TokenUsage readTokenUsage(Path processLog) {
    TokenUsage latest = null;
    try {
      for (String line : Files.readAllLines(processLog)) {
        if (line.isBlank()) continue;
        JsonNode event;
        try {
          event = json.readTree(line);
        } catch (IOException ex) {
          log.debug("Linha não JSON ignorada na telemetria de Atena.", ex);
          continue;
        }
        JsonNode usage = event.path("usage");
        if (!usage.isObject()) continue;
        var input = usage.path("input_tokens");
        var cached = usage.path("cached_input_tokens");
        var output = usage.path("output_tokens");
        if (!count(input) || !count(cached) || !count(output) || cached.asLong() > input.asLong())
          return null;
        latest = new TokenUsage(input.asLong(), cached.asLong(), output.asLong());
      }
    } catch (IOException ex) {
      log.warn("Falha ao ler consumo Codex de Atena. output={}", processLog, ex);
      return null;
    }
    return latest;
  }

  /** Exige um contador inteiro não negativo realmente recebido do runtime. */
  private boolean count(JsonNode value) {
    return value.isIntegralNumber() && value.canConvertToLong() && value.asLong() >= 0;
  }

  /** Responsabilidade: transportar o uso cumulativo completo sem atribuir tarifa no executor. */
  record TokenUsage(long inputTokens, long cachedInputTokens, long outputTokens) {}

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

  /**
   * Libera consultas anotadas do MCP com ambiente explícito; escritas continuam exigindo aprovação.
   */
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
    command.add("--json");
    command.add("--color");
    command.add("never");
    command.add("--config");
    command.add("service_tier=\"default\"");
    command.add("--config");
    command.add("mcp_servers.experiment_strategist.default_tools_approval_mode=\"writes\"");
    command.add("--config");
    command.add("mcp_servers.experiment_strategist.command=\"node\"");
    command.add("--config");
    command.add("mcp_servers.experiment_strategist.args=[\"" + mcp.toAbsolutePath() + "\"]");
    command.add("--config");
    command.add(
        "mcp_servers.experiment_strategist.env_vars=[\"MCP_BACKEND_URL\",\"MCP_EXECUTION_ID\"]");
    command.add("--config");
    command.add("mcp_servers.experiment_strategist.tool_timeout_sec=90");
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
            : "prompts/experiment-strategist/v2/research.md")
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
        || !result.hasNonNull("marketStrategicContract")
        || !"MARKET_STRATEGY_V2"
            .equals(result.path("marketStrategicContract").path("contractVersion").asText())
        || !"ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH"
            .equals(result.path("marketStrategicContract").path("operatorBoundary").asText())
        || !result.hasNonNull("recommendation")
        || !result.hasNonNull("diagnosis"))
      throw new IllegalArgumentException("Resposta Codex fora do contrato estratégico v2.");
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
