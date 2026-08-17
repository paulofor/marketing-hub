package com.marketinghub.agentlearning.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.registerMemory.RegisterMemoryRequest;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.dto.CreativeAgentReviewResultRequest;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioJob;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.planning.imagestudio.v1.service.CommercialPlanVisualAssetReviewResultRequest;
import com.marketinghub.repository.jpa.agentlearning.TemisVisualLearningAssetHistory;
import com.marketinghub.repository.jpa.agentlearning.TemisVisualLearningCaseRepository;
import com.marketinghub.repository.jpa.agentlearning.TemisVisualLearningCreativeHistory;
import com.marketinghub.repository.jpa.agentlearning.TemisVisualLearningRunRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: consolidar tentativas visuais reais em replay governado e métricas de Têmis.
 */
@Service
public class TemisVisualLearningService {
  private static final Logger log = LoggerFactory.getLogger(TemisVisualLearningService.class);
  private static final int REPLAY_CASES = 10;
  private static final int HOLDOUT_CASES = 5;
  private static final int REQUIRED_CASES = REPLAY_CASES + HOLDOUT_CASES;
  private static final Duration LEASE = Duration.ofMinutes(45);
  private static final String AGENT_KEY = "meta-ad-approver";
  private static final String SCOPE_TYPE = "VISUAL_CONTEXT";
  private final TemisVisualLearningCaseRepository caseRepository;
  private final TemisVisualLearningRunRepository runRepository;
  private final CommercialPlanRepository planRepository;
  private final CommercialPlanImageStudioJobRepository imageJobRepository;
  private final CreativeRepository creativeRepository;
  private final TemisVisualPlaybookService playbookService;
  private final AgentMemoryService memoryService;
  private final GovernedAgentLearningService learningService;
  private final ObjectMapper objectMapper;

  /** Inicializa o ciclo com histórico, planos, memória e governança compartilhada. */
  public TemisVisualLearningService(
      TemisVisualLearningCaseRepository caseRepository,
      TemisVisualLearningRunRepository runRepository,
      CommercialPlanRepository planRepository,
      CommercialPlanImageStudioJobRepository imageJobRepository,
      CreativeRepository creativeRepository,
      TemisVisualPlaybookService playbookService,
      AgentMemoryService memoryService,
      GovernedAgentLearningService learningService,
      ObjectMapper objectMapper) {
    this.caseRepository = caseRepository;
    this.runRepository = runRepository;
    this.planRepository = planRepository;
    this.imageJobRepository = imageJobRepository;
    this.creativeRepository = creativeRepository;
    this.playbookService = playbookService;
    this.memoryService = memoryService;
    this.learningService = learningService;
    this.objectMapper = objectMapper;
  }

  /** Registra o resultado independente de um entregável sem duplicar callbacks repetidos. */
  @Transactional
  public void recordLibraryReview(
      CommercialPlanVisualAsset asset,
      CommercialPlanImageStudioJob job,
      CommercialPlanVisualAssetReviewResultRequest request) {
    if (caseRepository
        .findBySourceTypeAndSourceId(TemisVisualLearningSourceType.LIBRARY_ASSET, asset.getId())
        .isPresent()) return;
    JsonNode response = readObject(request.responseJson());
    List<String> issueCodes = issueCodes(response, List.of());
    BigDecimal score =
        minimumScore(
            response.path("qualityScore"),
            response.path("deliveryFidelityScore"),
            response.path("commercialReuseScore"));
    TemisVisualLearningCase value = new TemisVisualLearningCase();
    value.setSourceType(TemisVisualLearningSourceType.LIBRARY_ASSET);
    value.setSourceId(asset.getId());
    value.setCommercialPlanId(job.getCommercialPlan().getId());
    value.setExperimentId(
        job.getCommercialPlan().getExperiment() == null
            ? null
            : job.getCommercialPlan().getExperiment().getId());
    List<String> purposes = strings(readArray(job.getPurposesJson()));
    value.setContextKey(
        StringUtils.hasText(job.getPlaybookContextKey())
            ? job.getPlaybookContextKey()
            : playbookService.contextKey(
                job.getCommercialPlan(), job.getLabel(), purposes, job.getSize()));
    value.setPlaybookVersion(
        StringUtils.hasText(job.getPlaybookVersion())
            ? job.getPlaybookVersion()
            : "temis-visual-playbook-v1");
    value.setPlacement(placement(asset.getLabel()));
    value.setFormat(job.getSize());
    value.setAttemptNumber(Math.max(1, Objects.requireNonNullElse(asset.getVersionNumber(), 1)));
    value.setApproved(request.decision() == CommercialPlanVisualAssetReviewStatus.APPROVED);
    value.setQualityScore(score);
    value.setCostUsd(Objects.requireNonNullElse(job.getCostUsd(), BigDecimal.ZERO));
    value.setIssueCodesJson(write(issueCodes));
    value.setEvidenceJson(
        write(
            Map.of(
                "assetId", asset.getId(),
                "jobId", job.getId(),
                "label", asset.getLabel(),
                "decision", request.decision(),
                "summary", Objects.toString(request.summary(), ""),
                "issueCodes", issueCodes,
                "qualityScore", score)));
    value.setCreatedAt(Instant.now());
    TemisVisualLearningCase saved = caseRepository.save(value);
    createRunWhenReady(saved.getContextKey(), saved.getPlaybookVersion());
  }

