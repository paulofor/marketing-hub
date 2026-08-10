package com.marketinghub.geralanding.agent.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.registerMemory.RegisterMemoryRequest;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStageService;
import com.marketinghub.geralanding.imageplanning.service.BackendImagePlanningService;
import com.marketinghub.geralanding.presetdesign.service.BackendPresetDesignService;
import com.marketinghub.geralanding.qualityreview.service.LandingQualityReviewedEvent;
import com.marketinghub.geralanding.wireframe.service.BackendWireframeService;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Responsabilidade: coordenar a convergência autônoma da landing sem aprovar ou publicar. */
@Service
public class LandingGenerationAgentCoordinator {
  private static final Logger log =
      LoggerFactory.getLogger(LandingGenerationAgentCoordinator.class);
  private static final String QUALITY_REVIEW_STAGE = "landing-page-quality-review";
  private static final int MAX_QUALITY_REVIEWS = 4;
  private static final String AGENT_KEY = "landing-generator";
  private static final String MEMORY_SCOPE = "EXPERIMENT";

  private final ObjectMapper objectMapper;
  private final GeraLandingStageExecutionRepository executionRepository;
  private final CreativeRepository creativeRepository;
  private final BackendWireframeService wireframeService;
  private final GeraLandingCopyStageService copyService;
  private final BackendImagePlanningService imagePlanningService;
  private final BackendPresetDesignService presetDesignService;
  private final AgentMemoryService memoryService;

  /** Inicializa o coordenador com as portas oficiais do backend para cada executor. */
  public LandingGenerationAgentCoordinator(
      ObjectMapper objectMapper,
      GeraLandingStageExecutionRepository executionRepository,
      CreativeRepository creativeRepository,
      BackendWireframeService wireframeService,
      GeraLandingCopyStageService copyService,
      BackendImagePlanningService imagePlanningService,
      BackendPresetDesignService presetDesignService,
      AgentMemoryService memoryService) {
    this.objectMapper = objectMapper;
    this.executionRepository = executionRepository;
    this.creativeRepository = creativeRepository;
    this.wireframeService = wireframeService;
    this.copyService = copyService;
    this.imagePlanningService = imagePlanningService;
    this.presetDesignService = presetDesignService;
    this.memoryService = memoryService;
  }

