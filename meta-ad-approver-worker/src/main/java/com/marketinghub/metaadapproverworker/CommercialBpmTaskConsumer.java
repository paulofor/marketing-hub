package com.marketinghub.metaadapproverworker;

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
  private static final List<BpmContract> CONTRACTS =
      List.of(
          new BpmContract("pde-communication-sales-journey", "contract"),
          new BpmContract("creative-production-approval", "library"),
          new BpmContract("creative-production-approval", "brief"),
          new BpmContract("creative-production-approval", "generate"),
          new BpmContract("creative-production-approval", "commercial"),
          new BpmContract("landing-page-generation", "commercial"),
          new BpmContract("pde-construction-approval", "deliverables"));
  private final RestClient backend;
  private final ObjectMapper json;
  private final String codex;
  private final String model;
  private final String repositoryPath;
  private final PdeReviewArtifactLoader pdeArtifactLoader;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Configura a fila canônica e a sandbox independente de Têmis. */
  public CommercialBpmTaskConsumer(
      MetaAdApproverProperties properties,
      @Value("${CODEX_COMMAND:codex}") String codex,
      @Value("${CODEX_MODEL:gpt-5.6-sol}") String model,
      @Value("${MARKETING_HUB_REPOSITORY:/workspace/marketing-hub}") String repositoryPath,
      ObjectMapper json) {
    this.backend = BackendRestClientFactory.create(properties);
    this.codex = codex;
    this.model = model;
    this.repositoryPath = repositoryPath;
    this.pdeArtifactLoader = new PdeReviewArtifactLoader(repositoryPath);
    this.json = json;
  }

  /** Reserva em PLAY e revisa uma atividade liberada sem decidir a próxima etapa. */
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
      validate(result);
      if ("APPROVED".equals(result.path("decision").asText()))
        report(task, result, execution.usage());
      else block(task, result, execution.usage());
    } catch (Exception ex) {
      log.error("Falha no gate comercial BPM de Têmis. taskId={}", taskId(task), ex);
      TokenUsage usage =
          execution != null
              ? execution.usage()
              : ex instanceof BpmExecutionException bpm ? bpm.usage() : null;
      fail(task, ex, usage);
    }
  }

  /** Reserva primeiro criativos e depois landings sem cruzar gates independentes. */
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

  /** Executa o prompt versionado e valida coerência, compliance e prontidão comercial. */
  BpmExecution execute(Map<String, Object> task) throws IOException, InterruptedException {
    Path output = Files.createTempFile("temis-bpm-result-", ".json");
    Path processLog = Files.createTempFile("temis-bpm-process-", ".log");
    Path schema = materialize(schemaResourceFor(processCode(task)), ".json");
    try {
      List<String> command =
          new ArrayList<>(
              List.of(
                  codex,
                  "--search",
                  "exec",
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
            "Timeout do gate BPM de Têmis após 40 minutos.", readTokenUsage(json, processLog));
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
            "Resposta inválida no gate BPM de Têmis. taskId={} output={}",
            taskId(task),
            output,
            ex);
        throw new BpmExecutionException("Resposta de Têmis não contém JSON válido.", usage, ex);
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
    }
  }

  /** Persiste a decisão auditável sem publicar landing, campanha ou experimento. */
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

  /** Mantém o gate fechado diante de falha técnica ou parecer inválido. */
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
      log.error("Falha ao registrar bloqueio BPM de Têmis. taskId={}", taskId(task), callbackEx);
    }
  }

  /** Preserva o parecer funcional e mantém o processo fechado quando o gate reprova. */
  private void block(Map<String, Object> task, JsonNode result, TokenUsage usage)
      throws IOException {
    String resultJson = json.writeValueAsString(result);
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Têmis bloqueou o avanço: " + result.path("commercialRationale").asText());
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

  /** Resolve o contexto congelado da tarefa no prompt versionado. */
  private String prompt(Map<String, Object> task) throws IOException {
    Map<String, Object> promptContext = new HashMap<>(task);
    if ("pde-construction-approval".equals(processCode(task))) {
      promptContext.put("versionedArtifactEvidence", pdeArtifactLoader.load());
    }
    return read(promptResourceFor(processCode(task)))
        .replace("{{TASK_CONTEXT}}", json.writeValueAsString(promptContext));
  }

  /** Seleciona o prompt versionado específico do gate avaliado. */
  static String promptResourceFor(String processCode) {
    return switch (processCode) {
      case "pde-communication-sales-journey" -> "prompts/bpm/pde-communication-review.md";
      case "creative-production-approval" -> "prompts/bpm/creative-commercial-review.md";
      case "pde-construction-approval" -> "prompts/bpm/pde-deliverables-review.md";
      default -> "prompts/bpm/landing-commercial-review.md";
    };
  }

  /** Seleciona o schema versionado específico do gate avaliado. */
  static String schemaResourceFor(String processCode) {
    return switch (processCode) {
      case "pde-communication-sales-journey" -> "prompts/bpm/pde-communication-review-schema.json";
      case "creative-production-approval" -> "prompts/bpm/creative-commercial-review-schema.json";
      case "pde-construction-approval" -> "prompts/bpm/pde-deliverables-review-schema.json";
      default -> "prompts/bpm/landing-commercial-review-schema.json";
    };
  }

  /** Lê o processo congelado no contrato da tarefa reservada. */
  private String processCode(Map<String, Object> task) {
    Object value = task.get("processCode");
    return value == null ? "" : value.toString();
  }

  /** Exige decisão, evidências e coerência explícita com o plano comercial. */
  static void validate(JsonNode result) {
    if (!List.of("APPROVED", "ADJUST", "BLOCKED").contains(result.path("decision").asText())) {
      throw new IllegalArgumentException("Gate de Têmis sem decisão válida");
    }
    if (result.path("commercialRationale").asText().isBlank()
        || result.path("evidence").isEmpty()
        || result.path("requiredChanges").isMissingNode()) {
      throw new IllegalArgumentException("Gate de Têmis sem evidências suficientes");
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
  private Map<String, Object> failureBody(Map<String, Object> task, String error, TokenUsage usage)
      throws IOException {
    Map<String, Object> body = new HashMap<>();
    body.put("error", error);
    body.put("evidenceJson", evidence(task));
    putModelUsage(body, usage);
    return body;
  }

  /** Preserva o contexto acessado e comprova que Têmis não realizou efeito externo. */
  private String evidence(Map<String, Object> task) throws IOException {
    return json.writeValueAsString(evidenceFields("Têmis", model, task));
  }

  /** Monta os campos mínimos que tornam sucesso e falha reconstruíveis na mesma tarefa. */
  static Map<String, Object> evidenceFields(
      String reviewer, String model, Map<String, Object> task) {
    return Map.of(
        "reviewer",
        reviewer,
        "model",
        model,
        "sourceReference",
        String.valueOf(task.get("sourceReference")),
        "activityId",
        String.valueOf(task.get("activityId")),
        "accessMode",
        "READ_ONLY",
        "externalSideEffects",
        false);
  }

  /** Acrescenta ao callback somente uma medição real informada pelo Codex. */
  private void putModelUsage(Map<String, Object> body, TokenUsage usage) {
    if (usage == null || !usage.informed()) return;
    body.put(
        "modelUsages",
        List.of(
            Map.of(
                "modelCode", model,
                "serviceTier", "STANDARD",
                "inputTokens", usage.inputTokens(),
                "cachedInputTokens", usage.cachedInputTokens(),
                "outputTokens", usage.outputTokens())));
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
