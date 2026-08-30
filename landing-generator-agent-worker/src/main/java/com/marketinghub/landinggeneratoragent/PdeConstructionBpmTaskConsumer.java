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
  private static final List<BpmContract> CONTRACTS =
      List.of(
          new BpmContract(
              "venda-entrega-satisfacao-cliente",
              "materialization",
              "prompts/pde-delivery/v1/personalization.md",
              "prompts/pde-delivery/v1/personalization-schema.json",
              "pde-delivery-v1",
              "READY"),
          new BpmContract(
              "pde-construction-approval",
              "journey",
              "prompts/pde-construction/v1/journey.md",
              "prompts/pde-construction/v1/journey-schema.json",
              "pde-construction-v1",
              "READY"),
          new BpmContract(
              "pde-construction-approval",
              "deliverables",
              "prompts/pde-construction/v1/deliverables.md",
              "prompts/pde-construction/v1/deliverables-schema.json",
              "pde-construction-v1",
              "READY"),
          new BpmContract(
              "pde-construction-approval",
              "access",
              "prompts/pde-construction/v1/access.md",
              "prompts/pde-construction/v1/access-schema.json",
              "pde-construction-v1",
              "READY"),
          new BpmContract(
              "pde-commercial-plan-offer",
              "productArchitecture",
              "prompts/pde-commercial-plan/v5/product-architecture.md",
              "prompts/pde-commercial-plan/v5/product-architecture-schema.json",
              "pde-commercial-plan-v5",
              "APPROVE"),
          new BpmContract(
              "pde-tasting-proof-of-value",
              "materialization",
              "prompts/pde-tasting/v1/materialization.md",
              "prompts/pde-tasting/v1/materialization-schema.json",
              "pde-tasting-v1",
              "READY"));

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
      BpmContract contract = contractFor(task);
      execution = execute(task);
      validate(execution.result(), contract);
      if (contract.successDecision().equals(execution.result().path("decision").asText())) {
        report(task, execution);
      } else {
        block(task, execution);
      }
    } catch (Exception ex) {
      log.error(
          "Falha na construção BPM do PDE por Dédalo. taskId={} activityId={}",
          taskId(task),
          activityId(task),
          ex);
      fail(task, ex, execution);
    }
  }

  /** Prioriza entrega paga e percorre somente os contratos de produto pertencentes a Dédalo. */
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

  /** Executa o prompt específico da atividade e preserva os contadores oficiais do Codex. */
  BpmExecution execute(Map<String, Object> task) throws IOException, InterruptedException {
    BpmContract contract = contractFor(task);
    Path output = Files.createTempFile("dedalo-pde-result-", ".json");
    Path processLog = Files.createTempFile("dedalo-pde-process-", ".jsonl");
    Path schema = materialize(contract.schemaResource(), ".json");
    PromptComposition prompt = promptComposition(task);
    try {
      Process process =
          new ProcessBuilder(command(output, processLog, schema))
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      process.getOutputStream().write(prompt.fullPrompt().getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        throw new BpmExecutionException(
            "Timeout da atividade de construção do PDE.",
            readTokenUsage(json, processLog),
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart());
      }
      TokenUsage usage = readTokenUsage(json, processLog);
      if (process.exitValue() != 0) {
        throw new BpmExecutionException(
            "Codex encerrou a construção do PDE com falha: " + Files.readString(processLog),
            usage,
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart());
      }
      try {
        return new BpmExecution(
            json.readTree(Files.readString(output)),
            usage,
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart());
      } catch (IOException ex) {
        log.error(
            "Resposta inválida na construção do PDE. taskId={} activityId={} output={}",
            taskId(task),
            contract.activityId(),
            output,
            ex);
        throw new BpmExecutionException(
            "Resposta de Dédalo não contém JSON válido.",
            usage,
            prompt.fullPrompt(),
            prompt.agentPromptPart(),
            prompt.activityPromptPart(),
            ex);
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
    command.addAll(
        List.of(
            "--config", "model_reasoning_effort=\"" + properties.requiredReasoningEffort() + "\""));
    command.addAll(List.of("--model", properties.getModel()));
    return command;
  }

  /** Resolve o prompt da atividade com o snapshot imutável recebido do backend. */
  private String prompt(Map<String, Object> task) throws IOException {
    return promptComposition(task).fullPrompt();
  }

  /** Compõe o núcleo estável de Dédalo com a missão resolvida da atividade. */
  private PromptComposition promptComposition(Map<String, Object> task) throws IOException {
    BpmContract contract = contractFor(task);
    String agentPromptPart = read("prompts/pde-construction/v1/agent-core.md");
    String activityPromptPart =
        read(contract.promptResource()).replace("{{TASK_CONTEXT}}", json.writeValueAsString(task));
    return new PromptComposition(
        agentPromptPart + "\n\n" + activityPromptPart, agentPromptPart, activityPromptPart);
  }

  /** Persiste a saída funcional, a evidência e o custo antes de liberar a atividade seguinte. */
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

  /** Bloqueia o processo quando Dédalo identifica contrato incompleto ou dependência oculta. */
  private void block(Map<String, Object> task, BpmExecution execution) throws IOException {
    JsonNode result = execution.result();
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Dédalo bloqueou a construção: " + result.path("rationale").asText());
    body.put("resultJson", json.writeValueAsString(result));
    body.put("evidenceJson", evidence(task));
    putModelUsage(body, execution.usage());
    body.put(
        "executionAudit",
        executionAudit(
            execution.promptSent(), execution.agentPromptPart(), execution.activityPromptPart()));
    body.put("blockerGuidance", functionalGuidance(result));
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
      Map<String, Object> body = new HashMap<>();
      body.put("error", ex.toString());
      body.put("evidenceJson", evidence(task));
      putModelUsage(body, usage);
      if (promptSent != null) {
        body.put("executionAudit", executionAudit(promptSent, agentPromptPart, activityPromptPart));
      }
      body.put("blockerGuidance", technicalGuidance());
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

  /** Monta a auditoria integral da chamada executada por Dédalo. */
  private Map<String, Object> executionAudit(
      String promptSent, String agentPromptPart, String activityPromptPart) {
    Map<String, Object> audit = new java.util.LinkedHashMap<>();
    audit.put("executionMode", "MODEL");
    audit.put("modelCode", properties.getModel());
    audit.put("reasoningEffort", properties.requiredReasoningEffort());
    audit.put("promptSent", promptSent);
    audit.put("agentPromptPart", agentPromptPart);
    audit.put("activityPromptPart", activityPromptPart);
    audit.put("accessedUrls", List.of());
    return audit;
  }

  /** Expõe a mudança funcional selecionada antes de permitir nova tentativa. */
  private Map<String, Object> functionalGuidance(JsonNode result) {
    String action = result.path("selectedApproach").asText("").trim();
    if (action.isBlank()) {
      action = "Complete o contrato indicado no parecer de Dédalo e reinicie a tarefa.";
    }
    return Map.of(
        "category",
        "FUNCTIONAL_ADJUSTMENT",
        "recommendedAction",
        action,
        "helpLinks",
        List.of(taskAuditLink()));
  }

  /** Orienta a recuperação da integração sem esconder a causa técnica. */
  private Map<String, Object> technicalGuidance() {
    return Map.of(
        "category", "TECHNICAL_FAILURE",
        "recommendedAction",
            "Verifique a causa técnica registrada, corrija a integração e reinicie a tarefa de Dédalo.",
        "helpLinks", List.of(taskAuditLink()));
  }

  /** Cria o atalho interno comum para a auditoria da tarefa. */
  private Map<String, String> taskAuditLink() {
    return Map.of("label", "Abrir tarefas dos agentes", "url", "/agent-tasks");
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
            contractFor(task).promptVersion(),
            "sourceReference",
            String.valueOf(task.get("sourceReference")),
            "processCode",
            processCode(task),
            "activityId",
            activityId(task),
            "accessMode",
            "READ_ONLY",
            "externalSideEffects",
            false));
  }

  /** Valida o contrato comum e a saída funcional específica da atividade de produto. */
  static void validate(JsonNode result, BpmContract contract) {
    String decision = result.path("decision").asText();
    boolean productArchitecture = "productArchitecture".equals(contract.activityId());
    List<String> allowedDecisions =
        productArchitecture ? List.of("APPROVE", "ADJUST", "REJECT") : List.of("READY", "BLOCKED");
    if (!allowedDecisions.contains(decision)
        || result.path("rationale").asText().isBlank()
        || result.path("alternatives").size() != 3
        || result.path("selectedApproach").asText().length() < 20
        || (!productArchitecture && result.path("acceptanceCriteria").isEmpty())) {
      throw new IllegalArgumentException("Construção do PDE sem decisão comparada e verificável");
    }
    if (productArchitecture && !result.path("productArchitecture").isObject()) {
      throw new IllegalArgumentException("Arquitetura do PDE incompleta");
    }
    if ("pde-construction-approval".equals(contract.processCode())
        && "journey".equals(contract.activityId())
        && (result.path("experienceContract").isMissingNode()
            || result.path("experienceContract").path("stages").size() < 5)) {
      throw new IllegalArgumentException("Jornada do PDE incompleta");
    }
    if ("pde-construction-approval".equals(contract.processCode())
        && "deliverables".equals(contract.activityId())
        && (result.path("deliveryPackage").isMissingNode()
            || result.path("deliveryPackage").path("assets").size() < 6)) {
      throw new IllegalArgumentException("Pacote do PDE incompleto");
    }
    if ("pde-construction-approval".equals(contract.processCode())
        && "access".equals(contract.activityId())
        && (result.path("accessContract").isMissingNode()
            || result.path("accessContract").path("errorStates").isEmpty())) {
      throw new IllegalArgumentException("Contrato de acesso do PDE incompleto");
    }
    if ("pde-tasting-proof-of-value".equals(contract.processCode())
        && (result.path("tastingExperience").path("steps").size() < 3
            || result.path("functionalArtifact").path("content").asText().isBlank()
            || result.path("instrumentationEvents").isEmpty()
            || result.path("testIsolation").asText().isBlank())) {
      throw new IllegalArgumentException("Microexperiência de degustação incompleta");
    }
    if ("venda-entrega-satisfacao-cliente".equals(contract.processCode())
        && (result.path("personalizationPackage").path("contractReference").asText().isBlank()
            || result.path("personalizationPackage").path("deliverables").isEmpty()
            || result.path("qualityChecks").isEmpty()
            || !result.path("accessHandoff").isObject())) {
      throw new IllegalArgumentException("Personalização contratada incompleta");
    }
  }

  /** Valida uma saída de teste usando o mesmo par processo/atividade consumido em produção. */
  static void validate(JsonNode result, String processCode, String activityId) {
    validate(result, contractFor(processCode, activityId));
  }

  /** Informa se o worker possui implementação explícita para o par processo/atividade. */
  static boolean supportsContract(String processCode, String activityId) {
    return CONTRACTS.stream()
        .anyMatch(
            contract ->
                contract.processCode().equals(processCode)
                    && contract.activityId().equals(activityId));
  }

  /** Expõe a ordem de polling para comprovar que venda paga precede trabalho de aquisição. */
  static List<String> contractKeysInPollingOrder() {
    return CONTRACTS.stream()
        .map(contract -> contract.processCode() + "/" + contract.activityId())
        .toList();
  }

  /** Seleciona o prompt imutável pelo contrato completo, inclusive atividades homônimas. */
  static String promptResourceFor(String processCode, String activityId) {
    return contractFor(processCode, activityId).promptResource();
  }

  /** Seleciona o schema imutável pelo contrato completo, inclusive atividades homônimas. */
  static String schemaResourceFor(String processCode, String activityId) {
    return contractFor(processCode, activityId).schemaResource();
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

  /** Extrai o processo declarado pelo backend sem inferir pela origem da tarefa. */
  private static String processCode(Map<String, Object> task) {
    return task == null || task.get("processCode") == null
        ? ""
        : task.get("processCode").toString();
  }

  /** Resolve o contrato recebido e recusa qualquer atividade não registrada no worker. */
  private static BpmContract contractFor(Map<String, Object> task) {
    return contractFor(processCode(task), activityId(task));
  }

  /** Resolve o contrato exato para impedir colisão entre atividades chamadas materialization. */
  private static BpmContract contractFor(String processCode, String activityId) {
    return CONTRACTS.stream()
        .filter(
            contract ->
                contract.processCode().equals(processCode)
                    && contract.activityId().equals(activityId))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Contrato de Dédalo não suportado: " + processCode + "/" + activityId));
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
  record BpmExecution(
      JsonNode result,
      TokenUsage usage,
      String promptSent,
      String agentPromptPart,
      String activityPromptPart) {}

  /** Representa as duas partes e a composição exata enviada ao modelo. */
  private record PromptComposition(
      String fullPrompt, String agentPromptPart, String activityPromptPart) {}

  /** Define o par BPM e os recursos versionados que Dédalo pode executar. */
  record BpmContract(
      String processCode,
      String activityId,
      String promptResource,
      String schemaResource,
      String promptVersion,
      String successDecision) {}

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
    private final String promptSent;
    private final String agentPromptPart;
    private final String activityPromptPart;

    /** Cria a falha com a última medição conhecida. */
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

    /** Cria a falha preservando também a causa original. */
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

    /** Retorna a medição preservada para o callback. */
    private TokenUsage usage() {
      return usage;
    }

    /** Retorna o prompt exato preservado para o callback de falha. */
    private String promptSent() {
      return promptSent;
    }

    /** Retorna o núcleo de Dédalo preservado para o callback de falha. */
    private String agentPromptPart() {
      return agentPromptPart;
    }

    /** Retorna a missão da atividade preservada para o callback de falha. */
    private String activityPromptPart() {
      return activityPromptPart;
    }
  }
}
