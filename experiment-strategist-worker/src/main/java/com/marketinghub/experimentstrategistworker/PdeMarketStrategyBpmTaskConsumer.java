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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: executar a estratégia de mercado BPM atribuída exclusivamente a Atena. */
@Component
public class PdeMarketStrategyBpmTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(PdeMarketStrategyBpmTaskConsumer.class);
  private static final String AGENT_KEY = "experiment-strategist";
  private static final String PROCESS_CODE = "pde-commercial-plan-offer";
  private static final String ACTIVITY_ID = "marketStrategy";
  private static final String PROMPT = "prompts/pde-commercial-plan/v7/market-strategy.md";
  private static final String SCHEMA = "prompts/pde-commercial-plan/v7/market-strategy-schema.json";
  private static final String READY_FOR_PRIVATE_VALIDATION = "READY_FOR_PRIVATE_VALIDATION";
  private static final String INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
  private static final List<String> REQUIRED_PRIVATE_SIGNALS =
      List.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");
  private final RestClient backend;
  private final WorkerProperties properties;
  private final ObjectMapper objectMapper;
  private final AutomaticExecutionControl automaticExecution;

  /** Configura fila canônica, sandbox Codex e controle operacional PLAY/STOP. */
  public PdeMarketStrategyBpmTaskConsumer(
      WorkerProperties properties,
      ObjectMapper objectMapper,
      AutomaticExecutionControl automaticExecution) {
    this.backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.automaticExecution = automaticExecution;
  }

  /** Reserva e processa no máximo um dossiê liberado pelo backend. */
  @Scheduled(cron = "40 */1 * * * *")
  public void processOne() {
    if (!automaticExecution.allowsAutomaticExecution()) return;
    Map<String, Object> task = null;
    Execution execution = null;
    try {
      task = claim();
      if (task == null) return;
      execution = execute(task);
      validate(execution.result(), sourceReference(task));
      if ("APPROVE".equals(execution.result().path("decision").asText())) {
        complete(task, execution);
      } else {
        block(task, execution);
      }
    } catch (Exception ex) {
      log.error(
          "Falha na estratégia BPM de Atena. taskId={} sourceReference={}",
          taskId(task),
          sourceReference(task),
          ex);
      fail(task, execution, ex);
    }
  }

  /** Reserva somente a atividade oficial de estratégia após predecessoras aprovadas. */
  private Map<String, Object> claim() {
    List<Map<String, Object>> pending =
        backend
            .get()
            .uri(
                "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode={process}&activityId={activity}",
                AGENT_KEY,
                PROCESS_CODE,
                ACTIVITY_ID)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return pending == null || pending.isEmpty() ? null : pending.getFirst();
  }

  /** Executa o prompt versionado em leitura somente e preserva a resposta bruta. */
  Execution execute(Map<String, Object> task) throws IOException, InterruptedException {
    Path output = Files.createTempFile("atena-market-strategy-", ".json");
    Path processLog = Files.createTempFile("atena-market-strategy-", ".jsonl");
    Path schema = materialize(SCHEMA, ".json");
    PromptComposition prompt = prompt(task);
    try {
      Process process =
          new ProcessBuilder(command(output, schema))
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      process.getOutputStream().write(prompt.full().getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
        terminateTree(process);
        throw new IllegalStateException("Timeout da estratégia de mercado de Atena.");
      }
      TokenUsage usage = readTokenUsage(processLog);
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
            "Codex de Atena encerrou com falha: " + Files.readString(processLog));
      }
      String raw = Files.readString(output);
      return new Execution(objectMapper.readTree(raw), usage, prompt, raw);
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o comando imutável com sandbox somente leitura e saída estruturada. */
  private List<String> command(Path output, Path schema) {
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
                "--json",
                "--color",
                "never",
                "--config",
                "approval_policy=\"never\"",
                "--config",
                "service_tier=\"default\"",
                "--config",
                "model_reasoning_effort=\"" + properties.requiredReasoningEffort() + "\""));
    if (properties.getModel() != null && !properties.getModel().isBlank()) {
      command.add("--model");
      command.add(properties.getModel());
    }
    return command;
  }

  /** Compõe identidade permanente e missão específica sem hardcode de contrato na classe. */
  private PromptComposition prompt(Map<String, Object> task) throws IOException {
    String agent = read("prompts/experiment-strategist/v1/agent-core.md");
    String activity =
        read(PROMPT).replace("{{TASK_CONTEXT}}", objectMapper.writeValueAsString(task));
    return new PromptComposition(agent + "\n\n" + activity, agent, activity);
  }

  /** Persiste parecer, evidência, auditoria e tokens antes de liberar Plutus. */
  private void complete(Map<String, Object> task, Execution execution) throws IOException {
    backend
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/result",
            AGENT_KEY,
            taskId(task))
        .body(callback(task, execution))
        .retrieve()
        .toBodilessEntity();
  }

  /** Bloqueia a cadeia quando a evidência não sustenta estratégia operacional. */
  private void block(Map<String, Object> task, Execution execution) throws IOException {
    Map<String, Object> body = callback(task, execution);
    body.put(
        "error", "Atena bloqueou a estratégia: " + execution.result().path("rationale").asText());
    body.put(
        "blockerGuidance",
        Map.of(
            "category",
            "MISSING_EVIDENCE",
            "recommendedAction",
            firstRequiredChange(execution.result()),
            "helpLinks",
            List.of(taskLink())));
    backend
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
            AGENT_KEY,
            taskId(task))
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  /** Registra falha técnica com contexto completo sem deixar tarefa invisível em andamento. */
  private void fail(Map<String, Object> task, Execution execution, Exception ex) {
    if (task == null) return;
    try {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("error", ex.toString());
      body.put("evidenceJson", evidence(task));
      if (execution != null) {
        body.put("resultJson", execution.raw());
        body.put("executionAudit", audit(execution.prompt()));
        putUsage(body, execution.usage());
      }
      body.put(
          "blockerGuidance",
          Map.of(
              "category",
              "TECHNICAL_FAILURE",
              "recommendedAction",
              "Corrija a falha técnica registrada e reinicie a atividade de Atena.",
              "helpLinks",
              List.of(taskLink())));
      backend
          .post()
          .uri(
              "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
              AGENT_KEY,
              taskId(task))
          .body(body)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception callbackEx) {
      log.error("Falha ao registrar bloqueio de Atena. taskId={}", taskId(task), callbackEx);
    }
  }

  /** Monta o callback comum sem omitir o envelope de execução do modelo. */
  private Map<String, Object> callback(Map<String, Object> task, Execution execution)
      throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("resultJson", execution.raw());
    body.put("evidenceJson", evidence(task));
    body.put("executionAudit", audit(execution.prompt()));
    putUsage(body, execution.usage());
    return body;
  }

  /** Declara a execução real e a exceção operacional ao Flex do runtime Codex OAuth. */
  private Map<String, Object> audit(PromptComposition prompt) {
    Map<String, Object> audit = new LinkedHashMap<>();
    audit.put("executionMode", "MODEL");
    audit.put("modelCode", modelCode());
    audit.put("reasoningEffort", properties.requiredReasoningEffort());
    audit.put("promptSent", prompt.full());
    audit.put("agentPromptPart", prompt.agent());
    audit.put("activityPromptPart", prompt.activity());
    audit.put("accessedUrls", List.of());
    return audit;
  }

  /** Preserva a origem e confirma que a atividade não publicou nem movimentou orçamento. */
  private String evidence(Map<String, Object> task) throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "agent",
            "Atena",
            "promptVersion",
            "pde-commercial-plan-v7",
            "sourceReference",
            sourceReference(task),
            "processCode",
            PROCESS_CODE,
            "activityId",
            ACTIVITY_ID,
            "accessMode",
            "READ_ONLY",
            "externalSideEffects",
            false,
            "serviceTierException",
            "Codex OAuth não anuncia Flex para este harness; execução auditada em default."));
  }

  /** Rejeita estratégia sem comparação, contrato versionado ou justificativa. */
  static void validate(JsonNode result) {
    validate(result, "product-discovery-cycle:test");
  }

  /** Valida seleção factual e o plano privado sem antecipar prontidão comercial. */
  static void validate(JsonNode result, String sourceReference) {
    JsonNode contract = result.path("marketStrategicContract");
    JsonNode validationPlan = contract.path("privateValidationPlan");
    String decision = result.path("decision").asText();
    String status = contract.path("status").asText();
    if (!List.of("APPROVE", "ADJUST", "REJECT").contains(result.path("decision").asText())
        || result.path("alternatives").size() != 3
        || result.path("selectedAlternative").asText().isBlank()
        || !contract.isObject()
        || !"MARKET_STRATEGY_V3".equals(contract.path("contractVersion").asText())
        || !List.of(READY_FOR_PRIVATE_VALIDATION, INSUFFICIENT_EVIDENCE).contains(status)
        || result.path("rationale").asText().isBlank()) {
      throw new IllegalArgumentException("Estratégia PDE fora do contrato versionado de Atena.");
    }
    if (("APPROVE".equals(decision) && !READY_FOR_PRIVATE_VALIDATION.equals(status))
        || (!"APPROVE".equals(decision) && !INSUFFICIENT_EVIDENCE.equals(status))) {
      throw new IllegalArgumentException(
          "A decisão de Atena não corresponde à prontidão para validação privada.");
    }
    if (sourceReference != null
        && sourceReference.startsWith("product-discovery-cycle:")
        && "APPROVE".equals(decision)
        && (!result.path("selectedDossierId").canConvertToLong()
            || !result.path("selectedOpportunityId").canConvertToLong())) {
      throw new IllegalArgumentException("Atena aprovou sem selecionar uma candidata factual.");
    }
    if ("APPROVE".equals(decision)
        && (!validationPlan.isObject()
            || validationPlan.path("minimumIndependentReadings").asInt(0) != 2
            || validationPlan.path("minimumEligibleParticipantsPerReading").asInt(0) != 1
            || !containsAllPrivateSignals(validationPlan.path("requiredSignals"))
            || !unitRate(validationPlan, "minimumExperienceStartRate")
            || !unitRate(validationPlan, "minimumValueMomentRate")
            || !unitRate(validationPlan, "minimumReadyResultUseRate")
            || !unitRate(validationPlan, "minimumPrototypePreferenceRate")
            || !unitRate(validationPlan, "minimumCheckoutStartRate")
            || validationPlan.path("sourceMaxAgeDays").asInt(0) < 1
            || validationPlan.path("sourceMaxAgeDays").asInt(0) > 90
            || validationPlan.path("prototypeObjective").asText().isBlank()
            || !completePurchaseScene(validationPlan.path("purchaseScene"))
            || !canonicalHumanValueDelivery(validationPlan.path("humanValueDelivery"))
            || validationPlan.path("strongestFreeAlternative").asText().isBlank()
            || validationPlan.path("prototypeAdvantage").asText().isBlank()
            || validationPlan.path("publicationBoundary").asText().isBlank()
            || (validationPlan.path("sourceRefreshRequired").asBoolean(false)
                && validationPlan.path("sourceRefreshAction").asText().isBlank()))) {
      throw new IllegalArgumentException(
          "Atena aprovou sem um plano completo de duas leituras privadas.");
    }
  }

  /** Confirma os cinco sinais canônicos sem aceitar um subconjunto conveniente. */
  private static boolean containsAllPrivateSignals(JsonNode signals) {
    if (!signals.isArray() || signals.size() != REQUIRED_PRIVATE_SIGNALS.size()) return false;
    List<String> values = new ArrayList<>();
    signals.forEach(item -> values.add(item.asText()));
    return values.stream().distinct().count() == REQUIRED_PRIVATE_SIGNALS.size()
        && values.containsAll(REQUIRED_PRIVATE_SIGNALS);
  }

  /** Exige uma taxa integral para que cada leitura individual prove todos os sinais. */
  private static boolean unitRate(JsonNode plan, String field) {
    return plan.path(field).isNumber() && Double.compare(plan.path(field).asDouble(), 1d) == 0;
  }

  /** Confirma os seis fatos mínimos da cena de compra sem aceitar texto agregado. */
  private static boolean completePurchaseScene(JsonNode scene) {
    return hasText(scene, "trigger")
        && hasText(scene, "deadline")
        && hasText(scene, "costOfError")
        && hasText(scene, "budgetEvidence")
        && hasText(scene, "failedAttempt")
        && hasText(scene, "currentPaidBehavior");
  }

  /** Confirma valor humano, saída pronta e baixo esforço no plano de Atena. */
  private static boolean canonicalHumanValueDelivery(JsonNode delivery) {
    return delivery.isObject()
        && delivery.path("territories").isArray()
        && !delivery.path("territories").isEmpty()
        && delivery.path("evidenceSourceIds").isArray()
        && delivery.path("evidenceSourceIds").size() >= 2
        && delivery.path("evidencePathways").isArray()
        && delivery.path("evidencePathways").size() >= 2
        && hasText(delivery, "desiredTransformation")
        && hasText(delivery, "readyMadeOutcome")
        && hasText(delivery, "minimumCustomerInput")
        && hasText(delivery, "automationBoundary")
        && !delivery.path("requiresPromptEngineering").asBoolean(true)
        && !delivery.path("requiresManualAssembly").asBoolean(true)
        && delivery.path("usableWithoutAiKnowledge").asBoolean(false)
        && delivery.path("customerStepsToValue").asInt(0) >= 1
        && delivery.path("customerStepsToValue").asInt(0) <= 5
        && delivery.path("timeToUsableResultMinutes").asInt(0) >= 1
        && delivery.path("timeToUsableResultMinutes").asInt(0) <= 10;
  }

  /** Verifica texto obrigatório em um objeto estruturado. */
  private static boolean hasText(JsonNode node, String field) {
    return node.isObject() && !node.path(field).asText("").trim().isBlank();
  }

  /** Lê o último total cumulativo de tokens realmente informado. */
  private TokenUsage readTokenUsage(Path processLog) {
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
          log.debug("Linha não JSON ignorada na telemetria de Atena.", ex);
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
      log.warn("Falha ao ler tokens de Atena. output={}", processLog, ex);
    }
    return new TokenUsage(input, cached, output, informed);
  }

  /** Lê um contador oficial ou seu alias sem inventar consumo ausente. */
  private long token(JsonNode usage, String official, String alias) {
    JsonNode value = usage.has(official) ? usage.path(official) : usage.path(alias);
    return value.canConvertToLong() ? Math.max(0, value.asLong()) : 0;
  }

  /** Acrescenta consumo apenas quando o runtime forneceu os três contadores. */
  private void putUsage(Map<String, Object> body, TokenUsage usage) {
    if (!usage.informed()) return;
    body.put(
        "modelUsages",
        List.of(
            Map.of(
                "modelCode",
                modelCode(),
                "serviceTier",
                "STANDARD",
                "inputTokens",
                usage.input(),
                "cachedInputTokens",
                usage.cached(),
                "outputTokens",
                usage.output())));
  }

  /** Encerra descendentes antes do processo principal para não deixar Codex órfão. */
  private void terminateTree(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }

  /** Materializa um schema versionado em arquivo temporário. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("atena-pde-resource-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Retorna a primeira correção pedida ou uma orientação segura de nova evidência. */
  private String firstRequiredChange(JsonNode result) {
    JsonNode changes = result.path("requiredChanges");
    return changes.isArray() && !changes.isEmpty()
        ? changes.get(0).asText()
        : "Aprofunde a evidência factual indicada por Atena e reinicie a atividade.";
  }

  /** Cria o atalho interno comum para a auditoria da tarefa. */
  private Map<String, String> taskLink() {
    return Map.of("label", "Abrir tarefas dos agentes", "url", "/agent-tasks");
  }

  /** Retorna o modelo efetivo ou o identificador do catálogo padrão. */
  private String modelCode() {
    return properties.getModel() == null || properties.getModel().isBlank()
        ? "codex-default"
        : properties.getModel();
  }

  /** Extrai o identificador persistido da tarefa. */
  private static long taskId(Map<String, Object> task) {
    return task == null ? -1L : ((Number) task.get("taskId")).longValue();
  }

  /** Extrai a referência de origem sem inferir produto ou experimento. */
  private static String sourceReference(Map<String, Object> task) {
    return task == null || task.get("sourceReference") == null
        ? "não informada"
        : task.get("sourceReference").toString();
  }

  /** Preserva resultado, consumo, prompts e resposta bruta da mesma execução. */
  record Execution(JsonNode result, TokenUsage usage, PromptComposition prompt, String raw) {}

  /** Representa a identidade, atividade e composição exata enviada ao modelo. */
  record PromptComposition(String full, String agent, String activity) {}

  /** Representa somente contadores efetivamente observados no runtime. */
  record TokenUsage(long input, long cached, long output, boolean informed) {}
}