  /** Incorpora pareceres antigos de um experimento sem carregar payloads base64 de produção. */
  @Transactional
  public TemisVisualLearningBackfillResponse backfillExperiment(Long experimentId) {
    List<CommercialPlan> plans = planRepository.findByExperimentReference(experimentId);
    if (plans.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Experimento sem plano comercial para aprendizado visual");
    }
    long casesBefore = caseRepository.count();
    long runsBefore = runRepository.count();
    int scannedAssets = 0;
    for (CommercialPlan plan : plans) {
      List<TemisVisualLearningAssetHistory> history =
          imageJobRepository.findVisualLearningHistoryByPlanId(plan.getId());
      scannedAssets += history.size();
      history.stream()
          .filter(value -> completed(value.reviewStatus()))
          .forEach(value -> recordHistoricalAsset(plan, value));
    }
    List<TemisVisualLearningCreativeHistory> creativeHistory =
        creativeRepository.findVisualLearningHistoryByExperimentId(experimentId);
    creativeHistory.stream()
        .filter(value -> completed(value.reviewStatus()))
        .forEach(this::recordHistoricalCreative);
    return new TemisVisualLearningBackfillResponse(
        experimentId,
        scannedAssets,
        creativeHistory.size(),
        Math.toIntExact(caseRepository.count() - casesBefore),
        runRepository.count() - runsBefore);
  }

  /** Reconstrói o contrato mínimo de um entregável já revisado e preserva sua auditoria. */
  private void recordHistoricalAsset(CommercialPlan plan, TemisVisualLearningAssetHistory history) {
    CommercialPlanVisualAsset asset = new CommercialPlanVisualAsset();
    asset.setId(history.assetId());
    asset.setCommercialPlan(plan);
    asset.setLabel(history.label());
    asset.setVersionNumber(history.versionNumber());
    asset.setStatus(history.status());
    CommercialPlanImageStudioJob job = new CommercialPlanImageStudioJob();
    job.setId(history.jobId());
    job.setCommercialPlan(plan);
    job.setLabel(history.label());
    job.setPurposesJson(history.purposesJson());
    job.setSize(history.size());
    job.setPlaybookVersion(history.playbookVersion());
    job.setPlaybookContextKey(history.playbookContextKey());
    job.setCostUsd(history.costUsd());
    String summary = readObject(history.reviewJson()).path("summary").asText("");
    recordLibraryReview(
        asset,
        job,
        new CommercialPlanVisualAssetReviewResultRequest(
            history.reviewStatus(),
            StringUtils.hasText(history.reviewerExecutionId())
                ? history.reviewerExecutionId()
                : "historical-asset-" + history.assetId(),
            summary,
            history.reviewRequestJson(),
            history.reviewResponseJson(),
            history.reviewStatus() == CommercialPlanVisualAssetReviewStatus.FAILED
                ? summary
                : null));
  }

