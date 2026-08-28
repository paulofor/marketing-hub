package com.marketinghub.geralanding.agent.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.AutomaticBusinessProcessActivityService;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.qualityreview.service.LandingQualityReviewedEvent;
import com.marketinghub.planning.service.CommercialPlanApprovedCreativeEvidenceService;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Responsabilidade: persistir a fila e os callbacks do Agente Gerador de Landing. */
@Service
public class LandingGenerationAgentExecutionService {
  private static final Logger log =
      LoggerFactory.getLogger(LandingGenerationAgentExecutionService.class);
  private static final String STAGE = "landing-generation-agent-v1";
  private static final String PENDING = "INICIADO";
  private static final String PROCESSING = "PROCESSANDO";
  private static final List<String> ACTIVE_STATUSES = List.of(PENDING, PROCESSING);
  private static final String BUILD_MARKER_PREFIX = "CLAIMED_BY_BUILD:";
  private static final String DEPLOY_RECOVERY_POLICY = "RETRY_ON_EXECUTOR_DEPLOY";
  private static final String COMMERCIAL_HOMOLOGATION_SOURCE =
      "COMMERCIAL_PLAN_JOURNEY_HOMOLOGATION";
  private static final Pattern EXPERIMENT_REFERENCE =
      Pattern.compile("(?i)experimento\\s*#?\\s*(\\d+)");
  private static final String BPM_TASK_PREFIX = "agent-task:";
  private final GeraLandingStageExecutionRepository repository;
  private final LandingGenerationAgentCoordinator coordinator;
  private final ExperimentRepository experimentRepository;
  private final LandingCheckoutEvidenceResolver checkoutEvidenceResolver;
  private final LandingCommercialContextResolver commercialContextResolver;
  private final ObjectMapper objectMapper;
  private final AgentTaskService agentTaskService;
  private final AutomaticBusinessProcessActivityService automaticActivityService;
  private final LandingGenerationResultApplicationService resultApplicationService;
  private final CommercialPlanLandingAssetService landingAssetService;
  private final CommercialPlanApprovedCreativeEvidenceService approvedCreativeEvidenceService;

  /** Inicializa a fila usando a persistência canônica do GeraLanding. */
  public LandingGenerationAgentExecutionService(
      GeraLandingStageExecutionRepository repository,
      LandingGenerationAgentCoordinator coordinator,
      ExperimentRepository experimentRepository,
      LandingCheckoutEvidenceResolver checkoutEvidenceResolver,
      LandingCommercialContextResolver commercialContextResolver,
      ObjectMapper objectMapper,
      AgentTaskService agentTaskService,
      AutomaticBusinessProcessActivityService automaticActivityService,
      LandingGenerationResultApplicationService resultApplicationService,
      CommercialPlanLandingAssetService landingAssetService,
      CommercialPlanApprovedCreativeEvidenceService approvedCreativeEvidenceService) {
    this.repository = repository;
    this.coordinator = coordinator;
    this.experimentRepository = experimentRepository;
    this.checkoutEvidenceResolver = checkoutEvidenceResolver;
    this.commercialContextResolver = commercialContextResolver;
    this.objectMapper = objectMapper;
    this.agentTaskService = agentTaskService;
    this.automaticActivityService = automaticActivityService;
    this.resultApplicationService = resultApplicationService;
    this.landingAssetService = landingAssetService;
    this.approvedCreativeEvidenceService = approvedCreativeEvidenceService;
  }

