package com.marketinghub.communicationagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar os contratos versionados de Íris e validar sua fronteira. */
@Component
public class CommunicationAgentCodexRunner {
  private static final Logger log = LoggerFactory.getLogger(CommunicationAgentCodexRunner.class);
  private final CommunicationAgentProperties properties;
  private final ObjectMapper json;
  private final CodexTelemetryReporter telemetry;

  /** Configura o executor com runtime e serialização auditável. */
  public CommunicationAgentCodexRunner(
      CommunicationAgentProperties properties,
      ObjectMapper json,
      CodexTelemetryReporter telemetry) {
    this.properties = properties;
    this.json = json;
    this.telemetry = telemetry;
  }

  /** Executa uma atividade em sandbox somente leitura e devolve resultado, prompt e tokens. */
  public Execution run(Map<String, Object> task) throws IOException, InterruptedException {
    Contract contract = contractFor(task);
    validateInput(task);
    PromptComposition prompt = promptComposition(task, contract);
    Path answer = Files.createTempFile("iris-result-", ".json");
    Path processLog = Files.createTempFile("iris-codex-", ".jsonl");
    Path schema = materialize("prompts/iris/v1/output-schema.json", ".json");
    Path mcp = materialize("mcp/communication-agent.mjs", ".mjs");
    try {
      ProcessBuilder builder =
          new ProcessBuilder(command(answer, schema, mcp))
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile());
      builder.environment().put("MCP_MARKETING_HUB_URL", properties.getBackendUrl());
      builder.environment().put("MCP_TASK_ID", String.valueOf(task.get("taskId")));
      builder
          .environment()
          .put("MCP_SOURCE_REFERENCE", String.valueOf(task.get("sourceReference")));
      log.info(
          "Executando request de Íris. taskId={} processCode={} activityId={} model={} serviceTier={}",
          task.get("taskId"),
          task.get("processCode"),
          task.get("activityId"),
          modelCode(),
          configuredServiceTier());
      Process process = builder.start();
      process.getOutputStream().write(prompt.fullPrompt().getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      try (CodexTelemetryReporter.Session session =
          telemetry.monitor(((Number) task.get("taskId")).longValue(), process, processLog)) {
        if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
          process.destroyForcibly();
          process.waitFor(10, TimeUnit.SECONDS);
          throw new ExecutionException(
              "Timeout da atividade de Íris após "
                  + properties.getCodexTimeout().toMinutes()
                  + " minutos.",
              prompt.fullPrompt(),
              prompt.agentPromptPart(),
              prompt.activityPromptPart(),
              readTokenUsage(processLog));
        }
        TokenUsage usage = readTokenUsage(processLog);
        String rawLog = Files.readString(processLog);
        if (process.exitValue() != 0) {
          throw new ExecutionException(
              "Codex encerrou com código " + process.exitValue() + ": " + rawLog,
              prompt.fullPrompt(),
              prompt.agentPromptPart(),
              prompt.activityPromptPart(),
              usage);
        }
        try {
          String rawResponse = Files.readString(answer);
          JsonNode result = json.readTree(rawResponse);
          validate(result, task, contract);
          session.success();
          log.info(
              "Resposta de Íris recebida. taskId={} processCode={} activityId={} status={}",
              task.get("taskId"),
              task.get("processCode"),
              task.get("activityId"),
              result.path("executionStatus").asText());
          return new Execution(
              result,
              rawResponse,
              prompt.fullPrompt(),
              prompt.agentPromptPart(),
              prompt.activityPromptPart(),
              usage);
        } catch (IOException | RuntimeException ex) {
          log.error(
              "Resposta inválida de Íris. taskId={} processCode={} activityId={}",
              task.get("taskId"),
              task.get("processCode"),
              task.get("activityId"),
              ex);
          throw new ExecutionException(
              "Resposta de Íris fora do contrato versionado.",
              prompt.fullPrompt(),
              prompt.agentPromptPart(),
              prompt.activityPromptPart(),
              usage,
              ex);
        }
      }
    } finally {
      Files.deleteIfExists(answer);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
      Files.deleteIfExists(mcp);
    }
  }

  /** Monta o comando Codex sem permissão de escrita ou aprovação interativa. */
  List<String> command(Path answer, Path schema, Path mcp) {
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
                "service_tier=\"" + configuredServiceTier() + "\"",
                "--config",
                "mcp_servers.iris_communication.command=\"node\"",
                "--config",
                "mcp_servers.iris_communication.args=[\"" + mcp.toAbsolutePath() + "\"]",
                "--config",
                "mcp_servers.iris_communication.env_vars=[\"MCP_MARKETING_HUB_URL\",\"MCP_TASK_ID\",\"MCP_SOURCE_REFERENCE\"]"));
    command.addAll(
        List.of(
            "--config", "model_reasoning_effort=\"" + properties.requiredReasoningEffort() + "\""));
    if (hasText(properties.getModel())) {
      command.addAll(List.of("--model", properties.getModel()));
    }
    return command;
  }

  /** Compõe constituição, instrução da atividade e contexto congelado do backend. */
  String prompt(Map<String, Object> task, Contract contract) throws IOException {
    return promptComposition(task, contract).fullPrompt();
  }

  /** Separa a constituição reutilizável da missão e do contexto específicos da atividade. */
  PromptComposition promptComposition(Map<String, Object> task, Contract contract)
      throws IOException {
    String agentPromptPart = read("prompts/iris/v1/behavioral-core.md");
    String activityPromptPart =
        read(contract.promptResource())
            + "\n\nCONTEXTO CONGELADO DA TAREFA:\n"
            + json.writeValueAsString(task);
    return new PromptComposition(
        agentPromptPart + "\n\n" + activityPromptPart, agentPromptPart, activityPromptPart);
  }

  /** Resolve somente atividades pertencentes ao domínio de comunicação. */
  static Contract contractFor(Map<String, Object> task) {
    String process = String.valueOf(task.getOrDefault("processCode", ""));
    String activity = String.valueOf(task.getOrDefault("activityId", ""));
    return switch (process + ":" + activity) {
      case "pde-communication-sales-journey:communicationContract" ->
          new Contract("COMMUNICATION_PACKAGE", "prompts/iris/v1/communication-package.md");
      case "creative-production-approval:nonAudiovisual" ->
          new Contract("NON_AUDIOVISUAL_PACKAGE", "prompts/iris/v1/non-audiovisual-package.md");
      case "landing-page-generation:select" ->
          new Contract("LANDING_EVIDENCE", "prompts/iris/v1/landing-evidence.md");
      case "landing-page-generation:strategy" ->
          new Contract("LANDING_STRATEGY", "prompts/iris/v1/landing-strategy.md");
      case "landing-page-generation:compose" ->
          new Contract("LANDING_COMPOSITION", "prompts/iris/v1/landing-composition.md");
      case "landing-page-generation:html" ->
          new Contract("LANDING_HTML", "prompts/iris/v1/landing-html.md");
      default -> throw new IllegalArgumentException("Atividade não pertence ao contrato de Íris.");
    };
  }

  /** Confirma identidade, fronteira, alternativas, guardrails e artefato esperado. */
  static void validate(JsonNode result, Map<String, Object> task, Contract contract) {
    String expectedSource = String.valueOf(task.getOrDefault("sourceReference", ""));
    String expectedActivity = String.valueOf(task.getOrDefault("activityId", ""));
    JsonNode guardrails = result.path("guardrails");
    if (!"IRIS_COMMUNICATION_V1".equals(result.path("contractVersion").asText())
        || !List.of("COMPLETED", "BLOCKED").contains(result.path("executionStatus").asText())
        || !expectedSource.equals(result.path("sourceReference").asText())
        || !expectedActivity.equals(result.path("activityId").asText())
        || !contract.outputType().equals(result.path("outputType").asText())
        || result.path("alternatives").size() != 3
        || result.path("chosenAlternative").asText().isBlank()
        || !guardrails.path("strategyPreserved").asBoolean()
        || !guardrails.path("productPreserved").asBoolean()
        || !guardrails.path("pricePreserved").asBoolean()
        || !guardrails.path("noFabricatedProof").asBoolean()
        || !guardrails.path("noPublication").asBoolean()
        || !guardrails.path("noExternalSpend").asBoolean()
        || result.path("expectedMetric").asText().isBlank()
        || result.path("continueCriteria").asText().isBlank()
        || result.path("adjustCriteria").asText().isBlank()
        || result.path("stopCriteria").asText().isBlank()) {
      throw new IllegalArgumentException("Saída de Íris incompleta ou fora da fronteira.");
    }
    JsonNode strategicReference = result.path("strategicContractReference");
    String expectedHash = strategicHash(task);
    if (!expectedHash.equals(strategicReference.path("contentHash").asText())
        || !strategicReference.path("preserved").asBoolean()) {
      throw new IllegalArgumentException("Íris não preservou o contrato estratégico de Atena.");
    }
    JsonNode output = result.path("functionalOutput");
    if ("COMPLETED".equals(result.path("executionStatus").asText())) {
      if (output.isMissingNode() || output.isEmpty() || result.path("evidenceGaps").size() > 0) {
        throw new IllegalArgumentException(
            "Íris não pode concluir com saída vazia ou prova ausente.");
      }
      validateActivityOutput(output, contract.outputType());
      validateResearchIntelligence(task, output);
    }
  }

  /** Confirma que a seleção de evidências registra somente cartões entregues na versão corrente. */
  private static void validateResearchIntelligence(
      Map<String, Object> task, JsonNode functionalOutput) {
    if (task.get("researchIntelligence") == null) return;
    JsonNode intelligence = new ObjectMapper().valueToTree(task.get("researchIntelligence"));
    String contractVersion = intelligence.path("contractVersion").asText();
    List<String> researchEvidence = new ArrayList<>();
    for (JsonNode evidence : functionalOutput.path("evidenceSelection")) {
      String reference = evidence.path("reference").asText();
      if (!reference.startsWith("RI1-")) continue;
      if (!contractVersion.equals(evidence.path("version").asText())) {
        throw new IllegalArgumentException(
            "Íris registrou cartão de pesquisa com versão de contrato divergente.");
      }
      researchEvidence.add(
          reference + " " + evidence.path("purpose").asText() + " " + contractVersion);
    }
    ResearchIntelligenceUsageValidator.validate(
        task, "communication-director", researchEvidence, true);
  }

  /** Bloqueia contratos ausentes antes de consumir modelo ou criar uma saída incompleta. */
  static void validateInput(Map<String, Object> task) {
    try {
      JsonNode context =
          new ObjectMapper()
              .readTree(String.valueOf(task.getOrDefault("processContextJson", "{}")));
      JsonNode strategy = context.path("marketStrategicContract");
      JsonNode communication = context.path("communicationMaterializationContext");
      if (!"AVAILABLE".equals(strategy.path("availability").asText())
          || !strategy.path("contentHash").asText().matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException(
            "Íris exige Contrato Estratégico de Mercado íntegro antes da execução.");
      }
      if (!"AVAILABLE".equals(communication.path("availability").asText())
          || !"READY".equals(communication.path("inputReadiness").asText())) {
        String missing = missingPredecessors(communication);
        throw new IllegalArgumentException(
            missing.isBlank()
                ? "Íris exige economia, PDE e provas predecessoras concluídas antes da execução."
                : "Íris aguarda estes predecessores: " + missing + ".");
      }
      String processCode = String.valueOf(task.getOrDefault("processCode", ""));
      if ("landing-page-generation".equals(processCode)
          && communication.path("approvedLandingAssets").isEmpty()) {
        throw new IllegalArgumentException(
            "Íris exige prova visual aprovada e rastreável antes de materializar a landing.");
      }
    } catch (IOException ex) {
      log.error(
          "Contexto congelado inválido para Íris. taskId={} sourceReference={}",
          task.get("taskId"),
          task.get("sourceReference"),
          ex);
      throw new IllegalArgumentException("Contexto congelado de Íris não contém JSON válido.", ex);
    }
  }

  /** Expõe no bloqueio os contratos exatos que o backend marcou como ausentes. */
  private static String missingPredecessors(JsonNode communication) {
    if (!communication.path("missingRequiredPredecessors").isArray()) return "";
    List<String> missing = new ArrayList<>();
    communication
        .path("missingRequiredPredecessors")
        .forEach(
            value -> {
              if (!value.asText().isBlank()) missing.add(value.asText());
            });
    return String.join("; ", missing);
  }

  /** Valida a materialização mínima específica de cada atividade. */
  private static void validateActivityOutput(JsonNode output, String outputType) {
    if ("COMMUNICATION_PACKAGE".equals(outputType)
        && (output.path("messageStrategy").asText().isBlank()
            || output.path("channelBriefings").isEmpty())) {
      throw new IllegalArgumentException("Pacote de comunicação de Íris incompleto.");
    }
    if ("NON_AUDIOVISUAL_PACKAGE".equals(outputType)
        && (output.path("copy").path("headline").asText().isBlank()
            || output.path("staticAssets").isEmpty())) {
      throw new IllegalArgumentException("Pacote não audiovisual de Íris incompleto.");
    }
    if ("LANDING_EVIDENCE".equals(outputType) && output.path("evidenceSelection").isEmpty()) {
      throw new IllegalArgumentException("Landing sem seleção de provas reais.");
    }
    if ("LANDING_STRATEGY".equals(outputType)
        && output.path("conversionArchitecture").asText().isBlank()) {
      throw new IllegalArgumentException("Landing sem arquitetura de conversão.");
    }
    if ("LANDING_COMPOSITION".equals(outputType)
        && output.path("visualComposition").asText().isBlank()) {
      throw new IllegalArgumentException("Landing sem composição visual.");
    }
    if ("LANDING_HTML".equals(outputType)) {
      String html = output.path("landingHtml").asText();
      if (html.length() < 500
          || !html.toLowerCase(java.util.Locale.ROOT).contains("<html")
          || !html.toLowerCase(java.util.Locale.ROOT).contains("</html>")) {
        throw new IllegalArgumentException("Íris deve entregar o HTML integral da landing.");
      }
    }
  }

  /** Extrai o hash estratégico congelado do contexto BPM. */
  private static String strategicHash(Map<String, Object> task) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode context =
          mapper.readTree(String.valueOf(task.getOrDefault("processContextJson", "{}")));
      String hash = context.path("marketStrategicContract").path("contentHash").asText();
      if (!hash.matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException("Íris exige contrato estratégico íntegro de Atena.");
      }
      return hash;
    } catch (IOException ex) {
      log.error(
          "Contrato estratégico inválido para Íris. taskId={} sourceReference={}",
          task.get("taskId"),
          task.get("sourceReference"),
          ex);
      throw new IllegalArgumentException(
          "Contexto estratégico de Íris não contém JSON válido.", ex);
    }
  }

  /** Lê o último total cumulativo de tokens informado pela telemetria do Codex. */
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
          event = json.readTree(line);
        } catch (IOException ex) {
          log.debug("Linha não JSON ignorada na telemetria de Íris.", ex);
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
      log.warn("Falha ao ler telemetria de tokens de Íris. output={}", processLog, ex);
      return TokenUsage.empty();
    }
    return informed ? new TokenUsage(input, cached, output, true) : TokenUsage.empty();
  }

  /** Lê uma das grafias aceitas de um contador sem inventar uso ausente. */
  private long token(JsonNode usage, String snakeCase, String camelCase) {
    JsonNode value = usage.has(snakeCase) ? usage.path(snakeCase) : usage.path(camelCase);
    return value.canConvertToLong() ? Math.max(0, value.asLong()) : 0;
  }

  /** Materializa um recurso versionado em arquivo temporário para o CLI. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path target = Files.createTempFile("iris-contract-", suffix);
    try (var input = new ClassPathResource(resource).getInputStream()) {
      Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    return target;
  }

  /** Lê integralmente um recurso textual versionado. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Retorna o modelo efetivamente declarado na auditoria. */
  String modelCode() {
    return hasText(properties.getModel()) ? properties.getModel() : "codex-default";
  }

  /** Confirma que a auditoria não solicite um tier omitido pelo catálogo atual do Codex. */
  String configuredServiceTier() {
    String configured = properties.getServiceTier();
    if (configured != null && "default".equalsIgnoreCase(configured.trim())) {
      return "default";
    }
    throw new IllegalArgumentException(
        "Íris só pode usar o tier default enquanto Flex não for anunciado pelo catálogo Codex.");
  }

  /** Verifica se uma configuração opcional possui conteúdo. */
  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Representa a especialização de prompt e saída de uma atividade suportada. */
  record Contract(String outputType, String promptResource) {}

  /** Representa as duas partes e a composição exata enviada ao modelo. */
  record PromptComposition(String fullPrompt, String agentPromptPart, String activityPromptPart) {}

  /** Preserva o resultado bruto, o prompt exato e o consumo medido. */
  record Execution(
      JsonNode result,
      String rawResponse,
      String promptSent,
      String agentPromptPart,
      String activityPromptPart,
      TokenUsage usage) {}

  /** Representa os contadores cumulativos realmente observados. */
  record TokenUsage(long inputTokens, long cachedInputTokens, long outputTokens, boolean informed) {
    /** Representa telemetria não informada pelo runtime. */
    static TokenUsage empty() {
      return new TokenUsage(0, 0, 0, false);
    }
  }

  /** Preserva prompt e tokens quando a execução não produz uma resposta válida. */
  static class ExecutionException extends RuntimeException {
    private final String promptSent;
    private final String agentPromptPart;
    private final String activityPromptPart;
    private final TokenUsage usage;

    /** Cria uma falha operacional sem causa aninhada. */
    ExecutionException(
        String message,
        String promptSent,
        String agentPromptPart,
        String activityPromptPart,
        TokenUsage usage) {
      super(message);
      this.promptSent = promptSent;
      this.agentPromptPart = agentPromptPart;
      this.activityPromptPart = activityPromptPart;
      this.usage = usage;
    }

    /** Cria uma falha de contrato preservando a exceção original. */
    ExecutionException(
        String message,
        String promptSent,
        String agentPromptPart,
        String activityPromptPart,
        TokenUsage usage,
        Throwable cause) {
      super(message, cause);
      this.promptSent = promptSent;
      this.agentPromptPart = agentPromptPart;
      this.activityPromptPart = activityPromptPart;
      this.usage = usage;
    }

    /** Retorna o prompt enviado antes da falha. */
    String promptSent() {
      return promptSent;
    }

    /** Retorna a constituição de Íris usada antes da falha. */
    String agentPromptPart() {
      return agentPromptPart;
    }

    /** Retorna a missão específica usada antes da falha. */
    String activityPromptPart() {
      return activityPromptPart;
    }

    /** Retorna a telemetria observada antes da falha. */
    TokenUsage usage() {
      return usage;
    }
  }
}
