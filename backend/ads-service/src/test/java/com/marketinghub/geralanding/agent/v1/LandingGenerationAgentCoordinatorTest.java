package com.marketinghub.geralanding.agent.v1;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.registerFeedback.RegisterMemoryFeedbackRequest;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStageService;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStageExecutionService;
import com.marketinghub.geralanding.imageplanning.service.BackendImagePlanningService;
import com.marketinghub.geralanding.presetdesign.service.BackendPresetDesignService;
import com.marketinghub.geralanding.wireframe.service.BackendWireframeService;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Valida os gates e a delegação causal do Agente Gerador de Landing. */
class LandingGenerationAgentCoordinatorTest {
  private static final String CYCLE_ID = "cycle-88-current";
  private GeraLandingStageExecutionRepository executionRepository;
  private CreativeRepository creativeRepository;
  private BackendWireframeService wireframeService;
  private GeraLandingCopyStageService copyService;
  private BackendImagePlanningService imagePlanningService;
  private BackendPresetDesignService presetDesignService;
  private GeraLandingDeliverablesStageExecutionService deliverablesService;
  private AgentMemoryService memoryService;
  private GovernedLandingHtmlService governedLandingHtmlService;
  private LandingGenerationAgentCoordinator coordinator;

  /** Prepara portas isoladas para cada cenário da matriz de convergência. */
  @BeforeEach
  void setUp() {
    executionRepository = mock(GeraLandingStageExecutionRepository.class);
    creativeRepository = mock(CreativeRepository.class);
    wireframeService = mock(BackendWireframeService.class);
    copyService = mock(GeraLandingCopyStageService.class);
    imagePlanningService = mock(BackendImagePlanningService.class);
    presetDesignService = mock(BackendPresetDesignService.class);
    deliverablesService = mock(GeraLandingDeliverablesStageExecutionService.class);
    memoryService = mock(AgentMemoryService.class);
    governedLandingHtmlService = mock(GovernedLandingHtmlService.class);
    coordinator =
        new LandingGenerationAgentCoordinator(
            new ObjectMapper(),
            executionRepository,
            creativeRepository,
            wireframeService,
            copyService,
            imagePlanningService,
            presetDesignService,
            deliverablesService,
            memoryService,
            governedLandingHtmlService);
    when(executionRepository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                88L, "landing-page-quality-review", CYCLE_ID))
        .thenReturn(List.of());
    when(memoryService.retrieve("landing-generator", null, "EXPERIMENT", "88", 8))
        .thenReturn(List.of());
  }

  /** Deve recuperar a memória persistida e anexá-la ao briefing da reconstrução. */
  @Test
  void shouldUsePersistentMemoryInCorrectionBrief() {
    when(memoryService.retrieve("landing-generator", null, "EXPERIMENT", "88", 8))
        .thenReturn(
            List.of(
                new MemoryResponse(
                    1L,
                    "CONFIRMED",
                    "landing-conversion-quality",
                    "CTA deve cumprir a promessa do anúncio",
                    "Reprovação independente",
                    "s3://bucket/evidence.json",
                    "review-87",
                    BigDecimal.ONE,
                    null,
                    2,
                    Instant.parse("2026-08-10T00:00:00Z"))));

    coordinator.continueAfterQualityReview(
        88L, CYCLE_ID, review("REGENERATE_BEFORE_PUBLICATION", 72, "LANDING_PAGE_WIREFRAME"));

    verify(wireframeService)
        .registerConvergenceExecution(
            org.mockito.ArgumentMatchers.eq(88L), contains("CTA deve cumprir a promessa"));
    verify(memoryService).retrieve("landing-generator", null, "EXPERIMENT", "88", 8);
  }

  /** Deve reiniciar pela causa estrutural mais antiga e deixar o backend avançar o restante. */
  @Test
  void shouldDispatchEarliestRootCause() {
    coordinator.continueAfterQualityReview(
        88L,
        CYCLE_ID,
        review(
            "REGENERATE_BEFORE_PUBLICATION",
            72,
            "LANDING_PAGE_WIREFRAME",
            "LANDING_PAGE_DESIGN_PRESET"));

    verify(wireframeService)
        .registerConvergenceExecution(org.mockito.ArgumentMatchers.eq(88L), contains("72"));
    verify(presetDesignService, never()).start(88L);
  }

  /** Deve usar o planejamento oficial para obter novas imagens pelo Gerador do Marketing Hub. */
  @Test
  void shouldDispatchImagePlanningForVisualRootCause() {
    coordinator.continueAfterQualityReview(
        88L,
        CYCLE_ID,
        review("REGENERATE_BEFORE_PUBLICATION", 78, "LANDING_PAGE_IMAGE_GENERATION"));

    verify(imagePlanningService).start(88L);
  }

  /** Deve encaminhar bloqueios de checkout e entrega para a etapa contratada de entregáveis. */
  @Test
  void shouldDispatchDeliverablesForCheckoutRootCause() {
    coordinator.continueAfterQualityReview(
        88L, CYCLE_ID, review("REGENERATE_BEFORE_PUBLICATION", 80, "LANDING_PAGE_DELIVERABLES"));

    verify(deliverablesService).registerInitialExecution(88L, "landing-page-deliverables");
  }

  /** Deve encaminhar a criação integral de HTML ao executor governado registrado no backend. */
  @Test
  void shouldDispatchFullHtmlToRegisteredGovernedExecutor() {
    coordinator.continueAfterQualityReview(
        88L,
        CYCLE_ID,
        reviewWithGeneratedHtml(
            "REGENERATE_BEFORE_PUBLICATION",
            78,
            "CODEX_CODE_IMPLEMENTATION",
            "<html><body><main>Oferta</main></body></html>",
            "LANDING_PAGE_HTML"));

    verify(governedLandingHtmlService).apply(88L, "<html><body><main>Oferta</main></body></html>");
    verify(presetDesignService, never()).start(88L);
  }

  /** Deve devolver as versões finais ao Aprovador sem publicar a landing ou a campanha. */
  @Test
  void shouldRequeueLatestCreativesAfterLandingApproval() {
    Creative creative = new Creative();
    creative.setAgentReviewStatus(CreativeAgentReviewStatus.ADJUST);
    when(creativeRepository.findLatestLineageCreativesByExperimentId(88L))
        .thenReturn(List.of(creative));

    coordinator.continueAfterQualityReview(88L, CYCLE_ID, review("APPROVE_FOR_PUBLICATION", 93));

    verify(creativeRepository).save(creative);
    org.junit.jupiter.api.Assertions.assertEquals(
        CreativeAgentReviewStatus.PENDING, creative.getAgentReviewStatus());
  }

  /** Deve fechar o gate quando uma reprovação não indicar correção verificável. */
  @Test
  void shouldRejectRegenerationWithoutStage() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            coordinator.continueAfterQualityReview(
                88L, CYCLE_ID, review("REGENERATE_BEFORE_PUBLICATION", 60)));
  }

  /** Deve ignorar revisões históricas de ciclos anteriores ao aplicar o limite do ciclo atual. */
  @Test
  void shouldCountOnlyReviewsFromCurrentAutonomousCycle() {
    when(executionRepository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                88L, "landing-page-quality-review", CYCLE_ID))
        .thenReturn(List.of());

    coordinator.continueAfterQualityReview(
        88L, CYCLE_ID, review("REGENERATE_BEFORE_PUBLICATION", 84, "LANDING_PAGE_COPY"));

    verify(copyService).start(88L);
    verify(executionRepository)
        .sumCompletedCostUsdByExperimentIdAndAutonomousCycleId(88L, CYCLE_ID);
  }

  /** Deve confirmar uma hipótese somente quando a revisão independente posterior evoluir. */
  @Test
  void shouldRewardLearningFromIndependentQualityReview() {
    when(memoryService.retrieve("landing-generator", null, "EXPERIMENT", "88", 12))
        .thenReturn(
            List.of(
                new MemoryResponse(
                    9L,
                    "CANDIDATE",
                    "landing-conversion-quality",
                    "Reduzir fricção no CTA",
                    "Quality Review independente reprovou a versão com score 70",
                    null,
                    "review-88",
                    BigDecimal.valueOf(0.7),
                    null,
                    0,
                    Instant.parse("2026-08-10T00:00:00Z"))));

    coordinator.learnFromIndependentQualityReview(
        88L, review("REGENERATE_BEFORE_PUBLICATION", 77, "LANDING_PAGE_COPY"));

    verify(memoryService)
        .feedback(
            org.mockito.ArgumentMatchers.eq("landing-generator"),
            org.mockito.ArgumentMatchers.eq(9L),
            org.mockito.ArgumentMatchers.argThat(
                (RegisterMemoryFeedbackRequest request) ->
                    "CONFIRMED".equals(request.outcome())
                        && request.evidence().contains("delta 7")));
  }

  /** Monta uma resposta mínima compatível com o contrato do Quality Review. */
  private String review(String recommendation, int score, String... stages) {
    return reviewWithApproach(recommendation, score, "GERALANDING_PIPELINE", stages);
  }

  /** Monta uma resposta mínima com abordagem explícita para testar o gate do executor. */
  private String reviewWithApproach(
      String recommendation, int score, String approach, String... stages) {
    String regeneration =
        java.util.Arrays.stream(stages)
            .map(stage -> "\"" + stage + "\"")
            .collect(java.util.stream.Collectors.joining(","));
    return "{\"score\":"
        + score
        + ",\"approvalRecommendation\":\""
        + recommendation
        + "\",\"selectedGenerationApproach\":{\"approachCode\":\""
        + approach
        + "\"},\"recommendedRegeneration\":["
        + regeneration
        + "]}";
  }

  /** Monta uma resposta com HTML integral para validar o executor governado. */
  private String reviewWithGeneratedHtml(
      String recommendation, int score, String approach, String html, String... stages) {
    com.fasterxml.jackson.databind.node.ObjectNode review = new ObjectMapper().createObjectNode();
    review.put("score", score);
    review.put("approvalRecommendation", recommendation);
    review.putObject("selectedGenerationApproach").put("approachCode", approach);
    review.put("generatedHtml", html);
    java.util.Arrays.stream(stages).forEach(review.putArray("recommendedRegeneration")::add);
    return review.toString();
  }
}
