package com.marketinghub.growthoperatorworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: executar atividades BPM de Hermes com contrato versionado e somente leitura.
 */
@Component
public class GrowthOperatorBpmRunner {
  private static final Logger log = LoggerFactory.getLogger(GrowthOperatorBpmRunner.class);
  private static final String EXPERIMENT_PROCESS = "operacao-otimizacao-experimento";
  private static final String COMMUNICATION_PROCESS = "pde-communication-sales-journey";
  private static final Set<String> FORBIDDEN_STRATEGIC_FIELDS =
      Set.of(
          "audience",
          "segment",
          "buyer",
          "pain",
          "problem",
          "desiredOutcome",
          "promise",
          "mechanism",
          "positioning",
          "offerFraming",
          "offerThesis",
          "priceDecision",
          "approvedPriceBrl");
  private final WorkerProperties properties;
  private final ObjectMapper json;

  /** Configura o executor BPM com propriedades operacionais e serialização auditável. */
  public GrowthOperatorBpmRunner(WorkerProperties properties, ObjectMapper json) {
    this.properties = properties;
    this.json = json;
  }

  /** Executa uma atividade com escopo segregado e correlação da memória pela tarefa. */
  public BpmExecution run(Map<String, Object> task) throws IOException, InterruptedException {
    String processCode = processCode(task);
    ExecutionScope scope = executionScope(task);
    String expectedStrategicHash = strategicContractHash(task);
    Path answer = Files.createTempFile("hermes-bpm-result-", ".json");
    Path processLog = Files.createTempFile("hermes-bpm-process-", ".log");
    Path schema = materialize(schemaResourceFor(processCode), ".json");
    Path mcpServer = materialize("mcp/marketing-hub-readonly.mjs", ".mjs");
    PromptComposition prompt = promptComposition(task);
    try {
      ProcessBuilder builder =
          new ProcessBuilder(buildCommand(answer, schema, mcpServer))
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile());
      builder.environment().remove("MCP_COMMERCIAL_PLAN_ID");
      builder.environment().remove("MCP_EXPERIMENT_ID");
      builder.environment().put(scope.environmentName(), String.valueOf(scope.id()));
      builder.environment().put("MCP_SOURCE_EXECUTION_ID", "bpm-task-" + task.get("taskId"));
      builder.environment().put("MCP_MARKETING_HUB_URL", properties.getMarketingHubUrl());
      Process process = builder.start();
      process.getOutputStream().write(prompt.fullPrompt().getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        process.waitFor(10, TimeUnit.SECONDS);
        throw new BpmExecutionException(
            "Timeout da atividade BPM de Hermes após "
                + properties.getCodexTimeout().toMinutes()
                + " minutos.",
            readTokenUsage(processLog),
            safeToolUsage(processLog, task, scope),
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart());
      }
      TokenUsage usage = readTokenUsage(processLog);
      if (process.exitValue() != 0) {
        throw new BpmExecutionException(
            "Codex encerrou com código "
                + process.exitValue()
                + ": "
                + Files.readString(processLog),
            usage,
            safeToolUsage(processLog, task, scope),
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart());
      }
      try {
        JsonNode result = json.readTree(Files.readString(answer));
        validate(result, processCode, expectedStrategicHash);
        return new BpmExecution(
            result,
            usage,
            extractToolUsage(processLog),
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart());
      } catch (IOException | RuntimeException ex) {
        log.error(
            "Resposta inválida na atividade BPM de Hermes. taskId={} sourceReference={}",
            task.get("taskId"),
            scope.reference(),
            ex);
        throw new BpmExecutionException(
            "Resposta BPM de Hermes fora do contrato versionado.",
            usage,
            safeToolUsage(processLog, task, scope),
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart(),
            ex);
      }
    } finally {
      Files.deleteIfExists(answer);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
      Files.deleteIfExists(mcpServer);
    }
  }

  /** Extrai a identidade estratégica recebida para impedir que o modelo devolva outro contrato. */
  private String strategicContractHash(Map<String, Object> task) {
    JsonNode contract = json.valueToTree(task.get("marketStrategicContract"));
    String hash = contract.path("contentHash").asText();
    if (!hash.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Tarefa de Hermes sem hash estratégico íntegro de Atena.");
    }
    return hash;
  }

  /** Monta o comando Codex em sandbox somente leitura e telemetria JSONL. */
  List<String> buildCommand(Path answer, Path schema, Path mcpServer) {
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
                answer.toString(),
                "--json",
                "--color",
                "never",
                "--config",
                "approval_policy=\"never\"",
                "--config",
                "mcp_servers.marketing_hub_readonly.command=\"node\"",
                "--config",
                "mcp_servers.marketing_hub_readonly.args=[\"" + mcpServer.toAbsolutePath() + "\"]",
                "--config",
                "mcp_servers.marketing_hub_readonly.env_vars=[\"MCP_MARKETING_HUB_URL\",\"MCP_COMMERCIAL_PLAN_ID\",\"MCP_EXPERIMENT_ID\",\"MCP_SOURCE_EXECUTION_ID\"]"));
    command.addAll(
        List.of(
            "--config", "model_reasoning_effort=\"" + properties.requiredReasoningEffort() + "\""));
    if (hasText(properties.getModel())) {
      command.addAll(List.of("--model", properties.getModel()));
    }
    return command;
  }

  /** Resolve o prompt versionado usando o contrato congelado da tarefa. */
  String prompt(Map<String, Object> task) throws IOException {
    return promptComposition(task).fullPrompt();
  }

  /** Compõe o núcleo estável de Hermes com a missão resolvida da atividade. */
  PromptComposition promptComposition(Map<String, Object> task) throws IOException {
    String agentPromptPart = read("prompts/growth-operator/v2/agent-core.md");
    String activityPromptPart =
        read(promptResourceFor(processCode(task)))
            .replace("{{TASK_CONTEXT}}", json.writeValueAsString(task));
    return new PromptComposition(
        agentPromptPart + "\n\n" + activityPromptPart, agentPromptPart, activityPromptPart);
  }

  /** Extrai o experimento da referência segregada e rejeita qualquer outro escopo. */
  static long experimentId(Map<String, Object> task) {
    ExecutionScope scope = executionScope(task);
    if (!"MCP_EXPERIMENT_ID".equals(scope.environmentName())) {
      throw new IllegalArgumentException(
          "Atividade BPM de Hermes exige sourceReference no formato experiment:<id>.");
    }
    return scope.id();
  }

  /** Resolve o escopo permitido pelo processo sem misturar plano e experimento. */
  static ExecutionScope executionScope(Map<String, Object> task) {
    String reference = String.valueOf(task.getOrDefault("sourceReference", "")).trim();
    if (EXPERIMENT_PROCESS.equals(processCode(task))
        && reference.matches("experiment:[1-9][0-9]*")) {
      return new ExecutionScope(
          "MCP_EXPERIMENT_ID",
          Long.parseLong(reference.substring("experiment:".length())),
          reference);
    }
    if (COMMUNICATION_PROCESS.equals(processCode(task))
        && reference.matches("commercial-plan:[1-9][0-9]*(?:@v[1-9][0-9]*)?")) {
      String id = reference.substring("commercial-plan:".length()).replaceFirst("@v.*$", "");
      return new ExecutionScope("MCP_COMMERCIAL_PLAN_ID", Long.parseLong(id), reference);
    }
    throw new IllegalArgumentException(
        "Atividade BPM de Hermes exige escopo canônico compatível com o processo.");
  }

  /** Seleciona o prompt versionado específico da responsabilidade executada. */
  static String promptResourceFor(String processCode) {
    return switch (processCode) {
      case COMMUNICATION_PROCESS -> "prompts/bpm/v2/pde-growth-operation-contract.md";
      case EXPERIMENT_PROCESS -> "prompts/bpm/v3/experiment-optimization.md";
      default -> throw new IllegalArgumentException("Processo BPM não suportado por Hermes.");
    };
  }

  /** Seleciona o schema versionado específico da responsabilidade executada. */
  static String schemaResourceFor(String processCode) {
    return switch (processCode) {
      case COMMUNICATION_PROCESS -> "prompts/bpm/v2/pde-growth-operation-contract-schema.json";
      case EXPERIMENT_PROCESS -> "prompts/bpm/v2/experiment-optimization-schema.json";
      default -> throw new IllegalArgumentException("Processo BPM não suportado por Hermes.");
    };
  }

  /** Exige decisão funcional, alternativas e critérios de governança do processo. */
  static void validate(JsonNode result, String processCode) {
    validate(result, processCode, null);
  }

  /** Confirma que a saída preserva exatamente a identidade estratégica recebida. */
  static void validate(JsonNode result, String processCode, String expectedStrategicHash) {
    if (!List.of("COMPLETED", "BLOCKED").contains(result.path("executionStatus").asText())
        || result.path("alternatives").size() != 3
        || result.path("observedFacts").isEmpty()
        || result.path("expectedMetric").asText().isBlank()
        || result.path("continueCriteria").asText().isBlank()
        || result.path("adjustCriteria").asText().isBlank()
        || result.path("stopCriteria").asText().isBlank()
        || result.path("recommendedAction").asText().isBlank()) {
      throw new IllegalArgumentException("Parecer BPM de Hermes incompleto.");
    }
    if (expectedStrategicHash != null
        && !expectedStrategicHash.equals(
            result.path("strategicContractReference").path("contentHash").asText())) {
      throw new IllegalArgumentException(
          "Parecer de Hermes não preserva o hash do contrato estratégico de Atena.");
    }
    if (COMMUNICATION_PROCESS.equals(processCode)
        && (result.path("strategicContractReference").isMissingNode()
            || !result.path("strategicContractReference").path("strategyPreserved").asBoolean()
            || result.path("strategicContractReference").path("contentHash").asText().length() != 64
            || result.path("growthOperationContract").isMissingNode()
            || result.path("growthOperationContract").path("eventContracts").size() < 5
            || result.path("growthOperationContract").path("attributionPlan").asText().isBlank()
            || result.path("growthOperationContract").path("instrumentationGate").asText().isBlank()
            || containsForbiddenStrategicField(result)
            || (result.path("strategicContractReference").path("revisionRequired").asBoolean()
                && "COMPLETED".equals(result.path("executionStatus").asText())))) {
      throw new IllegalArgumentException("Contrato operacional de crescimento do PDE incompleto.");
    }
    if (EXPERIMENT_PROCESS.equals(processCode)
        && (result.path("strategicContractReference").isMissingNode()
            || !result.path("strategicContractReference").path("strategyPreserved").asBoolean()
            || result.path("strategicContractReference").path("contentHash").asText().length() != 64
            || containsForbiddenStrategicField(result)
            || (result.path("strategicContractReference").path("revisionRequired").asBoolean()
                && "COMPLETED".equals(result.path("executionStatus").asText())))) {
      throw new IllegalArgumentException(
          "Otimização de Hermes não preserva o contrato estratégico de Atena.");
    }
  }

  /** Rejeita autoria estratégica escondida em qualquer nível da saída de Hermes. */
  private static boolean containsForbiddenStrategicField(JsonNode node) {
    if (node.isArray()) {
      for (JsonNode child : node) if (containsForbiddenStrategicField(child)) return true;
      return false;
    }
    if (!node.isObject()) return false;
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      if (FORBIDDEN_STRATEGIC_FIELDS.contains(field.getKey())
          || containsForbiddenStrategicField(field.getValue())) return true;
    }
    return false;
  }

  /** Lê o processo congelado da tarefa. */
  private static String processCode(Map<String, Object> task) {
    return String.valueOf(task.getOrDefault("processCode", "")).trim();
  }

  /** Lê a última medição cumulativa de tokens emitida pelo Codex. */
  TokenUsage readTokenUsage(Path processLog) {
    if (!Files.exists(processLog)) return TokenUsage.empty();
    long input = 0;
    long cached = 0;
    long output = 0;
    boolean informed = false;
    try {
      for (String line : Files.readAllLines(processLog)) {
        if (line.isBlank()) continue;
        JsonNode event;
        try {
          event = json.readTree(line);
        } catch (IOException ex) {
          log.debug("Linha não JSON ignorada na telemetria BPM de Hermes.", ex);
          continue;
        }
        JsonNode usage = event.path("usage");
        if (!usage.isObject()) continue;
        Long measuredInput = tokenValue(usage, "input_tokens", "inputTokens");
        Long measuredOutput = tokenValue(usage, "output_tokens", "outputTokens");
        Long measuredCache = tokenValue(usage, "cached_input_tokens", "cachedInputTokens");
        if (measuredCache == null) {
          measuredCache =
              tokenValue(usage.path("input_tokens_details"), "cached_tokens", "cachedTokens");
        }
        if (measuredInput != null || measuredOutput != null || measuredCache != null) {
          informed = true;
          input = Math.max(input, measuredInput == null ? 0 : measuredInput);
          cached = Math.max(cached, measuredCache == null ? 0 : measuredCache);
          output = Math.max(output, measuredOutput == null ? 0 : measuredOutput);
        }
      }
    } catch (IOException ex) {
      log.warn("Falha ao ler tokens da atividade BPM de Hermes. output={}", processLog, ex);
      return TokenUsage.empty();
    }
    return informed ? new TokenUsage(input, cached, output) : TokenUsage.empty();
  }

  /** Resume as ferramentas MCP usadas para compor a evidência auditável. */
  List<JsonNode> extractToolUsage(Path processLog) throws IOException {
    List<JsonNode> tools = new ArrayList<>();
    for (String line : Files.readAllLines(processLog)) {
      if (!line.startsWith("{") || !line.contains("\"tool\"")) continue;
      try {
        JsonNode candidate = json.readTree(line);
        if (candidate.hasNonNull("tool") && candidate.hasNonNull("status")) {
          tools.add(candidate);
          continue;
        }
        JsonNode item = candidate.path("item");
        if (!"mcp_tool_call".equals(item.path("type").asText())
            || item.path("tool").asText().isBlank()) {
          continue;
        }
        var observed = json.createObjectNode();
        observed.put("tool", item.path("tool").asText());
        observed.put("server", item.path("server").asText());
        observed.put("status", item.path("status").asText());
        JsonNode audit = mcpAudit(item.path("result"));
        if (audit != null) observed.set("audit", audit);
        tools.add(observed);
      } catch (IOException ex) {
        log.debug("Linha MCP inválida ignorada na evidência BPM de Hermes.", ex);
      }
    }
    return tools;
  }

  /** Recupera a origem e o horário auditável devolvidos dentro do conteúdo textual do MCP. */
  private JsonNode mcpAudit(JsonNode result) {
    for (JsonNode content : result.path("content")) {
      if (!"text".equals(content.path("type").asText())
          || content.path("text").asText().isBlank()) {
        continue;
      }
      try {
        JsonNode payload = json.readTree(content.path("text").asText());
        if (payload.has("audit")) return payload.get("audit");
      } catch (IOException ex) {
        log.debug("Conteúdo textual do MCP não contém auditoria JSON.", ex);
      }
    }
    return null;
  }

  /** Preserva as ferramentas já observadas sem substituir a falha funcional por erro de leitura. */
  private List<JsonNode> safeToolUsage(
      Path processLog, Map<String, Object> task, ExecutionScope scope) {
    try {
      return extractToolUsage(processLog);
    } catch (IOException ex) {
      log.warn(
          "Falha ao reconstruir ferramentas MCP da atividade BPM de Hermes. taskId={} sourceReference={} processLog={}",
          task.get("taskId"),
          scope.reference(),
          processLog,
          ex);
      return List.of();
    }
  }

  /** Lê um contador oficial ou seu alias sem inventar valor ausente. */
  private static Long tokenValue(JsonNode usage, String officialName, String alias) {
    JsonNode value = usage.hasNonNull(officialName) ? usage.get(officialName) : usage.get(alias);
    return value != null && value.isNumber() ? value.longValue() : null;
  }

  /** Materializa um recurso versionado apenas durante a execução. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("hermes-bpm-resource-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Indica se uma configuração textual foi informada. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Preserva resultado, ferramentas e consumo na mesma execução BPM. */
  public record BpmExecution(
      JsonNode result,
      TokenUsage usage,
      List<JsonNode> toolUsage,
      String promptSent,
      String agentPromptPart,
      String activityPromptPart) {
    /** Mantém compatibilidade com testes que não precisam representar o prompt. */
    public BpmExecution(JsonNode result, TokenUsage usage, List<JsonNode> toolUsage) {
      this(result, usage, toolUsage, null, null, null);
    }

    /** Mantém compatibilidade com testes que representam somente o prompt integral. */
    public BpmExecution(
        JsonNode result, TokenUsage usage, List<JsonNode> toolUsage, String promptSent) {
      this(result, usage, toolUsage, promptSent, null, null);
    }
  }

  /** Representa as duas partes e a composição exata enviada ao modelo. */
  record PromptComposition(String fullPrompt, String agentPromptPart, String activityPromptPart) {}

  /** Representa o único escopo de dados liberado para uma execução. */
  record ExecutionScope(String environmentName, long id, String reference) {}

  /** Representa contadores reais cumulativos informados pelo Codex. */
  public record TokenUsage(Long inputTokens, Long cachedInputTokens, Long outputTokens) {
    /** Indica se pelo menos um contador foi medido. */
    public boolean informed() {
      return inputTokens != null || cachedInputTokens != null || outputTokens != null;
    }

    /** Representa uma execução sem telemetria disponível. */
    public static TokenUsage empty() {
      return new TokenUsage(null, null, null);
    }
  }

  /** Preserva tokens medidos quando a execução termina em falha técnica. */
  public static final class BpmExecutionException extends IllegalStateException {
    private final TokenUsage usage;
    private final List<JsonNode> toolUsage;
    private final String promptSent;
    private final String agentPromptPart;
    private final String activityPromptPart;

    /** Cria a falha com a última medição disponível. */
    BpmExecutionException(String message, TokenUsage usage) {
      this(message, usage, List.of(), null, null, null);
    }

    /** Cria a falha com medição e ferramentas já consultadas. */
    BpmExecutionException(String message, TokenUsage usage, List<JsonNode> toolUsage) {
      this(message, usage, toolUsage, null, null, null);
    }

    /** Cria a falha com medição, ferramentas e prompt exato. */
    BpmExecutionException(
        String message, TokenUsage usage, List<JsonNode> toolUsage, String promptSent) {
      this(message, usage, toolUsage, promptSent, null, null);
    }

    /** Cria a falha com medição, ferramentas e as duas partes do prompt. */
    BpmExecutionException(
        String message,
        TokenUsage usage,
        List<JsonNode> toolUsage,
        String promptSent,
        String agentPromptPart,
        String activityPromptPart) {
      super(message);
      this.usage = usage;
      this.toolUsage = List.copyOf(toolUsage);
      this.promptSent = promptSent;
      this.agentPromptPart = agentPromptPart;
      this.activityPromptPart = activityPromptPart;
    }

    /** Cria a falha com medição e causa original. */
    BpmExecutionException(String message, TokenUsage usage, Throwable cause) {
      this(message, usage, List.of(), null, null, null, cause);
    }

    /** Cria a falha preservando medição, ferramentas e causa original. */
    BpmExecutionException(
        String message, TokenUsage usage, List<JsonNode> toolUsage, Throwable cause) {
      this(message, usage, toolUsage, null, cause);
    }

    /** Cria a falha preservando também o prompt integral enviado. */
    BpmExecutionException(
        String message,
        TokenUsage usage,
        List<JsonNode> toolUsage,
        String promptSent,
        Throwable cause) {
      this(message, usage, toolUsage, promptSent, null, null, cause);
    }

    /** Cria a falha preservando também as partes auditáveis do prompt. */
    BpmExecutionException(
        String message,
        TokenUsage usage,
        List<JsonNode> toolUsage,
        String promptSent,
        String agentPromptPart,
        String activityPromptPart,
        Throwable cause) {
      super(message, cause);
      this.usage = usage;
      this.toolUsage = List.copyOf(toolUsage);
      this.promptSent = promptSent;
      this.agentPromptPart = agentPromptPart;
      this.activityPromptPart = activityPromptPart;
    }

    /** Retorna a medição preservada antes da falha. */
    public TokenUsage usage() {
      return usage;
    }

    /** Retorna as ferramentas observadas antes da falha. */
    public List<JsonNode> toolUsage() {
      return toolUsage;
    }

    /** Retorna o prompt exato preservado antes da interrupção. */
    public String promptSent() {
      return promptSent;
    }

    /** Retorna o núcleo de Hermes preservado antes da interrupção. */
    public String agentPromptPart() {
      return agentPromptPart;
    }

    /** Retorna a missão da atividade preservada antes da interrupção. */
    public String activityPromptPart() {
      return activityPromptPart;
    }
  }
}
