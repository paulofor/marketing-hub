package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: executar as atividades BPM de percepção de cliente atribuídas à Psique. */
@Component
public class CustomerBpmTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(CustomerBpmTaskConsumer.class);
  private static final String AGENT_KEY = "customer-agent";
  private static final String REQUESTED_SERVICE_TIER = "flex";
  private static final String EFFECTIVE_SERVICE_TIER = "STANDARD";
  private static final String SERVICE_TIER_EXCEPTION =
      "O catálogo do Codex não anuncia Flex para gpt-5.6-sol; a CLI omite o tier solicitado e usa o tier padrão.";
  private static final List<BpmContract> CONTRACTS =
      List.of(
          new BpmContract("creative-production-approval", "customer"),
          new BpmContract("landing-page-generation", "customer"),
          new BpmContract("pde-commercial-homologation-activation", "humanExperienceReview"),
          new BpmContract("pde-construction-approval", "humanExperienceReview"));
  private final RestClient backend;
  private final ObjectMapper json;
  private final String codex;
  private final String model;
  private final String reasoningEffort;
  private final String repositoryPath;
  private final PdeExperienceEvidenceLoader pdeExperienceEvidenceLoader;
  private final BpmVisualEvidenceRunner visualEvidenceRunner;
  private final BpmVisualEvidenceBackendClient visualEvidenceBackendClient;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Configura a fila canônica, o modelo e a sandbox somente leitura de Psique. */
  @Autowired
  public CustomerBpmTaskConsumer(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl,
      @Value("${CUSTOMER_AGENT_CODEX_EXECUTABLE:codex}") String codex,
      @Value("${CUSTOMER_AGENT_MODEL:gpt-5.6-sol}") String model,
      @Value("${CUSTOMER_AGENT_REASONING_EFFORT:high}") String reasoningEffort,
      @Value("${CUSTOMER_AGENT_REPOSITORY_PATH:/workspace}") String repositoryPath,
      @Value("${CUSTOMER_AGENT_COMMERCIAL_EVIDENCE_PATH:}") String commercialEvidencePath,
      ObjectMapper json,
      BpmVisualEvidenceRunner visualEvidenceRunner,
      BpmVisualEvidenceBackendClient visualEvidenceBackendClient) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.codex = codex;
    this.model = model;
    this.reasoningEffort = requiredReasoningEffort(reasoningEffort);
    this.repositoryPath = repositoryPath;
    this.pdeExperienceEvidenceLoader =
        new PdeExperienceEvidenceLoader(
            commercialEvidencePath == null || commercialEvidencePath.isBlank()
                ? repositoryPath
                : commercialEvidencePath);
    this.json = json;
    this.visualEvidenceRunner = visualEvidenceRunner;
    this.visualEvidenceBackendClient = visualEvidenceBackendClient;
  }

  /** Mantém testes de configuração isolados sem iniciar integrações de browser ou backend. */
  CustomerBpmTaskConsumer(
      String backendUrl,
      String codex,
      String model,
      String reasoningEffort,
      String repositoryPath,
      String commercialEvidencePath,
      ObjectMapper json) {
    this(
        backendUrl,
        codex,
        model,
        reasoningEffort,
        repositoryPath,
        commercialEvidencePath,
        json,
        null,
        null);
  }

  /** Exige o esforço explícito antes de Psique reservar uma tarefa ou iniciar o modelo. */
  private static String requiredReasoningEffort(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "CUSTOMER_AGENT_REASONING_EFFORT é obrigatório para auditar Psique.");
    }
    return value.trim();
  }

  /** Reserva em PLAY e avalia uma atividade liberada sem escolher a próxima etapa do processo. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
    Map<String, Object> task = null;
    BpmExecution execution = null;
    PreparedVisualEvidence visualEvidence = null;
    try {
      task = claimNext();
      if (task == null) return;
      visualEvidence = prepareVisualEvidence(task);
      execution = execute(task, visualEvidence.uploaded());
      JsonNode result = execution.result();
      validate(result);
      validateVisualAudit(result, visualEvidence.uploaded());
      if ("APPROVED".equals(result.path("decision").asText())) report(task, execution);
      else block(task, execution);
    } catch (Exception ex) {
      log.error("Falha na atividade BPM de Psique. taskId={}", taskId(task), ex);
      fail(task, ex, execution, visualEvidence);
    } finally {
      deleteVisualWorkDirectory(visualEvidence, taskId(task));
    }
  }

  /** Reserva primeiro criativos e depois landings sem misturar contratos de avaliação. */
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

  /** Confirma que o polling conhece apenas uma atividade publicada de experiência humana. */
  static boolean supportsContract(String processCode, String activityId) {
    return CONTRACTS.stream()
        .anyMatch(
            contract ->
                contract.processCode().equals(processCode)
                    && contract.activityId().equals(activityId));
  }

  /** Informa quais processos avaliam telas e exigem captura por dobra antes do modelo. */
  static boolean requiresVisualAudit(String processCode) {
    return List.of(
            "landing-page-generation",
            "pde-commercial-homologation-activation",
            "pde-construction-approval")
        .contains(processCode);
  }

  /** Captura e persiste a página pública da tarefa antes de montar o prompt de Psique. */
  private PreparedVisualEvidence prepareVisualEvidence(Map<String, Object> task) throws Exception {
    if (!requiresVisualAudit(processCode(task))) return PreparedVisualEvidence.empty();
    if (visualEvidenceRunner == null || visualEvidenceBackendClient == null) {
      throw new BpmVisualEvidenceRunner.VisualEvidenceException(
          "Captura visual obrigatória de Psique não está configurada.");
    }
    String publicUrl = json.valueToTree(task.get("taskTarget")).path("publicUrl").asText("").trim();
    if (publicUrl.isBlank()) {
      throw new BpmVisualEvidenceRunner.VisualEvidenceException(
          "A tarefa não recebeu a URL pública da tela que Psique deve capturar.");
    }
    Path workDirectory = Files.createTempDirectory("psique-bpm-visual-task-" + taskId(task) + "-");
    try {
      BpmVisualEvidenceRunner.VisualEvidenceBundle bundle =
          visualEvidenceRunner.capture(publicUrl, workDirectory);
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> uploaded =
          visualEvidenceBackendClient.upload(taskId(task), bundle);
      if (uploaded.isEmpty()) {
        throw new BpmVisualEvidenceRunner.VisualEvidenceException(
            "Nenhum snapshot visual foi persistido para Psique.");
      }
      return new PreparedVisualEvidence(bundle, uploaded);
    } catch (Exception ex) {
      log.error(
          "Falha ao preparar prova visual de Psique. taskId={} processCode={}",
          taskId(task),
          processCode(task),
          ex);
      deleteDirectory(workDirectory, taskId(task));
      if (ex instanceof BpmVisualEvidenceRunner.VisualEvidenceException) throw ex;
      throw new BpmVisualEvidenceRunner.VisualEvidenceException(
          "Não foi possível capturar e persistir a prova visual obrigatória de Psique.", ex);
    }
  }

  /** Executa o prompt versionado e exige um parecer estruturado sobre a experiência da cliente. */
  BpmExecution execute(Map<String, Object> task) throws IOException, InterruptedException {
    return execute(task, List.of());
  }

  /** Executa o prompt somente após receber as provas visuais já persistidas no backend. */
  private BpmExecution execute(
      Map<String, Object> task,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence)
      throws IOException, InterruptedException {
    Path output = Files.createTempFile("psique-bpm-result-", ".json");
    Path processLog = Files.createTempFile("psique-bpm-process-", ".log");
    Path schema = materialize(schemaResourceFor(processCode(task)), ".json");
    String resolvedPrompt = prompt(task, visualEvidence);
    List<Map<String, Object>> visualAccesses = visualAccessedUrls(visualEvidence);
    try {
      recordExecutionAudit(task, resolvedPrompt, visualAccesses);
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
      Process process =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      process.getOutputStream().write(resolvedPrompt.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(40, TimeUnit.MINUTES)) {
        process.destroyForcibly();
        throw new BpmExecutionException(
            "Timeout da atividade BPM de Psique após 40 minutos.",
            readTokenUsage(json, processLog),
            resolvedPrompt,
            mergeAccessedUrls(visualAccesses, readAccessedUrls(json, processLog)),
            visualEvidence);
      }
      TokenUsage usage = readTokenUsage(json, processLog);
      if (process.exitValue() != 0) {
        throw new BpmExecutionException(
            "Codex encerrou com falha: " + Files.readString(processLog),
            usage,
            resolvedPrompt,
            mergeAccessedUrls(visualAccesses, readAccessedUrls(json, processLog)),
            visualEvidence);
      }
      try {
        JsonNode result = json.readTree(Files.readString(output));
        return new BpmExecution(
            result,
            usage,
            resolvedPrompt,
            mergeAccessedUrls(visualAccesses, readAccessedUrls(json, processLog)),
            visualEvidence);
      } catch (IOException ex) {
        log.error(
            "Resposta inválida na atividade BPM de Psique. taskId={} output={}",
            taskId(task),
            output,
            ex);
        throw new BpmExecutionException(
            "Resposta de Psique não contém JSON válido.",
            usage,
            resolvedPrompt,
            mergeAccessedUrls(visualAccesses, readAccessedUrls(json, processLog)),
            visualEvidence,
            ex);
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
    }
  }

  /** Persiste o parecer e as evidências na própria atividade BPM. */
  private void report(Map<String, Object> task, BpmExecution execution) throws IOException {
    Map<String, Object> body = new HashMap<>();
    body.put("resultJson", json.writeValueAsString(execution.result()));
    body.put("evidenceJson", evidence(task, execution.visualEvidence()));
    putModelUsage(body, execution.usage());
    body.put("executionAudit", executionAudit(execution.promptSent(), execution.accessedUrls()));
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

  /** Mantém o gate fechado quando o parecer não pôde ser produzido ou persistido. */
  private void fail(
      Map<String, Object> task,
      Exception ex,
      BpmExecution execution,
      PreparedVisualEvidence preparedVisualEvidence) {
    if (task == null) return;
    try {
      BpmExecutionException bpm = ex instanceof BpmExecutionException value ? value : null;
      TokenUsage usage = execution != null ? execution.usage() : bpm == null ? null : bpm.usage();
      String promptSent =
          execution != null ? execution.promptSent() : bpm == null ? null : bpm.promptSent();
      List<Map<String, Object>> urls =
          execution != null
              ? execution.accessedUrls()
              : bpm == null ? List.of() : bpm.accessedUrls();
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence =
          execution != null
              ? execution.visualEvidence()
              : bpm != null
                  ? bpm.visualEvidence()
                  : preparedVisualEvidence == null ? List.of() : preparedVisualEvidence.uploaded();
      backend
          .post()
          .uri(
              "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
              AGENT_KEY,
              taskId(task))
          .body(failureBody(task, ex, usage, promptSent, urls, visualEvidence))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception callbackEx) {
      log.error("Falha ao registrar bloqueio BPM de Psique. taskId={}", taskId(task), callbackEx);
    }
  }

  /** Preserva o parecer funcional e impede avanço quando a cliente exige ajuste. */
  private void block(Map<String, Object> task, BpmExecution execution) throws IOException {
    JsonNode result = execution.result();
    String resultJson = json.writeValueAsString(result);
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Psique bloqueou o avanço: " + result.path("customerPerspective").asText());
    body.put("resultJson", resultJson);
    body.put("evidenceJson", evidence(task, execution.visualEvidence()));
    putModelUsage(body, execution.usage());
    body.put("executionAudit", executionAudit(execution.promptSent(), execution.accessedUrls()));
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

  /** Resolve o contexto e injeta evidência versionada sem depender da sandbox interna do modelo. */
  private String prompt(
      Map<String, Object> task,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence)
      throws IOException {
    Map<String, Object> promptContext = new HashMap<>(task);
    if (visualEvidence != null && !visualEvidence.isEmpty()) {
      promptContext.put("visualEvidence", visualEvidence);
    }
    if ("pde-construction-approval".equals(processCode(task))) {
      promptContext.put("versionedExperienceEvidence", pdeExperienceEvidenceLoader.load());
    } else if ("pde-commercial-homologation-activation".equals(processCode(task))) {
      promptContext.put(
          "versionedCommercialHomologationEvidence",
          pdeExperienceEvidenceLoader.loadCommercialHomologationEvidence(task.get("taskTarget")));
    }
    return read(promptResourceFor(processCode(task)))
        .replace("{{PSIQUE_BEHAVIORAL_CORE_V3}}", behavioralCoreV3())
        .replace("{{TASK_CONTEXT}}", json.writeValueAsString(promptContext));
  }

  /** Lê a constituição comportamental e sensorial usada pelas atividades atuais de Psique. */
  private String behavioralCoreV3() throws IOException {
    return read("prompts/psique/behavioral-core-v3.md");
  }

  /** Seleciona o prompt versionado específico da entidade avaliada. */
  static String promptResourceFor(String processCode) {
    return switch (processCode) {
      case "creative-production-approval" -> "prompts/bpm/v2/creative-customer-review.md";
      case "pde-commercial-homologation-activation" ->
          "prompts/bpm/v2/pde-commercial-homologation-customer-review.md";
      case "pde-construction-approval" -> "prompts/bpm/v2/pde-experience-review.md";
      default -> "prompts/bpm/v2/landing-customer-review.md";
    };
  }

  /** Seleciona o schema versionado específico da entidade avaliada. */
  static String schemaResourceFor(String processCode) {
    return switch (processCode) {
      case "creative-production-approval" -> "prompts/bpm/v2/creative-customer-review-schema.json";
      case "pde-commercial-homologation-activation" ->
          "prompts/bpm/v2/pde-commercial-homologation-customer-review-schema.json";
      case "pde-construction-approval" -> "prompts/bpm/v2/pde-experience-review-schema.json";
      default -> "prompts/bpm/v2/landing-customer-review-schema.json";
    };
  }

  /** Lê o processo congelado no contrato da tarefa reservada. */
  private String processCode(Map<String, Object> task) {
    Object value = task.get("processCode");
    return value == null ? "" : value.toString();
  }

  /** Valida que a decisão inclui evidência, resposta humana e gates internamente coerentes. */
  static void validate(JsonNode result) {
    if (!List.of("APPROVED", "ADJUST", "BLOCKED").contains(result.path("decision").asText())) {
      throw new IllegalArgumentException("Parecer de Psique sem decisão válida");
    }
    if (result.path("customerPerspective").asText().isBlank()
        || !result.path("behavioralResponse").isObject()
        || result.path("behavioralResponse").path("firstImpulse").asText().isBlank()
        || result.path("behavioralResponse").path("belongingAdmirationLove").asText().isBlank()
        || !completePurchaseEmotion(result.path("purchaseEmotion"))
        || result.path("evidence").isEmpty()
        || result.path("requiredChanges").isMissingNode()) {
      throw new IllegalArgumentException("Parecer de Psique sem evidências suficientes");
    }
    PsiqueSensoryContract.validate(result.path("behavioralResponse").path("sensoryExperience"));
    if ("APPROVED".equals(result.path("decision").asText())
        && result.path("gateChecks").isArray()) {
      for (JsonNode gate : result.path("gateChecks")) {
        if (!"PASS".equals(gate.path("status").asText())) {
          throw new IllegalArgumentException("Parecer de Psique aprovou com gate não aprovado");
        }
      }
    }
  }

  /**
   * Exige expectativa, ansiedade e sentimento pós-entrega sem confundi-los com resultado humano.
   */
  private static boolean completePurchaseEmotion(JsonNode purchaseEmotion) {
    return purchaseEmotion.isObject()
        && !purchaseEmotion.path("acquisitionExpectation").asText().isBlank()
        && !purchaseEmotion.path("acquisitionAnxiety").asText().isBlank()
        && !purchaseEmotion.path("expectedPostDeliveryFeeling").asText().isBlank()
        && !purchaseEmotion.path("emotionalTension").asText().isBlank()
        && !purchaseEmotion.path("evidenceBoundary").asText().isBlank();
  }

  /** Confirma que toda imagem persistida foi referenciada e cada dobra recebeu análise estética. */
  static void validateVisualAudit(
      JsonNode result, List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence) {
    if (visualEvidence == null || visualEvidence.isEmpty()) return;
    Set<String> sessions =
        visualEvidence.stream()
            .map(BpmVisualEvidenceBackendClient.UploadedVisualEvidence::captureSessionId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (sessions.size() != 1) {
      throw new IllegalArgumentException("Prova visual de Psique mistura sessões de captura");
    }
    JsonNode visualAudit = result.path("visualAudit");
    if (!visualAudit.isObject()
        || !sessions.iterator().next().equals(visualAudit.path("captureSessionId").asText())
        || !visualAudit.path("mobileFirst").asBoolean(false)
        || visualAudit.path("overallAestheticAssessment").asText().isBlank()
        || visualAudit.path("fullPageContinuity").asText().isBlank()) {
      throw new IllegalArgumentException("Análise visual de Psique não corresponde à captura");
    }

    Set<Long> expectedFullPageIds = new LinkedHashSet<>();
    Map<Long, BpmVisualEvidenceBackendClient.UploadedVisualEvidence> expectedFolds =
        new LinkedHashMap<>();
    for (var evidence : visualEvidence) {
      if (evidence.id() == null) {
        throw new IllegalArgumentException("Snapshot de Psique sem id persistido");
      }
      if ("FULL_PAGE".equals(evidence.evidenceType())) expectedFullPageIds.add(evidence.id());
      if ("FOLD".equals(evidence.evidenceType())) expectedFolds.put(evidence.id(), evidence);
    }
    Set<Long> reportedFullPageIds = new LinkedHashSet<>();
    JsonNode fullPageEvidenceIds = visualAudit.path("fullPageEvidenceIds");
    fullPageEvidenceIds.forEach(value -> reportedFullPageIds.add(value.asLong(-1)));
    if (!fullPageEvidenceIds.isArray()
        || fullPageEvidenceIds.size() != reportedFullPageIds.size()
        || expectedFullPageIds.isEmpty()
        || !reportedFullPageIds.equals(expectedFullPageIds)) {
      throw new IllegalArgumentException("Captura full-page não foi auditada integralmente");
    }

    Set<Long> analyzedFoldIds = new LinkedHashSet<>();
    JsonNode foldAnalyses = visualAudit.path("foldAnalyses");
    if (!foldAnalyses.isArray() || foldAnalyses.size() != expectedFolds.size()) {
      throw new IllegalArgumentException("Quantidade de análises não cobre todas as dobras");
    }
    for (JsonNode analysis : foldAnalyses) {
      long artifactId = analysis.path("artifactId").asLong(-1);
      var evidence = expectedFolds.get(artifactId);
      if (evidence == null || !analyzedFoldIds.add(artifactId)) {
        throw new IllegalArgumentException("Análise referencia dobra ausente ou duplicada");
      }
      if (!evidence.deviceProfile().equals(analysis.path("deviceProfile").asText())
          || evidence.pageNumber() != analysis.path("pageNumber").asInt()
          || evidence.foldNumber() != analysis.path("foldNumber").asInt()
          || analysis.path("aestheticAssessment").asText().isBlank()
          || analysis.path("visualHierarchy").asText().isBlank()
          || analysis.path("legibility").asText().isBlank()
          || analysis.path("emotionEvoked").asText().isBlank()
          || analysis.path("ctaVisibility").asText().isBlank()) {
        throw new IllegalArgumentException("Análise estética por dobra está incompleta");
      }
    }
    if (!analyzedFoldIds.equals(expectedFolds.keySet())) {
      throw new IllegalArgumentException("Psique omitiu uma ou mais dobras capturadas");
    }
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
          log.debug("Linha não JSON ignorada na telemetria BPM de Psique. output={}", output, ex);
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
      log.warn("Falha ao ler tokens da tarefa BPM de Psique. output={}", output, ex);
      return TokenUsage.empty();
    }
    return informed ? new TokenUsage(input, cached, outputTokens) : TokenUsage.empty();
  }

  /** Extrai somente URLs de navegação confirmadas pelo item web_search oficial do Codex. */
  static List<Map<String, Object>> readAccessedUrls(ObjectMapper json, Path output) {
    if (!Files.exists(output)) return List.of();
    LinkedHashMap<String, Map<String, Object>> urls = new LinkedHashMap<>();
    try {
      for (String line : Files.readAllLines(output)) {
        if (line.isBlank()) continue;
        try {
          collectAccessedUrls(json.readTree(line), urls);
        } catch (IOException ex) {
          log.debug("Linha não JSON ignorada nas fontes BPM de Psique. output={}", output, ex);
        }
      }
    } catch (IOException ex) {
      log.warn("Falha ao ler fontes acessadas por Psique. output={}", output, ex);
      return List.of();
    }
    return List.copyOf(urls.values());
  }

  /**
   * Valida o envelope JSONL terminal e registra apenas open_page ou find_in_page com URL literal.
   */
  private static void collectAccessedUrls(JsonNode event, Map<String, Map<String, Object>> urls) {
    if (!"item.completed".equals(event.path("type").asText())) return;
    JsonNode item = event.path("item");
    if (!"web_search".equals(item.path("type").asText())) return;
    JsonNode action = item.path("action");
    String actionType = action.path("type").asText();
    if (!List.of("open_page", "find_in_page").contains(actionType)) return;
    String url = action.path("url").asText("").trim();
    if (!url.startsWith("https://") && !url.startsWith("http://")) return;
    urls.putIfAbsent(
        url,
        Map.of(
            "url", url,
            "label", "Fonte acessada por Psique",
            "accessMethod", "WEB_SEARCH"));
  }

  /** Registra prompt, modelo, raciocínio e acessos Playwright antes de iniciar o processo Codex. */
  private void recordExecutionAudit(
      Map<String, Object> task, String resolvedPrompt, List<Map<String, Object>> accessedUrls) {
    backend
        .put()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/execution-audit",
            AGENT_KEY,
            taskId(task))
        .body(executionAudit(resolvedPrompt, accessedUrls))
        .retrieve()
        .toBodilessEntity();
  }

  /** Converte páginas comprovadamente capturadas em URLs acessadas pelo método Playwright. */
  private List<Map<String, Object>> visualAccessedUrls(
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence) {
    LinkedHashMap<String, Map<String, Object>> accesses = new LinkedHashMap<>();
    if (visualEvidence == null) return List.of();
    for (var evidence : visualEvidence) {
      if (evidence.finalUrl() == null || accesses.containsKey(evidence.finalUrl())) continue;
      Map<String, Object> access = new LinkedHashMap<>();
      access.put("url", evidence.finalUrl());
      access.put("label", "Tela capturada por Psique");
      access.put("accessMethod", "PLAYWRIGHT");
      if (evidence.capturedAt() != null) access.put("accessedAt", evidence.capturedAt());
      accesses.put(evidence.finalUrl(), access);
    }
    return List.copyOf(accesses.values());
  }

  /** Une acessos do browser e da pesquisa sem duplicar a mesma URL na tarefa. */
  private static List<Map<String, Object>> mergeAccessedUrls(
      List<Map<String, Object>> first, List<Map<String, Object>> second) {
    LinkedHashMap<String, Map<String, Object>> merged = new LinkedHashMap<>();
    mergeAccessedUrlsFrom(first, merged);
    mergeAccessedUrlsFrom(second, merged);
    return List.copyOf(merged.values());
  }

  /** Acrescenta uma fonte opcional sem falhar quando a captura ou a pesquisa não gerou URLs. */
  private static void mergeAccessedUrlsFrom(
      List<Map<String, Object>> source, Map<String, Map<String, Object>> destination) {
    if (source == null) return;
    for (Map<String, Object> access : source) {
      Object url = access.get("url");
      if (url != null) destination.putIfAbsent(url.toString(), access);
    }
  }

  /** Lê um contador oficial ou seu alias sem inventar valor ausente. */
  private static Long tokenValue(JsonNode usage, String officialName, String alias) {
    JsonNode value = usage.hasNonNull(officialName) ? usage.get(officialName) : usage.get(alias);
    return value != null && value.isNumber() ? value.longValue() : null;
  }

  /** Monta a falha preservando o consumo ocorrido antes do bloqueio. */
  private Map<String, Object> failureBody(
      Map<String, Object> task,
      Exception error,
      TokenUsage usage,
      String promptSent,
      List<Map<String, Object>> accessedUrls,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence)
      throws IOException {
    Map<String, Object> body = new HashMap<>();
    body.put("error", error.toString());
    body.put("evidenceJson", evidence(task, visualEvidence));
    putModelUsage(body, usage);
    if (promptSent != null) {
      body.put("executionAudit", executionAudit(promptSent, accessedUrls));
    }
    body.put(
        "blockerGuidance",
        visualEvidenceFailure(error)
            ? missingVisualEvidenceGuidance(task)
            : technicalGuidance(task));
    return body;
  }

  /** Identifica a causa visual mesmo quando uma integração a encapsulou em outra exceção. */
  private boolean visualEvidenceFailure(Throwable error) {
    Throwable current = error;
    while (current != null) {
      if (current instanceof BpmVisualEvidenceRunner.VisualEvidenceException) return true;
      current = current.getCause();
    }
    return false;
  }

  /** Monta a auditoria completa e segregada da chamada efetivamente enviada ao Codex. */
  private Map<String, Object> executionAudit(
      String promptSent, List<Map<String, Object>> accessedUrls) {
    Map<String, Object> audit = new LinkedHashMap<>();
    audit.put("executionMode", "MODEL");
    audit.put("modelCode", model);
    audit.put("reasoningEffort", reasoningEffort);
    audit.put("promptSent", promptSent);
    audit.put("accessedUrls", accessedUrls == null ? List.of() : accessedUrls);
    return audit;
  }

  /** Traduz o parecer funcional de Psique em uma correção objetiva com atalhos seguros. */
  private Map<String, Object> functionalGuidance(Map<String, Object> task, JsonNode result) {
    List<String> changes = new ArrayList<>();
    if (result.path("requiredChanges").isArray()) {
      result
          .path("requiredChanges")
          .forEach(
              change -> {
                String action =
                    change.isTextual()
                        ? change.asText("").trim()
                        : change.path("action").asText("").trim();
                if (action.isBlank()) action = change.path("description").asText("").trim();
                if (!action.isBlank()) changes.add(action);
              });
    }
    String action =
        changes.stream()
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse(
                "Corrija os gates em ajuste descritos no parecer de Psique e reinicie a tarefa.");
    return Map.of(
        "category",
        "FUNCTIONAL_ADJUSTMENT",
        "recommendedAction",
        action,
        "helpLinks",
        helpLinks(task));
  }

  /** Orienta a recuperação técnica sem sugerir aprovação ou publicação automática. */
  private Map<String, Object> technicalGuidance(Map<String, Object> task) {
    return Map.of(
        "category", "TECHNICAL_FAILURE",
        "recommendedAction",
            "Verifique a causa técnica registrada, corrija a integração e reinicie a tarefa de Psique.",
        "helpLinks", helpLinks(task));
  }

  /** Orienta a restauração da URL ou do storage antes de repetir a inspeção visual. */
  private Map<String, Object> missingVisualEvidenceGuidance(Map<String, Object> task) {
    return Map.of(
        "category",
        "MISSING_EVIDENCE",
        "recommendedAction",
        "Disponibilize a tela pública e o storage privado de snapshots; depois reinicie a tarefa para Psique capturar e analisar todas as dobras.",
        "helpLinks",
        helpLinks(task));
  }

  /** Oferece a tela de tarefas e, quando seguro, a experiência exata revisada. */
  private List<Map<String, String>> helpLinks(Map<String, Object> task) {
    List<Map<String, String>> links = new ArrayList<>();
    links.add(Map.of("label", "Abrir tarefas dos agentes", "url", "/agent-tasks"));
    JsonNode target = json.valueToTree(task.get("taskTarget"));
    String publicUrl = target.path("publicUrl").asText("").trim();
    if (publicUrl.startsWith("https://") || publicUrl.startsWith("http://")) {
      links.add(Map.of("label", "Abrir experiência revisada", "url", publicUrl));
    }
    return List.copyOf(links);
  }

  /** Preserva o contexto acessado e comprova que Psique não realizou efeito externo. */
  private String evidence(
      Map<String, Object> task,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence)
      throws IOException {
    Map<String, Object> evidence = new LinkedHashMap<>(evidenceFields("Psique", model, task));
    if (visualEvidence != null && !visualEvidence.isEmpty()) {
      evidence.put("visualCaptureSessionId", visualEvidence.getFirst().captureSessionId());
      evidence.put(
          "visualEvidenceIds",
          visualEvidence.stream()
              .map(BpmVisualEvidenceBackendClient.UploadedVisualEvidence::id)
              .toList());
      evidence.put("visualEvidenceCount", visualEvidence.size());
    }
    return json.writeValueAsString(evidence);
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

  /** Remove a topologia temporária visual depois que o callback já preservou a auditoria. */
  private void deleteVisualWorkDirectory(PreparedVisualEvidence visualEvidence, long taskId) {
    if (visualEvidence == null || visualEvidence.bundle() == null) return;
    deleteDirectory(visualEvidence.bundle().workDirectory(), taskId);
  }

  /** Apaga arquivos temporários em ordem reversa sem ocultar a causa principal da tarefa. */
  private void deleteDirectory(Path directory, long taskId) {
    if (directory == null || !Files.exists(directory)) return;
    try (var paths = Files.walk(directory)) {
      paths
          .sorted(java.util.Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (Exception ex) {
                  log.warn(
                      "Falha ao remover prova visual temporária. taskId={} path={}",
                      taskId,
                      path,
                      ex);
                }
              });
    } catch (Exception ex) {
      log.warn(
          "Falha ao limpar diretório visual temporário. taskId={} directory={}",
          taskId,
          directory,
          ex);
    }
  }

  /** Materializa o schema versionado apenas durante a execução. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("psique-bpm-schema-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Define uma fronteira BPM suportada pelo executor de Psique. */
  private record BpmContract(String processCode, String activityId) {}

  /** Mantém juntos a captura temporária e os metadados já persistidos no backend. */
  private record PreparedVisualEvidence(
      BpmVisualEvidenceRunner.VisualEvidenceBundle bundle,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> uploaded) {
    /** Representa uma atividade que não avalia uma tela digital. */
    private static PreparedVisualEvidence empty() {
      return new PreparedVisualEvidence(null, List.of());
    }
  }

  /** Preserva o resultado funcional junto do consumo da mesma execução. */
  record BpmExecution(
      JsonNode result,
      TokenUsage usage,
      String promptSent,
      List<Map<String, Object>> accessedUrls,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence) {}

  /** Representa contadores reais cumulativos informados pelo processo Codex. */
  record TokenUsage(Long inputTokens, Long cachedInputTokens, Long outputTokens) {
    /** Indica se a execução informou ao menos os contadores canônicos. */
    boolean informed() {
      return inputTokens != null || cachedInputTokens != null || outputTokens != null;
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
    private final List<Map<String, Object>> accessedUrls;
    private final List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence;

    /** Cria a falha técnica com a última medição conhecida. */
    private BpmExecutionException(
        String message,
        TokenUsage usage,
        String promptSent,
        List<Map<String, Object>> accessedUrls,
        List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence) {
      super(message);
      this.usage = usage;
      this.promptSent = promptSent;
      this.accessedUrls = List.copyOf(accessedUrls);
      this.visualEvidence = List.copyOf(visualEvidence);
    }

    /** Cria a falha técnica mantendo também sua causa original. */
    private BpmExecutionException(
        String message,
        TokenUsage usage,
        String promptSent,
        List<Map<String, Object>> accessedUrls,
        List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence,
        Throwable cause) {
      super(message, cause);
      this.usage = usage;
      this.promptSent = promptSent;
      this.accessedUrls = List.copyOf(accessedUrls);
      this.visualEvidence = List.copyOf(visualEvidence);
    }

    /** Retorna a medição preservada para o callback de falha. */
    private TokenUsage usage() {
      return usage;
    }

    /** Retorna o prompt exato mesmo quando a chamada termina com falha. */
    private String promptSent() {
      return promptSent;
    }

    /** Retorna somente as URLs comprovadas pelo log da tentativa interrompida. */
    private List<Map<String, Object>> accessedUrls() {
      return accessedUrls;
    }

    /** Retorna os snapshots persistidos antes da tentativa interrompida. */
    private List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence() {
      return visualEvidence;
    }
  }
}
