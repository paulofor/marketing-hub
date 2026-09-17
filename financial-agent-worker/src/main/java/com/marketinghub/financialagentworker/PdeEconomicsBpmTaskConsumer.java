package com.marketinghub.financialagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
  private static final String LEGACY_PROMPT = "prompts/pde-commercial-plan/v4/economics.md";
  private static final String LEGACY_SCHEMA =
      "prompts/pde-commercial-plan/v4/economics-schema.json";
  private static final String PRIVATE_VALIDATION_PROMPT =
      "prompts/pde-commercial-plan/v5/economics.md";
  private static final String PRIVATE_VALIDATION_SCHEMA =
      "prompts/pde-commercial-plan/v5/economics-schema.json";
  private static final String OPALA_SCHEMA =
      "prompts/opala-commercial-preparation/v1/economics-schema.json";
  private static final List<String> PRIVATE_VALIDATION_SIGNALS =
      List.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");
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
      validateTaskContract(task);
      execution = execute(task);
      validate(execution.result(), isPrivateValidationTask(task));
      if (isOpalaTask(task)) validateOpalaResultAgainstPlan(execution.result(), taskContext(task));
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
    for (String process : List.of(PROCESS_CODE, "opala-commercial-preparation-v1")) {
      List<Map<String, Object>> pending =
          backend
              .get()
              .uri(
                  "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode={process}&activityId={activity}",
                  AGENT_KEY,
                  process,
                  ACTIVITY_ID)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      if (pending != null && !pending.isEmpty()) return pending.getFirst();
    }
    return null;
  }

  /** Executa o prompt financeiro em leitura somente e preserva a resposta bruta. */
  Execution execute(Map<String, Object> task) throws IOException, InterruptedException {
    Path output = Files.createTempFile("plutus-pde-economics-", ".json");
    Path processLog = Files.createTempFile("plutus-pde-economics-", ".jsonl");
    Path schema = materialize(schemaResource(task), ".json");
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

  /**
   * Compõe identidade financeira e texto fixado pelo catálogo no Opala; preserva os demais fluxos.
   */
  private PromptComposition prompt(Map<String, Object> task) throws IOException {
    String agent = read("prompts/financial-agent/v1/agent-core.md");
    String activity =
        (CatalogPromptInput.migrated(task)
                ? CatalogPromptInput.text(task, schemaResource(task))
                : read(promptResource(task)))
            .replace(
                "{{TASK_CONTEXT}}",
                objectMapper.writeValueAsString(CatalogPromptInput.context(task)));
    return new PromptComposition(agent + "\n\n" + activity, agent, activity);
  }

  /** Seleciona a atividade econômica compatível com a versão imutável do processo. */
  private String promptResource(Map<String, Object> task) {
    if ("opala-commercial-preparation-v1".equals(task.get("processCode")))
      return "catalogo-vivo:opala/economics";
    return isPrivateValidationTask(task) ? PRIVATE_VALIDATION_PROMPT : LEGACY_PROMPT;
  }

  /** Seleciona o schema econômico compatível com a versão imutável do processo. */
  private String schemaResource(Map<String, Object> task) {
    if ("opala-commercial-preparation-v1".equals(task.get("processCode"))) return OPALA_SCHEMA;
    return isPrivateValidationTask(task) ? PRIVATE_VALIDATION_SCHEMA : LEGACY_SCHEMA;
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
              isStrategyContractDrift(ex) ? "CONTRACT_DRIFT" : "TECHNICAL_FAILURE",
              "recommendedAction",
              isStrategyContractDrift(ex)
                  ? "Retome a execução com Atena para gerar MARKET_STRATEGY_V3 antes de reiniciar Plutus."
                  : "Corrija a falha técnica registrada e reinicie a atividade de Plutus.",
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
    var result =
        (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(baseEvidence(task));
    if ("opala-commercial-preparation-v1".equals(task.get("processCode"))) {
      result.put("processCode", "opala-commercial-preparation-v1");
      result.put("promptVersion", "opala-commercial-preparation-v1");
      result.set(
          "opalaScope",
          objectMapper
              .readTree(String.valueOf(task.get("processContextJson")))
              .path("opalaCommercial"));
    }
    return result.toString();
  }

  /** Preserva as evidências dos contratos financeiros anteriores. */
  private String baseEvidence(Map<String, Object> task) throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "agent",
            "Plutus",
            "promptVersion",
            isPrivateValidationTask(task) ? "pde-commercial-plan-v5" : "pde-commercial-plan-v4",
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

  /** Rejeita economia legada sem cenários, números reconciliáveis, prazo ou regra de decisão. */
  static void validate(JsonNode result) {
    validate(result, false);
  }

  /** Rejeita economia privada que antecipe venda, aquisição, orçamento ou gasto. */
  static void validatePrivateValidation(JsonNode result) {
    validate(result, true);
  }

  /** Valida o envelope comum e aplica as travas específicas da versão do processo. */
  private static void validate(JsonNode result, boolean privateValidation) {
    JsonNode economics = result.path("economics");
    if (!List.of("APPROVE", "ADJUST", "REJECT").contains(result.path("decision").asText())
        || result.path("scenarios").size() != 3
        || !economics.isObject()
        || !result.path("metrics").isObject()
        || result.path("rationale").asText().isBlank()) {
      throw new IllegalArgumentException("Economia PDE fora do contrato versionado de Plutus.");
    }
    try {
      LocalDate.parse(economics.path("deadline").asText());
    } catch (java.time.format.DateTimeParseException ex) {
      throw new IllegalArgumentException("Prazo econômico deve usar a data ISO YYYY-MM-DD.", ex);
    }
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
    if (privateValidation)
      validatePrivateValidationEnvelope(result, economics, price, contribution);
  }

  /** Confirma que a hipótese privada não foi convertida em operação ou resultado comercial. */
  private static void validatePrivateValidationEnvelope(
      JsonNode result, JsonNode economics, BigDecimal price, BigDecimal contribution) {
    long recommendedScenarios =
        java.util.stream.StreamSupport.stream(result.path("scenarios").spliterator(), false)
            .filter(scenario -> scenario.path("recommended").asBoolean(false))
            .count();
    boolean zeroCommercialTargets =
        economics.path("maxCacBrl").decimalValue().compareTo(BigDecimal.ZERO) == 0
            && economics.path("maxBudgetBrl").decimalValue().compareTo(BigDecimal.ZERO) == 0
            && economics.path("expectedTraffic").asInt(-1) == 0
            && economics.path("expectedConversionPercent").decimalValue().compareTo(BigDecimal.ZERO)
                == 0
            && economics.path("targetSales").asInt(-1) == 0
            && economics.path("targetRevenueBrl").decimalValue().compareTo(BigDecimal.ZERO) == 0;
    if (!"PDE_PRIVATE_ECONOMICS_V1".equals(result.path("contractVersion").asText())
        || !"PRIVATE_VALIDATION_HYPOTHESIS".equals(result.path("mode").asText())
        || economics.path("commercialSpendAuthorized").asBoolean(true)
        || economics.path("privateReadingsTarget").asInt(0) != 2
        || !zeroCommercialTargets
        || recommendedScenarios != 1) {
      throw new IllegalArgumentException(
          "Economia privada antecipou operação comercial ou não escolheu um cenário único.");
    }
    if ("APPROVE".equals(result.path("decision").asText())
        && (price.compareTo(BigDecimal.ZERO) <= 0
            || contribution.compareTo(BigDecimal.ZERO) <= 0)) {
      throw new IllegalArgumentException(
          "Hipótese privada aprovada exige preço de teste e contribuição positivos.");
    }
  }

  /** Exige estratégia v3 antes de consumir tokens no processo de validação privada. */
  private void validateTaskContract(Map<String, Object> task) throws IOException {
    JsonNode context = taskContext(task);
    if (isOpalaTask(task)) validateOpalaPlanContract(context);
    if (isPrivateValidationTask(task)) validatePrivateStrategyContract(context);
  }

  /** Converte o contexto estruturado sem perder o contrato persistido pelo backend. */
  private JsonNode taskContext(Map<String, Object> task) throws IOException {
    Object rawContext = task.get("processContextJson");
    return rawContext instanceof String text
        ? objectMapper.readTree(text)
        : objectMapper.valueToTree(rawContext);
  }

  /** Bloqueia antes da inferência quando plano, versão, fontes ou janela não estão prontos. */
  static void validateOpalaPlanContract(JsonNode context) {
    JsonNode opala = context.path("opalaCommercial");
    JsonNode plan = opala.path("financialPlan");
    JsonNode assumptions = plan.path("assumptions");
    JsonNode evaluation = plan.path("deterministicEvaluation");
    boolean baseFound = false;
    for (JsonNode scenario : evaluation.path("scenarios"))
      if ("BASE".equals(scenario.path("code").asText()) && scenario.path("viable").asBoolean())
        baseFound = true;
    boolean validWindow;
    try {
      validWindow =
          java.time.Instant.parse(opala.path("windowEnd").asText())
              .isAfter(java.time.Instant.now());
    } catch (java.time.format.DateTimeParseException ex) {
      validWindow = false;
    }
    boolean validUntil;
    try {
      validUntil =
          !LocalDate.parse(assumptions.path("validUntil").asText())
              .isBefore(LocalDate.now(java.time.ZoneOffset.UTC));
    } catch (java.time.format.DateTimeParseException ex) {
      validUntil = false;
    }
    if (!"READY".equals(plan.path("status").asText())
        || !"PROJECTED_VIABLE".equals(evaluation.path("status").asText())
        || !baseFound
        || !validWindow
        || !validUntil
        || !opala
            .path("productVersion")
            .asText()
            .equals(assumptions.path("productVersion").asText())
        || opala
                .path("priceBrl")
                .decimalValue()
                .compareTo(assumptions.path("priceBrl").decimalValue())
            != 0
        || assumptions.path("evidence").asText().isBlank())
      throw new IllegalArgumentException(
          "Opala exige plano financeiro LIVE, vigente, viável e da mesma versão antes de Plutus.");
  }

  /** Recusa aprovação que altere custo, contribuição antes de CAC ou limites determinísticos. */
  static void validateOpalaResultAgainstPlan(JsonNode result, JsonNode context) {
    if (!"APPROVE".equals(result.path("decision").asText())) return;
    JsonNode opala = context.path("opalaCommercial");
    JsonNode plan = opala.path("financialPlan");
    JsonNode assumptions = plan.path("assumptions");
    JsonNode base = null;
    for (JsonNode scenario : plan.path("deterministicEvaluation").path("scenarios"))
      if ("BASE".equals(scenario.path("code").asText())) base = scenario;
    if (base == null)
      throw new IllegalArgumentException("Plano financeiro Opala sem cenário-base.");
    JsonNode economics = result.path("economics");
    BigDecimal price = assumptions.path("priceBrl").decimalValue();
    BigDecimal contribution = base.path("contributionBeforeCacBrl").decimalValue();
    BigDecimal variable = price.subtract(contribution);
    BigDecimal margin =
        contribution.multiply(new BigDecimal("100")).divide(price, 6, RoundingMode.HALF_UP);
    if (!same(economics.path("offerPriceBrl").decimalValue(), price)
        || !same(economics.path("variableCostPerSaleBrl").decimalValue(), variable)
        || !same(economics.path("contributionPerSaleBrl").decimalValue(), contribution)
        || !same(economics.path("contributionMarginPercent").decimalValue(), margin)
        || !same(
            economics.path("maxCacBrl").decimalValue(),
            assumptions.path("maximumCacBrl").decimalValue())
        || !same(
            economics.path("expectedRefundPercent").decimalValue(),
            assumptions.path("costs").path("refundPercent").decimalValue()))
      throw new IllegalArgumentException(
          "Parecer Opala diverge do plano financeiro determinístico versionado.");
  }

  /** Compara valores financeiros até a precisão comercial de um centavo. */
  private static boolean same(BigDecimal left, BigDecimal right) {
    return left.subtract(right).abs().compareTo(new BigDecimal("0.01")) <= 0;
  }

  /** Identifica o subprocesso Opala sem inferir pelo produto ou pelo nome da atividade. */
  private static boolean isOpalaTask(Map<String, Object> task) {
    return "opala-commercial-preparation-v1".equals(task.get("processCode"));
  }

  /** Valida o contrato predecessor isoladamente para impedir regressão entre Atena e Plutus. */
  static void validatePrivateStrategyContract(JsonNode context) {
    JsonNode contract = privateStrategyContract(context);
    JsonNode plan = contract.path("privateValidationPlan");
    if (!"MARKET_STRATEGY_V3".equals(contract.path("contractVersion").asText())
        || !"READY_FOR_PRIVATE_VALIDATION".equals(contract.path("status").asText())
        || plan.path("minimumIndependentReadings").asInt(0) != 2
        || !hasExactPrivateSignals(plan.path("requiredSignals"))) {
      throw new IllegalArgumentException(
          "Contrato de Atena incompatível com a validação privada: MARKET_STRATEGY_V3 é obrigatório.");
    }
  }

  /** Localiza a estratégia mais recente no envelope real de predecessoras do processo. */
  private static JsonNode privateStrategyContract(JsonNode context) {
    JsonNode direct = context.path("marketStrategicContract");
    if (direct.isObject()) return direct;
    JsonNode latest = com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    long latestTaskId = Long.MIN_VALUE;
    for (JsonNode completed : context.path("completedActivities")) {
      if (!"marketStrategy".equals(completed.path("activityId").asText())) continue;
      JsonNode candidate = completed.path("result").path("marketStrategicContract");
      long taskId = completed.path("taskId").asLong(Long.MIN_VALUE);
      if (candidate.isObject() && taskId > latestTaskId) {
        latest = candidate;
        latestTaskId = taskId;
      }
    }
    return latest;
  }

  /** Confirma os cinco sinais canônicos da validação privada sem aceitar aliases. */
  private static boolean hasExactPrivateSignals(JsonNode values) {
    if (!values.isArray() || values.size() != PRIVATE_VALIDATION_SIGNALS.size()) return false;
    List<String> signals = new ArrayList<>();
    values.forEach(value -> signals.add(value.asText()));
    return signals.stream().distinct().count() == PRIVATE_VALIDATION_SIGNALS.size()
        && signals.containsAll(PRIVATE_VALIDATION_SIGNALS);
  }

  /** Aplica o contrato privado pela versão do BPM, inclusive aos experimentos sucessores. */
  private static boolean isPrivateValidationTask(Map<String, Object> task) {
    Object version = task == null ? null : task.get("processVersion");
    return task != null
        && !"opala-commercial-preparation-v1".equals(task.get("processCode"))
        && version instanceof Number number
        && number.intValue() >= 6;
  }

  /** Distingue incompatibilidade entre etapas de uma falha técnica genérica do executor. */
  private boolean isStrategyContractDrift(Exception ex) {
    return ex != null
        && ex.getMessage() != null
        && ex.getMessage().startsWith("Contrato de Atena incompatível");
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
