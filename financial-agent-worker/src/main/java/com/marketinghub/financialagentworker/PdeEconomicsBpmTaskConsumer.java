package com.marketinghub.financialagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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

/** Responsabilidade: executar a validação econômica BPM atribuída exclusivamente a Plutus. */
@Component
public class PdeEconomicsBpmTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(PdeEconomicsBpmTaskConsumer.class);
  private static final String AGENT_KEY = "financial-agent";
  private static final String PROCESS_CODE = "pde-commercial-plan-offer";
  private static final String ACTIVITY_ID = "economics";
  private static final String PROMPT = "prompts/pde-commercial-plan/v4/economics.md";
  private static final String SCHEMA = "prompts/pde-commercial-plan/v4/economics-schema.json";
  private final RestClient backend;
  private final FinancialAgentProperties properties;
  private final ObjectMapper objectMapper;
  private final AutomaticExecutionControl automaticExecution;

  /** Configura fila canônica, sandbox Codex e controle operacional PLAY/STOP. */
  public PdeEconomicsBpmTaskConsumer(
      FinancialAgentProperties properties,
      ObjectMapper objectMapper,
      AutomaticExecutionControl automaticExecution) {
    this.backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.automaticExecution = automaticExecution;
  }

  /** Reserva e processa no máximo um dossiê cuja estratégia já foi aprovada. */
  @Scheduled(cron = "50 */1 * * * *")
  public void processOne() {
    if (!automaticExecution.allowsAutomaticExecution()) return;
    Map<String, Object> task = null;
    Execution execution = null;
    try {
      task = claim();
      if (task == null) return;
      execution = execute(task);
      validate(execution.result());
      if ("APPROVE".equals(execution.result().path("decision").asText())) {
        complete(task, execution);
      } else {
        block(task, execution);
      }
    } catch (Exception ex) {
      log.error(
          "Falha na economia BPM de Plutus. taskId={} sourceReference={}",
          taskId(task),
          sourceReference(task),
          ex);
      fail(task, execution, ex);
    }
  }

  /** Reserva somente a atividade oficial de economia após a conclusão de Atena. */
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

  /** Executa o prompt financeiro em leitura somente e preserva a resposta bruta. */
  Execution execute(Map<String, Object> task) throws IOException, InterruptedException {
    Path output = Files.createTempFile("plutus-pde-economics-", ".json");
    Path processLog = Files.createTempFile("plutus-pde-economics-", ".jsonl");
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
        throw new IllegalStateException("Timeout da validação econômica de Plutus.");
      }
      TokenUsage usage = readTokenUsage(processLog);
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
            "Codex de Plutus encerrou com falha: " + Files.readString(processLog));
      }
      String raw = Files.readString(output);
      return new Execution(objectMapper.readTree(raw), usage, prompt, raw);
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
    }
  }

  /** Monta o comando imutável com tier permitido, raciocínio e schema auditáveis. */
  private List<String> command(Path output, Path schema) {
    if (properties.getServiceTier() == null
        || !"default".equalsIgnoreCase(properties.getServiceTier().trim())) {
      throw new IllegalArgumentException(
          "Plutus só pode usar default enquanto o catálogo Codex OAuth não anunciar Flex.");
    }
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

  /** Compõe identidade financeira estável e atividade resolvida pelo backend. */
  private PromptComposition prompt(Map<String, Object> task) throws IOException {
    String agent = read("prompts/financial-agent/v1/agent-core.md");
    String activity =
        read(PROMPT).replace("{{TASK_CONTEXT}}", objectMapper.writeValueAsString(task));
    return new PromptComposition(agent + "\n\n" + activity, agent, activity);
  }

  /** Persiste economia, evidência, auditoria e tokens antes de liberar Dédalo. */
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

  /** Bloqueia a cadeia quando contribuição, limites ou controles não forem seguros. */
  private void block(Map<String, Object> task, Execution execution) throws IOException {
    Map<String, Object> body = callback(task, execution);
    body.put(
        "error", "Plutus bloqueou a economia: " + execution.result().path("rationale").asText());
    body.put(
        "blockerGuidance",
        Map.of(
            "category",
            "COMMERCIAL_RISK",
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

  /** Registra falha técnica preservando o contexto para retentativa auditável. */
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
              "Corrija a falha técnica registrada e reinicie a atividade de Plutus.",
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
      log.error("Falha ao registrar bloqueio de Plutus. taskId={}", taskId(task), callbackEx);
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

  /** Declara o modelo, esforço e prompts exatos da decisão econômica. */
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

  /** Preserva a origem e confirma ausência de movimentação financeira ou publicação. */
  private String evidence(Map<String, Object> task) throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "agent",
            "Plutus",
            "promptVersion",
            "pde-commercial-plan-v4",
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
            properties.getServiceTierExceptionReason()));
  }

  /** Rejeita economia sem cenários, números reconciliáveis, prazo ou regra de decisão. */
  static void validate(JsonNode result) {
    JsonNode economics = result.path("economics");
    if (!List.of("APPROVE", "ADJUST", "REJECT").contains(result.path("decision").asText())
        || result.path("scenarios").size() != 3
        || !economics.isObject()
        || !result.path("metrics").isObject()
        || result.path("rationale").asText().isBlank()) {
      throw new IllegalArgumentException("Economia PDE fora do contrato versionado de Plutus.");
    }
    LocalDate.parse(economics.path("deadline").asText());
    BigDecimal price = economics.path("offerPriceBrl").decimalValue();
    BigDecimal variable = economics.path("variableCostPerSaleBrl").decimalValue();
    BigDecimal contribution = economics.path("contributionPerSaleBrl").decimalValue();
    if (price.subtract(variable).subtract(contribution).abs().compareTo(new BigDecimal("0.01"))
        > 0) {
      throw new IllegalArgumentException(
          "Contribuição por venda não reconcilia com preço e custo.");
    }
    if ("APPROVE".equals(result.path("decision").asText())
        && contribution.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Plutus não pode aprovar contribuição não positiva.");
    }
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
      log.warn("Falha ao ler tokens de Plutus. output={}", processLog, ex);
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
    Path path = Files.createTempFile("plutus-pde-resource-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Retorna a primeira correção pedida ou uma orientação econômica segura. */
  private String firstRequiredChange(JsonNode result) {
    JsonNode changes = result.path("requiredChanges");
    return changes.isArray() && !changes.isEmpty()
        ? changes.get(0).asText()
        : "Revise o envelope econômico indicado por Plutus e reinicie a atividade.";
  }

  /** Cria o atalho interno comum para a auditoria da tarefa. */
  private Map<String, String> taskLink() {
    return Map.of("label", "Abrir tarefas dos agentes", "url", "/agent-tasks");
  }

  /** Retorna o modelo efetivo configurado para Plutus. */
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

  /** Representa identidade, atividade e composição exata enviada ao modelo. */
  record PromptComposition(String full, String agent, String activity) {}

  /** Representa somente contadores efetivamente observados no runtime. */
  record TokenUsage(long input, long cached, long output, boolean informed) {}
}
