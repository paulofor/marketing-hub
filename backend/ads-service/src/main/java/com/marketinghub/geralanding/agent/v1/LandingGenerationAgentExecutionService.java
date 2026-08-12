package com.marketinghub.geralanding.agent.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.qualityreview.service.LandingQualityReviewedEvent;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  private final GeraLandingStageExecutionRepository repository;
  private final LandingGenerationAgentCoordinator coordinator;
  private final ExperimentRepository experimentRepository;
  private final ObjectMapper objectMapper;
  private final AgentTaskService agentTaskService;

  /** Inicializa a fila usando a persistência canônica do GeraLanding. */
  public LandingGenerationAgentExecutionService(
      GeraLandingStageExecutionRepository repository,
      LandingGenerationAgentCoordinator coordinator,
      ExperimentRepository experimentRepository,
      ObjectMapper objectMapper,
      AgentTaskService agentTaskService) {
    this.repository = repository;
    this.coordinator = coordinator;
    this.experimentRepository = experimentRepository;
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

  /** Cria uma execução segregada com o parecer que motivou a correção. */
  @Transactional
  public void enqueue(Long experimentId, String autonomousCycleId, String qualityReviewJson) {
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

  /** Reserva jobs de forma transacional antes de qualquer consumo do Codex. */
  @Transactional
  public List<LandingAgentPendingResponse> claimPending(int requestedLimit) {
    recoverLegacyTimeoutFailures();
    recoverExpiredLeases();
    int limit = Math.max(1, Math.min(3, requestedLimit));
    return repository
        .findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(STAGE, PENDING)
        .stream()
        .limit(limit)
        .map(this::claim)
        .toList();
  }

  /** Reabre uma única vez timeouts terminais gravados por versões antigas do worker. */
  private void recoverLegacyTimeoutFailures() {
    for (GeraLandingStageExecution execution :
        repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(STAGE, "FALHA")) {
      if (execution.getErrorDetail() == null
          && execution.getErrorMessage() != null
          && execution
              .getErrorMessage()
              .contains("Timeout do Codex do Agente Gerador de Landing")) {
        execution.setStatus(PENDING);
        execution.setProcessingStartedAt(null);
        execution.setCompletedAt(null);
        execution.setErrorMessage(null);
        execution.setErrorDetail("LEGACY_TIMEOUT_RECOVERED_ONCE");
        repository.save(execution);
      }
    }
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
    agentTaskService.finishOperationalDelegation(
        "landing-generator", execution.getAutonomousCycleId(), request.error() == null);
    if (request.error() == null) {
      coordinator.continueAfterQualityReview(
          execution.getExperimentId(), execution.getAutonomousCycleId(), request.decisionJson());
    }
  }

  /** Recupera o snapshot congelado usado pelo MCP exclusivo. */
  @Transactional(readOnly = true)
  public LandingAgentPendingResponse context(String executionId) {
    return response(find(executionId));
  }

  /** Marca a execução como reservada e devolve o contexto imutável. */
  private LandingAgentPendingResponse claim(GeraLandingStageExecution execution) {
    execution.setStatus(PROCESSING);
    execution.setProcessingStartedAt(Instant.now());
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
                  "available", false,
                  "executorContract", "NOT_REGISTERED")));
      experimentRepository
          .findById(execution.getExperimentId())
          .ifPresent(
              experiment -> {
                putWhenPresent(context, "experimentName", experiment.getName());
                putWhenPresent(context, "pain", experiment.getSinglePain());
                putWhenPresent(context, "promise", experiment.getFunnelPromise());
                putWhenPresent(context, "primaryCta", experiment.getPrimaryCta());
                putWhenPresent(context, "landingHtml", experiment.getHtmlGeraLanding());
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
