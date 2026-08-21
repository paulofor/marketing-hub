package com.marketinghub.landinggeneratoragent;

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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: materializar as atividades de construção do PDE atribuídas a Dédalo. */
@Component
public class PdeConstructionBpmTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(PdeConstructionBpmTaskConsumer.class);
  private static final String AGENT_KEY = "landing-generator";
  private static final String PROCESS_CODE = "pde-construction-approval";
  private static final List<String> ACTIVITIES = List.of("journey", "deliverables", "access");

  private final RestClient backend;
  private final ObjectMapper json;
  private final LandingGeneratorAgentProperties properties;
  private final AutomaticExecutionControl automaticExecution;

  /** Configura a fila canônica, o Codex somente leitura e os contratos versionados do PDE. */
  public PdeConstructionBpmTaskConsumer(
      LandingGeneratorAgentProperties properties,
      ObjectMapper json,
      AutomaticExecutionControl automaticExecution) {
    this.backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
    this.properties = properties;
    this.json = json;
    this.automaticExecution = automaticExecution;
  }

  /** Reserva em PLAY uma única atividade liberada sem decidir o avanço do processo. */
  @Scheduled(cron = "20 */1 * * * *")
  public void processOne() {
    if (!automaticExecution.allowsAutomaticExecution()) return;
    Map<String, Object> task = null;
    BpmExecution execution = null;
    try {
      task = claimNext();
      if (task == null) return;
      execution = execute(task);
      validate(execution.result(), activityId(task));
      if ("READY".equals(execution.result().path("decision").asText())) {
        report(task, execution.result(), execution.usage());
      } else {
        block(task, execution.result(), execution.usage());
      }
    } catch (Exception ex) {
      log.error(
          "Falha na construção BPM do PDE por Dédalo. taskId={} activityId={}",
          taskId(task),
          activityId(task),
          ex);
      TokenUsage usage =
          execution != null
              ? execution.usage()
              : ex instanceof BpmExecutionException bpm ? bpm.usage() : null;
      fail(task, ex, usage);
    }
  }

  /** Procura jornada, entregáveis e acesso nessa ordem sem consumir outras filas de Dédalo. */
  private Map<String, Object> claimNext() {
    for (String activity : ACTIVITIES) {
      List<Map<String, Object>> pending =
          backend
              .get()
              .uri(
                  "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode={processCode}&activityId={activityId}",
                  AGENT_KEY,
                  PROCESS_CODE,
                  activity)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      if (pending != null && !pending.isEmpty()) return pending.get(0);
    }
    return null;
  }

  /** Executa o prompt específico da atividade e preserva os contadores oficiais do Codex. */
  BpmExecution execute(Map<String, Object> task) throws IOException, InterruptedException {
    String activity = activityId(task);
    Path output = Files.createTempFile("dedalo-pde-result-", ".json");
    Path processLog = Files.createTempFile("dedalo-pde-process-", ".jsonl");
    Path schema = materialize(schemaResourceFor(activity), ".json");
    try {
      Process process =
          new ProcessBuilder(command(output, processLog, schema))
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      process.getOutputStream().write(prompt(task).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        throw new BpmExecutionException(
            "Timeout da atividade de construção do PDE.", readTokenUsage(json, processLog));
      }
      TokenUsage usage = readTokenUsage(json, processLog);
      if (process.exitValue() != 0) {
        throw new BpmExecutionException(
            "Codex encerrou a construção do PDE com falha: " + Files.readString(processLog), usage);
      }
      try {
        return new BpmExecution(json.readTree(Files.readString(output)), usage);
      } catch (IOException ex) {
        log.error(
            "Resposta inválida na construção do PDE. taskId={} activityId={} output={}",
            taskId(task),
            activity,
            output,
            ex);
        throw new BpmExecutionException("Resposta de Dédalo não contém JSON válido.", usage, ex);
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o processo Codex com filesystem somente leitura e schema obrigatório. */
  private List<String> command(Path output, Path processLog, Path schema) {
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
                "approval_policy=\"never\""));
    if (properties.getReasoningEffort() != null && !properties.getReasoningEffort().isBlank()) {
      command.addAll(
          List.of(
              "--config", "model_reasoning_effort=\"" + properties.getReasoningEffort() + "\""));
    }
    command.addAll(List.of("--model", properties.getModel()));
    return command;
  }

  /** Resolve o prompt da atividade com o snapshot imutável recebido do backend. */
  private String prompt(Map<String, Object> task) throws IOException {
    return read(promptResourceFor(activityId(task)))
        .replace("{{TASK_CONTEXT}}", json.writeValueAsString(task));
  }

  /** Persiste a saída funcional, a evidência e o custo antes de liberar a atividade seguinte. */
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

  /** Bloqueia o processo quando Dédalo identifica contrato incompleto ou dependência oculta. */
  private void block(Map<String, Object> task, JsonNode result, TokenUsage usage)
      throws IOException {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Dédalo bloqueou a construção: " + result.path("rationale").asText());
    body.put("resultJson", json.writeValueAsString(result));
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

  /** Registra falha técnica preservando contexto e eventual consumo de modelo. */
  private void fail(Map<String, Object> task, Exception ex, TokenUsage usage) {
    if (task == null) return;
    try {
      Map<String, Object> body = new HashMap<>();
      body.put("error", ex.toString());
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
    } catch (Exception callbackEx) {
      log.error(
          "Falha ao registrar bloqueio da construção do PDE. taskId={}", taskId(task), callbackEx);
    }
  }

  /** Declara o contexto acessado e comprova ausência de publicação ou gasto. */
  private String evidence(Map<String, Object> task) throws IOException {
    return json.writeValueAsString(
        Map.of(
            "agent",
            "Dédalo",
            "model",
            properties.getModel(),
            "promptVersion",
            "pde-construction-v1",
            "sourceReference",
            String.valueOf(task.get("sourceReference")),
            "activityId",
            activityId(task),
            "accessMode",
            "READ_ONLY",
            "externalSideEffects",
            false));
  }

  /** Valida os campos comuns e o conjunto mínimo exigido em cada atividade. */
  static void validate(JsonNode result, String activity) {
    if (!List.of("READY", "BLOCKED").contains(result.path("decision").asText())
        || result.path("rationale").asText().isBlank()
        || result.path("alternatives").size() < 3
        || result.path("selectedApproach").asText().length() < 20
        || result.path("acceptanceCriteria").isEmpty()) {
      throw new IllegalArgumentException("Construção do PDE sem decisão comparada e verificável");
    }
    if ("journey".equals(activity)
        && (result.path("experienceContract").isMissingNode()
            || result.path("experienceContract").path("stages").size() < 5)) {
      throw new IllegalArgumentException("Jornada do PDE incompleta");
    }
    if ("deliverables".equals(activity)
        && (result.path("deliveryPackage").isMissingNode()
            || result.path("deliveryPackage").path("assets").size() < 6)) {
      throw new IllegalArgumentException("Pacote do PDE incompleto");
    }
    if ("access".equals(activity)
        && (result.path("accessContract").isMissingNode()
            || result.path("accessContract").path("errorStates").isEmpty())) {
      throw new IllegalArgumentException("Contrato de acesso do PDE incompleto");
    }
  }

  /** Seleciona o prompt imutável correspondente à atividade. */
  static String promptResourceFor(String activity) {
    return "prompts/pde-construction/v1/" + activity + ".md";
  }

  /** Seleciona o schema imutável correspondente à atividade. */
  static String schemaResourceFor(String activity) {
    return "prompts/pde-construction/v1/" + activity + "-schema.json";
  }

  /** Lê a última medição cumulativa de tokens informada pelo Codex. */
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
          log.debug("Linha não JSON ignorada na telemetria do PDE. output={}", output, ex);
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
      log.warn("Falha ao ler tokens da construção do PDE. output={}", output, ex);
      return TokenUsage.empty();
    }
    return informed ? new TokenUsage(input, cached, outputTokens) : TokenUsage.empty();
  }

  /** Lê um contador oficial ou seu alias sem criar valor ausente. */
  private static Long tokenValue(JsonNode usage, String officialName, String alias) {
    JsonNode value = usage.hasNonNull(officialName) ? usage.get(officialName) : usage.get(alias);
    return value != null && value.isNumber() ? value.longValue() : null;
  }

  /** Acrescenta ao callback apenas uma medição real da execução. */
  private void putModelUsage(Map<String, Object> body, TokenUsage usage) {
    if (usage == null || !usage.informed()) return;
    body.put(
        "modelUsages",
        List.of(
            Map.of(
                "modelCode", properties.getModel(),
                "serviceTier", "STANDARD",
                "inputTokens", usage.inputTokens(),
                "cachedInputTokens", usage.cachedInputTokens(),
                "outputTokens", usage.outputTokens())));
  }

  /** Extrai o identificador estável da tarefa reservada. */
  private static long taskId(Map<String, Object> task) {
    return task == null ? -1L : ((Number) task.get("taskId")).longValue();
  }

  /** Extrai a atividade declarada no contrato recebido. */
  private static String activityId(Map<String, Object> task) {
    return task == null || task.get("activityId") == null ? "" : task.get("activityId").toString();
  }

  /** Materializa temporariamente um schema do classpath. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("dedalo-pde-schema-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso versionado do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Preserva resultado e consumo da mesma execução. */
  record BpmExecution(JsonNode result, TokenUsage usage) {}

  /** Representa os contadores reais cumulativos informados pelo Codex. */
  record TokenUsage(Long inputTokens, Long cachedInputTokens, Long outputTokens) {
    /** Indica se ao menos um contador foi efetivamente informado. */
    boolean informed() {
      return inputTokens != null || cachedInputTokens != null || outputTokens != null;
    }

    /** Representa execução sem medição disponível. */
    static TokenUsage empty() {
      return new TokenUsage(null, null, null);
    }
  }

  /** Preserva tokens mesmo quando a execução termina em falha. */
  private static final class BpmExecutionException extends IllegalStateException {
    private final TokenUsage usage;

    /** Cria a falha com a última medição conhecida. */
    private BpmExecutionException(String message, TokenUsage usage) {
      super(message);
      this.usage = usage;
    }

    /** Cria a falha preservando também a causa original. */
    private BpmExecutionException(String message, TokenUsage usage, Throwable cause) {
      super(message, cause);
      this.usage = usage;
    }

    /** Retorna a medição preservada para o callback. */
    private TokenUsage usage() {
      return usage;
    }
  }
}
