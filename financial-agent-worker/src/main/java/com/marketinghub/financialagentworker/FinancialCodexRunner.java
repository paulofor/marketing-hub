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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar o Codex financeiro em sandbox somente leitura e validar sua saida. */
@Component
public class FinancialCodexRunner {
  private static final Logger log = LoggerFactory.getLogger(FinancialCodexRunner.class);
  private final FinancialAgentProperties properties;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;
  private final FinancialAgentBackendClient backend;

  /** Configura o executor com telemetria auditável. */
  @Autowired
  public FinancialCodexRunner(
      FinancialAgentProperties properties,
      ObjectMapper objectMapper,
      CodexTelemetryReporter telemetry,
      FinancialAgentBackendClient backend) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
    this.backend = backend;
  }

  /** Mantém a construção direta usada por testes de contrato do comando. */
  public FinancialCodexRunner(FinancialAgentProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, null, null);
  }

  /** Executa uma conciliacao efemera sem permitir mutacoes financeiras ou no repositorio. */
  public Map<String, Object> run(FinancialAgentJob job) throws IOException, InterruptedException {
    Path output = Files.createTempFile("financial-agent-", ".json");
    Path processOutput = Files.createTempFile("financial-agent-process-", ".log");
    boolean assumptions = "COMMERCIAL_ASSUMPTIONS_VALIDATION".equals(job.authorityMode());
    boolean projection = "READ_ONLY_REVENUE_PROJECTION".equals(job.authorityMode());
    Path schema =
        materialize(
            assumptions
                ? "prompts/financial-agent/v1/commercial-assumptions-schema.json"
                : projection
                    ? "prompts/financial-agent/v1/revenue-projection-schema.json"
                    : "prompts/financial-agent/v1/report-schema.json",
            ".json");
    Path mcp = materialize("mcp/financial-agent.mjs", ".mjs");
    String prompt = buildPrompt(job);
    try {
      if (backend != null && job.agentTaskId() != null) {
        backend.recordTaskExecutionAudit(job.agentTaskId(), executionAudit(prompt));
      }
      ProcessBuilder builder = new ProcessBuilder(buildCommand(output, schema, mcp));
      builder.redirectErrorStream(true).redirectOutput(processOutput.toFile());
      builder.environment().put("MCP_BACKEND_URL", properties.getBackendUrl());
      builder.environment().put("MCP_EXECUTION_ID", job.id().toString());
      Process process = builder.start();
      process.getOutputStream().write(prompt.getBytes(StandardCharsets.UTF_8));
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
        TokenUsage usage = readTokenUsage(processOutput);
        String processLog = Files.readString(processOutput, StandardCharsets.UTF_8);
        int exitCode = process.exitValue();
        if (exitCode != 0)
          throw new IllegalStateException("Codex financeiro falhou: " + processLog);
        String raw = Files.readString(output);
        JsonNode result = objectMapper.readTree(raw);
        validate(result, projection, assumptions);
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("reconciliationJson", objectMapper.writeValueAsString(result));
        payload.put(
            "dailyReport",
            result
                .get(
                    assumptions
                        ? "executiveSummary"
                        : projection ? "executiveSummary" : "dailyReport")
                .asText());
        payload.put("rawModelResponse", raw);
        payload.put("model", properties.getModel());
        payload.put("estimatedCost", null);
        payload.put("promptSent", prompt);
        payload.put("reasoningEffort", properties.requiredReasoningEffort());
        payload.put("requestedServiceTier", configuredServiceTier());
        payload.put("effectiveServiceTier", "STANDARD");
        payload.put("serviceTierExceptionReason", properties.getServiceTierExceptionReason());
        if (usage.informed()) {
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
      Files.deleteIfExists(processOutput);
      Files.deleteIfExists(schema);
      Files.deleteIfExists(mcp);
    }
  }

  /** Monta o envelope completo que antecede a chamada de modelo vinculada à tarefa BPM. */
  Map<String, Object> executionAudit(String prompt) {
    LinkedHashMap<String, Object> audit = new LinkedHashMap<>();
    audit.put("executionMode", "MODEL");
    audit.put("modelCode", properties.getModel());
    audit.put("reasoningEffort", properties.requiredReasoningEffort());
    audit.put("promptSent", prompt);
    audit.put("accessedUrls", List.of());
    return audit;
  }

  /** Avalia um teto de vídeo sem movimentar dinheiro e devolve somente a decisão estruturada. */
  public Map<String, Object> reviewVideoCycle(VideoProductionCycleReview cycle)
      throws IOException, InterruptedException {
    Path output = Files.createTempFile("financial-video-cycle-", ".json");
    Path processOutput = Files.createTempFile("financial-video-cycle-process-", ".log");
    Path schema = materialize("prompts/financial-agent/v1/video-cycle-review-schema.json", ".json");
    try {
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
      Process process =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(processOutput.toFile())
              .start();
      String prompt =
          read("prompts/financial-agent/v1/video-cycle-review.md")
              .replace("{{CYCLE}}", objectMapper.writeValueAsString(cycle));
      process.getOutputStream().write(prompt.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        throw new IllegalStateException("Timeout ao avaliar ciclo de vídeo.");
      }
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
            "Codex financeiro falhou no ciclo: " + Files.readString(processOutput));
      }
      JsonNode result = objectMapper.readTree(Files.readString(output));
      if (!result.hasNonNull("decision") || !result.hasNonNull("reason")) {
        throw new IllegalArgumentException("Parecer financeiro de vídeo incompleto.");
      }
      return Map.of(
          "decision", result.get("decision").asText(),
          "reason", result.get("reason").asText(),
          "decidedByAgentKey", "financial-agent");
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processOutput);
      Files.deleteIfExists(schema);
    }
  }

  /** Pesquisa somente fontes oficiais e devolve preço comparável com resposta bruta auditável. */
  public Map<String, Object> researchProviderPricing(ProviderPricingCandidate candidate)
      throws IOException, InterruptedException {
    Path output = Files.createTempFile("financial-provider-pricing-", ".json");
    Path processOutput = Files.createTempFile("financial-provider-pricing-process-", ".log");
    Path schema = materialize("prompts/financial-agent/v1/provider-pricing-schema.json", ".json");
    try {
      List<String> command = new ArrayList<>(buildCommand(output, schema));
      Process process =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(processOutput.toFile())
              .start();
      String prompt =
          read("prompts/financial-agent/v1/provider-pricing.md")
              .replace("{{MODEL}}", objectMapper.writeValueAsString(candidate));
      process.getOutputStream().write(prompt.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        throw new IllegalStateException("Timeout na pesquisa de preço de provider.");
      }
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
            "Pesquisa de preço falhou: " + Files.readString(processOutput));
      }
      String raw = Files.readString(output);
      JsonNode result = objectMapper.readTree(raw);
      LinkedHashMap<String, Object> payload =
          objectMapper.convertValue(
              result, new com.fasterxml.jackson.core.type.TypeReference<>() {});
      payload.put("rawResponse", raw);
      payload.put("researchModel", properties.getModel());
      return payload;
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processOutput);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o comando Codex preservando sandbox e repositorio somente leitura. */
  List<String> buildCommand(Path output, Path schema) {
    String reasoningEffort = properties.requiredReasoningEffort();
    List<String> command =
        new ArrayList<>(
            List.of(
                properties.getCodexCommand(),
                "--search",
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
                "--json",
                "--color",
                "never",
                "--config",
                "approval_policy=\"never\"",
                "--config",
                "service_tier=\"" + configuredServiceTier() + "\""));
    command.addAll(List.of("--config", "model_reasoning_effort=\"" + reasoningEffort + "\""));
    if (properties.getModel() != null && !properties.getModel().isBlank()) {
      command.add("--model");
      command.add(properties.getModel());
    }
    return command;
  }

  /** Monta o comando com o servidor MCP exclusivo do agente e suas variáveis permitidas. */
  List<String> buildCommand(Path output, Path schema, Path mcp) {
    List<String> command = new ArrayList<>(buildCommand(output, schema));
    command.addAll(
        List.of(
            "--config",
            "mcp_servers.financial_agent.command=\"node\"",
            "--config",
            "mcp_servers.financial_agent.args=[\"" + mcp.toAbsolutePath() + "\"]",
            "--config",
            "mcp_servers.financial_agent.env_vars=[\"MCP_BACKEND_URL\",\"MCP_EXECUTION_ID\"]"));
    return command;
  }

  /** Lê o último total cumulativo de tokens realmente informado pelo runtime Codex. */
  TokenUsage readTokenUsage(Path processLog) {
    long input = 0;
    long cached = 0;
    long output = 0;
    boolean informed = false;
    try {
      for (String line : Files.readAllLines(processLog)) {
        if (line.isBlank()) continue;
        JsonNode event;
        try {
          event = objectMapper.readTree(line);
        } catch (IOException ex) {
          log.debug("Linha não JSON ignorada na telemetria de Plutus.", ex);
          continue;
        }
        JsonNode usage = event.path("usage");
        if (!usage.isObject()) continue;
        input = Math.max(input, token(usage, "input_tokens", "inputTokens"));
        cached = Math.max(cached, token(usage, "cached_input_tokens", "cachedInputTokens"));
        output = Math.max(output, token(usage, "output_tokens", "outputTokens"));
        informed = true;
      }
    } catch (IOException ex) {
      log.warn("Falha ao ler telemetria de tokens de Plutus. output={}", processLog, ex);
      return TokenUsage.empty();
    }
    return informed ? new TokenUsage(input, cached, output, true) : TokenUsage.empty();
  }

  /** Lê uma das grafias aceitas de um contador sem inventar consumo ausente. */
  private long token(JsonNode usage, String snakeCase, String camelCase) {
    JsonNode value = usage.has(snakeCase) ? usage.path(snakeCase) : usage.path(camelCase);
    return value.canConvertToLong() ? Math.max(0, value.asLong()) : 0;
  }

  /** Confirma a exceção explícita enquanto o catálogo Codex OAuth não anunciar Flex. */
  String configuredServiceTier() {
    String configured = properties.getServiceTier();
    if (configured != null && "default".equalsIgnoreCase(configured.trim())) return "default";
    throw new IllegalArgumentException(
        "Plutus só pode usar o tier default enquanto Flex não for anunciado pelo catálogo Codex.");
  }

  /** Resolve o prompt versionado com o snapshot congelado pelo backend. */
  private String buildPrompt(FinancialAgentJob job) throws IOException {
    boolean assumptions = "COMMERCIAL_ASSUMPTIONS_VALIDATION".equals(job.authorityMode());
    boolean projection = "READ_ONLY_REVENUE_PROJECTION".equals(job.authorityMode());
    return read(assumptions
            ? "prompts/financial-agent/v1/commercial-assumptions.md"
            : projection
                ? "prompts/financial-agent/v1/revenue-projection.md"
                : "prompts/financial-agent/v1/report.md")
        .replace("{{PLAN_ID}}", String.valueOf(job.commercialPlanId()))
        .replace("{{PLAN_VERSION}}", String.valueOf(job.commercialPlanVersion()))
        .replace("{{DECISION_CONTEXT}}", String.valueOf(job.projectionRequest()))
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
  private void validate(JsonNode result, boolean projection, boolean assumptions) {
    if (assumptions) {
      if (!result.hasNonNull("decision")
          || !result.has("validatedAssumptions")
          || !result.hasNonNull("executiveSummary")
          || !result.has("risks")) {
        throw new IllegalArgumentException(
            "Validação de premissas fora do contrato financeiro v1.");
      }
      return;
    }
    if (projection) {
      Set<String> scenarioNames = new java.util.HashSet<>();
      if (result.has("scenarios")) {
        result.get("scenarios").forEach(item -> scenarioNames.add(item.path("name").asText()));
      }
      if (scenarioNames.size() != 3
          || !scenarioNames.equals(Set.of("CONSERVATIVE", "BASE", "OPTIMISTIC"))
          || !result.has("recommendedInitialInvestmentBrl")
          || !result.has("breakEven")
          || !result.hasNonNull("executiveSummary")
          || !result.has("learningCandidate")) {
        throw new IllegalArgumentException("Projeção de receita fora do contrato financeiro v1.");
      }
      return;
    }
    if (!result.has("totals")
        || !result.has("sourceCoverage")
        || !result.has("divergences")
        || !result.hasNonNull("dailyReport")
        || !result.hasNonNull("decision")) {
      throw new IllegalArgumentException("Resposta fora do contrato financeiro v1.");
    }
  }

  /** Representa os contadores cumulativos realmente observados. */
  record TokenUsage(long inputTokens, long cachedInputTokens, long outputTokens, boolean informed) {
    /** Representa telemetria não informada pelo runtime. */
    static TokenUsage empty() {
      return new TokenUsage(0, 0, 0, false);
    }
  }
}
