package com.marketinghub.metaadapproverworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: executar os gates comerciais BPM atribuídos à Têmis. */
@Component
@ConditionalOnProperty(
    name = "meta-ad-approver.execution-role",
    havingValue = "review",
    matchIfMissing = true)
public class CommercialBpmTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(CommercialBpmTaskConsumer.class);
  private static final String AGENT_KEY = "meta-ad-approver";
  private static final String REQUESTED_SERVICE_TIER = "flex";
  private static final String EFFECTIVE_SERVICE_TIER = "STANDARD";
  private static final String SERVICE_TIER_EXCEPTION =
      "O catálogo do Codex não anuncia Flex para gpt-5.6-sol; a CLI omite o tier solicitado e usa o tier padrão.";
  private static final int MAX_PROMPT_CHARACTERS = 900_000;
  private static final List<BpmContract> CONTRACTS =
      List.of(
          new BpmContract("pde-commercial-homologation-activation", "commercialIntegrityReview"),
          new BpmContract("creative-production-approval", "commercial"),
          new BpmContract("landing-page-generation", "commercial"),
          new BpmContract("pde-construction-approval", "commercialIntegrityReview"));
  private final RestClient backend;
  private final ObjectMapper json;
  private final String codex;
  private final String model;
  private final String reasoningEffort;
  private final String repositoryPath;
  private final PdeReviewArtifactLoader pdeArtifactLoader;
  private final CodexProcessSupervisor processSupervisor;
  private final int maxModelAttempts;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Configura a fila canônica e a sandbox independente de Têmis. */
  @Autowired
  public CommercialBpmTaskConsumer(
      MetaAdApproverProperties properties,
      @Value("${CODEX_COMMAND:codex}") String codex,
      @Value("${CODEX_MODEL:gpt-5.6-sol}") String model,
      @Value("${MARKETING_HUB_REPOSITORY:/workspace/marketing-hub}") String repositoryPath,
      @Value("${TEMIS_COMMERCIAL_EVIDENCE_PATH:}") String commercialEvidencePath,
      ObjectMapper json,
      CodexProcessSupervisor processSupervisor,
      @Value("${meta-ad-approver.commercial-model-max-attempts:2}") int maxModelAttempts) {
    this.backend = BackendRestClientFactory.create(properties);
    this.codex = codex;
    this.model = model;
    this.reasoningEffort = properties.requiredReasoningEffort();
    this.repositoryPath = repositoryPath;
    this.pdeArtifactLoader =
        new PdeReviewArtifactLoader(
            commercialEvidencePath == null || commercialEvidencePath.isBlank()
                ? repositoryPath
                : commercialEvidencePath);
    this.json = json;
    this.processSupervisor = processSupervisor;
    this.maxModelAttempts = Math.max(1, maxModelAttempts);
  }

  /** Mantém testes de contrato isolados sem iniciar o contexto Spring completo. */
  CommercialBpmTaskConsumer(
      MetaAdApproverProperties properties,
      String codex,
      String model,
      String repositoryPath,
      String commercialEvidencePath,
      ObjectMapper json) {
    this(
        properties,
        codex,
        model,
        repositoryPath,
        commercialEvidencePath,
        json,
        new CodexProcessSupervisor(
            Duration.ofMinutes(10), Duration.ofMinutes(40), Duration.ofSeconds(15)),
        2);
  }

  /** Reserva em PLAY e revisa a atividade com integridade comercial e pesquisa rastreável. */
  @Scheduled(cron = "45 */1 * * * *")
  public void processOne() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
    Map<String, Object> task = null;
    BpmExecution execution = null;
    try {
      task = claimNext();
      if (task == null) return;
      execution = execute(task);
      JsonNode result = execution.result();
      if (isAgentValidationTask(task)) {
        validateAgentValidation(result, task);
      } else {
        validate(result, processCode(task));
        ResearchIntelligenceUsageValidator.validate(
            task,
            AGENT_KEY,
            jsonTextValues(result.path("evidence")),
            !"BLOCKED".equals(result.path("decision").asText()));
      }
      if ("APPROVED".equals(result.path("decision").asText())) report(task, execution);
      else block(task, execution);
    } catch (Exception ex) {
      log.error("Falha no gate comercial BPM de Têmis. taskId={}", taskId(task), ex);
      fail(task, ex, execution);
    }
  }

  /** Converte um array JSON textual em evidências usadas pelo gate determinístico. */
  private static List<String> jsonTextValues(JsonNode values) {
    List<String> result = new ArrayList<>();
    values.forEach(value -> result.add(value.asText()));
    return result;
  }

  /** Reserva somente gates independentes de integridade comercial em ordem explícita. */
  private Map<String, Object> claimNext() {
    for (BpmContract contract : CONTRACTS) {
      List<Map<String, Object>> pending =
          backend
              .get()
              .uri(
                  "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode={processCode}&activityId={activityId}",
                  AGENT_KEY,
                  contract.processCode(),
                  contract.activityId())
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      if (pending != null && !pending.isEmpty()) return pending.get(0);
    }
    return null;
  }

  /** Confirma que o polling conhece uma atividade publicada do contrato BPM. */
  static boolean supportsContract(String processCode, String activityId) {
    return CONTRACTS.stream()
        .anyMatch(
            contract ->
                contract.processCode().equals(processCode)
                    && contract.activityId().equals(activityId));
  }

  /** Executa o prompt versionado e valida coerência, compliance e prontidão comercial. */
  BpmExecution execute(Map<String, Object> task) throws IOException, InterruptedException {
    PromptComposition prompt = promptComposition(task);
    validatePromptSize(prompt.fullPrompt());
    Path schema = materialize(schemaResourceFor(task), ".json");
    TokenUsage accumulatedUsage = TokenUsage.empty();
    try {
      for (int attempt = 1; attempt <= maxModelAttempts; attempt++) {
        AttemptExecution execution = executeAttempt(task, schema, prompt);
        accumulatedUsage = accumulatedUsage.plus(execution.usage());
        if (execution.outcome() == CodexProcessSupervisor.WaitOutcome.COMPLETED) {
          return new BpmExecution(
              execution.result(),
              accumulatedUsage,
              prompt.fullPrompt(),
              prompt.agentPromptPart(),
              prompt.activityPromptPart());
        }
        if (execution.outcome() == CodexProcessSupervisor.WaitOutcome.INACTIVITY_TIMEOUT
            && attempt < maxModelAttempts) {
          log.warn(
              "Chamada de Têmis sem progresso será repetida. taskId={} attempt={} maxAttempts={} inactivityMinutes={}",
              taskId(task),
              attempt,
              maxModelAttempts,
              processSupervisor.inactivityTimeout().toMinutes());
          continue;
        }
        throw new BpmExecutionException(
            timeoutMessage(execution.outcome(), attempt),
            accumulatedUsage,
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart());
      }
      throw new IllegalStateException("Têmis não executou nenhuma tentativa do modelo.");
    } finally {
      Files.deleteIfExists(schema);
    }
  }

  /** Executa uma tentativa isolada e devolve conclusão ou motivo técnico de interrupção. */
  private AttemptExecution executeAttempt(
      Map<String, Object> task, Path schema, PromptComposition prompt)
      throws IOException, InterruptedException {
    Path output = Files.createTempFile("temis-bpm-result-", ".json");
    Path processLog = Files.createTempFile("temis-bpm-process-", ".log");
    Process process = null;
    try {
      process =
          new ProcessBuilder(command(output, schema))
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      process.getOutputStream().write(prompt.fullPrompt().getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      CodexProcessSupervisor.WaitOutcome outcome =
          processSupervisor.awaitCompletion(process, processLog);
      TokenUsage usage = readTokenUsage(json, processLog);
      if (outcome != CodexProcessSupervisor.WaitOutcome.COMPLETED) {
        return new AttemptExecution(null, usage, outcome);
      }
      if (process.exitValue() != 0) {
        throw new BpmExecutionException(
            "Codex encerrou com falha: " + Files.readString(processLog),
            usage,
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart());
      }
      try {
        return new AttemptExecution(
            json.readTree(Files.readString(output)),
            usage,
            CodexProcessSupervisor.WaitOutcome.COMPLETED);
      } catch (IOException ex) {
        log.error(
            "Resposta inválida no gate BPM de Têmis. taskId={} output={}",
            taskId(task),
            output,
            ex);
        throw new BpmExecutionException(
            "Resposta de Têmis não contém JSON válido.",
            usage,
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart(),
            ex);
      }
    } finally {
      if (process != null && process.isAlive()) processSupervisor.terminateTree(process);
      Files.deleteIfExists(output);
      Files.deleteIfExists(processLog);
    }
  }

  /** Monta o comando imutável usado por cada tentativa de revisão comercial. */
  private List<String> command(Path output, Path schema) {
    List<String> command =
        new ArrayList<>(
            List.of(
                codex,
                "--search",
                "exec",
                "-c",
                "service_tier=\"" + REQUESTED_SERVICE_TIER + "\"",
                "--config",
                "model_reasoning_effort=\"" + reasoningEffort + "\"",
                "-",
                "--skip-git-repo-check",
                "--sandbox",
                "read-only",
                "--cd",
                repositoryPath,
                "--output-schema",
                schema.toString(),
                "--output-last-message",
                output.toString(),
                "--json",
                "--color",
                "never"));
    if (model != null && !model.isBlank()) command.addAll(List.of("--model", model));
    return List.copyOf(command);
  }

  /** Bloqueia localmente uma entrada sem margem antes que a CLI consuma uma tentativa do modelo. */
  static void validatePromptSize(String prompt) {
    if (prompt.length() > MAX_PROMPT_CHARACTERS) {
      throw new IllegalArgumentException(
          "Prompt comercial excede o limite preventivo de "
              + MAX_PROMPT_CHARACTERS
              + " caracteres: "
              + prompt.length());
    }
  }

  /** Expõe o teto preventivo somente para testes de contrato do executor. */
  static int promptCharacterLimit() {
    return MAX_PROMPT_CHARACTERS;
  }

  /** Explica se Têmis parou por inatividade repetida ou pelo teto absoluto da tentativa. */
  private String timeoutMessage(CodexProcessSupervisor.WaitOutcome outcome, int attempts) {
    if (outcome == CodexProcessSupervisor.WaitOutcome.INACTIVITY_TIMEOUT) {
      return "Timeout do gate BPM de Têmis após "
          + attempts
          + " tentativa(s) sem atividade por "
          + processSupervisor.inactivityTimeout().toMinutes()
          + " minutos.";
    }
    return "Teto absoluto do gate BPM de Têmis após "
        + processSupervisor.absoluteTimeout().toMinutes()
        + " minutos na tentativa "
        + attempts
        + ".";
  }

  /** Persiste a decisão auditável sem publicar landing, campanha ou experimento. */
  private void report(Map<String, Object> task, BpmExecution execution) throws IOException {
    Map<String, Object> body = new HashMap<>();
    body.put("resultJson", json.writeValueAsString(execution.result()));
    body.put("evidenceJson", evidence(task));
    putModelUsage(body, execution.usage());
    body.put(
        "executionAudit",
        executionAudit(
            execution.promptSent(), execution.agentPromptPart(), execution.activityPromptPart()));
    backend
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/result",
            AGENT_KEY,
            taskId(task))
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  /** Mantém o gate fechado diante de falha técnica ou parecer inválido. */
  private void fail(Map<String, Object> task, Exception ex, BpmExecution execution) {
    if (task == null) return;
    try {
      BpmExecutionException bpm = ex instanceof BpmExecutionException value ? value : null;
      TokenUsage usage = execution != null ? execution.usage() : bpm == null ? null : bpm.usage();
      String promptSent =
          execution != null ? execution.promptSent() : bpm == null ? null : bpm.promptSent();
      String agentPromptPart =
          execution != null
              ? execution.agentPromptPart()
              : bpm == null ? null : bpm.agentPromptPart();
      String activityPromptPart =
          execution != null
              ? execution.activityPromptPart()
              : bpm == null ? null : bpm.activityPromptPart();
      backend
          .post()
          .uri(
              "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
              AGENT_KEY,
              taskId(task))
          .body(
              failureBody(
                  task, ex.toString(), usage, promptSent, agentPromptPart, activityPromptPart))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception callbackEx) {
      log.error("Falha ao registrar bloqueio BPM de Têmis. taskId={}", taskId(task), callbackEx);
    }
  }

  /** Preserva o parecer funcional e mantém o processo fechado quando o gate reprova. */
  private void block(Map<String, Object> task, BpmExecution execution) throws IOException {
    JsonNode result = execution.result();
    String resultJson = json.writeValueAsString(result);
    Map<String, Object> body = new HashMap<>();
    String rationale =
        isAgentValidationTask(task)
            ? result.path("rootCause").asText()
            : result.path("commercialRationale").asText();
    body.put("error", "Têmis bloqueou o avanço: " + rationale);
    body.put("resultJson", resultJson);
    body.put("evidenceJson", evidence(task));
    putModelUsage(body, execution.usage());
    body.put(
        "executionAudit",
        executionAudit(
            execution.promptSent(), execution.agentPromptPart(), execution.activityPromptPart()));
    body.put("blockerGuidance", functionalGuidance(task, result));
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

  /** Resolve o contexto congelado e injeta os artefatos da revisão comercial correspondente. */
  String prompt(Map<String, Object> task) throws IOException {
    return promptComposition(task).fullPrompt();
  }

  /** Compõe o núcleo independente de Têmis com o gate e o contexto avaliados. */
  private PromptComposition promptComposition(Map<String, Object> task) throws IOException {
    Map<String, Object> promptContext = new HashMap<>(task);
    if ("pde-construction-approval".equals(processCode(task)) && !isPrivateValidationTask(task)) {
      promptContext.put("versionedArtifactEvidence", pdeArtifactLoader.load());
    } else if ("pde-commercial-homologation-activation".equals(processCode(task))) {
      promptContext.put(
          "versionedCommercialHomologationEvidence",
          pdeArtifactLoader.loadCommercialHomologationEvidence(task.get("taskTarget")));
    }
    String agentPromptPart = read("prompts/temis/v1/agent-core.md");
    String activityPromptPart =
        read(promptResourceFor(task))
            .replace("{{TASK_CONTEXT}}", json.writeValueAsString(promptContext));
    return new PromptComposition(
        agentPromptPart + "\n\n" + activityPromptPart, agentPromptPart, activityPromptPart);
  }

  /** Seleciona o prompt versionado específico do gate avaliado. */
  static String promptResourceFor(String processCode) {
    return switch (processCode) {
      case "pde-commercial-homologation-activation" ->
          "prompts/bpm/pde-commercial-homologation-independent-review.md";
      case "creative-production-approval" -> "prompts/bpm/creative-commercial-review.md";
      case "pde-construction-approval" -> "prompts/bpm/pde-private-validation-review-v2.md";
      default -> "prompts/bpm/landing-commercial-review.md";
    };
  }

  /** Seleciona o schema versionado específico do gate avaliado. */
  static String schemaResourceFor(String processCode) {
    return switch (processCode) {
      case "pde-commercial-homologation-activation" ->
          "prompts/bpm/pde-commercial-homologation-independent-review-schema.json";
      case "creative-production-approval" -> "prompts/bpm/creative-commercial-review-schema.json";
      case "pde-construction-approval" ->
          "prompts/bpm/pde-private-validation-review-v2-schema.json";
      default -> "prompts/bpm/landing-commercial-review-schema.json";
    };
  }

  /** Seleciona a auditoria multiagente sem substituir o contrato privado histórico. */
  private String promptResourceFor(Map<String, Object> task) {
    return isAgentValidationTask(task)
        ? "prompts/bpm/pde-agent-validation-review-v3.md"
        : promptResourceFor(processCode(task));
  }

  /** Seleciona o schema que proíbe alegações humanas na validação sintética. */
  private String schemaResourceFor(Map<String, Object> task) {
    return isAgentValidationTask(task)
        ? "prompts/bpm/pde-agent-validation-review-v3-schema.json"
        : schemaResourceFor(processCode(task));
  }

  /** Lê o processo congelado no contrato da tarefa reservada. */
  private String processCode(Map<String, Object> task) {
    Object value = task.get("processCode");
    return value == null ? "" : value.toString();
  }

  /** Reconhece a validação privada para não importar entregáveis globais de outro produto. */
  private boolean isPrivateValidationTask(Map<String, Object> task) {
    Object value = task.get("sourceReference");
    String sourceReference = value == null ? "" : value.toString();
    return sourceReference.startsWith("product:")
        && (sourceReference.contains("@private-validation-v1")
            || sourceReference.contains("@agent-validation-v1"));
  }

  /** Distingue a ocorrência v7 sem inferir validação humana a partir do mesmo processo. */
  private boolean isAgentValidationTask(Map<String, Object> task) {
    Object value = task.get("sourceReference");
    String sourceReference = value == null ? "" : value.toString();
    return sourceReference.matches("product:[1-9][0-9]*@agent-validation-v1")
        && "pde-construction-approval".equals(processCode(task));
  }

  /** Exige decisão, evidências e nota de preço coerente quando o contrato a declarar. */
  static void validate(JsonNode result) {
    if (!List.of("APPROVED", "ADJUST", "BLOCKED").contains(result.path("decision").asText())) {
      throw new IllegalArgumentException("Gate de Têmis sem decisão válida");
    }
    if (result.path("commercialRationale").asText().isBlank()
        || result.path("evidence").isEmpty()
        || result.path("requiredChanges").isMissingNode()) {
      throw new IllegalArgumentException("Gate de Têmis sem evidências suficientes");
    }
    if ("APPROVED".equals(result.path("decision").asText())
        && result.has("priceClarityScore")
        && result.path("priceClarityScore").asInt() < 80) {
      throw new IllegalArgumentException("Gate de Têmis aprovou preço com nota inferior a 80/100");
    }
  }

  /** Exige que uma aprovação privada confirme todos os controles comerciais do schema. */
  static void validate(JsonNode result, String processCode) {
    validate(result);
    if (!"pde-construction-approval".equals(processCode)) return;
    JsonNode checks = result.path("privateValidationChecks");
    List<String> requiredChecks =
        List.of(
            "sameProductAndVersion",
            "criteriaPredeclared",
            "twoDistinctParticipants",
            "fiveSignalsPassedTwice",
            "firstPartyEvents",
            "privateAndUnpublished",
            "paymentDisabled",
            "zeroMediaSpend",
            "privacyPreserved");
    if (!checks.isObject()
        || requiredChecks.stream().anyMatch(check -> !checks.path(check).isBoolean())) {
      throw new IllegalArgumentException("Parecer privado de Têmis sem checks estruturados");
    }
    if ("APPROVED".equals(result.path("decision").asText())
        && requiredChecks.stream().anyMatch(check -> !checks.path(check).asBoolean(false))) {
      throw new IllegalArgumentException("Têmis aprovou a validação privada com check reprovado");
    }
  }

  /** Exige revisão integral da técnica e dos três cenários, sem alegação de mercado. */
  static void validateAgentValidation(JsonNode result, Map<String, Object> task) {
    @SuppressWarnings("unchecked")
    Map<String, Object> target =
        task.get("taskTarget") instanceof Map<?, ?> value ? (Map<String, Object>) value : Map.of();
    String expectedReference = String.valueOf(task.get("sourceReference"));
    long expectedProductId =
        target.get("productId") instanceof Number value ? value.longValue() : -1L;
    String expectedProductSlug = String.valueOf(target.getOrDefault("productSlug", ""));
    String expectedPrototypeVersion = String.valueOf(target.getOrDefault("experienceVersion", ""));
    List<String> requiredChecks =
        List.of(
            "sameProductAndVersion",
            "criteriaPredeclared",
            "technicalHarnessPassed",
            "threeScenarioReviewsApproved",
            "syntheticEvidenceLabeled",
            "internalTrafficSegregated",
            "privacyPreserved",
            "paymentDisabled",
            "publicationDisabled",
            "campaignDisabled",
            "zeroMediaSpend",
            "noHumanOrCommercialClaim",
            "strategyFidelity");
    JsonNode checks = result.path("agentValidationChecks");
    if (!"PDE_TEMIS_AGENT_VALIDATION_V1".equals(result.path("contractVersion").asText())
        || !List.of("APPROVED", "ADJUST", "BLOCKED").contains(result.path("decision").asText())
        || result.path("commercialRationale").asText().isBlank()
        || result.path("rootCause").asText().isBlank()
        || result.path("humanEvidenceClaimed").asBoolean(true)
        || result.path("commercialEvidenceClaimed").asBoolean(true)
        || !"AGENT_VALIDATION".equals(result.path("trafficClass").asText())
        || !"mh_internal_test".equals(result.path("internalMarker").asText())
        || !result
            .path("sourceReference")
            .asText()
            .matches("product:[1-9][0-9]*@agent-validation-v1")
        || !expectedReference.equals(result.path("sourceReference").asText())
        || expectedProductId < 1
        || expectedProductId != result.path("productId").asLong()
        || expectedProductSlug.isBlank()
        || !expectedProductSlug.equals(result.path("productSlug").asText())
        || expectedPrototypeVersion.isBlank()
        || !expectedPrototypeVersion.equals(result.path("prototypeVersion").asText())
        || hasExternalSideEffects(result.path("sideEffects"))
        || !checks.isObject()
        || requiredChecks.stream().anyMatch(check -> !checks.path(check).isBoolean())
        || result.path("evidence").isEmpty()
        || !result.path("requiredChanges").isArray()) {
      throw new IllegalArgumentException("Parecer multiagente de Têmis está incompleto.");
    }
    if ("APPROVED".equals(result.path("decision").asText())
        && requiredChecks.stream().anyMatch(check -> !checks.path(check).asBoolean(false))) {
      throw new IllegalArgumentException(
          "Têmis aprovou a validação multiagente com check reprovado.");
    }
  }

  /** Detecta qualquer efeito externo declarado na revisão multiagente. */
  private static boolean hasExternalSideEffects(JsonNode effects) {
    return !effects.isObject()
        || effects.path("paymentEnabled").asBoolean(true)
        || effects.path("published").asBoolean(true)
        || effects.path("campaignCreated").asBoolean(true)
        || effects.path("mediaSpendBrl").asDouble(-1) != 0;
  }

  /** Lê a última medição cumulativa de entrada, cache e saída dos eventos JSONL do Codex. */
  static TokenUsage readTokenUsage(ObjectMapper json, Path output) {
    if (!Files.exists(output)) return TokenUsage.empty();
    long input = 0;
    long cached = 0;
    long outputTokens = 0;
    boolean informed = false;
    try {
      for (String line : Files.readAllLines(output)) {
        if (line.isBlank()) continue;
        JsonNode event;
        try {
          event = json.readTree(line);
        } catch (IOException ex) {
          log.debug("Linha não JSON ignorada na telemetria BPM de Têmis. output={}", output, ex);
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
          outputTokens = Math.max(outputTokens, measuredOutput == null ? 0 : measuredOutput);
        }
      }
    } catch (IOException ex) {
      log.warn("Falha ao ler tokens da tarefa BPM de Têmis. output={}", output, ex);
      return TokenUsage.empty();
    }
    return informed ? new TokenUsage(input, cached, outputTokens) : TokenUsage.empty();
  }

  /** Lê um contador oficial ou seu alias sem inventar valor ausente. */
  private static Long tokenValue(JsonNode usage, String officialName, String alias) {
    JsonNode value = usage.hasNonNull(officialName) ? usage.get(officialName) : usage.get(alias);
    return value != null && value.isNumber() ? value.longValue() : null;
  }

  /** Monta a falha preservando o consumo ocorrido antes do bloqueio. */
  private Map<String, Object> failureBody(
      Map<String, Object> task,
      String error,
      TokenUsage usage,
      String promptSent,
      String agentPromptPart,
      String activityPromptPart)
      throws IOException {
    Map<String, Object> body = new HashMap<>();
    body.put("error", error);
    body.put("evidenceJson", evidence(task));
    putModelUsage(body, usage);
    if (promptSent != null) {
      body.put("executionAudit", executionAudit(promptSent, agentPromptPart, activityPromptPart));
    }
    body.put("blockerGuidance", technicalGuidance(task));
    return body;
  }

  /** Monta a auditoria integral da chamada efetivamente enviada ao Codex. */
  private Map<String, Object> executionAudit(
      String promptSent, String agentPromptPart, String activityPromptPart) {
    Map<String, Object> audit = new java.util.LinkedHashMap<>();
    audit.put("executionMode", "MODEL");
    audit.put("modelCode", model);
    audit.put("reasoningEffort", reasoningEffort);
    audit.put("promptSent", promptSent);
    audit.put("agentPromptPart", agentPromptPart);
    audit.put("activityPromptPart", activityPromptPart);
    audit.put("accessedUrls", List.of());
    return audit;
  }

  /** Converte a reprovação funcional em uma mudança objetiva e navegável. */
  private Map<String, Object> functionalGuidance(Map<String, Object> task, JsonNode result) {
    String action =
        "Corrija os itens obrigatórios descritos no parecer de Têmis e reinicie a tarefa.";
    if (result.path("requiredChanges").isArray()) {
      for (JsonNode change : result.path("requiredChanges")) {
        String requiredChange =
            change.isTextual() ? change.asText("").trim() : change.path("action").asText("").trim();
        if (requiredChange.isBlank()) {
          requiredChange = change.path("description").asText("").trim();
        }
        if (!requiredChange.isBlank()) {
          action = requiredChange;
          break;
        }
      }
    }
    return Map.of(
        "category",
        "FUNCTIONAL_ADJUSTMENT",
        "recommendedAction",
        action,
        "helpLinks",
        helpLinks(task));
  }

  /** Orienta a recuperação técnica mantendo o gate comercial fechado. */
  private Map<String, Object> technicalGuidance(Map<String, Object> task) {
    return Map.of(
        "category", "TECHNICAL_FAILURE",
        "recommendedAction",
            "Verifique a causa técnica registrada, corrija a integração e reinicie a tarefa de Têmis.",
        "helpLinks", helpLinks(task));
  }

  /** Oferece a auditoria e, quando disponível, o destino comercial avaliado. */
  private List<Map<String, String>> helpLinks(Map<String, Object> task) {
    List<Map<String, String>> links = new ArrayList<>();
    links.add(Map.of("label", "Abrir tarefas dos agentes", "url", "/agent-tasks"));
    JsonNode target = json.valueToTree(task.get("taskTarget"));
    String publicUrl = target.path("publicUrl").asText("").trim();
    if (publicUrl.startsWith("https://") || publicUrl.startsWith("http://")) {
      links.add(Map.of("label", "Abrir destino avaliado", "url", publicUrl));
    }
    return List.copyOf(links);
  }

  /** Preserva o contexto acessado e comprova que Têmis não realizou efeito externo. */
  private String evidence(Map<String, Object> task) throws IOException {
    return json.writeValueAsString(evidenceFields("Têmis", model, task));
  }

  /** Monta os campos mínimos, inclusive a exceção de tier, para reconstruir a mesma tarefa. */
  static Map<String, Object> evidenceFields(
      String reviewer, String model, Map<String, Object> task) {
    Map<String, Object> evidence = new java.util.LinkedHashMap<>();
    evidence.put("reviewer", reviewer);
    evidence.put("model", model);
    evidence.put("sourceReference", String.valueOf(task.get("sourceReference")));
    evidence.put("activityId", String.valueOf(task.get("activityId")));
    evidence.put("accessMode", "READ_ONLY");
    evidence.put("externalSideEffects", false);
    evidence.put("requestedServiceTier", REQUESTED_SERVICE_TIER.toUpperCase(java.util.Locale.ROOT));
    evidence.put("effectiveServiceTier", EFFECTIVE_SERVICE_TIER);
    evidence.put("serviceTierException", SERVICE_TIER_EXCEPTION);
    if (task.get("taskTarget") != null) evidence.put("taskTarget", task.get("taskTarget"));
    return java.util.Collections.unmodifiableMap(evidence);
  }

  /** Acrescenta ao callback somente uma medição real informada pelo Codex. */
  private void putModelUsage(Map<String, Object> body, TokenUsage usage) {
    if (usage == null || !usage.informed()) return;
    body.put(
        "modelUsages",
        List.of(
            Map.of(
                "modelCode", model,
                "serviceTier", EFFECTIVE_SERVICE_TIER,
                "inputTokens", usage.inputTokens(),
                "cachedInputTokens", usage.cachedInputTokens(),
                "outputTokens", usage.outputTokens())));
  }

  /** Informa o tier efetivamente solicitado ao Codex para auditoria e teste de contrato. */
  static String serviceTier() {
    return REQUESTED_SERVICE_TIER;
  }

  /** Informa o tier aplicado quando o modelo Codex não anuncia suporte ao Flex solicitado. */
  static String effectiveServiceTier() {
    return EFFECTIVE_SERVICE_TIER;
  }

  /** Extrai o identificador estável da tarefa reservada. */
  private static long taskId(Map<String, Object> task) {
    return task == null ? -1L : ((Number) task.get("taskId")).longValue();
  }

  /** Materializa o schema versionado apenas durante a execução. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("temis-bpm-schema-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Define uma fronteira BPM suportada pelo executor revisor de Têmis. */
  private record BpmContract(String processCode, String activityId) {}

  /** Preserva o resultado funcional junto do consumo da mesma execução. */
  record BpmExecution(
      JsonNode result,
      TokenUsage usage,
      String promptSent,
      String agentPromptPart,
      String activityPromptPart) {}

  /** Preserva o resultado técnico de uma tentativa antes da política de repetição. */
  private record AttemptExecution(
      JsonNode result, TokenUsage usage, CodexProcessSupervisor.WaitOutcome outcome) {}

  /** Representa as duas partes e a composição exata enviada ao modelo. */
  private record PromptComposition(
      String fullPrompt, String agentPromptPart, String activityPromptPart) {}

  /** Representa contadores reais cumulativos informados pelo processo Codex. */
  record TokenUsage(Long inputTokens, Long cachedInputTokens, Long outputTokens) {
    /** Indica se a execução informou ao menos os contadores canônicos. */
    boolean informed() {
      return inputTokens != null || cachedInputTokens != null || outputTokens != null;
    }

    /** Soma medições de tentativas sem inventar contadores que o Codex não informou. */
    TokenUsage plus(TokenUsage other) {
      if (other == null) return this;
      return new TokenUsage(
          sumNullable(inputTokens, other.inputTokens),
          sumNullable(cachedInputTokens, other.cachedInputTokens),
          sumNullable(outputTokens, other.outputTokens));
    }

    /** Soma dois contadores preservando ausência quando ambos não foram medidos. */
    private static Long sumNullable(Long first, Long second) {
      if (first == null && second == null) return null;
      return (first == null ? 0L : first) + (second == null ? 0L : second);
    }

    /** Representa execução sem medição disponível. */
    static TokenUsage empty() {
      return new TokenUsage(null, null, null);
    }
  }

  /** Preserva tokens mesmo quando a execução técnica termina em falha. */
  private static final class BpmExecutionException extends IllegalStateException {
    private final TokenUsage usage;
    private final String promptSent;
    private final String agentPromptPart;
    private final String activityPromptPart;

    /** Cria a falha técnica com a última medição conhecida. */
    private BpmExecutionException(
        String message,
        TokenUsage usage,
        String promptSent,
        String agentPromptPart,
        String activityPromptPart) {
      super(message);
      this.usage = usage;
      this.promptSent = promptSent;
      this.agentPromptPart = agentPromptPart;
      this.activityPromptPart = activityPromptPart;
    }

    /** Cria a falha técnica mantendo também sua causa original. */
    private BpmExecutionException(
        String message,
        TokenUsage usage,
        String promptSent,
        String agentPromptPart,
        String activityPromptPart,
        Throwable cause) {
      super(message, cause);
      this.usage = usage;
      this.promptSent = promptSent;
      this.agentPromptPart = agentPromptPart;
      this.activityPromptPart = activityPromptPart;
    }

    /** Retorna a medição preservada para o callback de falha. */
    private TokenUsage usage() {
      return usage;
    }

    /** Retorna o prompt exato preservado antes da falha técnica. */
    private String promptSent() {
      return promptSent;
    }

    /** Retorna o núcleo de Têmis preservado antes da falha técnica. */
    private String agentPromptPart() {
      return agentPromptPart;
    }

    /** Retorna o gate específico preservado antes da falha técnica. */
    private String activityPromptPart() {
      return activityPromptPart;
    }
  }
}
