package com.marketinghub.experiment.run.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.funnel.ExperimentFinancialGuardrailPolicy;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunDataQualityStatus;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.product.Product;
import com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialChecks;
import com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: comprovar cada objetivo do subprocesso técnico com evidências persistidas do
 * run vigente.
 */
@Service
@Slf4j
public class ExperimentTechnicalPreflightEvidenceService {
  static final String PROCESS_CODE = "experiment-homologation-activation";
  static final Set<String> ACTIVITIES =
      Set.of("surfaces", "transaction", "measurement", "financialGuardrails");
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("^experiment:([1-9][0-9]*)$");
  private static final Set<ExperimentRunStatus> ACCEPTED_RUN_STATUSES =
      Set.of(
          ExperimentRunStatus.READY_TO_PUBLISH,
          ExperimentRunStatus.PUBLICATION_PENDING,
          ExperimentRunStatus.PUBLISHING,
          ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE,
          ExperimentRunStatus.RUNNING,
          ExperimentRunStatus.PAUSE_REQUESTED,
          ExperimentRunStatus.PAUSED,
          ExperimentRunStatus.STOP_REQUESTED,
          ExperimentRunStatus.COMPLETED);

  private final ExperimentRepository experiments;
  private final ExperimentRunRepository runs;
  private final ExperimentRunGateResultRepository gates;
  private final CommercialPlanRepository plans;
  private final QuartzoPreflightEvidenceScopeService quartzoEvidence;
  private final QuartzoCommercialContext quartzoContext;
  private final QuartzoCommercialChecks quartzoChecks;
  private final ObjectMapper json;
  private final Clock clock;

  /** Configura fontes técnicas, financeiras e de identidade da publicação vigente. */
  @Autowired
  public ExperimentTechnicalPreflightEvidenceService(
      ExperimentRepository experiments,
      ExperimentRunRepository runs,
      ExperimentRunGateResultRepository gates,
      CommercialPlanRepository plans,
      QuartzoPreflightEvidenceScopeService quartzoEvidence,
      QuartzoCommercialContext quartzoContext,
      QuartzoCommercialChecks quartzoChecks,
      ObjectMapper json) {
    this(
        experiments,
        runs,
        gates,
        plans,
        quartzoEvidence,
        quartzoContext,
        quartzoChecks,
        json,
        Clock.systemUTC());
  }

  /** Permite testes determinísticos sem alterar o relógio global da aplicação. */
  ExperimentTechnicalPreflightEvidenceService(
      ExperimentRepository experiments,
      ExperimentRunRepository runs,
      ExperimentRunGateResultRepository gates,
      CommercialPlanRepository plans,
      QuartzoPreflightEvidenceScopeService quartzoEvidence,
      QuartzoCommercialContext quartzoContext,
      QuartzoCommercialChecks quartzoChecks,
      ObjectMapper json,
      Clock clock) {
    this.experiments = experiments;
    this.runs = runs;
    this.gates = gates;
    this.plans = plans;
    this.quartzoEvidence = quartzoEvidence;
    this.quartzoContext = quartzoContext;
    this.quartzoChecks = quartzoChecks;
    this.json = json;
    this.clock = clock;
  }

  /** Valida a fonte correspondente à atividade e devolve uma prova funcional imutável. */
  @Transactional(readOnly = true)
  public Evidence evaluate(String activityId, Product product, String sourceReference) {
    require(ACTIVITIES.contains(activityId), "Atividade técnica desconhecida.");
    Experiment experiment = referencedExperiment(product, sourceReference);
    ExperimentRun run =
        runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(
                experiment.getId(), ExperimentRunMode.PRODUCTION)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Execute primeiro uma homologação produtiva para este experimento."));
    require(
        ACCEPTED_RUN_STATUSES.contains(run.getStatus())
            && run.getDataQualityStatus() == ExperimentRunDataQualityStatus.VALID
            && run.getPreflightCompletedAt() != null,
        "O run produtivo ainda não possui homologação técnica integral e válida.");
    List<ExperimentRunGateResult> allGates =
        gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(run.getId());
    Map<String, ExperimentRunGateResult> byCode = index(allGates);
    List<ExperimentRunGateResult> selected =
        switch (activityId) {
          case "surfaces" ->
              List.of(requiredGate(byCode, ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED));
          case "transaction" -> List.of(requiredGate(byCode, transactionGate(experiment)));
          case "measurement" ->
              List.of(
                  requiredGate(byCode, ExperimentRunGateCodes.DATA_FRESHNESS_VALID),
                  requiredGate(byCode, distributionGate(experiment)));
          case "financialGuardrails" -> {
            validateFinancialGuardrails(experiment, product, sourceReference);
            yield List.of();
          }
          default -> throw new IllegalStateException("Atividade técnica desconhecida.");
        };
    if ("surfaces".equals(activityId)
        && quartzoEvidence.applies(run)
        && !quartzoEvidence.hasCurrentEvidence(run)) {
      throw new IllegalStateException(
          "A publicação, o HTML ou o contrato Quartzo mudaram após a homologação.");
    }
    ObjectNode evidence = evidence(activityId, sourceReference, experiment, run, selected);
    String fingerprint = fingerprint(evidence);
    evidence.put("inputFingerprint", fingerprint);
    evidence.put("evaluatedAt", Instant.now(clock).toString());
    return new Evidence(experiment.getId(), run.getId(), fingerprint, evidence);
  }