  /** Reconstrói o parecer mínimo de um criativo antigo sem reexecutar o agente. */
  private void recordHistoricalCreative(TemisVisualLearningCreativeHistory history) {
    JsonNode review = readObject(history.reviewJson());
    Creative creative = new Creative();
    creative.setId(history.creativeId());
    creative.setVersionNumber(history.versionNumber());
    creative.setFormat(history.format());
    creative.setCostUsd(history.costUsd());
    creative.setAgentImprovementJson(history.improvementJson());
    com.marketinghub.experiment.Experiment experiment =
        new com.marketinghub.experiment.Experiment();
    experiment.setId(history.experimentId());
    creative.setExperiment(experiment);
    recordCreativeReview(
        creative,
        new CreativeAgentReviewResultRequest(
            history.reviewStatus(),
            review.path("attentionScore").asInt(0),
            review.path("clarityScore").asInt(0),
            review.path("desireScore").asInt(0),
            review.path("credibilityScore").asInt(0),
            review.path("actionScore").asInt(0),
            review.path("copyAssessment").asText(""),
            review.path("commercialAestheticAssessment").asText(""),
            review.path("destinationIntegrationAssessment").asText(""),
            review.path("summary").asText(""),
            jsonText(review.path("issues"), "[]"),
            jsonText(review.path("recommendations"), "[]"),
            "historical-review",
            history.reviewRequestJson(),
            history.reviewResponseJson(),
            review.path("inputTokens").asInt(0),
            review.path("outputTokens").asInt(0),
            history.costUsd(),
            review.path("error").asText(null),
            null,
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            List.of(),
            List.of()));
  }

  /** Reconhece apenas decisões finais da biblioteca como evidência histórica. */
  private boolean completed(CommercialPlanVisualAssetReviewStatus status) {
    return status == CommercialPlanVisualAssetReviewStatus.APPROVED
        || status == CommercialPlanVisualAssetReviewStatus.ADJUST
        || status == CommercialPlanVisualAssetReviewStatus.FAILED;
  }

  /** Reconhece apenas decisões finais de criativo como evidência histórica. */
  private boolean completed(CreativeAgentReviewStatus status) {
    return status == CreativeAgentReviewStatus.APPROVED
        || status == CreativeAgentReviewStatus.ADJUST
        || status == CreativeAgentReviewStatus.REJECTED
        || status == CreativeAgentReviewStatus.FAILED;
  }

  /** Preserva JSON já serializado ou materializa o nó estruturado de um parecer antigo. */
  private String jsonText(JsonNode node, String fallback) {
    if (node == null || node.isMissingNode() || node.isNull()) return fallback;
    return node.isTextual() ? node.asText(fallback) : node.toString();
  }

  /** Registra o parecer de um anúncio e preserva seu contexto comercial segregado. */
  @Transactional
  public void recordCreativeReview(Creative creative, CreativeAgentReviewResultRequest request) {
    if (caseRepository
        .findBySourceTypeAndSourceId(TemisVisualLearningSourceType.CREATIVE, creative.getId())
        .isPresent()) return;
    CommercialPlan plan =
        planRepository.findByExperimentReference(creative.getExperiment().getId()).stream()
            .findFirst()
            .orElse(null);
    if (plan == null) {
      log.warn(
          "Parecer visual sem plano comercial; aprendizado não registrado. creativeId={} experimentId={}",
          creative.getId(),
          creative.getExperiment().getId());
      return;
    }
    List<String> issueCodes =
        issueCodes(
            readObject(request.responseJson()),
            request.correctionTargets() == null
                ? List.of()
                : request.correctionTargets().stream()
                    .map(CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget::issueCode)
                    .toList());
    BigDecimal score =
        minimumScore(
            request.attentionScore(),
            request.clarityScore(),
            request.desireScore(),
            request.credibilityScore(),
            request.actionScore());
    String placement = placement(creative.getFormat());
    String size = "STORY".equals(placement) ? "1152x2048" : "2048x2048";
    String context = playbookService.contextKey(plan, creative.getFormat(), List.of("ADS"), size);
    String playbookVersion =
        latestPlaybookVersionForCreative(creative).orElse("temis-visual-playbook-v1");
    TemisVisualLearningCase value = new TemisVisualLearningCase();
    value.setSourceType(TemisVisualLearningSourceType.CREATIVE);
    value.setSourceId(creative.getId());
    value.setCommercialPlanId(plan.getId());
    value.setExperimentId(creative.getExperiment().getId());
    value.setContextKey(context);
    value.setPlaybookVersion(playbookVersion);
    value.setPlacement(placement);
    value.setFormat(Objects.toString(creative.getFormat(), "UNKNOWN"));
    value.setAttemptNumber(Math.max(1, Objects.requireNonNullElse(creative.getVersionNumber(), 1)));
    value.setApproved(request.decision() == CreativeAgentReviewStatus.APPROVED);
    value.setQualityScore(score);
    value.setCostUsd(Objects.requireNonNullElse(creative.getCostUsd(), BigDecimal.ZERO));
    value.setIssueCodesJson(write(issueCodes));
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("creativeId", creative.getId());
    evidence.put("experimentId", creative.getExperiment().getId());
    evidence.put("decision", request.decision());
    evidence.put("summary", Objects.toString(request.summary(), ""));
    evidence.put("issueCodes", issueCodes);
    evidence.put("qualityScore", score);
    value.setEvidenceJson(write(evidence));
    value.setCreatedAt(Instant.now());
    TemisVisualLearningCase saved = caseRepository.save(value);
    createRunWhenReady(saved.getContextKey(), saved.getPlaybookVersion());
  }

