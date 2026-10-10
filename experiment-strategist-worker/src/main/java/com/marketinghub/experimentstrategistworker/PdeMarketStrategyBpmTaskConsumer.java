package com.marketinghub.experimentstrategistworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: executar a estratégia de mercado BPM atribuída exclusivamente a Atena. */
@Component
public class PdeMarketStrategyBpmTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(PdeMarketStrategyBpmTaskConsumer.class);
  private static final ObjectMapper CONTRACT_MAPPER = new ObjectMapper();
  private static final String AGENT_KEY = "experiment-strategist";
  private static final String PROCESS_CODE = "pde-commercial-plan-offer";
  private static final String ACTIVITY_ID = "marketStrategy";
  private static final String LEGACY_PROMPT = "prompts/pde-commercial-plan/v8/market-strategy.md";
  private static final String LEGACY_SCHEMA =
      "prompts/pde-commercial-plan/v7/market-strategy-schema.json";
  private static final String IDENTITY_PROMPT = "prompts/pde-commercial-plan/v9/market-strategy.md";
  private static final String IDENTITY_SCHEMA =
      "prompts/pde-commercial-plan/v9/market-strategy-schema.json";
  private static final String AGENT_VALIDATION_PROMPT =
      "prompts/pde-commercial-plan/v11/market-strategy.md";
  private static final String AGENT_VALIDATION_SCHEMA =
      "prompts/pde-commercial-plan/v10/market-strategy-schema.json";
  private static final String READY_FOR_PRIVATE_VALIDATION = "READY_FOR_PRIVATE_VALIDATION";
  private static final String READY_FOR_AGENT_VALIDATION = "READY_FOR_AGENT_VALIDATION";
  private static final String INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
  private static final List<String> REQUIRED_PRIVATE_SIGNALS =
      List.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");
  private static final List<String> REQUIRED_AGENT_SCENARIOS =
      List.of("ADHERENT", "RECOVERY", "SAFETY");
  private static final List<String> REQUIRED_AGENT_DEVICES =
      List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7");
  private final RestClient backend;
  private final WorkerProperties properties;
  private final ObjectMapper objectMapper;
  private final AutomaticExecutionControl automaticExecution;
  private final PdeMarketStrategyOutbox outbox;
  private final PdeMarketStrategyPublicationPause publicationPause;

  /** Configura fila canônica, sandbox Codex e controle operacional PLAY/STOP. */
  public PdeMarketStrategyBpmTaskConsumer(
      WorkerProperties properties,
      ObjectMapper objectMapper,
      AutomaticExecutionControl automaticExecution) {
    var requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(10000);
    requests.setReadTimeout(30000);
    this.backend =
        RestClient.builder().baseUrl(properties.getBackendUrl()).requestFactory(requests).build();
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.automaticExecution = automaticExecution;
    this.outbox =
        new PdeMarketStrategyOutbox(Path.of(properties.getBpmStateDirectory()), objectMapper);
    this.publicationPause =
        new PdeMarketStrategyPublicationPause(Path.of(properties.getBpmStateDirectory()));
  }

  /** Reenvia entregas e só reserva estratégia em PLAY, fora da pausa protegida de publicação. */
  @Scheduled(cron = "40 */1 * * * *")
  public synchronized void processOne() {
    try (var lock = outbox.lock()) {
      if (lock == null) return;
      var pending = outbox.read();
      if (pending != null && pending.callback() != null) {
        deliver(pending);
        return;
      }
      if (pending != null && pending.modelStarted()) {
        var failure =
            technicalFailure(
                pending.task(),
                pending.audit(),
                new IllegalStateException(
                    "Execução de Atena interrompida antes de confirmar o resultado; reinicie a atividade pelo BPM."));
        enqueue(pending.task(), pending.audit(), "failure", failure);
        return;
      }
      if (publicationPause.paused() || !automaticExecution.allowsAutomaticExecution()) return;
      Map<String, Object> task = pending == null ? claim() : pending.task();
      if (task == null) return;
      if (pending == null) {
        outbox.save(new PdeMarketStrategyOutbox.Pending(task, null, false, null, null));
      }
      PromptComposition prompt;
      try {
        validateTaskContext(task);
        prompt = prompt(task);
      } catch (Exception ex) {
        log.error(
            "Atena recusou entrada antes do modelo. taskId={} sourceReference={}",
            taskId(task),
            sourceReference(task),
            ex);
        var notStarted =
            Map.<String, Object>of(
                "executionMode",
                "NOT_STARTED",
                "reasoningEffort",
                "NOT_APPLICABLE",
                "accessedUrls",
                List.of());
        enqueue(task, notStarted, "failure", technicalFailure(task, notStarted, ex));
        return;
      }
      Map<String, Object> audit = audit(prompt);
      outbox.save(new PdeMarketStrategyOutbox.Pending(task, audit, false, null, null));
      backend
          .put()
          .uri(
              "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/execution-audit",
              AGENT_KEY,
              taskId(task))
          .body(audit)
          .retrieve()
          .toBodilessEntity();
      outbox.save(new PdeMarketStrategyOutbox.Pending(task, audit, true, null, null));
      Execution execution = null;
      try {
        execution = execute(task, prompt);
        validate(
            execution.result(),
            sourceReference(task),
            requiresProductIdentity(task),
            objectMapper.valueToTree(task));
      } catch (Exception ex) {
        log.error(
            "Falha na inferência de Atena. taskId={} sourceReference={}",
            taskId(task),
            sourceReference(task),
            ex);
        Map<String, Object> failure = technicalFailure(task, audit, ex);
        if (execution != null) failure.putAll(callback(task, execution));
        failure.put("error", "Atena não concluiu a estratégia: " + ex.getMessage());
        enqueue(task, audit, "failure", failure);
        return;
      }
      String operation =
          "APPROVE".equals(execution.result().path("decision").asText()) ? "result" : "failure";
      Map<String, Object> body = callback(task, execution);
      if ("failure".equals(operation)) {
        body.put(
            "error",
            "Atena bloqueou a estratégia: " + execution.result().path("rationale").asText());
        body.put(
            "blockerGuidance",
            Map.of(
                "category",
                "MISSING_EVIDENCE",
                "recommendedAction",
                firstRequiredChange(execution.result()),
                "helpLinks",
                List.of(taskLink())));
      }
      enqueue(task, audit, operation, body);
    } catch (Exception ex) {
      log.error(
          "Atena preservou a entrega local para retomada; nenhuma nova inferência será iniciada. backend={}",
          properties.getBackendUrl(),
          ex);
    }
  }

  /**
   * Persiste o envelope completo antes de tentar entregá-lo, sem trocar falha de rede por parecer.
   */
  private void enqueue(
      Map<String, Object> task,
      Map<String, Object> audit,
      String operation,
      Map<String, Object> body)
      throws IOException {
    var pending = new PdeMarketStrategyOutbox.Pending(task, audit, true, operation, body);
    outbox.save(pending);
    deliver(pending);
  }

  /** Reenvia o mesmo callback e remove a cópia local somente após confirmação do backend. */
  private void deliver(PdeMarketStrategyOutbox.Pending pending) throws IOException {
    log.info(
        "Atena enviando callback preservado. taskId={} sourceReference={} operation={}",
        taskId(pending.task()),
        sourceReference(pending.task()),
        pending.operation());
    backend
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/{operation}",
            AGENT_KEY,
            taskId(pending.task()),
            pending.operation())
        .body(pending.callback())
        .retrieve()
        .toBodilessEntity();
    log.info(
        "Atena recebeu confirmação do callback. taskId={} operation={}",
        taskId(pending.task()),
        pending.operation());
    outbox.acknowledge();
  }

  /** Preserva auditoria, resposta disponível e consumo real em falha ou interrupção do modelo. */
  private Map<String, Object> technicalFailure(
      Map<String, Object> task, Map<String, Object> audit, Exception ex) throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", ex.toString());
    body.put("evidenceJson", evidence(task, audit));
    if (audit != null) body.put("executionAudit", audit);
    if (Files.exists(outbox.output())) body.put("resultJson", Files.readString(outbox.output()));
    if (Files.exists(outbox.events()))
      putUsage(
          body,
          readTokenUsage(outbox.events()),
          audit == null ? modelCode() : String.valueOf(audit.get("modelCode")));
    body.put(
        "blockerGuidance",
        Map.of(
            "category",
            "TECHNICAL_FAILURE",
            "recommendedAction",
            "Corrija a falha técnica registrada e reinicie a atividade de Atena pelo BPM.",
            "helpLinks",
            List.of(taskLink())));
    return body;
  }

  /** Reserva a atividade oficial com handshake que impede claims da imagem anterior ao rollout. */
  private Map<String, Object> claim() {
    List<Map<String, Object>> pending =
        backend
            .get()
            .uri(
                "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode={process}&activityId={activity}&workerContract={contract}",
                AGENT_KEY,
                PROCESS_CODE,
                ACTIVITY_ID,
                "ATENA_PDE_MARKET_STRATEGY_V1")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return pending == null || pending.isEmpty() ? null : pending.getFirst();
  }

  /** Executa o prompt versionado preservando resposta e eventos até confirmação do callback. */
  Execution execute(Map<String, Object> task, PromptComposition prompt)
      throws IOException, InterruptedException {
    Path output = outbox.output();
    Path processLog = outbox.events();
    Path schema = materialize(schemaResource(task), ".json");
    Process process = null;
    try {
      process =
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
      if (process != null && process.isAlive()) terminateTree(process);
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

  /** Distingue o primeiro planejamento de um sucessor e impede contexto cruzado ou incompleto. */
  private void validateTaskContext(Map<String, Object> task) throws IOException {
    JsonNode context =
        objectMapper.readTree(String.valueOf(task.getOrDefault("processContextJson", "{}")));
    JsonNode cycle = context.path("learningSalesCycle");
    JsonNode target = objectMapper.valueToTree(task.get("taskTarget"));
    if (requiresProductIdentity(task)
        && !sourceReference(task).startsWith("product-discovery-cycle:")) {
      JsonNode product = target.path("pdeContext").path("product");
      if (!hasText(target, "productInternalName")
          || !hasText(product, "productTypeCode")
          || !hasText(product, "productTypeInternalName")) {
        throw new IllegalArgumentException(
            "O produto existente não possui identidade catalogada suficiente para preservação.");
      }
    }
    if (requiresProductIdentity(task)
        && sourceReference(task).startsWith("product-discovery-cycle:")) {
      JsonNode policy = productIdentityPolicy(objectMapper.valueToTree(task));
      if (!"PRODUCT_IDENTITY_V1".equals(policy.path("contractVersion").asText())
          || !"STAR".equals(policy.path("internalNameUniverse").asText())
          || !policy.path("reservedInternalNames").isArray()
          || !policy.path("activeProductTypes").isArray()
          || policy.path("activeProductTypes").isEmpty()) {
        throw new IllegalArgumentException(
            "A descoberta não recebeu catálogo e nomes reservados para decidir a identidade.");
      }
    }
    if (!cycle.isMissingNode() && !cycle.isNull()) {
      if (!sourceReference(task).equals("experiment:" + cycle.path("experimentId").asLong(-1))
          || !cycle.path("productId").canConvertToLong()
          || cycle.path("productId").asLong() != target.path("productId").asLong(-1)) {
        throw new IllegalArgumentException(
            "O ciclo de aprendizado não corresponde ao produto e experimento da tarefa.");
      }
      implementedInput(objectMapper.valueToTree(task));
      return;
    }
    if (sourceReference(task).startsWith("experiment:")) {
      JsonNode planning = target.path("pdeContext");
      JsonNode experiment = planning.path("experiment");
      if (!"PDE_COMMERCIAL_PLANNING_INPUT_V1".equals(planning.path("contractVersion").asText())
          || !"INITIAL_PLANNED_EXPERIMENT".equals(planning.path("mode").asText())
          || !sourceReference(task).equals("experiment:" + target.path("experimentId").asLong(-1))
          || experiment.path("id").asLong(-1) != target.path("experimentId").asLong(-2)
          || planning.path("product").path("id").asLong(-1) != target.path("productId").asLong(-2)
          || !experiment.path("sourceExperimentId").isNull()
          || planning.path("hypothesis").path("id").asText().isBlank()
          || !planning.path("commercialPlan").path("id").canConvertToLong()) {
        throw new IllegalArgumentException(
            "O experimento não possui contexto inicial completo nem ciclo sucessor rastreável.");
      }
    }
  }

  /** Compõe identidade permanente e missão específica sem hardcode de contrato na classe. */
  private PromptComposition prompt(Map<String, Object> task) throws IOException {
    String agent = read("prompts/experiment-strategist/v1/agent-core.md");
    String activity =
        read(promptResource(task))
            .replace("{{TASK_CONTEXT}}", objectMapper.writeValueAsString(task));
    return new PromptComposition(agent + "\n\n" + activity, agent, activity);
  }

  /** Monta o callback comum sem omitir o envelope de execução do modelo. */
  private Map<String, Object> callback(Map<String, Object> task, Execution execution)
      throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("resultJson", execution.raw());
    body.put("evidenceJson", evidence(task));
    body.put("executionAudit", audit(execution.prompt()));
    putUsage(body, execution.usage(), modelCode());
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
    return evidence(task, null);
  }

  /** Usa a versão auditada antes da inferência, sem atribuir o prompt novo a um trabalho antigo. */
  private String evidence(Map<String, Object> task, Map<String, Object> audit) throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "agent",
            "Atena",
            "promptVersion",
            audit != null
                ? auditedPromptVersion(audit)
                : requiresAgentValidation(task)
                    ? "pde-commercial-plan-v11"
                    : requiresProductIdentity(task)
                        ? "pde-commercial-plan-v9"
                        : "pde-commercial-plan-v8",
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

  /** Recupera a versão da auditoria ou do cabeçalho legado; ausência não vira versão corrente. */
  private String auditedPromptVersion(Map<String, Object> audit) {
    Object declared = audit.get("promptVersion");
    if (declared instanceof String value && value.startsWith("pde-commercial-plan-v")) return value;
    Object part = audit.get("activityPromptPart");
    if (part instanceof String value) {
      var match =
          java.util.regex.Pattern.compile("(?m)^# Atividade[^\\n]*Processo 2 v([0-9]+)\\b")
              .matcher(value);
      if (match.find()) return "pde-commercial-plan-v" + match.group(1);
    }
    return "UNKNOWN_PROMPT_VERSION";
  }

  /** Rejeita estratégia sem comparação, contrato versionado ou justificativa. */
  static void validate(JsonNode result) {
    validate(result, "product-discovery-cycle:test", false, null);
  }

  /** Valida seleção factual e o plano privado sem antecipar prontidão comercial. */
  static void validate(JsonNode result, String sourceReference) {
    validate(result, sourceReference, false, null);
  }

  /** Valida identidade, entrada executável preservada e homologação multiagente do Processo 2. */
  static void validate(
      JsonNode result, String sourceReference, boolean requiresProductIdentity, JsonNode task) {
    JsonNode contract = result.path("marketStrategicContract");
    boolean agentValidation = requiresAgentValidation(task);
    JsonNode validationPlan =
        contract.path(agentValidation ? "agentValidationPlan" : "privateValidationPlan");
    String decision = result.path("decision").asText();
    String status = contract.path("status").asText();
    String expectedVersion = agentValidation ? "MARKET_STRATEGY_V4" : "MARKET_STRATEGY_V3";
    String readyStatus =
        agentValidation ? READY_FOR_AGENT_VALIDATION : READY_FOR_PRIVATE_VALIDATION;
    if (!List.of("APPROVE", "ADJUST", "REJECT").contains(result.path("decision").asText())
        || result.path("alternatives").size() != 3
        || result.path("selectedAlternative").asText().isBlank()
        || !contract.isObject()
        || !expectedVersion.equals(contract.path("contractVersion").asText())
        || !List.of(readyStatus, INSUFFICIENT_EVIDENCE).contains(status)
        || result.path("rationale").asText().isBlank()) {
      throw new IllegalArgumentException("Estratégia PDE fora do contrato versionado de Atena.");
    }
    if (("APPROVE".equals(decision) && !readyStatus.equals(status))
        || (!"APPROVE".equals(decision) && !INSUFFICIENT_EVIDENCE.equals(status))) {
      throw new IllegalArgumentException(
          "A decisão de Atena não corresponde à prontidão declarada no contrato.");
    }
    if (sourceReference != null
        && sourceReference.startsWith("product-discovery-cycle:")
        && "APPROVE".equals(decision)
        && (!result.path("selectedDossierId").canConvertToLong()
            || !result.path("selectedOpportunityId").canConvertToLong())) {
      throw new IllegalArgumentException("Atena aprovou sem selecionar uma candidata factual.");
    }
    if ("APPROVE".equals(decision)
        && (agentValidation
            ? !validAgentValidationPlan(validationPlan)
            : !validPrivateValidationPlan(validationPlan))) {
      throw new IllegalArgumentException(
          agentValidation
              ? "Atena aprovou sem um plano completo de homologação multiagente."
              : "Atena aprovou sem um plano completo de duas leituras privadas.");
    }
    if (requiresProductIdentity) {
      validateProductIdentity(result.path("productIdentity"), decision, sourceReference, task);
    }
    if (agentValidation && "APPROVE".equals(decision)) {
      JsonNode implemented = implementedInput(task);
      if (implemented.isObject()
          && !implemented
              .path("minimumCustomerInput")
              .asText()
              .equals(
                  validationPlan
                      .path("customerValueDelivery")
                      .path("minimumCustomerInput")
                      .asText()))
        throw new IllegalArgumentException(
            "Atena alterou a entrada implementada da candidata; preserve o contrato recebido.");
    }
  }

  /** Confere a origem da descrição implementada sem aceitar outro produto, ciclo ou versão. */
  private static JsonNode implementedInput(JsonNode task) {
    if (task == null) return CONTRACT_MAPPER.missingNode();
    try {
      JsonNode cycle =
          CONTRACT_MAPPER
              .readTree(task.path("processContextJson").asText("{}"))
              .path("learningSalesCycle");
      JsonNode input = cycle.path("implementedInput");
      if (input.isMissingNode() || input.isNull()) return CONTRACT_MAPPER.missingNode();
      if (!input.isObject()
          || !"PDE_IMPLEMENTED_INPUT_V1".equals(input.path("contractVersion").asText())
          || !hasText(input, "minimumCustomerInput")
          || !hasText(input, "sourceReference")
          || !input.path("requiredFields").isArray()
          || input.path("requiredFields").isEmpty()
          || input.path("productId").asLong(-1) != cycle.path("productId").asLong(-2)
          || input.path("productId").asLong(-1)
              != task.path("taskTarget").path("productId").asLong(-2)
          || input.path("cycleId").asLong(-1) != cycle.path("cycleId").asLong(-2)
          || input.path("experimentId").asLong(-1) != cycle.path("experimentId").asLong(-2)
          || !task.path("sourceReference")
              .asText()
              .equals("experiment:" + cycle.path("experimentId").asLong(-2))
          || !input.path("prototypeVersion").asText().equals(cycle.path("productVersion").asText()))
        throw new IllegalArgumentException(
            "A entrada implementada não corresponde ao alvo da tarefa.");
      return input;
    } catch (IOException ex) {
      log.error(
          "Falha ao conferir entrada implementada de Atena sourceReference={}",
          task.path("sourceReference").asText(),
          ex);
      throw new IllegalArgumentException("Contexto da entrada implementada inválido.", ex);
    }
  }

  /** Preserva a validação estrita do contrato histórico sem usá-lo em execuções novas. */
  private static boolean validPrivateValidationPlan(JsonNode validationPlan) {
    return validationPlan.isObject()
        && validationPlan.path("minimumIndependentReadings").asInt(0) == 2
        && validationPlan.path("minimumEligibleParticipantsPerReading").asInt(0) == 1
        && containsAllPrivateSignals(validationPlan.path("requiredSignals"))
        && unitRate(validationPlan, "minimumExperienceStartRate")
        && unitRate(validationPlan, "minimumValueMomentRate")
        && unitRate(validationPlan, "minimumReadyResultUseRate")
        && unitRate(validationPlan, "minimumPrototypePreferenceRate")
        && unitRate(validationPlan, "minimumCheckoutStartRate")
        && validationPlan.path("sourceMaxAgeDays").asInt(0) >= 1
        && validationPlan.path("sourceMaxAgeDays").asInt(0) <= 90
        && hasText(validationPlan, "prototypeObjective")
        && completePurchaseScene(validationPlan.path("purchaseScene"))
        && canonicalCustomerValueDelivery(validationPlan.path("humanValueDelivery"))
        && hasText(validationPlan, "strongestFreeAlternative")
        && hasText(validationPlan, "prototypeAdvantage")
        && hasText(validationPlan, "publicationBoundary")
        && (!validationPlan.path("sourceRefreshRequired").asBoolean(false)
            || hasText(validationPlan, "sourceRefreshAction"));
  }

  /** Exige o plano canônico sem multiplicar a matriz técnica pelos pareceres de experiência. */
  private static boolean validAgentValidationPlan(JsonNode plan) {
    return plan.isObject()
        && "PDE_AGENT_VALIDATION_V1".equals(plan.path("contractVersion").asText())
        && plan.path("technicalMatrixRuns").asInt(-1) == 1
        && plan.path("independentExperienceReviewCount").asInt(-1) == 3
        && completePurchaseScene(plan.path("purchaseScene"))
        && canonicalCustomerValueDelivery(plan.path("customerValueDelivery"))
        && hasText(plan, "hypothesis")
        && hasText(plan, "prototypeObjective")
        && hasText(plan, "strongestFreeAlternative")
        && hasText(plan, "prototypeAdvantage")
        && exactValues(plan.path("requiredScenarios"), REQUIRED_AGENT_SCENARIOS)
        && exactValues(plan.path("requiredDevices"), REQUIRED_AGENT_DEVICES)
        && plan.path("maxReadyResultSeconds").asInt(0) == 600
        && "AGENT_VALIDATION".equals(plan.path("trafficClass").asText())
        && "mh_internal_test".equals(plan.path("internalMarker").asText())
        && !plan.path("humanEvidenceClaimed").asBoolean(true)
        && !plan.path("commercialEvidenceClaimed").asBoolean(true)
        && !plan.path("paymentEnabled").asBoolean(true)
        && !plan.path("publicationAuthorized").asBoolean(true)
        && !plan.path("campaignAuthorized").asBoolean(true)
        && plan.path("mediaSpendAuthorizedBrl").asInt(-1) == 0
        && plan.path("sourceMaxAgeDays").asInt(0) >= 1
        && plan.path("sourceMaxAgeDays").asInt(0) <= 90
        && hasText(plan, "publicationBoundary")
        && (!plan.path("sourceRefreshRequired").asBoolean(false)
            || hasText(plan, "sourceRefreshAction"));
  }

  /** Confirma criação única na descoberta e preservação exata para produtos já cadastrados. */
  private static void validateProductIdentity(
      JsonNode identity, String decision, String sourceReference, JsonNode task) {
    String expectedMode =
        "APPROVE".equals(decision)
            ? sourceReference != null && sourceReference.startsWith("product-discovery-cycle:")
                ? "CREATE"
                : "PRESERVE"
            : "NOT_APPLICABLE";
    if (!identity.isObject()
        || !"PRODUCT_IDENTITY_V1".equals(identity.path("contractVersion").asText())
        || !expectedMode.equals(identity.path("mode").asText())) {
      throw new IllegalArgumentException(
          "Atena não devolveu a identidade PRODUCT_IDENTITY_V1 no modo esperado.");
    }
    if (!"APPROVE".equals(decision)) return;
    if (!hasText(identity, "internalName")
        || !hasText(identity, "productTypeCode")
        || !hasText(identity, "productTypeInternalName")
        || !hasText(identity, "classificationRationale")
        || provisionalInternalName(identity.path("internalName").asText())) {
      throw new IllegalArgumentException(
          "Atena aprovou sem nome interno estável e tipo catalogado.");
    }
    if ("CREATE".equals(expectedMode)) {
      validateCreatedIdentityAgainstPolicy(identity, task);
    }
    if (!"PRESERVE".equals(expectedMode) || task == null) return;
    JsonNode target = task.path("taskTarget");
    JsonNode product = target.path("pdeContext").path("product");
    if (!identity.path("internalName").asText().equals(target.path("productInternalName").asText())
        || !identity
            .path("productTypeCode")
            .asText()
            .equals(product.path("productTypeCode").asText())
        || !identity
            .path("productTypeInternalName")
            .asText()
            .equals(product.path("productTypeInternalName").asText())) {
      throw new IllegalArgumentException(
          "Atena tentou alterar a identidade de um produto já cadastrado.");
    }
  }

  /** Bloqueia nome ocupado ou classificação que não pertença ao catálogo entregue a Atena. */
  private static void validateCreatedIdentityAgainstPolicy(JsonNode identity, JsonNode task) {
    JsonNode policy = productIdentityPolicy(task);
    String requestedName = canonicalIdentity(identity.path("internalName").asText());
    boolean occupied = false;
    for (JsonNode reserved : policy.path("reservedInternalNames")) {
      if (requestedName.equals(canonicalIdentity(reserved.asText()))) {
        occupied = true;
        break;
      }
    }
    if (occupied) {
      throw new IllegalArgumentException("Atena escolheu um nome interno já ocupado.");
    }
    boolean catalogedType = false;
    for (JsonNode type : policy.path("activeProductTypes")) {
      if (identity.path("productTypeCode").asText().equals(type.path("code").asText())
          && identity
              .path("productTypeInternalName")
              .asText()
              .equals(type.path("internalName").asText())) {
        catalogedType = true;
        break;
      }
    }
    if (!catalogedType) {
      throw new IllegalArgumentException("Atena escolheu um tipo fora do catálogo ativo.");
    }
  }

  /** Recupera o contrato estruturado anexado à descrição da tarefa de descoberta. */
  private static JsonNode productIdentityPolicy(JsonNode task) {
    String description = task == null ? "" : task.path("description").asText("");
    int marker = description.indexOf("Contexto: ");
    if (marker < 0) {
      throw new IllegalArgumentException(
          "A tarefa não contém o contexto de identidade do produto.");
    }
    try {
      return CONTRACT_MAPPER
          .readTree(description.substring(marker + "Contexto: ".length()))
          .path("productIdentityPolicy");
    } catch (IOException ex) {
      log.error("Falha ao ler a política de identidade anexada à tarefa de Atena.", ex);
      throw new IllegalArgumentException("O contexto de identidade do produto está inválido.", ex);
    }
  }

  /** Canonicaliza o codinome para comparar caixa, acentos e espaços como o backend. */
  private static String canonicalIdentity(String value) {
    String decomposed =
        Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD);
    return decomposed
        .replaceAll("\\p{M}", "")
        .replaceAll("\\s+", " ")
        .toLowerCase(java.util.Locale.ROOT);
  }

  /** Reconhece rótulos temporários que não podem voltar como nome interno oficial. */
  private static boolean provisionalInternalName(String value) {
    String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("planejado")
        || normalized.contains("rascunho")
        || normalized.startsWith("pde ")
        || normalized.startsWith("produto #");
  }

  /** Confirma os cinco sinais canônicos sem aceitar um subconjunto conveniente. */
  private static boolean containsAllPrivateSignals(JsonNode signals) {
    if (!signals.isArray() || signals.size() != REQUIRED_PRIVATE_SIGNALS.size()) return false;
    List<String> values = new ArrayList<>();
    signals.forEach(item -> values.add(item.asText()));
    return values.stream().distinct().count() == REQUIRED_PRIVATE_SIGNALS.size()
        && values.containsAll(REQUIRED_PRIVATE_SIGNALS);
  }

  /** Compara listas como conjuntos exatos sem aceitar duplicidade ou cenário extra. */
  private static boolean exactValues(JsonNode values, List<String> expected) {
    if (!values.isArray() || values.size() != expected.size()) return false;
    List<String> actual = new ArrayList<>();
    values.forEach(item -> actual.add(item.asText()));
    return actual.stream().distinct().count() == expected.size() && actual.containsAll(expected);
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

  /** Confirma valor ao cliente, saída pronta e baixo esforço no plano de Atena. */
  private static boolean canonicalCustomerValueDelivery(JsonNode delivery) {
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

  /** Seleciona o prompt compatível com a versão persistida da definição BPM. */
  private String promptResource(Map<String, Object> task) {
    return requiresAgentValidation(task)
        ? AGENT_VALIDATION_PROMPT
        : requiresProductIdentity(task) ? IDENTITY_PROMPT : LEGACY_PROMPT;
  }

  /** Seleciona o schema compatível com a versão persistida da definição BPM. */
  private String schemaResource(Map<String, Object> task) {
    return requiresAgentValidation(task)
        ? AGENT_VALIDATION_SCHEMA
        : requiresProductIdentity(task) ? IDENTITY_SCHEMA : LEGACY_SCHEMA;
  }

  /** Reconhece a versão do Processo 2 que tornou a identidade parte do contrato. */
  private static boolean requiresProductIdentity(Map<String, Object> task) {
    Object value = task == null ? null : task.get("processVersion");
    return value instanceof Number number && number.intValue() >= 9;
  }

  /** Reconhece a versão que removeu tarefas e gates dependentes de participantes humanos. */
  private static boolean requiresAgentValidation(Map<String, Object> task) {
    Object value = task == null ? null : task.get("processVersion");
    return value instanceof Number number && number.intValue() >= 10;
  }

  /** Reconhece a versão multiagente dentro do envelope serializado usado pela validação. */
  private static boolean requiresAgentValidation(JsonNode task) {
    return task != null && task.path("processVersion").asInt(0) >= 10;
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
  private void putUsage(Map<String, Object> body, TokenUsage usage, String executionModel) {
    if (!usage.informed()) return;
    body.put(
        "modelUsages",
        List.of(
            Map.of(
                "modelCode",
                executionModel,
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