  /** Resolve a referência explícita e impede mistura de produto ou experimento. */
  private Experiment referencedExperiment(Product product, String sourceReference) {
    var matcher = EXPERIMENT_REFERENCE.matcher(Objects.toString(sourceReference, ""));
    require(matcher.matches(), "Informe a referência no formato experiment:<id>.");
    Long experimentId = Long.valueOf(matcher.group(1));
    Experiment experiment = experiments.findById(experimentId).orElseThrow();
    require(
        experiment.getProduct() != null
            && product != null
            && Objects.equals(experiment.getProduct().getId(), product.getId()),
        "O experimento técnico pertence a outro produto.");
    return experiment;
  }

  /** Indexa uma única fotografia dos gates para manter a avaliação consistente. */
  private Map<String, ExperimentRunGateResult> index(List<ExperimentRunGateResult> allGates) {
    Map<String, ExperimentRunGateResult> indexed = new LinkedHashMap<>();
    for (ExperimentRunGateResult gate : allGates) indexed.put(gate.getGateCode(), gate);
    return indexed;
  }

  /** Exige aprovação direta, referência, horário e versão do avaliador para cada prova. */
  private ExperimentRunGateResult requiredGate(
      Map<String, ExperimentRunGateResult> indexed, String code) {
    ExperimentRunGateResult gate = indexed.get(code);
    require(gate != null, "O run não registrou o gate técnico " + code + ".");
    require(
        gate.getStatus() == ExperimentRunGateStatus.PASS,
        "O gate técnico " + code + " não foi aprovado.");
    require(
        gate.getEvidenceReference() != null
            && !gate.getEvidenceReference().isBlank()
            && gate.getEvaluatedAt() != null
            && gate.getEvaluatorVersion() != null
            && !gate.getEvaluatorVersion().isBlank(),
        "O gate técnico " + code + " não possui evidência verificável.");
    return gate;
  }

  /** Escolhe compra e entrega para venda ou submissão para captação. */
  private String transactionGate(Experiment experiment) {
    return experiment.getCampaignObjective() == ExperimentCampaignObjective.SALES
        ? ExperimentRunGateCodes.CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED
        : ExperimentRunGateCodes.FORM_CAN_BE_SUBMITTED;
  }

  /** Escolhe a prova do canal real sem obrigar Meta a um experimento direto. */
  private String distributionGate(Experiment experiment) {
    return experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE
        ? ExperimentRunGateCodes.DIRECT_CHANNEL_READINESS_CONFIRMED
        : ExperimentRunGateCodes.META_EFFECTIVE_STATUS_CONFIRMED;
  }

  /** Confere orçamento, janela, paradas, plano governante e parecer financeiro do tipo. */
  private void validateFinancialGuardrails(
      Experiment experiment, Product product, String sourceReference) {
    if (experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE) {
      require(
          nonPositive(experiment.getDailyBudget()) && nonPositive(experiment.getMediaSpendLimit()),
          "O canal direto não pode carregar orçamento de mídia.");
    } else {
      require(
          positive(experiment.getDailyBudget())
              && positive(experiment.getMediaSpendLimit())
              && experiment.getDailyBudget().compareTo(experiment.getMediaSpendLimit()) <= 0,
          "Defina orçamento diário e teto acumulado coerentes.");
      require(
          experiment.getStartDate() != null
              && experiment.getEndDate() != null
              && !experiment.getStartDate().isAfter(experiment.getEndDate()),
          "Defina uma janela financeira válida.");
      require(
          positive(experiment.getZeroResultSpendLimit())
              && experiment.getZeroResultSpendLimit().compareTo(experiment.getMediaSpendLimit())
                  <= 0
              && experiment
                      .getZeroResultSpendLimit()
                      .compareTo(
                          ExperimentFinancialGuardrailPolicy.zeroPrimaryResultMinimumSpend(
                              experiment))
                  >= 0,
          "Defina a parada sem resultado dentro do teto autorizado.");
      if (experiment.getCampaignObjective() == ExperimentCampaignObjective.SALES) {
        require(
            positive(experiment.getZeroPurchaseSpendLimit())
                && experiment.getZeroPurchaseSpendLimit().compareTo(experiment.getMediaSpendLimit())
                    <= 0
                && experiment.getPurchaseStopCount() != null
                && experiment.getPurchaseStopCount() > 0,
            "Defina as paradas sem compra e por quantidade de compras.");
      }
      CommercialPlan governing = governingPlan(experiment.getId());
      require(
          positive(governing.getMaxBudget())
              && experiment.getMediaSpendLimit().compareTo(governing.getMaxBudget()) <= 0,
          "O teto do experimento ultrapassa o plano comercial governante.");
    }
    require(
        positive(experiment.getUnitPrice()),
        "O experimento precisa preservar preço positivo antes da ativação.");
    if (quartzoContext.applies(product)) {
      var scope = quartzoContext.scope(sourceReference, product.getId(), false);
      quartzoChecks.check("economics", scope, quartzoContext.snapshot(sourceReference));
    }
  }