  /** Reserva uma única consolidação por vez sem transferir a decisão de promoção ao worker. */
  @Transactional
  public List<TemisVisualLearningPendingDto> claimPending(int limit) {
    return runRepository
        .findClaimable(
            TemisVisualLearningRunStatus.PENDING,
            TemisVisualLearningRunStatus.PROCESSING,
            Instant.now().minus(LEASE))
        .stream()
        .limit(Math.max(1, Math.min(3, limit)))
        .map(this::claim)
        .toList();
  }

  /** Conclui replay e holdout, criando memória candidata sem permitir autopromoção. */
  @Transactional
  public TemisVisualLearningRunResponse complete(
      Long runId, TemisVisualLearningResultRequest request) {
    TemisVisualLearningRun run = processing(runId, request.producerExecutionId());
    Evaluation evaluation = validateResult(run, request);
    String content = playbookContent(request.rules(), request.avoid());
    MemoryResponse memory =
        memoryService.register(
            AGENT_KEY,
            new RegisterMemoryRequest(
                null,
                SCOPE_TYPE,
                run.getContextKey(),
                "PLAYBOOK_VISUAL",
                content,
                "Amostra congelada de 10 replays e 5 holdouts da execução " + run.getId(),
                "temis-visual-learning-run/" + run.getId(),
                "temis-visual-learning/" + run.getProducerExecutionId(),
                new BigDecimal("0.80"),
                null));
    LearningExperimentResponse created =
        learningService.create(
            new CreateLearningExperimentRequest(
                AGENT_KEY,
                SCOPE_TYPE,
                run.getContextKey(),
                memory.id(),
                run.getCandidateVersion(),
                run.getBaselineVersion(),
                frozenJson(run.getReplayCaseIdsJson()),
                frozenJson(run.getHoldoutCaseIdsJson()),
                new BigDecimal("5.00"),
                BigDecimal.ZERO));
    BigDecimal averageCost = averageCost(run);
    Map<String, Object> candidateResult = new LinkedHashMap<>();
    candidateResult.put("playbook", Map.of("rules", request.rules(), "avoid", request.avoid()));
    candidateResult.put("caseAssessments", request.caseAssessments());
    candidateResult.put("reviewerExecutionId", request.producerExecutionId());
    LearningExperimentResponse evaluated =
        learningService.evaluate(
            created.id(),
            new EvaluateLearningExperimentRequest(
                evaluation.baselineHoldoutScore(),
                evaluation.candidateHoldoutScore(),
                averageCost,
                averageCost,
                REPLAY_CASES,
                HOLDOUT_CASES,
                request.regressionPassed() && evaluation.regressionPassed(),
                request.localValidationPassed(),
                false,
                false,
                false,
                write(
                    Map.of(
                        "score", evaluation.baselineReplayScore(),
                        "candidateScore", evaluation.candidateReplayScore(),
                        "cases", REPLAY_CASES)),
                write(candidateResult)));
    run.setMemoryId(memory.id());
    run.setLearningExperimentId(evaluated.id());
    run.setOutputJson(
        write(
            Map.of(
                "request",
                Objects.toString(request.requestJson(), ""),
                "response",
                Objects.toString(request.responseJson(), ""),
                "candidateResult",
                candidateResult,
                "deterministicEvaluation",
                evaluation)));
    run.setStatus(
        "READY_FOR_PROMOTION".equals(evaluated.status())
            ? TemisVisualLearningRunStatus.READY_FOR_PROMOTION
            : TemisVisualLearningRunStatus.REJECTED);
    run.setFinishedAt(Instant.now());
    run.setUpdatedAt(Instant.now());
    return response(runRepository.save(run));
  }

