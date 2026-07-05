package com.marketinghub.experiment.run.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.run.ExperimentEvidenceValidity;
import com.marketinghub.experiment.run.ExperimentRunGateEvaluatorType;
import com.marketinghub.experiment.run.ExperimentRunGateGroup;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateSeverity;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunDataQualityStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.experiment.run.ExperimentRunStopPolicy;
import com.marketinghub.experiment.run.service.create.CreateExperimentRunRequest;
import com.marketinghub.experiment.run.service.get.ExperimentRunResponse;
import com.marketinghub.experiment.run.service.preflight.ExperimentRunGateResultResponse;
import com.marketinghub.experiment.run.service.preflight.ExperimentRunPreflightResponse;
import com.marketinghub.experiment.run.service.MoisCommercialDossierPreflightService.CommercialDossierPreflightResult;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Orquestra criação e leitura dos runs operacionais vinculados a experimentos.
 */
@Service
public class BackendExperimentRunService {
    private final ExperimentRepository experimentRepository;
    private static final String EVALUATOR_VERSION = "experiment-run-preflight.v1";

    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentRunGateResultRepository gateResultRepository;
    private final MoisCommercialDossierPreflightService moisCommercialDossierPreflightService;

    /** Inicializa o serviço com os repositórios canônicos de experimento e run. */
    public BackendExperimentRunService(ExperimentRepository experimentRepository,
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
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experimento %d não encontrado".formatted(experimentId)));
        int nextRunNumber = experimentRunRepository.findMaxRunNumberByExperimentId(experimentId) + 1;
        Instant now = Instant.now();
        ExperimentRun run = ExperimentRun.builder()
                .experiment(experiment)
                .runNumber(nextRunNumber)
                .mode(request != null && request.mode() != null ? request.mode() : ExperimentRunMode.PRODUCTION)
                .status(ExperimentRunStatus.DRAFT)
                .evidenceValidity(ExperimentEvidenceValidity.NOT_EVALUATED)
                .stopPolicy(request != null && request.stopPolicy() != null ? request.stopPolicy() : ExperimentRunStopPolicy.MANUAL_ONLY)
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
        ExperimentRun run = experimentRunRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("Run de experimento %d não encontrado".formatted(runId)));
        gateResultRepository.deleteByExperimentRunId(runId);
        List<ExperimentRunGateResult> gates = buildInitialGateResults(run);
        List<ExperimentRunGateResult> savedGates = gateResultRepository.saveAll(gates);
        boolean hasBlockers = savedGates.stream().anyMatch(gate -> gate.getStatus() == ExperimentRunGateStatus.FAIL);
        run.setStatus(hasBlockers ? ExperimentRunStatus.PREFLIGHT_FAILED : ExperimentRunStatus.READY_TO_PUBLISH);
        run.setDataQualityStatus(hasBlockers ? ExperimentRunDataQualityStatus.BLOCKED : ExperimentRunDataQualityStatus.VALID);
        run.setEvidenceValidity(hasBlockers ? ExperimentEvidenceValidity.STRATEGICALLY_INVALID : ExperimentEvidenceValidity.NOT_EVALUATED);
        run.setPreflightStartedAt(run.getPreflightStartedAt() != null ? run.getPreflightStartedAt() : Instant.now());
        run.setPreflightCompletedAt(Instant.now());
        ExperimentRun savedRun = experimentRunRepository.save(run);
        return toPreflightResponse(savedRun, savedGates);
    }

    /** Consulta o último resultado de preflight persistido para o run. */
    @Transactional(readOnly = true)
    public ExperimentRunPreflightResponse getPreflight(Long runId) {
        ExperimentRun run = experimentRunRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("Run de experimento %d não encontrado".formatted(runId)));
        List<ExperimentRunGateResult> gates = gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(runId);
        return toPreflightResponse(run, gates);
    }

    /** Busca um run específico pelo identificador técnico. */
    @Transactional(readOnly = true)
    public ExperimentRunResponse get(Long runId) {
        return experimentRunRepository.findById(runId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Run de experimento %d não encontrado".formatted(runId)));
    }


    /** Monta a lista inicial de gates determinísticos com base no experimento vinculado ao run. */
    private List<ExperimentRunGateResult> buildInitialGateResults(ExperimentRun run) {
        Experiment experiment = run.getExperiment();
        Hypothesis hypothesis = experiment.getHypothesisRef();
        Instant evaluatedAt = Instant.now();
        CommercialDossierPreflightResult commercialDossierPreflight =
                moisCommercialDossierPreflightService.evaluate(experiment);
        return List.of(
                gate(run, "HYPOTHESIS_ARTIFACT_APPROVED", ExperimentRunGateGroup.UPSTREAM_QUALITY,
                        hypothesis != null, "Hipótese vinculada ao experimento.",
                        "Experimento sem hipótese vinculada.", "LINK_HYPOTHESIS", evaluatedAt),
                gate(run, "PERSONA_MINIMUM_COMPLETE", ExperimentRunGateGroup.UPSTREAM_QUALITY,
                        hypothesis != null && hasUsefulText(hypothesis.getPersona()), "Persona mínima preenchida.",
                        "Persona ausente ou preenchida como teste.", "REVIEW_PERSONA", evaluatedAt),
                gate(run, "DRPO_FRAMEWORK_COMPLETE", ExperimentRunGateGroup.UPSTREAM_QUALITY,
                        hypothesis != null && hasUsefulText(hypothesis.getProblem()) && hasUsefulText(hypothesis.getPromise())
                                && hasUsefulText(hypothesis.getMechanism()) && hasUsefulText(hypothesis.getEntrega()),
                        "Dor, promessa, mecanismo e entrega estão preenchidos.",
                        "Framework Dor → Resultado → Mecanismo → Prova → Oferta incompleto.",
                        "COMPLETE_DRPO_FRAMEWORK", evaluatedAt),
                commercialDossierGate(run, commercialDossierPreflight, evaluatedAt),
                gate(run, "PRIMARY_VARIABLE_DEFINED", ExperimentRunGateGroup.EXPERIMENT_DESIGN,
                        hasUsefulText(experiment.getPrimaryVariable()), "Variável primária definida.",
                        "Variável primária ausente.", "DEFINE_PRIMARY_VARIABLE", evaluatedAt),
                gate(run, "PRIMARY_METRIC_DEFINED", ExperimentRunGateGroup.EXPERIMENT_DESIGN,
                        hasUsefulText(experiment.getPrimaryMetric()), "Métrica primária definida.",
                        "Métrica primária ausente.", "DEFINE_PRIMARY_METRIC", evaluatedAt),
                gate(run, "KPI_TARGET_CPL_VALID", ExperimentRunGateGroup.EXPERIMENT_DESIGN,
                        experiment.getKpiTargetCpl() != null && experiment.getKpiTargetCpl().signum() > 0,
                        "KPI alvo de CPL definido com valor positivo.",
                        "KPI alvo de CPL ausente ou igual a zero.", "DEFINE_KPI_TARGET_CPL", evaluatedAt),
                pendingGate(run, "LANDING_QUALITY_REVIEW_APPROVED", ExperimentRunGateGroup.ASSET_QUALITY,
                        "Revisão da landing ainda não avaliada neste incremento.", evaluatedAt),
                pendingGate(run, "FORM_CAN_BE_SUBMITTED", ExperimentRunGateGroup.FUNCTIONAL_E2E,
                        "Teste E2E do formulário será executado em incremento posterior.", evaluatedAt),
                pendingGate(run, "META_EFFECTIVE_STATUS_CONFIRMED", ExperimentRunGateGroup.META_PUBLICATION,
                        "Publicação Meta será vinculada ao run em incremento posterior.", evaluatedAt),
                pendingGate(run, "DATA_FRESHNESS_VALID", ExperimentRunGateGroup.MEASUREMENT,
                        "Freshness de dados comerciais será calculado em incremento posterior.", evaluatedAt)
        );
    }

    /** Cria o gate que exige dossiê comercial MOIS aderente antes de liberar mídia. */
    private ExperimentRunGateResult commercialDossierGate(ExperimentRun run,
                                                          CommercialDossierPreflightResult result,
                                                          Instant evaluatedAt) {
        return ExperimentRunGateResult.builder()
                .experimentRun(run)
                .gateCode("MOIS_COMMERCIAL_DOSSIER_PREFLIGHT")
                .gateGroup(ExperimentRunGateGroup.COMMERCIAL_EVIDENCE)
                .status(result.approved() ? ExperimentRunGateStatus.PASS : ExperimentRunGateStatus.FAIL)
                .severity(result.approved() ? ExperimentRunGateSeverity.INFO : ExperimentRunGateSeverity.BLOCKER)
                .summary(result.summary())
                .evidenceReference(result.evidenceReference())
                .remediationCode(result.approved() ? null : "GENERATE_MOIS_COMMERCIAL_DOSSIER")
                .evaluatedAt(evaluatedAt)
                .evaluatorType(ExperimentRunGateEvaluatorType.DETERMINISTIC)
                .evaluatorVersion(EVALUATOR_VERSION)
                .build();
    }

    /** Cria um gate determinístico com PASS ou FAIL conforme a condição objetiva. */
    private ExperimentRunGateResult gate(ExperimentRun run, String gateCode, ExperimentRunGateGroup group, boolean passed,
                                         String passSummary, String failSummary, String remediationCode, Instant evaluatedAt) {
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
    private ExperimentRunGateResult pendingGate(ExperimentRun run, String gateCode, ExperimentRunGateGroup group,
                                                String summary, Instant evaluatedAt) {
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

    /** Verifica se um texto possui conteúdo útil para decisão comercial. */
    private boolean hasUsefulText(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return !normalized.equals("teste") && !normalized.equals("test") && !normalized.equals("n/a");
    }

    /** Converte os gates persistidos em contrato de preflight para o frontend. */
    private ExperimentRunPreflightResponse toPreflightResponse(ExperimentRun run, List<ExperimentRunGateResult> gates) {
        boolean hasBlockers = gates.stream().anyMatch(gate -> gate.getStatus() == ExperimentRunGateStatus.FAIL);
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
