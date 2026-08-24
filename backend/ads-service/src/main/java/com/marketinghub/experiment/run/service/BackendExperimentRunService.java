package com.marketinghub.experiment.run.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.run.ExperimentEvidenceValidity;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunDataQualityStatus;
import com.marketinghub.experiment.run.ExperimentRunGateEvaluatorType;
import com.marketinghub.experiment.run.ExperimentRunGateGroup;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateSeverity;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.experiment.run.ExperimentRunStopPolicy;
import com.marketinghub.experiment.run.service.MoisCommercialDossierPreflightService.CommercialDossierPreflightResult;
import com.marketinghub.experiment.run.service.create.CreateExperimentRunRequest;
import com.marketinghub.experiment.run.service.get.ExperimentRunResponse;
import com.marketinghub.experiment.run.service.homologation.ExperimentRunHomologationRequest;
import com.marketinghub.experiment.run.service.homologation.ExperimentRunHomologationRequest.GateEvidence;
import com.marketinghub.experiment.run.service.preflight.ExperimentRunGateResultResponse;
import com.marketinghub.experiment.run.service.preflight.ExperimentRunPreflightResponse;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestra criação e leitura dos runs operacionais vinculados a experimentos. */
@Service
public class BackendExperimentRunService {
  private final ExperimentRepository experimentRepository;
  private static final String EVALUATOR_VERSION = "experiment-run-preflight.v1";
  private static final String HOMOLOGATION_EVALUATOR_VERSION = "experiment-run-homologation.v1";
  private static final String LANDING_GATE = "LANDING_QUALITY_REVIEW_APPROVED";
  private static final String FORM_GATE = "FORM_CAN_BE_SUBMITTED";
  private static final String SALES_JOURNEY_GATE = "CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED";
  private static final String META_DISTRIBUTION_GATE = "META_EFFECTIVE_STATUS_CONFIRMED";
  private static final String DIRECT_DISTRIBUTION_GATE = "DIRECT_CHANNEL_READINESS_CONFIRMED";
  private static final String DATA_GATE = "DATA_FRESHNESS_VALID";

  private final ExperimentRunRepository experimentRunRepository;
  private final ExperimentRunGateResultRepository gateResultRepository;
  private final MoisCommercialDossierPreflightService moisCommercialDossierPreflightService;

  /** Inicializa o serviço com os repositórios canônicos de experimento e run. */
  public BackendExperimentRunService(
      ExperimentRepository experimentRepository,
      ExperimentRunRepository experimentRunRepository,
      ExperimentRunGateResultRepository gateResultRepository,
      MoisCommercialDossierPreflightService moisCommercialDossierPreflightService) {
    this.experimentRepository = experimentRepository;
    this.experimentRunRepository = experimentRunRepository;
    this.gateResultRepository = gateResultRepository;
    this.moisCommercialDossierPreflightService = moisCommercialDossierPreflightService;
  }