  /** Registra falha técnica preservando os casos para auditoria e nova decisão humana. */
  @Transactional
  public TemisVisualLearningRunResponse fail(
      Long runId, TemisVisualLearningFailureRequest request) {
    TemisVisualLearningRun run = processing(runId, request.producerExecutionId());
    run.setStatus(TemisVisualLearningRunStatus.FAILED);
    run.setError(required(request.error(), "erro"));
    run.setFinishedAt(Instant.now());
    run.setUpdatedAt(Instant.now());
    return response(runRepository.save(run));
  }

  /** Executa a promoção humana explícita somente após replay, holdout e regressão. */
  @Transactional
  public TemisVisualLearningRunResponse promote(Long runId) {
    TemisVisualLearningRun run = runRepository.findById(runId).orElseThrow();
    if (run.getStatus() != TemisVisualLearningRunStatus.READY_FOR_PROMOTION
        || run.getLearningExperimentId() == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Playbook de Têmis sem evidência suficiente para promoção");
    }
    learningService.promote(run.getLearningExperimentId());
    run.setStatus(TemisVisualLearningRunStatus.PROMOTED);
    run.setUpdatedAt(Instant.now());
    return response(runRepository.save(run));
  }

  /** Lista a trilha completa de consolidações para supervisão administrativa. */
  @Transactional(readOnly = true)
  public List<TemisVisualLearningRunResponse> listRuns() {
    return runRepository.findAllByOrderByIdDesc().stream().map(this::response).toList();
  }

  /** Consolida métricas reais por versão sem atribuir vendas ao aprendizado visual. */
  @Transactional(readOnly = true)
  public List<TemisVisualLearningMetricResponse> metrics() {
    return caseRepository.findAll().stream()
        .collect(
            Collectors.groupingBy(
                value -> value.getContextKey() + "\u0000" + value.getPlaybookVersion(),
                LinkedHashMap::new,
                Collectors.toList()))
        .entrySet()
        .stream()
        .map(entry -> metric(entry.getValue()))
        .sorted(Comparator.comparing(TemisVisualLearningMetricResponse::contextKey))
        .toList();
  }

  /** Congela automaticamente a próxima amostra homogênea quando alcança quinze casos. */
  private void createRunWhenReady(String contextKey, String playbookVersion) {
    List<TemisVisualLearningCase> available =
        caseRepository.findByContextKeyAndPlaybookVersionAndLearningRunIdIsNullOrderByIdAsc(
            contextKey, playbookVersion);
    if (available.size() < REQUIRED_CASES) return;
    List<TemisVisualLearningCase> frozen = available.subList(0, REQUIRED_CASES);
    TemisVisualLearningRun run = new TemisVisualLearningRun();
    run.setContextKey(contextKey);
    run.setStatus(TemisVisualLearningRunStatus.PENDING);
    run.setBaselineVersion(frozen.getFirst().getPlaybookVersion());
    run.setCandidateVersion(
        "temis-visual-" + frozen.getFirst().getId() + "-" + frozen.getLast().getId());
    run.setReplayCaseIdsJson(
        write(
            frozen.subList(0, REPLAY_CASES).stream().map(TemisVisualLearningCase::getId).toList()));
    run.setHoldoutCaseIdsJson(
        write(
            frozen.subList(REPLAY_CASES, REQUIRED_CASES).stream()
                .map(TemisVisualLearningCase::getId)
                .toList()));
    run.setInputJson(write(Map.of("cases", frozen.stream().map(this::caseSnapshot).toList())));
    run.setCreatedAt(Instant.now());
    run.setUpdatedAt(Instant.now());
    TemisVisualLearningRun saved = runRepository.save(run);
    frozen.forEach(value -> value.setLearningRunId(saved.getId()));
    caseRepository.saveAll(frozen);
  }

  /** Reserva a execução e entrega o snapshot congelado sem JSON aninhado em texto. */
  private TemisVisualLearningPendingDto claim(TemisVisualLearningRun run) {
    run.setStatus(TemisVisualLearningRunStatus.PROCESSING);
    run.setProducerExecutionId(UUID.randomUUID().toString());
    run.setStartedAt(Instant.now());
    run.setError(null);
    run.setUpdatedAt(Instant.now());
    runRepository.save(run);
    return new TemisVisualLearningPendingDto(
        run.getId(),
        run.getContextKey(),
        run.getBaselineVersion(),
        run.getCandidateVersion(),
        readObject(run.getInputJson()),
        run.getProducerExecutionId());
  }