  /** Escolhe o plano mais recente ainda válido para governar o teto operacional. */
  private CommercialPlan governingPlan(Long experimentId) {
    return plans.findByExperimentReference(experimentId).stream()
        .filter(
            plan ->
                plan.getStatus() == CommercialPlanStatus.IN_PROGRESS
                    || plan.getStatus() == CommercialPlanStatus.COMPLETED)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "O experimento não possui plano comercial governante em execução ou concluído."));
  }

  /** Monta a evidência sem serializar documentos JSON dentro de outro JSON. */
  private ObjectNode evidence(
      String activityId,
      String sourceReference,
      Experiment experiment,
      ExperimentRun run,
      List<ExperimentRunGateResult> selected) {
    ObjectNode evidence = json.createObjectNode();
    evidence.put("evidenceType", "EXPERIMENT_TECHNICAL_PREFLIGHT_ACTIVITY_V1");
    evidence.put("processCode", PROCESS_CODE);
    evidence.put("activityId", activityId);
    evidence.put("sourceReference", sourceReference);
    evidence.put("productId", experiment.getProduct().getId());
    evidence.put("experimentId", experiment.getId());
    evidence.put("runId", run.getId());
    evidence.put("runNumber", run.getRunNumber());
    evidence.put("runStatus", run.getStatus().name());
    evidence.put("dataQualityStatus", run.getDataQualityStatus().name());
    evidence.put("preflightCompletedAt", run.getPreflightCompletedAt().toString());
    var gateEvidence = evidence.putArray("gates");
    selected.forEach(
        gate ->
            gateEvidence
                .addObject()
                .put("code", gate.getGateCode())
                .put("status", gate.getStatus().name())
                .put("summary", gate.getSummary())
                .put("evidenceReference", gate.getEvidenceReference())
                .put("evaluatedAt", gate.getEvaluatedAt().toString())
                .put("evaluatorVersion", gate.getEvaluatorVersion()));
    if ("financialGuardrails".equals(activityId)) {
      evidence.put("dailyBudgetBrl", experiment.getDailyBudget());
      evidence.put("mediaSpendLimitBrl", experiment.getMediaSpendLimit());
      evidence.put("zeroResultSpendLimitBrl", experiment.getZeroResultSpendLimit());
      evidence.put("zeroPurchaseSpendLimitBrl", experiment.getZeroPurchaseSpendLimit());
      if (experiment.getPurchaseStopCount() == null) evidence.putNull("purchaseStopCount");
      else evidence.put("purchaseStopCount", experiment.getPurchaseStopCount());
      if (experiment.getStartDate() == null) evidence.putNull("startDate");
      else evidence.put("startDate", experiment.getStartDate().toString());
      if (experiment.getEndDate() == null) evidence.putNull("endDate");
      else evidence.put("endDate", experiment.getEndDate().toString());
      evidence.put("spendAuthorizedByThisActivity", false);
      if (quartzoContext.applies(experiment.getProduct())) {
        evidence.put(
            "commercialFingerprint",
            quartzoContext.snapshot(sourceReference).path("fingerprint").asText());
      }
    }
    return evidence;
  }

  /** Calcula a identidade dos insumos antes de acrescentar o horário da projeção. */
  private String fingerprint(ObjectNode evidence) {
    try {
      byte[] bytes = json.writeValueAsString(evidence).getBytes(StandardCharsets.UTF_8);
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception ex) {
      log.error(
          "Falha calculando identidade da evidência técnica. activityId={} experimentId={} runId={}",
          evidence.path("activityId").asText(),
          evidence.path("experimentId").asLong(),
          evidence.path("runId").asLong(),
          ex);
      throw new IllegalStateException("Não foi possível identificar a evidência técnica.", ex);
    }
  }

  /** Reconhece um valor monetário estritamente positivo. */
  private boolean positive(BigDecimal value) {
    return value != null && value.signum() > 0;
  }

  /** Reconhece ausência de gasto ou valor não positivo no canal sem mídia. */
  private boolean nonPositive(BigDecimal value) {
    return value == null || value.signum() <= 0;
  }

  /** Interrompe a avaliação quando uma premissa persistida não foi comprovada. */
  private void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }

  /** Transporta a prova já verificada para persistência idempotente no BPM. */
  public record Evidence(
      Long experimentId, Long runId, String inputFingerprint, ObjectNode objectiveEvidence) {}
}
