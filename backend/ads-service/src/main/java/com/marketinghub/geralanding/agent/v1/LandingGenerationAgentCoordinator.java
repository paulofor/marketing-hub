package com.marketinghub.geralanding.agent.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.registerFeedback.RegisterMemoryFeedbackRequest;
import com.marketinghub.agentmemory.service.registerMemory.RegisterMemoryRequest;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStageService;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStageExecutionService;
import com.marketinghub.geralanding.imageplanning.service.BackendImagePlanningService;
import com.marketinghub.geralanding.presetdesign.service.BackendPresetDesignService;
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

/** Responsabilidade: coordenar a convergência autônoma da landing sem aprovar ou publicar. */
@Service
public class LandingGenerationAgentCoordinator {
  private static final Logger log =
      LoggerFactory.getLogger(LandingGenerationAgentCoordinator.class);
  private static final String QUALITY_REVIEW_STAGE = "landing-page-quality-review";
  private static final int MAX_QUALITY_REVIEWS = 4;
  private static final BigDecimal MAX_AUTONOMOUS_COST_USD = new BigDecimal("5.00");
  private static final String AGENT_KEY = "landing-generator";
  private static final String MEMORY_SCOPE = "EXPERIMENT";

  private final ObjectMapper objectMapper;
  private final GeraLandingStageExecutionRepository executionRepository;
  private final CreativeRepository creativeRepository;
  private final BackendWireframeService wireframeService;
  private final GeraLandingCopyStageService copyService;
  private final BackendImagePlanningService imagePlanningService;
  private final BackendPresetDesignService presetDesignService;
  private final GeraLandingDeliverablesStageExecutionService deliverablesService;
  private final AgentMemoryService memoryService;
  private final GovernedLandingHtmlService governedLandingHtmlService;

  /** Inicializa o coordenador com as portas oficiais do backend para cada executor. */
  public LandingGenerationAgentCoordinator(
      ObjectMapper objectMapper,
      GeraLandingStageExecutionRepository executionRepository,
      CreativeRepository creativeRepository,
      BackendWireframeService wireframeService,
      GeraLandingCopyStageService copyService,
      BackendImagePlanningService imagePlanningService,
      BackendPresetDesignService presetDesignService,
      GeraLandingDeliverablesStageExecutionService deliverablesService,
      AgentMemoryService memoryService,
      GovernedLandingHtmlService governedLandingHtmlService) {
    this.objectMapper = objectMapper;
    this.executionRepository = executionRepository;
    this.creativeRepository = creativeRepository;
    this.wireframeService = wireframeService;
    this.copyService = copyService;
    this.imagePlanningService = imagePlanningService;
    this.presetDesignService = presetDesignService;
    this.deliverablesService = deliverablesService;
    this.memoryService = memoryService;
    this.governedLandingHtmlService = governedLandingHtmlService;
  }

  /** Interpreta o Quality Review e agenda a correção causal ou uma nova revisão dos anúncios. */
  @Transactional
  public void continueAfterQualityReview(
      Long experimentId, String autonomousCycleId, String reviewJson) {
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
      enforceIterationAndProgressGates(experimentId, autonomousCycleId, review);
      enforceRegisteredGenerationApproach(review);
      if ("CODEX_CODE_IMPLEMENTATION"
          .equals(review.path("selectedGenerationApproach").path("approachCode").asText())) {
        governedLandingHtmlService.apply(experimentId, review.path("generatedHtml").asText());
        return;
      }
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

  /** Permite avanço somente por uma abordagem com executor contratado no backend. */
  private void enforceRegisteredGenerationApproach(JsonNode decision) {
    JsonNode selected = decision.path("selectedGenerationApproach");
    if (selected.isMissingNode() || selected.isEmpty()) {
      throw new IllegalArgumentException("Decisão sem abordagem de geração selecionada");
    }
    if (!Set.of("GERALANDING_PIPELINE", "CODEX_CODE_IMPLEMENTATION")
        .contains(selected.path("approachCode").asText())) {
      throw new IllegalArgumentException("Abordagem de geração sem executor registrado no backend");
    }
  }

  /** Aplica recompensa somente a partir do Quality Review independente posterior à intervenção. */
  @Transactional
  public void learnFromIndependentQualityReview(Long experimentId, String reviewJson) {
    try {
      JsonNode review = objectMapper.readTree(reviewJson);
      int currentScore = review.path("score").asInt(-1);
      boolean approved =
          "APPROVE_FOR_PUBLICATION".equals(review.path("approvalRecommendation").asText());
      if (currentScore < 0) {
        return;
      }
      for (MemoryResponse memory :
          memoryService.retrieve(AGENT_KEY, null, MEMORY_SCOPE, experimentId.toString(), 12)) {
        if (!"CANDIDATE".equals(memory.status())) {
          continue;
        }
        int baselineScore = scoreFromEvidence(memory.evidence());
        if (baselineScore < 0) {
          continue;
        }
        int delta = currentScore - baselineScore;
        String outcome =
            approved || delta >= 5 ? "CONFIRMED" : delta <= 0 ? "CONTRADICTED" : "INCONCLUSIVE";
        memoryService.feedback(
            AGENT_KEY,
            memory.id(),
            new RegisterMemoryFeedbackRequest(
                outcome,
                "Quality Review independente posterior: score "
                    + currentScore
                    + ", baseline "
                    + baselineScore
                    + ", delta "
                    + delta
                    + ", aprovação "
                    + approved,
                "gera-landing-quality-review/experiment/" + experimentId));
      }
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao aplicar aprendizado por reforço da landing (experimentId={})",
          experimentId,
          ex);
      throw ex;
    } catch (Exception ex) {
      log.error(
          "Falha ao interpretar recompensa independente da landing (experimentId={})",
          experimentId,
          ex);
      throw new IllegalArgumentException("Quality Review inválido para aprendizado", ex);
    }
  }

  /** Extrai o score de referência persistido na evidência da memória candidata. */
  private int scoreFromEvidence(String evidence) {
    if (evidence == null) {
      return -1;
    }
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("score\\s+(\\d{1,3})").matcher(evidence);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
  }

  /** Registra a causa observada como hipótese, sem permitir que o agente a confirme sozinho. */
  private void registerLearningCandidate(Long experimentId, JsonNode review, Set<String> stages) {
    JsonNode issues = review.path("blockingIssues");
    String content =
        "Abordagem "
            + review.path("selectedGenerationApproach").path("approachCode").asText("UNKNOWN")
            + "; evitar reincidência nas etapas "
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

  /** Bloqueia ciclos excessivos ou sem melhora objetiva antes de consumir nova geração. */
  private void enforceIterationAndProgressGates(
      Long experimentId, String autonomousCycleId, JsonNode currentReview) throws Exception {
    if (autonomousCycleId == null || autonomousCycleId.isBlank()) {
      throw new IllegalStateException("Ciclo autônomo da landing não informado");
    }
    BigDecimal accumulatedCost =
        executionRepository.sumCompletedCostUsdByExperimentIdAndAutonomousCycleId(
            experimentId, autonomousCycleId);
    if (accumulatedCost != null && accumulatedCost.compareTo(MAX_AUTONOMOUS_COST_USD) >= 0) {
      throw new IllegalStateException("Limite de custo autônomo da landing atingido");
    }
    List<GeraLandingStageExecution> reviews =
        executionRepository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                experimentId, QUALITY_REVIEW_STAGE, autonomousCycleId);
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
    } else if (stages.contains("LANDING_PAGE_DELIVERABLES")) {
      deliverablesService.registerInitialExecution(experimentId, "landing-page-deliverables");
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