  /** Interpreta o Quality Review e agenda a correção causal ou uma nova revisão dos anúncios. */
  @Transactional
  public void continueAfterQualityReview(Long experimentId, String reviewJson) {
    try {
      JsonNode review = objectMapper.readTree(reviewJson);
      String recommendation = review.path("approvalRecommendation").asText();
      if ("APPROVE_FOR_PUBLICATION".equals(recommendation)) {
        enqueueLatestCreativesForIndependentReview(experimentId);
        return;
      }
      if (!"REGENERATE_BEFORE_PUBLICATION".equals(recommendation)) {
        throw new IllegalArgumentException("Quality Review sem recomendação canônica");
      }
      enforceIterationAndProgressGates(experimentId, review);
      Set<String> stages = readRecommendedStages(review);
      registerLearningCandidate(experimentId, review, stages);
      String memoryAwareBrief = enrichWithMemory(experimentId, review);
      dispatchEarliestRootCause(experimentId, stages, memoryAwareBrief);
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao coordenar Agente Gerador de Landing (experimentId={}, reviewLength={})",
          experimentId,
          reviewJson != null ? reviewJson.length() : 0,
          ex);
      throw ex;
    } catch (Exception ex) {
      log.error(
          "Falha ao interpretar Quality Review do Agente Gerador de Landing (experimentId={})",
          experimentId,
          ex);
      throw new IllegalArgumentException("Quality Review inválido", ex);
    }
  }

  /** Registra a causa observada como hipótese, sem permitir que o agente a confirme sozinho. */
  private void registerLearningCandidate(Long experimentId, JsonNode review, Set<String> stages) {
    JsonNode issues = review.path("blockingIssues");
    String content =
        "Evitar reincidência nas etapas "
            + String.join(",", stages)
            + ": "
            + (issues.isMissingNode() || issues.isEmpty() ? review.toString() : issues.toString());
    if (content.length() > 4000) {
      content = content.substring(0, 4000);
    }
    memoryService.register(
        AGENT_KEY,
        new RegisterMemoryRequest(
            null,
            MEMORY_SCOPE,
            experimentId.toString(),
            "landing-conversion-quality",
            content,
            "Quality Review independente reprovou a versão com score "
                + review.path("score").asInt(-1),
            "gera-landing-quality-review/experiment/" + experimentId,
            latestReviewExecutionId(experimentId),
            BigDecimal.valueOf(0.70),
            null));
  }

  /** Recupera memória curta do experimento e anexa somente contexto tratado como evidência. */
  private String enrichWithMemory(Long experimentId, JsonNode review) throws Exception {
    List<MemoryResponse> memories =
        memoryService.retrieve(AGENT_KEY, null, MEMORY_SCOPE, experimentId.toString(), 8);
    ObjectNode enriched = review.deepCopy();
    ArrayNode context = enriched.putArray("agentMemory");
    for (MemoryResponse memory : memories) {
      ObjectNode item = context.addObject();
      item.put("status", memory.status());
      item.put("learning", memory.content());
      item.put("evidence", memory.evidence());
      if (memory.sourceReference() != null) {
        item.put("artifactReference", memory.sourceReference());
      }
    }
    return objectMapper.writeValueAsString(enriched);
  }

  /** Resolve uma correlação estável com a execução que originou a aprendizagem. */
  private String latestReviewExecutionId(Long experimentId) {
    return executionRepository
        .findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
            experimentId, QUALITY_REVIEW_STAGE)
        .stream()
        .findFirst()
        .map(
            value ->
                value.getIdJob() != null ? value.getIdJob().toString() : "review-" + experimentId)
        .orElse("review-" + experimentId);
  }

  /** Recebe a conclusão persistida da etapa sem acoplar o Quality Review ao coordenador. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onQualityReviewCompleted(LandingQualityReviewedEvent event) {
    try {
      continueAfterQualityReview(event.experimentId(), event.reviewJson());
    } catch (RuntimeException ex) {
      log.error(
          "Agente Gerador de Landing bloqueado após Quality Review persistido (experimentId={})",
          event.experimentId(),
          ex);
    }
  }

  /** Bloqueia ciclos excessivos ou sem melhora objetiva antes de consumir nova geração. */
  private void enforceIterationAndProgressGates(Long experimentId, JsonNode currentReview)
      throws Exception {
    List<GeraLandingStageExecution> reviews =
        executionRepository.findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
            experimentId, QUALITY_REVIEW_STAGE);
    if (reviews.size() >= MAX_QUALITY_REVIEWS) {
      throw new IllegalStateException("Limite seguro de revisões autônomas da landing atingido");
    }
    int currentScore = currentReview.path("score").asInt(-1);
    if (reviews.size() < 2 || currentScore < 0) {
      return;
    }
    String previousResponse = reviews.get(1).getModelResponse();
    if (previousResponse == null || previousResponse.isBlank()) {
      return;
    }
    JsonNode previousReview = objectMapper.readTree(previousResponse);
    int previousScore = previousReview.path("score").asInt(-1);
    if (previousScore >= 0
        && currentScore <= previousScore
        && readRecommendedStages(previousReview).equals(readRecommendedStages(currentReview))) {
      throw new IllegalStateException("Landing repetiu as mesmas causas sem evolução de score");
    }
  }

  /** Converte a lista de regeneração em um conjunto validado e não vazio. */
  private Set<String> readRecommendedStages(JsonNode review) {
    Set<String> stages = new HashSet<>();
    review.path("recommendedRegeneration").forEach(value -> stages.add(value.asText()));
    if (stages.isEmpty()) {
      throw new IllegalArgumentException("Reprovação da landing sem etapa de correção");
    }
    return stages;
  }

  /** Agenda somente a etapa causal mais antiga; o backend conduz as etapas seguintes. */
  private void dispatchEarliestRootCause(Long experimentId, Set<String> stages, String reviewJson) {
    if (stages.contains("LANDING_PAGE_WIREFRAME")) {
      wireframeService.registerConvergenceExecution(experimentId, reviewJson);
    } else if (stages.contains("LANDING_PAGE_COPY")) {
      copyService.start(experimentId);
    } else if (stages.contains("LANDING_PAGE_IMAGE_PLANNING")
        || stages.contains("LANDING_PAGE_IMAGE_GENERATION")) {
      imagePlanningService.start(experimentId);
    } else if (stages.contains("LANDING_PAGE_DESIGN_PRESET")
        || stages.contains("LANDING_PAGE_HTML")) {
      presetDesignService.start(experimentId);
    } else {
      throw new IllegalArgumentException(
          "Etapa não automatizável pelo Agente de Landing: " + stages);
    }
  }

  /** Reabre somente as versões mais recentes para o Aprovador independente validar a jornada. */
  private void enqueueLatestCreativesForIndependentReview(Long experimentId) {
    for (Creative creative :
        creativeRepository.findLatestLineageCreativesByExperimentId(experimentId)) {
      creative.setAgentReviewStatus(CreativeAgentReviewStatus.PENDING);
      creative.setAgentReviewJson(null);
      creative.setAgentReviewRequestJson(null);
      creative.setAgentReviewResponseJson(null);
      creative.setAgentReviewModel(null);
      creative.setAgentReviewedAt(null);
      creative.setAgentReviewStartedAt(null);
      creative.setAgentReviewRecoveryCount(0);
      creative.setAgentReviewLastRecoveredAt(null);
      creativeRepository.save(creative);
    }
  }
}
