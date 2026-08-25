package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
          new BpmContract("pde-commercial-homologation-activation", "pdeGate"),
          new BpmContract("pde-construction-approval", "review"));
  private final RestClient backend;
  private final ObjectMapper json;
  private final String codex;
  private final String model;
  private final String repositoryPath;
  private final PdeExperienceEvidenceLoader pdeExperienceEvidenceLoader;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Configura a fila canônica, o modelo e a sandbox somente leitura de Psique. */
  public CustomerBpmTaskConsumer(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl,
      @Value("${CUSTOMER_AGENT_CODEX_EXECUTABLE:codex}") String codex,
      @Value("${CUSTOMER_AGENT_MODEL:gpt-5.6-sol}") String model,
      @Value("${CUSTOMER_AGENT_REPOSITORY_PATH:/workspace}") String repositoryPath,
      ObjectMapper json) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.codex = codex;
    this.model = model;
    this.repositoryPath = repositoryPath;
    this.pdeExperienceEvidenceLoader = new PdeExperienceEvidenceLoader(repositoryPath);
    this.json = json;
  }

  /** Reserva em PLAY e avalia uma atividade liberada sem escolher a próxima etapa do processo. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
    Map<String, Object> task = null;
    BpmExecution execution = null;
    try {
      task = claimNext();
      if (task == null) return;
      execution = execute(task);
      JsonNode result = execution.result();
      validate(result);
      if ("APPROVED".equals(result.path("decision").asText()))
        report(task, result, execution.usage());
      else block(task, result, execution.usage());
    } catch (Exception ex) {
      log.error("Falha na atividade BPM de Psique. taskId={}", taskId(task), ex);
      TokenUsage usage =
          execution != null
              ? execution.usage()
              : ex instanceof BpmExecutionException bpm ? bpm.usage() : null;
      fail(task, ex, usage);
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

  /** Executa o prompt versionado e exige um parecer estruturado sobre a experiência da cliente. */
  BpmExecution execute(Map<String, Object> task) throws IOException, InterruptedException {
    Path output = Files.createTempFile("psique-bpm-result-", ".json");
    Path processLog = Files.createTempFile("psique-bpm-process-", ".log");
    Path schema = materialize(schemaResourceFor(processCode(task)), ".json");
    try {
      List<String> command =
          new ArrayList<>(
              List.of(
                  codex,
                  "--search",
                  "exec",
                  "-c",
                  "service_tier=\"" + REQUESTED_SERVICE_TIER + "\"",
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
      process.getOutputStream().write(prompt(task).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(40, TimeUnit.MINUTES)) {
        process.destroyForcibly();
        throw new BpmExecutionException(
            "Timeout da atividade BPM de Psique após 40 minutos.",
            readTokenUsage(json, processLog));
      }
      TokenUsage usage = readTokenUsage(json, processLog);
      if (process.exitValue() != 0) {
        throw new BpmExecutionException(
            "Codex encerrou com falha: " + Files.readString(processLog), usage);
      }
      try {
        JsonNode result = json.readTree(Files.readString(output));
        return new BpmExecution(result, usage);
      } catch (IOException ex) {
        log.error(
            "Resposta inválida na atividade BPM de Psique. taskId={} output={}",
            taskId(task),
            output,
            ex);
        throw new BpmExecutionException("Resposta de Psique não contém JSON válido.", usage, ex);
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
    }
  }

  /** Persiste o parecer e as evidências na própria atividade BPM. */
  private void report(Map<String, Object> task, JsonNode result, TokenUsage usage)
      throws IOException {
    Map<String, Object> body = new HashMap<>();
    body.put("resultJson", json.writeValueAsString(result));
    body.put("evidenceJson", evidence(task));
    putModelUsage(body, usage);
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
  private void fail(Map<String, Object> task, Exception ex, TokenUsage usage) {
    if (task == null) return;
    try {
      backend
          .post()
          .uri(
              "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
              AGENT_KEY,
              taskId(task))
          .body(failureBody(task, ex.toString(), usage))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception callbackEx) {
      log.error("Falha ao registrar bloqueio BPM de Psique. taskId={}", taskId(task), callbackEx);
    }
  }

  /** Preserva o parecer funcional e impede avanço quando a cliente exige ajuste. */
  private void block(Map<String, Object> task, JsonNode result, TokenUsage usage)
      throws IOException {
    String resultJson = json.writeValueAsString(result);
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Psique bloqueou o avanço: " + result.path("customerPerspective").asText());
    body.put("resultJson", resultJson);
    body.put("evidenceJson", evidence(task));
    putModelUsage(body, usage);
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
  private String prompt(Map<String, Object> task) throws IOException {
    Map<String, Object> promptContext = new HashMap<>(task);
    if ("pde-construction-approval".equals(processCode(task))) {
      promptContext.put("versionedExperienceEvidence", pdeExperienceEvidenceLoader.load());
    } else if ("pde-commercial-homologation-activation".equals(processCode(task))) {
      promptContext.put(
          "versionedCommercialHomologationEvidence",
          pdeExperienceEvidenceLoader.loadCommercialHomologationEvidence());
    }
    return read(promptResourceFor(processCode(task)))
        .replace("{{PSIQUE_BEHAVIORAL_CORE_V2}}", behavioralCoreV2())
        .replace("{{TASK_CONTEXT}}", json.writeValueAsString(promptContext));
  }

  /** Lê a mesma constituição comportamental usada por todas as atividades de Psique. */
  private String behavioralCoreV2() throws IOException {
    return read("prompts/psique/behavioral-core-v2.md");
  }

  /** Seleciona o prompt versionado específico da entidade avaliada. */
  static String promptResourceFor(String processCode) {
    return switch (processCode) {
      case "creative-production-approval" -> "prompts/bpm/creative-customer-review.md";
      case "pde-commercial-homologation-activation" ->
          "prompts/bpm/pde-commercial-homologation-customer-review.md";
      case "pde-construction-approval" -> "prompts/bpm/pde-experience-review.md";
      default -> "prompts/bpm/landing-customer-review.md";
    };
  }

  /** Seleciona o schema versionado específico da entidade avaliada. */
  static String schemaResourceFor(String processCode) {
    return switch (processCode) {
      case "creative-production-approval" -> "prompts/bpm/creative-customer-review-schema.json";
      case "pde-commercial-homologation-activation" ->
          "prompts/bpm/pde-commercial-homologation-customer-review-schema.json";
      case "pde-construction-approval" -> "prompts/bpm/pde-experience-review-schema.json";
      default -> "prompts/bpm/landing-customer-review-schema.json";
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
        || result.path("evidence").isEmpty()
        || result.path("requiredChanges").isMissingNode()) {
      throw new IllegalArgumentException("Parecer de Psique sem evidências suficientes");
    }
    if ("APPROVED".equals(result.path("decision").asText())
        && result.path("gateChecks").isArray()) {
      for (JsonNode gate : result.path("gateChecks")) {
        if (!"PASS".equals(gate.path("status").asText())) {
          throw new IllegalArgumentException("Parecer de Psique aprovou com gate não aprovado");
        }
      }
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

  /** Lê um contador oficial ou seu alias sem inventar valor ausente. */
  private static Long tokenValue(JsonNode usage, String officialName, String alias) {
    JsonNode value = usage.hasNonNull(officialName) ? usage.get(officialName) : usage.get(alias);
    return value != null && value.isNumber() ? value.longValue() : null;
  }

  /** Monta a falha preservando o consumo ocorrido antes do bloqueio. */
  private Map<String, Object> failureBody(Map<String, Object> task, String error, TokenUsage usage)
      throws IOException {
    Map<String, Object> body = new HashMap<>();
    body.put("error", error);
    body.put("evidenceJson", evidence(task));
    putModelUsage(body, usage);
    return body;
  }

  /** Preserva o contexto acessado e comprova que Psique não realizou efeito externo. */
  private String evidence(Map<String, Object> task) throws IOException {
    return json.writeValueAsString(evidenceFields("Psique", model, task));
  }

  /** Monta os campos mínimos, inclusive a exceção de tier, para reconstruir a mesma tarefa. */
  static Map<String, Object> evidenceFields(
      String reviewer, String model, Map<String, Object> task) {
    return Map.ofEntries(
        Map.entry("reviewer", reviewer),
        Map.entry("model", model),
        Map.entry("sourceReference", String.valueOf(task.get("sourceReference"))),
        Map.entry("activityId", String.valueOf(task.get("activityId"))),
        Map.entry("accessMode", "READ_ONLY"),
        Map.entry("externalSideEffects", false),
        Map.entry(
            "requestedServiceTier", REQUESTED_SERVICE_TIER.toUpperCase(java.util.Locale.ROOT)),
        Map.entry("effectiveServiceTier", EFFECTIVE_SERVICE_TIER),
        Map.entry("serviceTierException", SERVICE_TIER_EXCEPTION));
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

  /** Preserva o resultado funcional junto do consumo da mesma execução. */
  record BpmExecution(JsonNode result, TokenUsage usage) {}

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

    /** Cria a falha técnica com a última medição conhecida. */
    private BpmExecutionException(String message, TokenUsage usage) {
      super(message);
      this.usage = usage;
    }

    /** Cria a falha técnica mantendo também sua causa original. */
    private BpmExecutionException(String message, TokenUsage usage, Throwable cause) {
      super(message, cause);
      this.usage = usage;
    }

    /** Retorna a medição preservada para o callback de falha. */
    private TokenUsage usage() {
      return usage;
    }
  }
}