  /** Cria um novo run sequencial para o experimento sem alterar o status legado do experimento. */
  @Transactional
  public ExperimentRunResponse create(Long experimentId, CreateExperimentRunRequest request) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Experimento %d não encontrado".formatted(experimentId)));
    int nextRunNumber = experimentRunRepository.findMaxRunNumberByExperimentId(experimentId) + 1;
    Instant now = Instant.now();
    ExperimentRun run =
        ExperimentRun.builder()
            .experiment(experiment)
            .runNumber(nextRunNumber)
            .mode(
                request != null && request.mode() != null
                    ? request.mode()
                    : ExperimentRunMode.PRODUCTION)
            .status(ExperimentRunStatus.DRAFT)
            .evidenceValidity(ExperimentEvidenceValidity.NOT_EVALUATED)
            .stopPolicy(
                request != null && request.stopPolicy() != null
                    ? request.stopPolicy()
                    : ExperimentRunStopPolicy.MANUAL_ONLY)
            .dataQualityStatus(ExperimentRunDataQualityStatus.UNKNOWN)
            .requestedAt(now)
            .createdBy(request != null ? request.createdBy() : null)
            .build();
    return toResponse(experimentRunRepository.save(run));
  }

  /** Lista todos os runs de um experimento na ordem em que foram criados. */
  @Transactional(readOnly = true)
  public List<ExperimentRunResponse> listByExperiment(Long experimentId) {
    if (!experimentRepository.existsById(experimentId)) {
      throw new EntityNotFoundException("Experimento %d não encontrado".formatted(experimentId));
    }
    return experimentRunRepository.findByExperimentIdOrderByRunNumberAsc(experimentId).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Avalia novamente os gates determinísticos iniciais e atualiza o status operacional do run. */
  @Transactional
  public ExperimentRunPreflightResponse runPreflight(Long runId) {
    ExperimentRun run =
        experimentRunRepository
            .findById(runId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Run de experimento %d não encontrado".formatted(runId)));
    gateResultRepository.deleteByExperimentRunId(runId);
    List<ExperimentRunGateResult> gates = buildInitialGateResults(run);
    List<ExperimentRunGateResult> savedGates = gateResultRepository.saveAll(gates);
    run.setPreflightStartedAt(
        run.getPreflightStartedAt() != null ? run.getPreflightStartedAt() : Instant.now());
    updateRunFromGates(run, savedGates);
    ExperimentRun savedRun = experimentRunRepository.save(run);
    return toPreflightResponse(savedRun, savedGates);
  }

  /** Consolida resultados funcionais e libera o run somente quando nenhum gate ficar pendente. */
  @Transactional
  public ExperimentRunPreflightResponse recordHomologationResults(
      Long runId, ExperimentRunHomologationRequest request) {
    ExperimentRun run =
        experimentRunRepository
            .findById(runId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Run de experimento %d não encontrado".formatted(runId)));
    List<ExperimentRunGateResult> gates =
        gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(runId);
    if (gates.isEmpty()) {
      throw new IllegalStateException("Execute o preflight inicial antes da homologação funcional");
    }
    Set<String> expectedGateCodes = homologationGateCodes(run.getExperiment());
    Map<String, GateEvidence> evidenceByCode =
        validateHomologationRequest(run, request, expectedGateCodes);
    Instant evaluatedAt = Instant.now();
    gates.stream()
        .filter(gate -> expectedGateCodes.contains(gate.getGateCode()))
        .forEach(
            gate ->
                applyHomologationEvidence(
                    gate, evidenceByCode.get(gate.getGateCode()), evaluatedAt));
    List<ExperimentRunGateResult> savedGates = gateResultRepository.saveAll(gates);
    updateRunFromGates(run, savedGates);
    ExperimentRun savedRun = experimentRunRepository.save(run);
    return toPreflightResponse(savedRun, savedGates);
  }

  /** Consulta o último resultado de preflight persistido para o run. */
  @Transactional(readOnly = true)
  public ExperimentRunPreflightResponse getPreflight(Long runId) {
    ExperimentRun run =
        experimentRunRepository
            .findById(runId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Run de experimento %d não encontrado".formatted(runId)));
    List<ExperimentRunGateResult> gates =
        gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(runId);
    return toPreflightResponse(run, gates);
  }

  /** Busca um run específico pelo identificador técnico. */
  @Transactional(readOnly = true)
  public ExperimentRunResponse get(Long runId) {
    return experimentRunRepository
        .findById(runId)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Run de experimento %d não encontrado".formatted(runId)));
  }

  /** Monta a lista inicial de gates determinísticos com base no experimento vinculado ao run. */
  private List<ExperimentRunGateResult> buildInitialGateResults(ExperimentRun run) {
    Experiment experiment = run.getExperiment();
    Hypothesis hypothesis = experiment.getHypothesisRef();
    Instant evaluatedAt = Instant.now();
    CommercialDossierPreflightResult commercialDossierPreflight =
        moisCommercialDossierPreflightService.evaluate(experiment);
    return List.of(
        gate(
            run,
            "HYPOTHESIS_ARTIFACT_APPROVED",
            ExperimentRunGateGroup.UPSTREAM_QUALITY,
            hypothesis != null,
            "Hipótese vinculada ao experimento.",
            "Experimento sem hipótese vinculada.",
            "LINK_HYPOTHESIS",
            evaluatedAt),
        gate(
            run,
            "PERSONA_MINIMUM_COMPLETE",
            ExperimentRunGateGroup.UPSTREAM_QUALITY,
            hypothesis != null && hasUsefulText(hypothesis.getPersona()),
            "Persona mínima preenchida.",
            "Persona ausente ou preenchida como teste.",
            "REVIEW_PERSONA",
            evaluatedAt),
        gate(
            run,
            "DRPO_FRAMEWORK_COMPLETE",
            ExperimentRunGateGroup.UPSTREAM_QUALITY,
            hypothesis != null
                && hasUsefulText(hypothesis.getProblem())
                && hasUsefulText(hypothesis.getPromise())
                && hasUsefulText(hypothesis.getMechanism())
                && hasUsefulText(hypothesis.getEntrega()),
            "Dor, promessa, mecanismo e entrega estão preenchidos.",
            "Framework Dor → Resultado → Mecanismo → Prova → Oferta incompleto.",
            "COMPLETE_DRPO_FRAMEWORK",
            evaluatedAt),
        commercialDossierGate(run, commercialDossierPreflight, evaluatedAt),
        gate(
            run,
            "PRIMARY_VARIABLE_DEFINED",
            ExperimentRunGateGroup.EXPERIMENT_DESIGN,
            hasUsefulText(experiment.getPrimaryVariable()),
            "Variável primária definida.",
            "Variável primária ausente.",
            "DEFINE_PRIMARY_VARIABLE",
            evaluatedAt),
        gate(
            run,
            "PRIMARY_METRIC_DEFINED",
            ExperimentRunGateGroup.EXPERIMENT_DESIGN,
            hasUsefulText(experiment.getPrimaryMetric()),
            "Métrica primária definida.",
            "Métrica primária ausente.",
            "DEFINE_PRIMARY_METRIC",
            evaluatedAt),
        commercialTargetGate(run, experiment, evaluatedAt),
        pendingGate(
            run,
            LANDING_GATE,
            ExperimentRunGateGroup.ASSET_QUALITY,
            "Revisão da superfície comercial ainda não avaliada neste run.",
            evaluatedAt),
        pendingGate(
            run,
            journeyGateCode(experiment),
            ExperimentRunGateGroup.FUNCTIONAL_E2E,
            isSalesExperiment(experiment)
                ? "Checkout, pagamento de teste, acesso e entrega ainda não foram comprovados neste run."
                : "Submissão funcional ainda não foi comprovada neste run.",
            evaluatedAt),
        pendingGate(
            run,
            distributionGateCode(experiment),
            distributionGateGroup(experiment),
            experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE
                ? "Canal direto consentido, origem, amostra e ausência de gasto ainda não foram comprovados neste run."
                : "Status efetivo da distribuição Meta ainda não foi comprovado neste run.",
            evaluatedAt),
        pendingGate(
            run,
            DATA_GATE,
            ExperimentRunGateGroup.MEASUREMENT,
            "Freshness, correlação, deduplicação e segregação de QA ainda não foram comprovadas neste run.",
            evaluatedAt));
  }

  /** Exige meta econômica coerente com venda ou preserva o CPL para captação de leads. */
  private ExperimentRunGateResult commercialTargetGate(
      ExperimentRun run, Experiment experiment, Instant evaluatedAt) {
    if (isSalesExperiment(experiment)) {
      boolean complete =
          experiment.getUnitPrice() != null
              && experiment.getUnitPrice().signum() > 0
              && experiment.getSampleSize() != null
              && experiment.getSampleSize() > 0
              && experiment.getTargetCvr() != null
              && experiment.getTargetCvr().signum() > 0;
      return gate(
          run,
          "SALES_VALIDATION_TARGET_DEFINED",
          ExperimentRunGateGroup.EXPERIMENT_DESIGN,
          complete,
          "Preço, amostra e conversão-alvo de venda estão definidos.",
          "Experimento de venda sem preço, amostra ou conversão-alvo positiva.",
          "DEFINE_SALES_VALIDATION_TARGET",
          evaluatedAt);
    }
    return gate(
        run,
        "KPI_TARGET_CPL_VALID",
        ExperimentRunGateGroup.EXPERIMENT_DESIGN,
        experiment.getKpiTargetCpl() != null && experiment.getKpiTargetCpl().signum() > 0,
        "KPI alvo de CPL definido com valor positivo.",
        "KPI alvo de CPL ausente ou igual a zero.",
        "DEFINE_KPI_TARGET_CPL",
        evaluatedAt);
  }

  /** Resolve os quatro gates funcionais conforme objetivo e canal congelados no experimento. */
  private Set<String> homologationGateCodes(Experiment experiment) {
    return Set.of(
        LANDING_GATE, journeyGateCode(experiment), distributionGateCode(experiment), DATA_GATE);
  }

  /** Usa jornada de compra e entrega para vendas e mantém formulário para captação de leads. */
  private String journeyGateCode(Experiment experiment) {
    return isSalesExperiment(experiment) ? SALES_JOURNEY_GATE : FORM_GATE;
  }

  /** Exige evidência do canal efetivamente escolhido, sem forçar contrato Meta no canal direto. */
  private String distributionGateCode(Experiment experiment) {
    return experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE
        ? DIRECT_DISTRIBUTION_GATE
        : META_DISTRIBUTION_GATE;
  }

  /** Separa distribuição direta da publicação Meta na persistência do relatório de preflight. */
  private ExperimentRunGateGroup distributionGateGroup(Experiment experiment) {
    return experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE
        ? ExperimentRunGateGroup.DISTRIBUTION
        : ExperimentRunGateGroup.META_PUBLICATION;
  }

  /** Reconhece venda como objetivo do experimento independentemente do canal de aquisição. */
  private boolean isSalesExperiment(Experiment experiment) {
    return experiment.getCampaignObjective() == ExperimentCampaignObjective.SALES;
  }

  /** Cria o gate que exige dossiê comercial MOIS aderente antes de liberar mídia. */
  private ExperimentRunGateResult commercialDossierGate(
      ExperimentRun run, CommercialDossierPreflightResult result, Instant evaluatedAt) {
    return ExperimentRunGateResult.builder()
        .experimentRun(run)
        .gateCode("MOIS_COMMERCIAL_DOSSIER_PREFLIGHT")
        .gateGroup(ExperimentRunGateGroup.COMMERCIAL_EVIDENCE)
        .status(result.approved() ? ExperimentRunGateStatus.PASS : ExperimentRunGateStatus.FAIL)
        .severity(
            result.approved() ? ExperimentRunGateSeverity.INFO : ExperimentRunGateSeverity.BLOCKER)
        .summary(result.summary())
        .evidenceReference(result.evidenceReference())
        .remediationCode(result.approved() ? null : "GENERATE_MOIS_COMMERCIAL_DOSSIER")
        .evaluatedAt(evaluatedAt)
        .evaluatorType(ExperimentRunGateEvaluatorType.DETERMINISTIC)
        .evaluatorVersion(EVALUATOR_VERSION)
        .build();
  }

  /** Cria um gate determinístico com PASS ou FAIL conforme a condição objetiva. */
  private ExperimentRunGateResult gate(
      ExperimentRun run,
      String gateCode,
      ExperimentRunGateGroup group,
      boolean passed,
      String passSummary,
      String failSummary,
      String remediationCode,
      Instant evaluatedAt) {
    return ExperimentRunGateResult.builder()
        .experimentRun(run)
        .gateCode(gateCode)
        .gateGroup(group)
        .status(passed ? ExperimentRunGateStatus.PASS : ExperimentRunGateStatus.FAIL)
        .severity(passed ? ExperimentRunGateSeverity.INFO : ExperimentRunGateSeverity.BLOCKER)
        .summary(passed ? passSummary : failSummary)
        .remediationCode(passed ? null : remediationCode)
        .evaluatedAt(evaluatedAt)
        .evaluatorType(ExperimentRunGateEvaluatorType.DETERMINISTIC)
        .evaluatorVersion(EVALUATOR_VERSION)
        .build();
  }

  /** Cria um gate pendente para etapas que ainda serão implementadas nos próximos incrementos. */
  private ExperimentRunGateResult pendingGate(
      ExperimentRun run,
      String gateCode,
      ExperimentRunGateGroup group,
      String summary,
      Instant evaluatedAt) {
    return ExperimentRunGateResult.builder()
        .experimentRun(run)
        .gateCode(gateCode)
        .gateGroup(group)
        .status(ExperimentRunGateStatus.PENDING)
        .severity(ExperimentRunGateSeverity.WARNING)
        .summary(summary)
        .evaluatedAt(evaluatedAt)
        .evaluatorType(ExperimentRunGateEvaluatorType.DETERMINISTIC)
        .evaluatorVersion(EVALUATOR_VERSION)
        .build();
  }

  /** Valida completude, unicidade e estados aceitos nas evidencias da rodada de homologacao. */
  private Map<String, GateEvidence> validateHomologationRequest(
      ExperimentRun run, ExperimentRunHomologationRequest request, Set<String> expectedGateCodes) {
    if (request == null || request.gates() == null) {
      throw new IllegalArgumentException("Resultados de homologação são obrigatórios");
    }
    Map<String, GateEvidence> evidenceByCode = new HashMap<>();
    for (GateEvidence evidence : request.gates()) {
      if (evidence == null || !expectedGateCodes.contains(evidence.gateCode())) {
        throw new IllegalArgumentException("Gate funcional desconhecido na homologação");
      }
      if (evidenceByCode.putIfAbsent(evidence.gateCode(), evidence) != null) {
        throw new IllegalArgumentException("Gate funcional duplicado na homologação");
      }
      validateHomologationEvidence(run, evidence);
    }
    if (!evidenceByCode.keySet().equals(expectedGateCodes)) {
      throw new IllegalArgumentException("A homologação deve informar os quatro gates funcionais");
    }
    return evidenceByCode;
  }

  /** Impede evidencias vazias e uso indevido de NOT_APPLICABLE em execucao de producao. */
  private void validateHomologationEvidence(ExperimentRun run, GateEvidence evidence) {
    if (evidence.status() != ExperimentRunGateStatus.PASS
        && evidence.status() != ExperimentRunGateStatus.FAIL
        && evidence.status() != ExperimentRunGateStatus.NOT_APPLICABLE) {
      throw new IllegalArgumentException(
          "Gate homologado deve terminar como PASS, FAIL ou NOT_APPLICABLE");
    }
    if (evidence.status() == ExperimentRunGateStatus.NOT_APPLICABLE
        && (!META_DISTRIBUTION_GATE.equals(evidence.gateCode())
            || run.getMode() != ExperimentRunMode.TEST)) {
      throw new IllegalArgumentException(
          "NOT_APPLICABLE só é permitido para Meta em run técnico de teste");
    }
    if (!hasUsefulText(evidence.summary()) || !hasUsefulText(evidence.evidenceReference())) {
      throw new IllegalArgumentException("Gate homologado exige resumo e referência de evidência");
    }
    if (evidence.summary().length() > 512 || evidence.evidenceReference().length() > 512) {
      throw new IllegalArgumentException("Evidência de homologação excede o limite persistível");
    }
  }

  /**
   * Aplica a evidencia validada ao resultado atual sem criar gate paralelo ou perder correlacao.
   */
  private void applyHomologationEvidence(
      ExperimentRunGateResult gate, GateEvidence evidence, Instant evaluatedAt) {
    gate.setStatus(evidence.status());
    gate.setSeverity(
        evidence.status() == ExperimentRunGateStatus.FAIL
            ? ExperimentRunGateSeverity.BLOCKER
            : ExperimentRunGateSeverity.INFO);
    gate.setSummary(evidence.summary());
    gate.setEvidenceReference(evidence.evidenceReference());
    gate.setRemediationCode(
        evidence.status() == ExperimentRunGateStatus.FAIL ? "REPEAT_E2E_HOMOLOGATION" : null);
    gate.setEvaluatedAt(evaluatedAt);
    gate.setEvaluatorType(ExperimentRunGateEvaluatorType.DETERMINISTIC);
    gate.setEvaluatorVersion(HOMOLOGATION_EVALUATOR_VERSION);
  }

  /** Atualiza o run distinguindo falha comprovada, homologacao pendente e prontidao integral. */
  private void updateRunFromGates(ExperimentRun run, List<ExperimentRunGateResult> gates) {
    boolean failed =
        gates.stream().anyMatch(gate -> gate.getStatus() == ExperimentRunGateStatus.FAIL);
    boolean unresolved = gates.stream().anyMatch(gate -> !isApprovedGateStatus(gate.getStatus()));
    if (failed) {
      run.setStatus(ExperimentRunStatus.PREFLIGHT_FAILED);
      run.setDataQualityStatus(ExperimentRunDataQualityStatus.BLOCKED);
      run.setEvidenceValidity(ExperimentEvidenceValidity.STRATEGICALLY_INVALID);
      run.setPreflightCompletedAt(Instant.now());
      return;
    }
    if (unresolved) {
      run.setStatus(ExperimentRunStatus.PREFLIGHT_PENDING);
      run.setDataQualityStatus(ExperimentRunDataQualityStatus.UNKNOWN);
      run.setEvidenceValidity(ExperimentEvidenceValidity.NOT_EVALUATED);
      run.setPreflightCompletedAt(null);
      return;
    }
    run.setStatus(ExperimentRunStatus.READY_TO_PUBLISH);
    run.setDataQualityStatus(ExperimentRunDataQualityStatus.VALID);
    run.setEvidenceValidity(ExperimentEvidenceValidity.NOT_EVALUATED);
    run.setPreflightCompletedAt(Instant.now());
  }

  /** Considera concluido somente gate aprovado ou explicitamente inaplicavel. */
  private boolean isApprovedGateStatus(ExperimentRunGateStatus status) {
    return status == ExperimentRunGateStatus.PASS
        || status == ExperimentRunGateStatus.NOT_APPLICABLE;
  }

  /** Verifica se um texto possui conteúdo útil para decisão comercial. */
  private boolean hasUsefulText(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
    return !normalized.equals("teste") && !normalized.equals("test") && !normalized.equals("n/a");
  }

  /** Converte os gates persistidos em contrato de preflight para o frontend. */
  private ExperimentRunPreflightResponse toPreflightResponse(
      ExperimentRun run, List<ExperimentRunGateResult> gates) {
    boolean hasBlockers = gates.stream().anyMatch(gate -> !isApprovedGateStatus(gate.getStatus()));
    return new ExperimentRunPreflightResponse(
        run.getId(),
        run.getStatus(),
        hasBlockers,
        gates.stream().map(this::toGateResponse).toList());
  }

  /** Converte um gate persistido para o contrato de leitura do preflight. */
  private ExperimentRunGateResultResponse toGateResponse(ExperimentRunGateResult gate) {
    return new ExperimentRunGateResultResponse(
        gate.getGateCode(),
        gate.getGateGroup(),
        gate.getStatus(),
        gate.getSeverity(),
        gate.getSummary(),
        gate.getEvidenceReference(),
        gate.getRemediationCode(),
        gate.getEvaluatedAt(),
        gate.getEvaluatorType(),
        gate.getEvaluatorVersion());
  }

  /** Converte a entidade persistida no contrato de leitura usado pelo frontend. */
  private ExperimentRunResponse toResponse(ExperimentRun run) {
    return new ExperimentRunResponse(
        run.getId(),
        run.getExperiment().getId(),
        run.getRunNumber(),
        run.getMode(),
        run.getStatus(),
        run.getEvidenceValidity(),
        run.getStrategyVersion(),
        run.getAssetBundleVersion(),
        run.getAudienceVersion(),
        run.getStopPolicy(),
        run.getStopReason(),
        run.getFailureClassification(),
        run.getFailureDetail(),
        run.getDataQualityStatus(),
        run.getRequestedAt(),
        run.getPreflightStartedAt(),
        run.getPreflightCompletedAt(),
        run.getPublicationRequestedAt(),
        run.getPublishedAt(),
        run.getFirstVerifiedImpressionAt(),
        run.getCommercialWindowStartedAt(),
        run.getEndedAt(),
        run.getCreatedBy(),
        run.getCreatedAt(),
        run.getUpdatedAt());
  }
}