  /** Valida a resposta contra a amostra congelada e bloqueia qualquer efeito externo. */
  private Evaluation validateResult(
      TemisVisualLearningRun run, TemisVisualLearningResultRequest request) {
    if (request.externalProviderCalled()
        || request.spendingAuthorized()
        || request.publicationPerformed()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Replay visual não permite provider, gasto ou publicação");
    }
    if (request.rules() == null
        || request.rules().isEmpty()
        || request.rules().size() > 8
        || request.avoid() == null
        || request.avoid().isEmpty()
        || request.avoid().size() > 8) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Playbook visual deve conter de uma a oito regras e proibições");
    }
    if (playbookContent(request.rules(), request.avoid()).length() > 3500) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Playbook visual excede o contexto operacional permitido");
    }
    validateScore(request.baselineReplayScore());
    validateScore(request.candidateReplayScore());
    validateScore(request.baselineHoldoutScore());
    validateScore(request.candidateHoldoutScore());
    if (request.caseAssessments() == null
        || !request.caseAssessments().isArray()
        || request.caseAssessments().size() != REQUIRED_CASES) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Replay visual deve avaliar exatamente quinze casos congelados");
    }
    Set<Long> expected = new LinkedHashSet<>();
    expected.addAll(readLongs(run.getReplayCaseIdsJson()));
    expected.addAll(readLongs(run.getHoldoutCaseIdsJson()));
    Set<Long> actual = new LinkedHashSet<>();
    request.caseAssessments().forEach(value -> actual.add(value.path("caseId").asLong(-1)));
    if (!actual.equals(expected)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Replay visual diverge dos casos congelados");
    }
    Map<Long, TemisVisualLearningCase> cases =
        caseRepository.findAllById(expected).stream()
            .collect(Collectors.toMap(TemisVisualLearningCase::getId, value -> value));
    if (!cases.keySet().equals(expected)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Replay visual contém caso histórico indisponível");
    }
    Set<Long> replayIds = new LinkedHashSet<>(readLongs(run.getReplayCaseIdsJson()));
    List<JsonNode> replayAssessments = new ArrayList<>();
    List<JsonNode> holdoutAssessments = new ArrayList<>();
    request
        .caseAssessments()
        .forEach(
            assessment -> {
              Long id = assessment.path("caseId").asLong(-1);
              TemisVisualLearningCase history = cases.get(id);
              String expectedSet = replayIds.contains(id) ? "REPLAY" : "HOLDOUT";
              String expectedDecision = history.isApproved() ? "APPROVED" : "BLOCKED";
              if (!expectedSet.equals(assessment.path("set").asText())
                  || !expectedDecision.equals(assessment.path("actualDecision").asText())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Avaliação visual contradiz a amostra congelada");
              }
              if ("REPLAY".equals(expectedSet)) replayAssessments.add(assessment);
              else holdoutAssessments.add(assessment);
            });
    Score replay = deterministicScore(replayAssessments, cases);
    Score holdout = deterministicScore(holdoutAssessments, cases);
    return new Evaluation(
        replay.baseline(),
        replay.candidate(),
        holdout.baseline(),
        holdout.candidate(),
        replay.regressionPassed() && holdout.regressionPassed());
  }

  /** Recalcula a efetividade sem confiar nas notas resumidas declaradas pelo consolidador. */
  private Score deterministicScore(
      List<JsonNode> assessments, Map<Long, TemisVisualLearningCase> cases) {
    long baselineSuccess = 0;
    long candidateSuccess = 0;
    boolean regressionPassed = true;
    for (JsonNode assessment : assessments) {
      TemisVisualLearningCase history = cases.get(assessment.path("caseId").asLong());
      if (history.isApproved()) {
        baselineSuccess++;
        boolean preserved = assessment.path("candidatePreservesApproved").asBoolean(false);
        if (preserved) candidateSuccess++;
        else regressionPassed = false;
      } else if (assessment.path("candidateWouldPreventRecurrence").asBoolean(false)) {
        candidateSuccess++;
      }
    }
    return new Score(
        percentage(baselineSuccess, assessments.size()),
        percentage(candidateSuccess, assessments.size()),
        regressionPassed);
  }

  /** Converte acertos em score percentual comparável. */
  private BigDecimal percentage(long successes, long total) {
    if (total == 0) return BigDecimal.ZERO;
    return BigDecimal.valueOf(successes)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
  }

  /** Exige pontuação percentual válida. */
  private void validateScore(BigDecimal value) {
    if (value == null
        || value.compareTo(BigDecimal.ZERO) < 0
        || value.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score visual fora de 0 a 100");
    }
  }

  /** Localiza uma execução PROCESSING e confirma a correlação da reserva. */
  private TemisVisualLearningRun processing(Long id, String executionId) {
    TemisVisualLearningRun run = runRepository.findById(id).orElseThrow();
    if (run.getStatus() != TemisVisualLearningRunStatus.PROCESSING
        || !Objects.equals(run.getProducerExecutionId(), executionId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Execução de consolidação não corresponde à reserva vigente");
    }
    return run;
  }

  /** Monta um caso sem JSON serializado dentro do snapshot principal. */
  private Map<String, Object> caseSnapshot(TemisVisualLearningCase value) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("caseId", value.getId());
    result.put("sourceType", value.getSourceType());
    result.put("sourceId", value.getSourceId());
    result.put("placement", value.getPlacement());
    result.put("format", value.getFormat());
    result.put("attemptNumber", value.getAttemptNumber());
    result.put("approved", value.isApproved());
    result.put("qualityScore", value.getQualityScore());
    result.put("costUsd", value.getCostUsd());
    result.put("issueCodes", readArray(value.getIssueCodesJson()));
    result.put("evidence", readObject(value.getEvidenceJson()));
    return result;
  }

  /** Calcula as metas operacionais sem confundir ganho de produção com venda. */
  private TemisVisualLearningMetricResponse metric(List<TemisVisualLearningCase> values) {
    long cases = values.size();
    long firstAttempts = values.stream().filter(value -> value.getAttemptNumber() == 1).count();
    long firstApprovals =
        values.stream()
            .filter(value -> value.getAttemptNumber() == 1 && value.isApproved())
            .count();
    long withinThree =
        values.stream()
            .filter(value -> value.getAttemptNumber() <= 3 && value.isApproved())
            .count();
    List<String> allIssues =
        values.stream()
            .flatMap(value -> strings(readArray(value.getIssueCodesJson())).stream())
            .toList();
    long recurring =
        allIssues.stream()
            .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
            .values()
            .stream()
            .filter(count -> count > 1)
            .mapToLong(count -> count - 1)
            .sum();
    List<TemisVisualLearningCase> approved =
        values.stream().filter(TemisVisualLearningCase::isApproved).toList();
    BigDecimal approvedCost =
        approved.stream()
            .map(TemisVisualLearningCase::getCostUsd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal minimumScore =
        values.stream()
            .map(TemisVisualLearningCase::getQualityScore)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    return new TemisVisualLearningMetricResponse(
        values.getFirst().getContextKey(),
        values.getFirst().getPlaybookVersion(),
        cases,
        ratio(firstApprovals, firstAttempts),
        ratio(withinThree, cases),
        ratio(recurring, Math.max(1, allIssues.size())),
        approved.isEmpty()
            ? BigDecimal.ZERO
            : approvedCost.divide(BigDecimal.valueOf(approved.size()), 4, RoundingMode.HALF_UP),
        minimumScore);
  }

  /** Calcula razão decimal estável. */
  private BigDecimal ratio(long numerator, long denominator) {
    if (denominator == 0) return BigDecimal.ZERO;
    return BigDecimal.valueOf(numerator)
        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
  }

  /** Calcula custo médio real dos casos congelados sem imputar custo ao replay. */
  private BigDecimal averageCost(TemisVisualLearningRun run) {
    List<Long> ids = new ArrayList<>(readLongs(run.getReplayCaseIdsJson()));
    ids.addAll(readLongs(run.getHoldoutCaseIdsJson()));
    List<TemisVisualLearningCase> values = caseRepository.findAllById(ids);
    return values.stream()
        .map(TemisVisualLearningCase::getCostUsd)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(Math.max(1, values.size())), 4, RoundingMode.HALF_UP);
  }

  /** Recupera a versão congelada no job que materializou a imagem do criativo. */
  private java.util.Optional<String> latestPlaybookVersionForCreative(Creative creative) {
    return java.util.Optional.ofNullable(creative.getAgentImprovementJson())
        .map(this::readObject)
        .map(value -> value.path("playbookVersion").asText(null))
        .filter(StringUtils::hasText);
  }

  /** Produz conteúdo conciso para a memória candidata sem embutir seu próprio JSON. */
  private String playbookContent(List<String> rules, List<String> avoid) {
    return "Regras promovíveis:\n- "
        + String.join("\n- ", rules)
        + "\nEvitar:\n- "
        + String.join("\n- ", avoid);
  }

  /** Materializa os casos congelados exigidos pelo contrato compartilhado de replay. */
  private String frozenJson(String idsJson) {
    return write(
        caseRepository.findAllById(readLongs(idsJson)).stream().map(this::caseSnapshot).toList());
  }

  /** Extrai códigos estáveis e usa fallback determinístico quando o schema legado não os possui. */
  private List<String> issueCodes(JsonNode response, List<String> fallback) {
    List<String> values = strings(response.path("issueCodes"));
    if (values.isEmpty()) values = fallback.stream().filter(StringUtils::hasText).toList();
    if (values.isEmpty() && !response.path("issues").isEmpty())
      values = List.of("VISUAL_ISSUE_UNCLASSIFIED");
    return values.stream()
        .map(String::trim)
        .filter(StringUtils::hasText)
        .distinct()
        .limit(8)
        .toList();
  }

  /** Calcula a menor nota para impedir que uma média esconda falha bloqueante. */
  private BigDecimal minimumScore(JsonNode... nodes) {
    return java.util.Arrays.stream(nodes)
        .map(node -> BigDecimal.valueOf(node.asDouble(0)))
        .min(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
  }

  /** Calcula a menor nota do parecer comercial. */
  private BigDecimal minimumScore(Integer... scores) {
    return java.util.Arrays.stream(scores)
        .map(value -> BigDecimal.valueOf(Objects.requireNonNullElse(value, 0)))
        .min(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
  }

  /** Normaliza placement pelo formato persistido. */
  private String placement(String value) {
    String normalized = Objects.toString(value, "").toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("story") || normalized.contains("9:16") ? "STORY" : "FEED";
  }

  /** Lê um objeto JSON e registra contexto antes de converter falha técnica. */
  private JsonNode readObject(String value) {
    try {
      JsonNode parsed = objectMapper.readTree(StringUtils.hasText(value) ? value : "{}");
      return parsed == null ? objectMapper.createObjectNode() : parsed;
    } catch (JsonProcessingException ex) {
      log.error("Falha ao ler evidência JSON do aprendizado visual", ex);
      throw new IllegalArgumentException("Evidência do aprendizado visual é inválida", ex);
    }
  }

  /** Lê um array JSON persistido. */
  private JsonNode readArray(String value) {
    JsonNode node = readObject(value);
    return node.isArray() ? node : objectMapper.createArrayNode();
  }

  /** Converte um array em lista textual. */
  private List<String> strings(JsonNode node) {
    if (!node.isArray()) return List.of();
    List<String> values = new ArrayList<>();
    node.forEach(
        item -> {
          if (StringUtils.hasText(item.asText())) values.add(item.asText().trim());
        });
    return values;
  }

  /** Lê identificadores de uma amostra congelada. */
  private List<Long> readLongs(String value) {
    try {
      return objectMapper.readValue(
          value, objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
    } catch (JsonProcessingException ex) {
      log.error("Falha ao ler IDs congelados do aprendizado visual", ex);
      throw new IllegalStateException("Amostra congelada de Têmis é inválida", ex);
    }
  }

  /** Serializa auditoria estruturada. */
  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao serializar aprendizado visual de Têmis", ex);
      throw new IllegalStateException("Aprendizado visual de Têmis não pôde ser persistido", ex);
    }
  }

  /** Exige correlação textual preenchida. */
  private String required(String value, String field) {
    if (!StringUtils.hasText(value)) throw new IllegalArgumentException(field + " é obrigatório");
    return value.trim();
  }

  /** Converte a execução para a visão administrativa. */
  private TemisVisualLearningRunResponse response(TemisVisualLearningRun run) {
    return new TemisVisualLearningRunResponse(
        run.getId(),
        run.getContextKey(),
        run.getStatus(),
        run.getBaselineVersion(),
        run.getCandidateVersion(),
        run.getMemoryId(),
        run.getLearningExperimentId(),
        run.getError(),
        run.getStartedAt(),
        run.getFinishedAt(),
        run.getCreatedAt());
  }

  /** Resultado determinístico de um conjunto congelado. */
  private record Score(BigDecimal baseline, BigDecimal candidate, boolean regressionPassed) {}

  /** Comparação determinística completa enviada à governança compartilhada. */
  private record Evaluation(
      BigDecimal baselineReplayScore,
      BigDecimal candidateReplayScore,
      BigDecimal baselineHoldoutScore,
      BigDecimal candidateHoldoutScore,
      boolean regressionPassed) {}
}
