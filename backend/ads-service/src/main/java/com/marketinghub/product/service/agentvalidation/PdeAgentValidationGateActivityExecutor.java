package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityRequirementResponse;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: calcular o gate multiagente do PDE e liberar somente a preparação comercial.
 */
@Service
@Slf4j
public class PdeAgentValidationGateActivityExecutor
    implements BackendProductProcessActivityExecutor {
  static final String PROCESS_CODE = "pde-construction-approval";
  static final String ACTIVITY_ID = "agentValidationGate";
  static final String SOURCE_SUFFIX = "@agent-validation-v1";
  static final String ACTIVE_CONTRACT = "PDE_AGENT_VALIDATION_V1";
  static final String COMPLETED_CONTRACT = "PDE_AGENT_VALIDATED_V1";
  private static final List<String> PSIQUE_ACTIVITIES =
      List.of("psiqueAdherent", "psiqueRecovery", "psiqueSafety");
  private static final Map<String, String> SCENARIO_BY_ACTIVITY =
      Map.of(
          "psiqueAdherent", "ADHERENT",
          "psiqueRecovery", "RECOVERY",
          "psiqueSafety", "SAFETY");
  private static final List<String> TECHNICAL_CHECKS =
      List.of(
          "sameVersion",
          "desktopAndMobile",
          "happyResultWithinTenMinutes",
          "recoveryPreserved",
          "safetyBlocked",
          "accessibilityBasic",
          "responsiveLayout",
          "privacyPreserved",
          "internalTrafficSegregated",
          "paymentDisabled",
          "publicationDisabled",
          "campaignDisabled",
          "zeroMediaSpend");
  private static final List<String> PSIQUE_CHECKS =
      List.of(
          "sameProductAndVersion",
          "isolatedFreshSession",
          "functionalOutcomeMatchesScenario",
          "lowEffortNoPrompting",
          "accessibilityAndResponsive",
          "privacyPreserved",
          "internalTrafficSegregated",
          "safeLimits",
          "noExternalSideEffects");
  private static final List<String> TEMIS_CHECKS =
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

  private final AgentTaskRepository tasks;
  private final BusinessProcessActivityInstanceRepository instances;
  private final ProductRepository products;
  private final ProductProcessPeriodService periods;
  private final ObjectMapper json;
  private final Clock clock;

  /** Configura as fontes persistidas e o relógio do gate. */
  @Autowired
  public PdeAgentValidationGateActivityExecutor(
      AgentTaskRepository tasks,
      BusinessProcessActivityInstanceRepository instances,
      ProductRepository products,
      ProductProcessPeriodService periods,
      ObjectMapper json) {
    this(tasks, instances, products, periods, json, Clock.systemUTC());
  }

  /** Permite validar transições e horários com relógio determinístico. */
  PdeAgentValidationGateActivityExecutor(
      AgentTaskRepository tasks,
      BusinessProcessActivityInstanceRepository instances,
      ProductRepository products,
      ProductProcessPeriodService periods,
      ObjectMapper json,
      Clock clock) {
    this.tasks = tasks;
    this.instances = instances;
    this.products = products;
    this.periods = periods;
    this.json = json;
    this.clock = clock;
  }

  /** Reconhece somente o gate backend da versão multiagente publicada. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && PROCESS_CODE.equals(process.getProcessCode())
        && process.getVersionNumber() != null
        && process.getVersionNumber() == 7
        && ACTIVITY_ID.equals(activityDefinition.getActivityId());
  }

  /** Explica todos os pré-requisitos persistidos sem produzir efeito no produto. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    if (!supports(process, activityDefinition)) {
      return new BackendProductProcessActivityReadiness(
          false, "Não existe executor multiagente para esta atividade.");
    }
    try {
      GateEvaluation evaluation = evaluate(process, product, sourceReference);
      return new BackendProductProcessActivityReadiness(
          evaluation.ready(),
          evaluation.ready()
              ? "Harness, três cenários de Psique e Têmis estão aprovados na mesma versão."
              : evaluation.issues().getFirst(),
          "Calcular gate multiagente",
          "O backend verificará as provas persistidas e manterá o produto em STOP antes da comunicação.",
          null,
          product == null ? null : product.getId(),
          requirements(evaluation));
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao verificar o gate multiagente. processDefinitionId={} productId={} sourceReference={}",
          process.getId(),
          product == null ? null : product.getId(),
          sourceReference,
          ex);
      return new BackendProductProcessActivityReadiness(
          false, "Não foi possível confirmar as evidências persistidas da validação multiagente.");
    }
  }

  /** Persiste a decisão, avança para comunicação em STOP e não cria efeito comercial externo. */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    GateEvaluation evaluation = evaluate(process, product, sourceReference);
    if (!evaluation.ready()) throw new IllegalStateException(evaluation.issues().getFirst());
    Instant now = Instant.now(clock);
    Optional<BusinessProcessActivityInstance> latest =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activityDefinition.getId(), sourceReference);
    if (latest.isPresent() && "COMPLETED".equals(latest.orElseThrow().getStatus())) {
      advanceProduct(product, evaluation, now);
      return new BackendProductProcessActivityExecutionResult(
          sourceReference,
          "COMPLETED",
          true,
          "A validação multiagente já estava aprovada; a preparação comercial permanece liberada em STOP.");
    }
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activityDefinition);
    instance.setSourceReference(sourceReference);
    instance.setOccurrenceNumber(latest.map(value -> value.getOccurrenceNumber() + 1).orElse(1));
    instance.setStatus("COMPLETED");
    instance.setEnteredAt(now);
    instance.setExitedAt(now);
    instance.setObjectiveAchieved(true);
    instance.setObjectiveEvidenceJson(gateEvidence(product, evaluation, now).toString());
    instance.setBlockedReason(null);
    instance.setKnownCostUsd(knownCost(evaluation.evidenceTasks()));
    instance.setCostCoverage(costCoverage(evaluation.evidenceTasks()));
    instance.setEvidenceQuality("DIRECT");
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    instances.saveAndFlush(instance);
    advanceProduct(product, evaluation, now);
    return new BackendProductProcessActivityExecutionResult(
        sourceReference,
        "COMPLETED",
        true,
        "Validação multiagente aprovada. O PDE foi liberado para preparar comunicação e continua em STOP, sem campanha ou gasto.");
  }

  /** Avalia separadamente contrato, harness, Psique, Têmis, ordem e efeitos externos. */
  private GateEvaluation evaluate(
      BusinessProcessDefinition process, Product product, String sourceReference) {
    List<String> issues = new ArrayList<>();
    AgentValidationContract contract = contract(product, sourceReference, issues);
    List<AgentTask> processTasks =
        process == null || process.getId() == null || sourceReference == null
            ? List.of()
            : tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
                process.getId(), sourceReference);
    AgentTask technical = completedTask(processTasks, "technicalHomologation").orElse(null);
    boolean technicalApproved = validateTechnical(technical, contract, issues);
    Map<String, AgentTask> psique = new LinkedHashMap<>();
    boolean psiqueApproved = true;
    for (String activity : PSIQUE_ACTIVITIES) {
      AgentTask task = completedTask(processTasks, activity).orElse(null);
      psique.put(activity, task);
      psiqueApproved &= validatePsique(task, activity, contract, issues);
    }
    AgentTask temis = completedTask(processTasks, "commercialIntegrityReview").orElse(null);
    boolean temisApproved = validateTemis(temis, contract, issues);
    boolean chronologyApproved = validateChronology(technical, psique, temis, issues);
    List<AgentTask> evidenceTasks = new ArrayList<>();
    if (technical != null) evidenceTasks.add(technical);
    psique.values().stream().filter(java.util.Objects::nonNull).forEach(evidenceTasks::add);
    if (temis != null) evidenceTasks.add(temis);
    return new GateEvaluation(
        issues.isEmpty(),
        contract,
        contract.valid(),
        technicalApproved,
        psiqueApproved,
        temisApproved,
        chronologyApproved,
        List.copyOf(issues),
        technical,
        java.util.Collections.unmodifiableMap(psique),
        temis,
        List.copyOf(evidenceTasks));
  }

  /** Valida a versão vigente e os critérios predeclarados do próprio produto. */
  private AgentValidationContract contract(
      Product product, String sourceReference, List<String> issues) {
    if (product == null || product.getId() == null) {
      issues.add("O produto da validação multiagente não foi encontrado.");
      return AgentValidationContract.invalid();
    }
    String expectedReference = "product:" + product.getId() + SOURCE_SUFFIX;
    if (!expectedReference.equals(sourceReference)) {
      issues.add("A referência da execução não corresponde ao produto e à versão multiagente.");
    }
    if (!ACTIVE_CONTRACT.equals(product.getValidationDefinitionVersion())
        && !COMPLETED_CONTRACT.equals(product.getValidationDefinitionVersion())) {
      issues.add("O produto não possui o contrato PDE_AGENT_VALIDATION_V1 vigente.");
    }
    if (!"PLANNED".equals(product.getCommercialStatus())
        && !"COMUNICACAO_E_JORNADA".equals(product.getCommercialStatus())) {
      issues.add("O produto está fora da etapa permitida para a validação multiagente.");
    }
    try {
      JsonNode validation = json.readTree(product.getValidationDefinitionJson());
      JsonNode plan = validation.path("agentValidationPlan");
      JsonNode acceptance = validation.path("privatePrototypeAcceptance");
      String publicUrl = acceptance.path("privateAccessUrl").asText();
      String prototypeVersion = acceptance.path("prototypeVersion").asText();
      boolean valid =
          ACTIVE_CONTRACT.equals(plan.path("contractVersion").asText())
              && expectedReference.equals(plan.path("sourceReference").asText())
              && "AGENT_VALIDATION".equals(plan.path("trafficClass").asText())
              && "mh_internal_test".equals(plan.path("internalMarker").asText())
              && setOfText(plan.path("requiredScenarios"))
                  .equals(Set.of("ADHERENT", "RECOVERY", "SAFETY"))
              && setOfText(plan.path("requiredDevices"))
                  .equals(Set.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7"))
              && plan.path("maxReadyResultSeconds").asInt() == 600
              && !plan.path("humanEvidenceClaimed").asBoolean(true)
              && !plan.path("commercialEvidenceClaimed").asBoolean(true)
              && !plan.path("paymentEnabled").asBoolean(true)
              && !plan.path("publicationAuthorized").asBoolean(true)
              && !plan.path("campaignAuthorized").asBoolean(true)
              && plan.path("mediaSpendAuthorizedBrl").asInt(-1) == 0
              && "READY".equals(acceptance.path("status").asText())
              && publicUrl.startsWith("https://")
              && !prototypeVersion.isBlank();
      if (!valid) issues.add("O plano multiagente ou o protótipo aceito está incompleto.");
      return new AgentValidationContract(
          valid,
          expectedReference,
          product.getId(),
          product.getSlug(),
          publicUrl,
          prototypeVersion);
    } catch (Exception ex) {
      log.error(
          "Falha ao ler o contrato multiagente. productId={} sourceReference={}",
          product.getId(),
          sourceReference,
          ex);
      issues.add("O contrato JSON da validação multiagente é inválido.");
      return AgentValidationContract.invalid();
    }
  }

  /** Exige a homologação determinística integral da mesma URL e versão. */
  private boolean validateTechnical(
      AgentTask task, AgentValidationContract contract, List<String> issues) {
    JsonNode result = result(task, "homologação técnica", issues);
    boolean valid =
        task != null
            && "customer-agent".equals(agentKey(task))
            && "DETERMINISTIC".equals(task.getExecutionMode())
            && "pde-agent-validation-harness-v1".equals(task.getExecutionModelCode())
            && result != null
            && "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1".equals(result.path("contractVersion").asText())
            && "TECHNICAL".equals(result.path("mode").asText())
            && "APPROVED".equals(result.path("decision").asText())
            && contract.sourceReference().equals(result.path("sourceReference").asText())
            && java.util.Objects.equals(contract.productId(), result.path("productId").asLong())
            && contract.productSlug().equals(result.path("productSlug").asText())
            && contract.publicUrl().equals(result.path("publicUrl").asText())
            && contract.prototypeVersion().equals(result.path("prototypeVersion").asText())
            && "AGENT_VALIDATION".equals(result.path("trafficClass").asText())
            && "mh_internal_test".equals(result.path("internalMarker").asText())
            && !result.path("humanEvidenceClaimed").asBoolean(true)
            && !result.path("commercialEvidenceClaimed").asBoolean(true)
            && allTrue(result.path("checks"), TECHNICAL_CHECKS)
            && approvedSet(result.path("devices"), "deviceProfile")
                .equals(Set.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7"))
            && result.path("devices").size() == 3
            && approvedSet(result.path("scenarios"), "scenarioCode")
                .equals(Set.of("ADHERENT", "RECOVERY", "SAFETY"))
            && result.path("scenarios").size() == 5
            && result.path("scenarios").findValues("resultReadySeconds").stream()
                .allMatch(value -> value.asInt(601) <= 600)
            && noSideEffects(result.path("sideEffects"));
    if (!valid)
      issues.add("A homologação técnica determinística ainda não está integralmente aprovada.");
    return valid;
  }

  /** Exige um parecer isolado, modelado e aprovado para o cenário correspondente. */
  private boolean validatePsique(
      AgentTask task, String activityId, AgentValidationContract contract, List<String> issues) {
    JsonNode result = result(task, "cenário " + SCENARIO_BY_ACTIVITY.get(activityId), issues);
    boolean valid =
        task != null
            && "customer-agent".equals(agentKey(task))
            && "MODEL".equals(task.getExecutionMode())
            && result != null
            && "PDE_PSIQUE_AGENT_SCENARIO_V1".equals(result.path("contractVersion").asText())
            && "APPROVED".equals(result.path("decision").asText())
            && SCENARIO_BY_ACTIVITY.get(activityId).equals(result.path("scenarioCode").asText())
            && contract.sourceReference().equals(result.path("sourceReference").asText())
            && java.util.Objects.equals(contract.productId(), result.path("productId").asLong())
            && contract.productSlug().equals(result.path("productSlug").asText())
            && contract.prototypeVersion().equals(result.path("prototypeVersion").asText())
            && "AGENT_VALIDATION".equals(result.path("trafficClass").asText())
            && "mh_internal_test".equals(result.path("internalMarker").asText())
            && result.path("syntheticEvaluation").asBoolean(false)
            && !result.path("humanEvidenceClaimed").asBoolean(true)
            && !result.path("commercialEvidenceClaimed").asBoolean(true)
            && noSideEffects(result.path("sideEffects"))
            && allTrue(result.path("checks"), PSIQUE_CHECKS)
            && result.path("visualAudit").path("evidenceIds").isArray()
            && !result.path("visualAudit").path("evidenceIds").isEmpty()
            && !result.path("evidence").isEmpty()
            && contract.sourceReference().equals(task.getSourceReference());
    if (!valid) {
      issues.add(
          "Psique ainda não aprovou o cenário " + SCENARIO_BY_ACTIVITY.get(activityId) + ".");
    }
    return valid;
  }

  /** Exige o parecer independente de integridade após os três cenários. */
  private boolean validateTemis(
      AgentTask task, AgentValidationContract contract, List<String> issues) {
    JsonNode result = result(task, "revisão de Têmis", issues);
    boolean valid =
        task != null
            && "meta-ad-approver".equals(agentKey(task))
            && "MODEL".equals(task.getExecutionMode())
            && result != null
            && "PDE_TEMIS_AGENT_VALIDATION_V1".equals(result.path("contractVersion").asText())
            && "APPROVED".equals(result.path("decision").asText())
            && contract.sourceReference().equals(result.path("sourceReference").asText())
            && java.util.Objects.equals(contract.productId(), result.path("productId").asLong())
            && contract.productSlug().equals(result.path("productSlug").asText())
            && contract.prototypeVersion().equals(result.path("prototypeVersion").asText())
            && "AGENT_VALIDATION".equals(result.path("trafficClass").asText())
            && "mh_internal_test".equals(result.path("internalMarker").asText())
            && !result.path("humanEvidenceClaimed").asBoolean(true)
            && !result.path("commercialEvidenceClaimed").asBoolean(true)
            && noSideEffects(result.path("sideEffects"))
            && allTrue(result.path("agentValidationChecks"), TEMIS_CHECKS)
            && result.path("evidence").size() >= 4;
    if (!valid) issues.add("Têmis ainda não aprovou a integridade da validação multiagente.");
    return valid;
  }

  /** Confirma que nenhum revisor decidiu antes das evidências que deveria auditar. */
  private boolean validateChronology(
      AgentTask technical, Map<String, AgentTask> psique, AgentTask temis, List<String> issues) {
    Instant technicalAt = deliveredAt(technical);
    List<Instant> psiqueTimes = psique.values().stream().map(this::deliveredAt).toList();
    boolean valid =
        technicalAt != null
            && psiqueTimes.stream().allMatch(java.util.Objects::nonNull)
            && psiqueTimes.stream().allMatch(value -> !value.isBefore(technicalAt))
            && deliveredAt(temis) != null
            && psiqueTimes.stream().allMatch(value -> !deliveredAt(temis).isBefore(value));
    if (!valid) issues.add("A ordem temporal entre harness, Psique e Têmis não está comprovada.");
    return valid;
  }

  /** Localiza a tentativa concluída mais recente de uma atividade sem aceitar bloqueio antigo. */
  private Optional<AgentTask> completedTask(List<AgentTask> values, String activityId) {
    return values.stream()
        .filter(task -> activityId.equals(task.getProcessActivityId()))
        .filter(task -> "COMPLETED".equals(task.getStatus()))
        .filter(
            task ->
                task.getActivityInstance() == null
                    || ("COMPLETED".equals(task.getActivityInstance().getStatus())
                        && task.getActivityInstance().isObjectiveAchieved()))
        .max(Comparator.comparing(AgentTask::getCreatedAt).thenComparing(AgentTask::getId));
  }

  /** Lê um resultado JSON concluído preservando a causa no log e no requisito. */
  private JsonNode result(AgentTask task, String label, List<String> issues) {
    if (task == null || task.getResultJson() == null || task.getResultJson().isBlank()) return null;
    try {
      return json.readTree(task.getResultJson());
    } catch (Exception ex) {
      log.error("Falha ao ler resultado do gate. taskId={} label={}", task.getId(), label, ex);
      issues.add("O resultado persistido de " + label + " não contém JSON válido.");
      return null;
    }
  }

  /** Exige todos os booleanos declarados e verdadeiros. */
  private boolean allTrue(JsonNode checks, List<String> names) {
    return checks.isObject()
        && names.stream()
            .allMatch(name -> checks.path(name).isBoolean() && checks.path(name).asBoolean());
  }

  /** Extrai o conjunto de itens aprovados de uma coleção estruturada. */
  private Set<String> approvedSet(JsonNode values, String field) {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    if (!values.isArray()) return Set.of();
    values.forEach(
        value -> {
          if ("PASS".equals(value.path("status").asText())) {
            result.add(value.path(field).asText());
          }
        });
    return Set.copyOf(result);
  }

  /** Extrai textos únicos de um array JSON. */
  private Set<String> setOfText(JsonNode values) {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    if (values.isArray()) values.forEach(value -> result.add(value.asText()));
    return Set.copyOf(result);
  }

  /** Confirma efeitos externos nulos no envelope determinístico. */
  private boolean noSideEffects(JsonNode effects) {
    return effects.isObject()
        && !effects.path("paymentEnabled").asBoolean(true)
        && !effects.path("published").asBoolean(true)
        && !effects.path("campaignCreated").asBoolean(true)
        && effects.path("mediaSpendBrl").asInt(-1) == 0;
  }

  /** Extrai o agente responsável sem depender do nome exibido. */
  private String agentKey(AgentTask task) {
    return task == null || task.getAssignedAgent() == null
        ? null
        : task.getAssignedAgent().getAgentKey();
  }

  /** Usa a entrega terminal como marco da evidência. */
  private Instant deliveredAt(AgentTask task) {
    return task == null ? null : task.getDeliveredAt();
  }

  /** Projeta requisitos separados para a tela explicar exatamente o bloqueio atual. */
  private List<ProductProcessActivityRequirementResponse> requirements(GateEvaluation evaluation) {
    return List.of(
        requirement(
            "AGENT_CONTRACT",
            "Contrato multiagente vigente",
            evaluation.contractApproved(),
            "Mesma referência, versão, dispositivos, cenários e limites predeclarados."),
        requirement(
            "TECHNICAL_HARNESS",
            "Homologação técnica",
            evaluation.technicalApproved(),
            "Desktop, iPhone, Android, recuperação, segurança e resultado em até dez minutos."),
        requirement(
            "PSIQUE_SCENARIOS",
            "Três cenários de Psique",
            evaluation.psiqueApproved(),
            "ADHERENT, RECOVERY e SAFETY isolados, sintéticos e aprovados."),
        requirement(
            "TEMIS_INTEGRITY",
            "Integridade de Têmis",
            evaluation.temisApproved(),
            "Verdade, fidelidade, privacidade, segregação e efeitos externos nulos."),
        requirement(
            "EVIDENCE_ORDER",
            "Ordem das evidências",
            evaluation.chronologyApproved(),
            "Harness antes de Psique e Têmis depois dos três cenários."));
  }

  /** Monta uma exigência acionável e uniforme para o frontend. */
  private ProductProcessActivityRequirementResponse requirement(
      String code, String title, boolean satisfied, String detail) {
    return new ProductProcessActivityRequirementResponse(
        code,
        title,
        satisfied,
        detail,
        satisfied ? "Requisito comprovado." : "Conclua ou corrija esta evidência antes do gate.");
  }

  /** Registra ids, hashes, custo e fronteiras comerciais na decisão do backend. */
  private ObjectNode gateEvidence(Product product, GateEvaluation evaluation, Instant completedAt) {
    ObjectNode evidence = json.createObjectNode();
    evidence.put("evidenceType", "PDE_AGENT_VALIDATION_GATE_V1");
    evidence.put("productId", product.getId());
    evidence.put("productSlug", product.getSlug());
    evidence.put("sourceReference", evaluation.contract().sourceReference());
    evidence.put("prototypeVersion", evaluation.contract().prototypeVersion());
    evidence.put("publicUrl", evaluation.contract().publicUrl());
    evidence.put("trafficClass", "AGENT_VALIDATION");
    evidence.put("internalMarker", "mh_internal_test");
    evidence.put("completedAt", completedAt.toString());
    evidence.put("nextProcessCode", "pde-communication-sales-journey");
    evidence.put("productExecutionState", "STOP");
    evidence.put("humanEvidenceClaimed", false);
    evidence.put("commercialEvidenceClaimed", false);
    evidence.put("paymentEnabled", false);
    evidence.put("publicationAuthorized", false);
    evidence.put("campaignAuthorized", false);
    evidence.put("mediaSpendAuthorizedBrl", 0);
    ArrayNode taskEvidence = evidence.putArray("taskEvidence");
    evaluation.evidenceTasks().forEach(task -> taskEvidence.add(taskEvidence(task)));
    BigDecimal knownCost = knownCost(evaluation.evidenceTasks());
    if (knownCost != null) evidence.put("knownCostUsd", knownCost);
    evidence.put("costCoverage", costCoverage(evaluation.evidenceTasks()));
    return evidence;
  }

  /** Resume uma tarefa por identidade e hash sem serializar novamente o documento bruto. */
  private ObjectNode taskEvidence(AgentTask task) {
    ObjectNode value = json.createObjectNode();
    value.put("taskId", task.getId());
    value.put("activityId", task.getProcessActivityId());
    value.put("agentKey", agentKey(task));
    value.put("executionMode", task.getExecutionMode());
    value.put("deliveredAt", String.valueOf(task.getDeliveredAt()));
    value.put("resultSha256", sha256(task.getResultJson()));
    if (task.getEstimatedCostUsd() != null)
      value.put("estimatedCostUsd", task.getEstimatedCostUsd());
    return value;
  }

  /** Calcula uma identidade imutável do resultado usado pelo gate. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Falha ao calcular hash de uma evidência do gate multiagente", ex);
      throw new IllegalStateException("Não foi possível identificar a evidência do gate.", ex);
    }
  }

  /** Soma somente os custos realmente calculados pelos executores. */
  private BigDecimal knownCost(List<AgentTask> evidenceTasks) {
    List<BigDecimal> known =
        evidenceTasks.stream()
            .map(AgentTask::getEstimatedCostUsd)
            .filter(java.util.Objects::nonNull)
            .toList();
    return known.isEmpty()
        ? null
        : known.stream().reduce(BigDecimal.ZERO.setScale(8), BigDecimal::add).setScale(8);
  }

  /** Distingue custo completo, parcial e ainda não informado. */
  private String costCoverage(List<AgentTask> evidenceTasks) {
    long informed =
        evidenceTasks.stream().filter(task -> task.getEstimatedCostUsd() != null).count();
    if (informed == 0) return "NOT_REPORTED";
    return informed == evidenceTasks.size() ? "COMPLETE" : "PARTIAL";
  }

  /** Atualiza o produto e seus períodos sem transferir orçamento ou autorização de mídia. */
  private void advanceProduct(Product product, GateEvaluation evaluation, Instant completedAt) {
    if (COMPLETED_CONTRACT.equals(product.getValidationDefinitionVersion())
        && "COMUNICACAO_E_JORNADA".equals(product.getCommercialStatus())
        && Boolean.FALSE.equals(product.getAutomaticExecutionEnabled())) {
      return;
    }
    String previousStatus = product.getCommercialStatus();
    try {
      ObjectNode validation = (ObjectNode) json.readTree(product.getValidationDefinitionJson());
      validation.put("purchaseMomentStatus", "WAITING_MARKET_VALIDATION");
      validation.put("finalCommercialPrioritizationEligible", false);
      validation.put("communicationPreparationEligible", true);
      ObjectNode gate = validation.putObject("agentValidation");
      writeGateSummary(gate, evaluation, completedAt);
      ObjectNode experience = (ObjectNode) json.readTree(product.getPdeExperienceJson());
      experience.put("status", "AGENT_VALIDATED");
      writeGateSummary(experience.putObject("agentValidation"), evaluation, completedAt);
      product.setValidationDefinitionVersion(COMPLETED_CONTRACT);
      product.setValidationDefinitionJson(json.writeValueAsString(validation));
      product.setPdeExperienceJson(json.writeValueAsString(experience));
      product.setCommercialStatus("COMUNICACAO_E_JORNADA");
      product.setAutomaticExecutionEnabled(false);
      product.setAutomaticExecutionChangedAt(completedAt);
      product.setAutomaticExecutionChangedBy("pde-agent-validation-gate-v1");
      products.save(product);
      periods.recordTransition(product, previousStatus);
    } catch (Exception ex) {
      log.error(
          "Falha ao persistir aprovação multiagente. productId={} sourceReference={}",
          product.getId(),
          evaluation.contract().sourceReference(),
          ex);
      throw new IllegalStateException("Não foi possível persistir a decisão multiagente.", ex);
    }
  }

  /** Grava o mesmo resumo funcional nos dois contratos do produto. */
  private void writeGateSummary(ObjectNode target, GateEvaluation evaluation, Instant completedAt) {
    target.put("contractVersion", ACTIVE_CONTRACT);
    target.put("status", "PASS");
    target.put("sourceReference", evaluation.contract().sourceReference());
    target.put("completedAt", completedAt.toString());
    target.put("technicalTaskId", evaluation.technical().getId());
    ObjectNode psique = target.putObject("psiqueTaskIds");
    evaluation
        .psique()
        .forEach((activity, task) -> psique.put(SCENARIO_BY_ACTIVITY.get(activity), task.getId()));
    target.put("temisTaskId", evaluation.temis().getId());
    target.put("humanEvidenceClaimed", false);
    target.put("commercialEvidenceClaimed", false);
    target.put("marketValidationStatus", "WAITING");
  }

  /** Representa o contrato mínimo necessário para comparar todas as evidências. */
  private record AgentValidationContract(
      boolean valid,
      String sourceReference,
      Long productId,
      String productSlug,
      String publicUrl,
      String prototypeVersion) {
    /** Representa uma leitura inválida sem fabricar identidade ou URL. */
    private static AgentValidationContract invalid() {
      return new AgentValidationContract(false, "", null, "", "", "");
    }
  }

  /** Mantém o diagnóstico e as tarefas exatas usadas na decisão. */
  private record GateEvaluation(
      boolean ready,
      AgentValidationContract contract,
      boolean contractApproved,
      boolean technicalApproved,
      boolean psiqueApproved,
      boolean temisApproved,
      boolean chronologyApproved,
      List<String> issues,
      AgentTask technical,
      Map<String, AgentTask> psique,
      AgentTask temis,
      List<AgentTask> evidenceTasks) {}
}
