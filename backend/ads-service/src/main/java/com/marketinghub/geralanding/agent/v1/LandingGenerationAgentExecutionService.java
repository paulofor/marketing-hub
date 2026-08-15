package com.marketinghub.geralanding.agent.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.qualityreview.service.LandingQualityReviewedEvent;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private final GeraSalesPagePublicationAuditRepository publicationRepository;
  private final ObjectMapper objectMapper;
  private final AgentTaskService agentTaskService;

  /** Inicializa a fila usando a persistência canônica do GeraLanding. */
  public LandingGenerationAgentExecutionService(
      GeraLandingStageExecutionRepository repository,
      LandingGenerationAgentCoordinator coordinator,
      ExperimentRepository experimentRepository,
      GeraSalesPagePublicationAuditRepository publicationRepository,
      ObjectMapper objectMapper,
      AgentTaskService agentTaskService) {
    this.repository = repository;
    this.coordinator = coordinator;
    this.experimentRepository = experimentRepository;
    this.publicationRepository = publicationRepository;
    this.objectMapper = objectMapper;
    this.agentTaskService = agentTaskService;
  }

  /** Converte o parecer independente em trabalho do agente ou conclui a jornada aprovada. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onQualityReviewCompleted(LandingQualityReviewedEvent event) {
    try {
      coordinator.learnFromIndependentQualityReview(event.experimentId(), event.reviewJson());
      Map<String, Object> review =
          objectMapper.readValue(event.reviewJson(), new TypeReference<>() {});
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

  /** Conclui a atividade de Dédalo somente depois do Quality Review independente aprovado. */
  private void completeBpmTaskAfterQualityApproval(LandingQualityReviewedEvent event) {
    Long taskId = bpmTaskId(event.autonomousCycleId());
    agentTaskService.completeClaimedProcessTask(
        "landing-generator",
        taskId,
        new com.marketinghub.agenttask.CompleteAgentTaskRequest(
            event.reviewJson(),
            objectMapper
                .createObjectNode()
                .put("experimentId", event.experimentId())
                .put("stageCode", "landing-page-quality-review")
                .put("approvalRecommendation", "APPROVE_FOR_PUBLICATION")
                .toString()));
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
        && (execution.getPromptContent().contains(DEPLOY_RECOVERY_POLICY)
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
    execution.setModelResponse(request.responseJson());
    execution.setInputTokens(request.inputTokens());
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
      coordinator.continueAfterQualityReview(
          execution.getExperimentId(), execution.getAutonomousCycleId(), request.decisionJson());
    }
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
                    .toString()));
      } else {
        agentTaskService.failClaimedProcessTask(
            "landing-generator",
            taskId,
            new com.marketinghub.agenttask.FailAgentTaskRequest(request.error()));
      }
      return;
    }
    agentTaskService.finishOperationalDelegation(
        "landing-generator", reference, request.error() == null);
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
                putWhenPresent(context, "pain", experiment.getSinglePain());
                putWhenPresent(context, "promise", experiment.getFunnelPromise());
                putWhenPresent(context, "primaryCta", experiment.getPrimaryCta());
                putWhenPresent(context, "landingHtml", experiment.getHtmlGeraLanding());
                publicationRepository
                    .findTopByExperimentIdOrderByPublishedAtDesc(experiment.getId())
                    .ifPresent(
                        publication ->
                            putWhenPresent(context, "checkoutUrl", publication.getCheckoutUrl()));
              });
      return new LandingAgentPendingResponse(
          textId(execution),
          execution.getExperimentId(),
          execution.getExecutionRequestedAt(),
          Map.copyOf(context));
    } catch (Exception ex) {
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