  /** Converte o parecer independente em trabalho do agente ou conclui a jornada aprovada. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onQualityReviewCompleted(LandingQualityReviewedEvent event) {
    try {
      coordinator.learnFromIndependentQualityReview(event.experimentId(), event.reviewJson());
      Map<String, Object> review =
          objectMapper.readValue(event.reviewJson(), new TypeReference<>() {});
      if (isBpmTask(event.autonomousCycleId()) && isIrisTask(event.autonomousCycleId())) {
        if ("APPROVE_FOR_PUBLICATION".equals(review.get("approvalRecommendation"))) {
          completeIrisLandingAfterQualityApproval(event);
        } else {
          failIrisLandingAfterQualityReview(event);
        }
        return;
      }
      if ("APPROVE_FOR_PUBLICATION".equals(review.get("approvalRecommendation"))) {
        if (isBpmTask(event.autonomousCycleId())) {
          completeBpmTaskAfterQualityApproval(event);
          return;
        }
        coordinator.continueAfterQualityReview(
            event.experimentId(), event.autonomousCycleId(), event.reviewJson());
      } else {
        enqueue(event.experimentId(), event.autonomousCycleId(), event.reviewJson());
      }
    } catch (Exception ex) {
      log.error(
          "Falha ao enfileirar Agente Gerador de Landing. experimentId={}",
          event.experimentId(),
          ex);
    }
  }

  /**
   * Consolida a validação técnica no BPM e conclui Dédalo somente após o Quality Review aprovado.
   */
  private void completeBpmTaskAfterQualityApproval(LandingQualityReviewedEvent event)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    Long taskId = bpmTaskId(event.autonomousCycleId());
    GeraLandingStageExecution qualityReviewExecution = approvedQualityReviewExecution(event);
    automaticActivityService.completeFromExecution(
        taskId,
        "technical",
        textId(qualityReviewExecution),
        qualityReviewExecution.getExecutionRequestedAt(),
        qualityReviewExecution.getCompletedAt(),
        qualityReviewExecution.getCostUsd(),
        technicalActivityEvidence(event, qualityReviewExecution));
    Optional<GeraLandingStageExecution> technicalExecution =
        latestCompletedBpmExecution(event.experimentId(), taskId);
    agentTaskService.completeClaimedProcessTask(
        "landing-generator",
        taskId,
        new com.marketinghub.agenttask.CompleteAgentTaskRequest(
            event.reviewJson(),
            bpmEvidence(event),
            technicalExecution.map(this::modelUsage).orElseGet(List::of),
            technicalExecution.map(this::executionAudit).orElse(null)));
  }

  /** Conclui a materialização de Íris somente depois do Quality Review independente. */
  private void completeIrisLandingAfterQualityApproval(LandingQualityReviewedEvent event)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    Long taskId = bpmTaskId(event.autonomousCycleId());
    GeraLandingStageExecution qualityReviewExecution = approvedQualityReviewExecution(event);
    automaticActivityService.completeFromExecution(
        taskId,
        "technical",
        textId(qualityReviewExecution),
        qualityReviewExecution.getExecutionRequestedAt(),
        qualityReviewExecution.getCompletedAt(),
        qualityReviewExecution.getCostUsd(),
        technicalActivityEvidence(event, qualityReviewExecution));
    agentTaskService.completeDeferredProcessTask(
        "communication-director", taskId, bpmEvidence(event));
  }

  /** Bloqueia a candidata de Íris sem criar correção sob a identidade histórica de Dédalo. */
  private void failIrisLandingAfterQualityReview(LandingQualityReviewedEvent event) {
    Long taskId = bpmTaskId(event.autonomousCycleId());
    agentTaskService.failDeferredProcessTask(
        "communication-director",
        taskId,
        "Quality Review reprovou a landing materializada por Íris.",
        objectMapper
            .createObjectNode()
            .put("experimentId", event.experimentId())
            .put("stageCode", "landing-page-quality-review")
            .set("qualityReview", readTree(event.reviewJson()))
            .toString());
  }

  /** Verifica a identidade persistida antes de aplicar a regra de transição da nova agente. */
  private boolean isIrisTask(String reference) {
    return "communication-director".equals(agentTaskService.assignedAgentKey(bpmTaskId(reference)));
  }

  /** Converte JSON de gate já persistido em árvore sem esconder erro de contrato. */
  private com.fasterxml.jackson.databind.JsonNode readTree(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      log.error(
          "Quality Review de Íris contém JSON inválido. reviewLength={}",
          json == null ? 0 : json.length(),
          ex);
      throw new IllegalArgumentException("Quality Review de Íris inválido.", ex);
    }
  }

  /** Localiza a execução aprovada exata que originou o evento antes de avançar o BPM. */
  private GeraLandingStageExecution approvedQualityReviewExecution(
      LandingQualityReviewedEvent event) {
    return repository
        .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
            event.experimentId(), "landing-page-quality-review", event.autonomousCycleId())
        .stream()
        .filter(execution -> "CONCLUIDO".equals(execution.getStatus()))
        .filter(execution -> event.reviewJson().equals(execution.getModelResponse()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Quality Review aprovado sem execução técnica correlacionada."));
  }

  /** Monta a evidência funcional da atividade sem duplicar a auditoria técnica já persistida. */
  private String technicalActivityEvidence(
      LandingQualityReviewedEvent event, GeraLandingStageExecution execution)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    ObjectNode evidence = objectMapper.createObjectNode();
    evidence.put("experimentId", event.experimentId());
    evidence.put("stageCode", execution.getStageCode());
    evidence.put(
        "qualityReviewAuditReference", "gera_landing_stage_execution:" + textId(execution));
    evidence.set("qualityReview", objectMapper.readTree(event.reviewJson()));
    return objectMapper.writeValueAsString(evidence);
  }

  /** Entrega à próxima atividade a candidata visual, anúncio e checkout realmente avaliados. */
  private String bpmEvidence(LandingQualityReviewedEvent event) {
    com.fasterxml.jackson.databind.node.ObjectNode evidence = objectMapper.createObjectNode();
    evidence.put("experimentId", event.experimentId());
    evidence.put("stageCode", "landing-page-quality-review");
    evidence.put("approvalRecommendation", "APPROVE_FOR_PUBLICATION");
    experimentRepository
        .findById(event.experimentId())
        .ifPresent(
            experiment -> {
              evidence.put("landingHtml", experiment.getHtmlGeraLanding());
              evidence.put("adCopy", experiment.getAdCopy());
              evidence.put("adImageBriefing", experiment.getAdImageBriefing());
              Map<String, Object> checkoutContract = checkoutEvidenceResolver.resolve(experiment);
              evidence.set("checkoutContract", objectMapper.valueToTree(checkoutContract));
              evidence.set(
                  "approvedCreativeEvidence",
                  objectMapper.valueToTree(
                      approvedCreativeEvidenceService.resolve(experiment.getId())));
              Object checkoutUrl = checkoutContract.get("canonicalUrl");
              if (checkoutUrl != null) evidence.put("checkoutUrl", checkoutUrl.toString());
              if (experiment.getUnitPrice() != null)
                evidence.put("unitPriceBrl", experiment.getUnitPrice());
            });
    repository
        .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
            event.experimentId(), "landing-page-quality-review", event.autonomousCycleId())
        .stream()
        .map(GeraLandingStageExecution::getQualityReviewAudit)
        .filter(java.util.Objects::nonNull)
        .filter(audit -> !audit.isBlank())
        .findFirst()
        .ifPresent(audit -> evidence.put("qualityReviewAudit", audit));
    return evidence.toString();
  }

  /** Cria uma execução segregada com o parecer que motivou a correção. */
  @Transactional
  public void enqueue(Long experimentId, String autonomousCycleId, String qualityReviewJson) {
    if (repository.existsByExperimentIdAndStageCodeAndAutonomousCycleIdAndStatusIn(
        experimentId, STAGE, autonomousCycleId, ACTIVE_STATUSES)) {
      log.info(
          "Correção de landing já ativa; nova reprovação não será duplicada. experimentId={} autonomousCycleId={}",
          experimentId,
          autonomousCycleId);
      return;
    }
    Instant now = Instant.now();
    repository.save(
        GeraLandingStageExecution.builder()
            .experimentId(experimentId)
            .stageCode(STAGE)
            .autonomousCycleId(autonomousCycleId)
            .executionRequestedAt(now)
            .createdAt(now)
            .promptTemplateId("landing-generator/v1/remediation")
            .promptContent(qualityReviewJson)
            .status(PENDING)
            .idJob(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
            .build());
  }

  /** Converte o bloqueio comercial apontado por Têmis em um briefing autônomo para Dédalo. */
  @Transactional
  public void enqueueCreativeConvergenceCorrection(
      Long experimentId,
      String sourceReference,
      String issueCode,
      String correctionBrief,
      String acceptanceCriterion) {
    try {
      Map<String, Object> review = new LinkedHashMap<>();
      review.put("approvalRecommendation", "REGENERATE_BEFORE_PUBLICATION");
      review.put("score", 0);
      review.put("summary", correctionBrief);
      review.put(
          "blockingIssues",
          List.of(
              Map.of(
                  "code", issueCode,
                  "rootCause", correctionBrief,
                  "impact", "Impede a continuidade verificável entre anúncio e landing.",
                  "requiredChange", correctionBrief,
                  "evidence", acceptanceCriterion)));
      review.put("acceptanceCriteria", List.of(acceptanceCriterion));
      review.put(
          "authority",
          "Dédalo pode reconstruir livremente copy, hierarquia, imagens e HTML pelas etapas canônicas; não pode publicar, alterar oferta, preço, checkout ou tracking.");
      enqueue(experimentId, sourceReference, objectMapper.writeValueAsString(review));
    } catch (Exception ex) {
      log.error(
          "Falha ao preparar correção de convergência para Dédalo. experimentId={} sourceReference={}",
          experimentId,
          sourceReference,
          ex);
      throw new IllegalStateException("Briefing de correção da landing inválido", ex);
    }
  }

  /** Converte a atividade BPM reservada em uma execução técnica idempotente do GeraLanding. */
  @Transactional
  public void activateProcessTask(Long taskId) {
    activateProcessTask(agentTaskService.claimedProcessTask("landing-generator", taskId));
  }

  /** Reserva e materializa a próxima atividade de Dédalo na mesma transação do backend. */
  @Transactional
  public void activateNextProcessTask() {
    agentTaskService
        .claimEligibleProcessTask("landing-generator")
        .ifPresent(this::activateProcessTask);
  }

  /** Materializa o snapshot BPM já validado sem criar uma segunda fronteira HTTP. */
  private void activateProcessTask(com.marketinghub.agenttask.AgentTaskPendingResponse task) {
    Long taskId = task.taskId();
    Long experimentId = experimentId(task.title() + "\n" + task.description());
    if (hasMaterializedBpmAttempt(experimentId, taskId)) {
      log.info(
          "Tarefa BPM já foi materializada e aguarda gate ou correção causal. taskId={} experimentId={}",
          taskId,
          experimentId);
      return;
    }
    try {
      Map<String, Object> review = new LinkedHashMap<>();
      review.put("approvalRecommendation", "REGENERATE_BEFORE_PUBLICATION");
      review.put("score", 0);
      review.put("summary", task.description());
      review.put("blockingIssues", List.of());
      review.put("acceptanceCriteria", List.of(task.description()));
      review.put("processCode", task.processCode());
      review.put("processVersion", task.processVersion());
      review.put("processActivityId", task.activityId());
      review.put("processActivityName", task.activityName());
      review.put("agentTaskId", task.taskId());
      review.put(
          "authority",
          "Dédalo pode reconstruir arquitetura, narrativa, copy, imagens e HTML; não pode publicar, alterar preço, oferta, checkout ou tracking.");
      enqueue(
          experimentId, BPM_TASK_PREFIX + task.taskId(), objectMapper.writeValueAsString(review));
    } catch (Exception ex) {
      log.error(
          "Falha ao ativar tarefa BPM de Dédalo. taskId={} experimentId={}",
          taskId,
          experimentId,
          ex);
      throw new IllegalStateException("Não foi possível ativar a tarefa BPM de Dédalo", ex);
    }
  }

  /** Impede que o polling recrie o briefing original enquanto o Quality Review decide o avanço. */
  private boolean hasMaterializedBpmAttempt(Long experimentId, Long taskId) {
    return repository
        .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
            experimentId, STAGE, BPM_TASK_PREFIX + taskId)
        .stream()
        .anyMatch(execution -> !"FALHA".equals(execution.getStatus()));
  }

  /** Extrai a entidade operacional explicitamente declarada no título ou briefing da tarefa. */
  private Long experimentId(String taskText) {
    Matcher matcher = EXPERIMENT_REFERENCE.matcher(taskText == null ? "" : taskText);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Tarefa BPM de Dédalo sem experimento explícito");
    }
    return Long.parseLong(matcher.group(1));
  }

  /** Reserva jobs de forma transacional antes de qualquer consumo do Codex. */
  @Transactional
  public List<LandingAgentPendingResponse> claimPending(int requestedLimit) {
    return claimPending(requestedLimit, null);
  }

  /** Reconcilia homologações interrompidas por deploy e reserva a fila para a versão informada. */
  @Transactional
  public List<LandingAgentPendingResponse> claimPending(int requestedLimit, String buildReference) {
    recoverLegacyTimeoutFailures();
    recoverExpiredLeases();
    recoverCommercialHomologationsAfterDeploy(buildReference);
    int limit = Math.max(1, Math.min(3, requestedLimit));
    return repository
        .findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(STAGE, PENDING)
        .stream()
        .limit(limit)
        .map(execution -> claim(execution, buildReference))
        .toList();
  }

  /**
   * Reabre uma homologação técnica somente quando um executor realmente novo substituiu o anterior.
   */
  private void recoverCommercialHomologationsAfterDeploy(String buildReference) {
    String normalizedBuild = normalizeBuildReference(buildReference);
    if (normalizedBuild == null) return;
    Instant graceThreshold = Instant.now().minusSeconds(2 * 60L);
    for (GeraLandingStageExecution execution :
        repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(STAGE, PROCESSING)) {
      if (isDeployRecoverableHomologation(execution, normalizedBuild, graceThreshold)) {
        recordDeploymentRecovery(execution, normalizedBuild);
        execution.setStatus(PENDING);
        execution.setProcessingStartedAt(null);
        execution.setCompletedAt(null);
        execution.setErrorMessage(null);
        repository.save(execution);
      }
    }
    for (GeraLandingStageExecution execution :
        repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(STAGE, "FALHA")) {
      if (isDeployRecoverableHomologation(execution, normalizedBuild, graceThreshold)) {
        recordDeploymentRecovery(execution, normalizedBuild);
        execution.setStatus(PENDING);
        execution.setProcessingStartedAt(null);
        execution.setCompletedAt(null);
        execution.setErrorMessage(null);
        repository.save(execution);
      }
    }
  }

  /** Confirma política comercial, troca de build e idade mínima antes de permitir retomada. */
  private boolean isDeployRecoverableHomologation(
      GeraLandingStageExecution execution, String currentBuild, Instant graceThreshold) {
    String previousBuild = claimedBuild(execution);
    return execution.getPromptContent() != null
        && (isBpmTask(execution.getAutonomousCycleId())
            || execution.getPromptContent().contains(DEPLOY_RECOVERY_POLICY)
            || execution.getPromptContent().contains(COMMERCIAL_HOMOLOGATION_SOURCE))
        && execution.getExecutionRequestedAt() != null
        && execution.getExecutionRequestedAt().isBefore(graceThreshold)
        && !currentBuild.equals(previousBuild);
  }

  /** Extrai a versão que reservou originalmente a execução. */
  private String claimedBuild(GeraLandingStageExecution execution) {
    String detail = execution.getErrorDetail();
    if (detail == null || !detail.startsWith(BUILD_MARKER_PREFIX)) return "LEGACY_OR_UNKNOWN";
    return detail.substring(BUILD_MARKER_PREFIX.length());
  }

  /** Registra a troca de executor sem apagar auditorias anteriores da homologação. */
  private void recordDeploymentRecovery(GeraLandingStageExecution execution, String currentBuild) {
    String event =
        "DEPLOY_RECOVERY|from="
            + claimedBuild(execution)
            + "|to="
            + currentBuild
            + "|at="
            + Instant.now();
    String currentAudit = execution.getQualityReviewAudit();
    execution.setQualityReviewAudit(
        currentAudit == null || currentAudit.isBlank() ? event : currentAudit + "\n" + event);
  }

  /** Normaliza a referência de build recebida sem aceitar valores vazios ou excessivos. */
  private String normalizeBuildReference(String buildReference) {
    if (buildReference == null || buildReference.isBlank()) return null;
    String normalized = buildReference.trim();
    return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
  }

  /** Reabre uma única vez timeouts terminais gravados por versões antigas do worker. */
  private void recoverLegacyTimeoutFailures() {
    for (GeraLandingStageExecution execution :
        repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(STAGE, "FALHA")) {
      if (execution.getErrorDetail() == null
          && isRecoverableExecutorFailure(execution.getErrorMessage())) {
        String recoveryMarker = recoveryMarker(execution.getErrorMessage());
        execution.setStatus(PENDING);
        execution.setProcessingStartedAt(null);
        execution.setCompletedAt(null);
        execution.setErrorMessage(null);
        execution.setErrorDetail(recoveryMarker);
        repository.save(execution);
      }
    }
  }

  /** Reconhece somente falhas transitórias de timeout ou autenticação do executor. */
  private boolean isRecoverableExecutorFailure(String message) {
    if (message == null) return false;
    String normalized = message.toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("timeout do codex")
        || normalized.contains("refresh token")
        || normalized.contains("oauth")
        || normalized.contains("not authenticated");
  }

  /** Preserva o marcador legado de timeout e distingue as novas falhas do executor. */
  private String recoveryMarker(String message) {
    if (message != null
        && message.toLowerCase(java.util.Locale.ROOT).contains("timeout do codex")) {
      return "LEGACY_TIMEOUT_RECOVERED_ONCE";
    }
    return "EXECUTOR_FAILURE_RECOVERED_ONCE";
  }

  /** Recupera uma lease órfã uma vez e bloqueia reincidência para evitar loop infinito. */
  private void recoverExpiredLeases() {
    Instant threshold = Instant.now().minusSeconds(45 * 60L);
    for (GeraLandingStageExecution execution :
        repository
            .findTop20ByStageCodeAndStatusAndExecutionRequestedAtBeforeOrderByExecutionRequestedAtAsc(
                STAGE, PROCESSING, threshold)) {
      if ("LEASE_RECOVERED_ONCE".equals(execution.getErrorDetail())) {
        execution.setStatus("FALHA");
        execution.setErrorMessage("Lease do Agente de Landing expirou novamente");
        execution.setCompletedAt(Instant.now());
      } else {
        execution.setStatus(PENDING);
        execution.setProcessingStartedAt(null);
        execution.setErrorDetail("LEASE_RECOVERED_ONCE");
      }
      repository.save(execution);
    }
  }

  /** Persiste resposta ou falha e somente então delega o avanço ao coordenador do backend. */
  @Transactional
  public void complete(String executionId, LandingAgentResultRequest request) {
    GeraLandingStageExecution execution = find(executionId);
    if (!PROCESSING.equals(execution.getStatus())) {
      if ("CONCLUIDO".equals(execution.getStatus()) || "FALHA".equals(execution.getStatus()))
        return;
      throw new IllegalStateException("Execução do Agente de Landing não está reservada");
    }
    execution.setPrompt(request.requestJson());
    execution.setOpenAiRequestBody(request.requestJson());
    execution.setOpenAiModel(request.model());
    execution.setExecutionReasoningEffort(request.reasoningEffort().trim());
    execution.setModelResponse(request.responseJson());
    execution.setInputTokens(request.inputTokens());
    execution.setCachedInputTokens(request.cachedInputTokens());
    execution.setOutputTokens(request.outputTokens());
    execution.setCostUsd(request.costUsd());
    execution.setCompletedAt(Instant.now());
    execution.setErrorMessage(request.error());
    execution.setStatus(request.error() == null ? "CONCLUIDO" : "FALHA");
    repository.save(execution);
    if (request.error() != null || !isBpmTask(execution.getAutonomousCycleId())) {
      finishRelatedTask(execution, request);
    }
    if (request.error() == null) {
      continueOrBlockWithAuditableCause(execution, request);
    }
  }

  /** Converte falha de aplicação/orquestração em bloqueio auditável sem provocar callback 500. */
  private void continueOrBlockWithAuditableCause(
      GeraLandingStageExecution execution, LandingAgentResultRequest request) {
    try {
      resultApplicationService.apply(
          execution.getExperimentId(), execution.getAutonomousCycleId(), request.decisionJson());
    } catch (RuntimeException ex) {
      String cause = rootMessage(ex);
      log.error(
          "Falha ao aplicar resultado do Agente Gerador de Landing. experimentId={} executionId={}",
          execution.getExperimentId(),
          textId(execution),
          ex);
      execution.setStatus("FALHA");
      execution.setErrorMessage("Falha ao aplicar resultado: " + cause);
      repository.save(execution);
      if (isBpmTask(execution.getAutonomousCycleId())) {
        agentTaskService.failClaimedProcessTask(
            "landing-generator",
            bpmTaskId(execution.getAutonomousCycleId()),
            new com.marketinghub.agenttask.FailAgentTaskRequest(
                "Dédalo produziu a candidata, mas o backend não conseguiu aplicá-la: " + cause,
                null,
                null,
                modelUsage(execution),
                executionAudit(execution)));
      }
    }
  }

  /** Extrai a causa mais específica sem perder o stack trace registrado no log. */
  private String rootMessage(Throwable error) {
    Throwable current = error;
    while (current.getCause() != null) current = current.getCause();
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }

  /** Identifica correlações originadas por uma atividade do processo de negócio. */
  private boolean isBpmTask(String reference) {
    return reference != null && reference.startsWith(BPM_TASK_PREFIX);
  }

  /** Extrai o identificador da tarefa de uma correlação BPM já validada. */
  private Long bpmTaskId(String reference) {
    return Long.parseLong(reference.substring(BPM_TASK_PREFIX.length()));
  }

  /** Sincroniza a conclusão técnica com a atividade BPM ou delegação operacional de origem. */
  private void finishRelatedTask(
      GeraLandingStageExecution execution, LandingAgentResultRequest request) {
    String reference = execution.getAutonomousCycleId();
    if (isBpmTask(reference)) {
      Long taskId = bpmTaskId(reference);
      if (request.error() == null) {
        agentTaskService.completeClaimedProcessTask(
            "landing-generator",
            taskId,
            new com.marketinghub.agenttask.CompleteAgentTaskRequest(
                request.decisionJson(),
                objectMapper
                    .createObjectNode()
                    .put("executionId", textId(execution))
                    .put("experimentId", execution.getExperimentId())
                    .toString(),
                modelUsage(execution),
                executionAudit(execution)));
      } else {
        agentTaskService.failClaimedProcessTask(
            "landing-generator",
            taskId,
            new com.marketinghub.agenttask.FailAgentTaskRequest(
                request.error(), null, null, modelUsage(execution), executionAudit(execution)));
      }
      return;
    }
    agentTaskService.finishOperationalDelegation(
        "landing-generator", reference, request.error() == null);
  }

  /** Recupera a última execução técnica aprovada para propagar sua auditoria à tarefa BPM. */
  private Optional<GeraLandingStageExecution> latestCompletedBpmExecution(
      Long experimentId, Long taskId) {
    return repository
        .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
            experimentId, STAGE, BPM_TASK_PREFIX + taskId)
        .stream()
        .filter(execution -> "CONCLUIDO".equals(execution.getStatus()))
        .findFirst();
  }

  /** Converte a medição persistida da execução técnica no contrato econômico da tarefa. */
  private List<com.marketinghub.agenttask.AgentTaskModelUsageRequest> modelUsage(
      GeraLandingStageExecution execution) {
    if (execution.getOpenAiModel() == null
        || execution.getOpenAiModel().isBlank()
        || (execution.getInputTokens() == null && execution.getOutputTokens() == null)) {
      return List.of();
    }
    long input = execution.getInputTokens() == null ? 0 : execution.getInputTokens();
    long cached = execution.getCachedInputTokens() == null ? 0 : execution.getCachedInputTokens();
    long output = execution.getOutputTokens() == null ? 0 : execution.getOutputTokens();
    return List.of(
        new com.marketinghub.agenttask.AgentTaskModelUsageRequest(
            execution.getOpenAiModel(), "STANDARD", input, cached, output));
  }

  /** Converte a auditoria técnica completa em metadados imutáveis da tarefa BPM. */
  private com.marketinghub.agenttask.AgentTaskExecutionAuditRequest executionAudit(
      GeraLandingStageExecution execution) {
    String model = textOrNull(execution.getOpenAiModel());
    String reasoningEffort = textOrNull(execution.getExecutionReasoningEffort());
    String prompt = textOrNull(execution.getPrompt());
    if (model == null || reasoningEffort == null || prompt == null) return null;
    return new com.marketinghub.agenttask.AgentTaskExecutionAuditRequest(
        model, reasoningEffort, prompt);
  }

  /** Normaliza texto de auditoria sem inventar valores ausentes em execuções históricas. */
  private String textOrNull(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }

  /** Recupera o snapshot congelado usado pelo MCP exclusivo. */
  @Transactional(readOnly = true)
  public LandingAgentPendingResponse context(String executionId) {
    return response(find(executionId));
  }

  /** Marca a execução como reservada e devolve o contexto imutável. */
  private LandingAgentPendingResponse claim(
      GeraLandingStageExecution execution, String buildReference) {
    execution.setStatus(PROCESSING);
    execution.setProcessingStartedAt(Instant.now());
    String normalizedBuild = normalizeBuildReference(buildReference);
    if (normalizedBuild != null) execution.setErrorDetail(BUILD_MARKER_PREFIX + normalizedBuild);
    repository.save(execution);
    return response(execution);
  }

  /** Converte o registro persistido para o contrato do worker. */
  private LandingAgentPendingResponse response(GeraLandingStageExecution execution) {
    try {
      Map<String, Object> context = new LinkedHashMap<>();
      context.put("qualityReview", objectMapper.readTree(execution.getPromptContent()));
      context.put("agentKey", "landing-generator");
      context.put("authority", "DRAFT_ONLY_NO_PUBLICATION");
      context.put(
          "generationApproachCatalog",
          List.of(
              Map.of(
                  "approachCode", "GERALANDING_PIPELINE",
                  "available", true,
                  "executorContract", "canonical-stages-v1"),
              Map.of(
                  "approachCode", "COMPONENT_TEMPLATE_COMPOSER",
                  "available", false,
                  "executorContract", "NOT_REGISTERED"),
              Map.of(
                  "approachCode", "CODEX_CODE_IMPLEMENTATION",
                  "available", true,
                  "executorContract", "governed-full-html-v1")));
      experimentRepository
          .findById(execution.getExperimentId())
          .ifPresent(
              experiment -> {
                putWhenPresent(context, "experimentName", experiment.getName());
                if (experiment.getProduct() != null) {
                  putWhenPresent(context, "productName", experiment.getProduct().getName());
                }
                context.putAll(commercialContextResolver.resolve(experiment));
                putWhenPresent(context, "pain", experiment.getSinglePain());
                putWhenPresent(context, "freeReward", experiment.getFreeReward());
                putWhenPresent(context, "promise", experiment.getFunnelPromise());
                putWhenPresent(context, "primaryCta", experiment.getPrimaryCta());
                putWhenPresent(context, "unitPriceBrl", experiment.getUnitPrice());
                putWhenPresent(context, "commercialObjective", experiment.getCommercialObjective());
                putWhenPresent(
                    context,
                    "desireTerritorySnapshotJson",
                    experiment.getDesireTerritorySnapshotJson());
                putWhenPresent(context, "primaryVariable", experiment.getPrimaryVariable());
                putWhenPresent(context, "primaryMetric", experiment.getPrimaryMetric());
                putWhenPresent(context, "adCopy", experiment.getAdCopy());
                putWhenPresent(context, "adImageBriefing", experiment.getAdImageBriefing());
                putWhenPresent(context, "landingHtml", experiment.getHtmlGeraLanding());
                context.put(
                    "approvedLandingVisualAssets",
                    landingAssetService.payloadForExperiment(experiment.getId()));
                context.put(
                    "approvedCreativeEvidence",
                    approvedCreativeEvidenceService.resolve(experiment.getId()));
                context.put(
                    "minimumApprovedLandingVisualAssets",
                    landingAssetService.requiredReferenceCount(experiment.getId()));
                context.put(
                    "visualAssetRule",
                    "Reutilize os arquivos APPROVED literalmente; não redesenhe o produto. A landing deve exibir ao menos minimumApprovedLandingVisualAssets URLs distintas do catálogo.");
                Map<String, Object> checkoutContract = checkoutEvidenceResolver.resolve(experiment);
                context.put("checkoutContract", checkoutContract);
                Object checkoutUrl = checkoutContract.get("canonicalUrl");
                if (checkoutUrl != null) {
                  context.put("checkoutUrl", checkoutUrl.toString());
                }
              });
      return new LandingAgentPendingResponse(
          textId(execution),
          execution.getExperimentId(),
          execution.getExecutionRequestedAt(),
          Map.copyOf(context));
    } catch (Exception ex) {
      log.error(
          "Falha ao montar snapshot do Agente Gerador de Landing. experimentId={} executionId={}",
          execution.getExperimentId(),
          textId(execution),
          ex);
      throw new IllegalStateException("Snapshot inválido do Agente Gerador de Landing", ex);
    }
  }

  /** Inclui no snapshot somente campos opcionais efetivamente disponíveis. */
  private void putWhenPresent(Map<String, Object> context, String key, Object value) {
    if (value != null) context.put(key, value);
  }

  /** Localiza uma execução pelo identificador textual. */
  private GeraLandingStageExecution find(String executionId) {
    return repository
        .findTopByIdJobOrderByExecutionRequestedAtDesc(executionId.getBytes(StandardCharsets.UTF_8))
        .filter(value -> STAGE.equals(value.getStageCode()))
        .orElseThrow(() -> new EntityNotFoundException("Execução do Agente de Landing ausente"));
  }

  /** Converte o identificador binário canônico em texto. */
  private String textId(GeraLandingStageExecution execution) {
    return new String(execution.getIdJob(), StandardCharsets.UTF_8);
  }
}
