package com.marketinghub.geralanding.agent.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskPendingResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.AutomaticBusinessProcessActivityService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.qualityreview.service.LandingQualityReviewedEvent;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.planning.service.CommercialPlanApprovedCreativeEvidenceService;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Valida fila, segregação e callback do executor premium de landing. */
class LandingGenerationAgentExecutionServiceTest {
  private GeraLandingStageExecutionRepository repository;
  private LandingGenerationAgentCoordinator coordinator;
  private ExperimentRepository experimentRepository;
  private AgentTaskService agentTaskService;
  private AutomaticBusinessProcessActivityService automaticActivityService;
  private LandingGenerationResultApplicationService resultApplicationService;
  private GeraSalesPagePublicationAuditRepository publicationRepository;
  private CommercialPlanLandingAssetService landingAssetService;
  private CommercialPlanApprovedCreativeEvidenceService approvedCreativeEvidenceService;
  private LandingCommercialContextResolver commercialContextResolver;
  private LandingGenerationAgentExecutionService service;

  /** Prepara dependências isoladas antes de cada cenário. */
  @BeforeEach
  void setUp() {
    repository = mock(GeraLandingStageExecutionRepository.class);
    coordinator = mock(LandingGenerationAgentCoordinator.class);
    experimentRepository = mock(ExperimentRepository.class);
    agentTaskService = mock(AgentTaskService.class);
    automaticActivityService = mock(AutomaticBusinessProcessActivityService.class);
    resultApplicationService = mock(LandingGenerationResultApplicationService.class);
    publicationRepository = mock(GeraSalesPagePublicationAuditRepository.class);
    landingAssetService = mock(CommercialPlanLandingAssetService.class);
    approvedCreativeEvidenceService = mock(CommercialPlanApprovedCreativeEvidenceService.class);
    commercialContextResolver = mock(LandingCommercialContextResolver.class);
    when(commercialContextResolver.resolve(any())).thenReturn(java.util.Map.of());
    when(approvedCreativeEvidenceService.resolve(any()))
        .thenReturn(java.util.Map.of("status", "UNAVAILABLE"));
    ObjectMapper objectMapper = new ObjectMapper();
    service =
        new LandingGenerationAgentExecutionService(
            repository,
            coordinator,
            experimentRepository,
            new LandingCheckoutEvidenceResolver(
                new LandingCheckoutContractResolver(publicationRepository), objectMapper),
            commercialContextResolver,
            objectMapper,
            agentTaskService,
            automaticActivityService,
            resultApplicationService,
            landingAssetService,
            approvedCreativeEvidenceService);
    when(experimentRepository.findById(88L)).thenReturn(Optional.empty());
    when(repository
            .findTop20ByStageCodeAndStatusAndExecutionRequestedAtBeforeOrderByExecutionRequestedAtAsc(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                any()))
        .thenReturn(List.of());
    when(repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "FALHA"))
        .thenReturn(List.of());
  }

  /** Deve enfileirar reprovação sem executar Codex dentro do backend. */
  @Test
  void shouldEnqueueRejectedQualityReview() {
    service.onQualityReviewCompleted(
        new LandingQualityReviewedEvent(
            88L,
            "cycle-88",
            "{\"approvalRecommendation\":\"REGENERATE_BEFORE_PUBLICATION\",\"score\":70}"));

    verify(repository).save(any(GeraLandingStageExecution.class));
  }

  /** Deve manter Dédalo responsável até o Quality Review independente aprovar a candidata BPM. */
  @Test
  void shouldCompleteBpmTaskOnlyAfterIndependentQualityApproval() {
    GeraLandingStageExecution qualityReview = approvedQualityReview("quality-review-30", 91);
    when(repository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                88L, "landing-page-quality-review", "agent-task:30"))
        .thenReturn(List.of(qualityReview));

    service.onQualityReviewCompleted(
        new LandingQualityReviewedEvent(
            88L,
            "agent-task:30",
            "{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\",\"score\":91}"));

    verify(agentTaskService)
        .completeClaimedProcessTask(
            org.mockito.ArgumentMatchers.eq("landing-generator"),
            org.mockito.ArgumentMatchers.eq(30L),
            any(com.marketinghub.agenttask.CompleteAgentTaskRequest.class));
    verify(automaticActivityService)
        .completeFromExecution(
            org.mockito.ArgumentMatchers.eq(30L),
            org.mockito.ArgumentMatchers.eq("technical"),
            org.mockito.ArgumentMatchers.eq("quality-review-30"),
            org.mockito.ArgumentMatchers.eq(qualityReview.getExecutionRequestedAt()),
            org.mockito.ArgumentMatchers.eq(qualityReview.getCompletedAt()),
            org.mockito.ArgumentMatchers.eq(qualityReview.getCostUsd()),
            org.mockito.ArgumentMatchers.contains("\"score\":91"));
    verify(coordinator, never()).continueAfterQualityReview(any(), any(), any());
  }

  /** Deve concluir Íris pelo callback diferido somente após a aprovação técnica correlacionada. */
  @Test
  void shouldCompleteDeferredIrisTaskAfterIndependentQualityApproval() {
    GeraLandingStageExecution qualityReview = approvedQualityReview("quality-review-30", 91);
    when(agentTaskService.assignedAgentKey(30L)).thenReturn("communication-director");
    when(repository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                88L, "landing-page-quality-review", "agent-task:30"))
        .thenReturn(List.of(qualityReview));

    service.onQualityReviewCompleted(
        new LandingQualityReviewedEvent(
            88L,
            "agent-task:30",
            "{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\",\"score\":91}"));

    verify(agentTaskService)
        .completeDeferredProcessTask(
            org.mockito.ArgumentMatchers.eq("communication-director"),
            org.mockito.ArgumentMatchers.eq(30L),
            org.mockito.ArgumentMatchers.contains("APPROVE_FOR_PUBLICATION"));
    verify(agentTaskService, never())
        .completeClaimedProcessTask(
            any(), any(), any(com.marketinghub.agenttask.CompleteAgentTaskRequest.class));
    verify(automaticActivityService)
        .completeFromExecution(
            org.mockito.ArgumentMatchers.eq(30L),
            org.mockito.ArgumentMatchers.eq("technical"),
            org.mockito.ArgumentMatchers.eq("quality-review-30"),
            org.mockito.ArgumentMatchers.eq(qualityReview.getExecutionRequestedAt()),
            org.mockito.ArgumentMatchers.eq(qualityReview.getCompletedAt()),
            org.mockito.ArgumentMatchers.eq(qualityReview.getCostUsd()),
            org.mockito.ArgumentMatchers.contains("\"score\":91"));
  }

  /** Deve devolver a reprovação técnica para Íris sem criar uma correção sob Dédalo. */
  @Test
  void shouldBlockIrisTaskWithoutEnqueueingDedaloCorrection() {
    when(agentTaskService.assignedAgentKey(30L)).thenReturn("communication-director");

    service.onQualityReviewCompleted(
        new LandingQualityReviewedEvent(
            88L,
            "agent-task:30",
            "{\"approvalRecommendation\":\"REGENERATE_BEFORE_PUBLICATION\",\"score\":70}"));

    verify(agentTaskService)
        .failDeferredProcessTask(
            org.mockito.ArgumentMatchers.eq("communication-director"),
            org.mockito.ArgumentMatchers.eq(30L),
            org.mockito.ArgumentMatchers.contains("reprovou"),
            org.mockito.ArgumentMatchers.contains("REGENERATE_BEFORE_PUBLICATION"));
    verify(repository, never()).save(any(GeraLandingStageExecution.class));
  }

  /** Não deve concluir Dédalo quando a aprovação não possui execução visual correlacionada. */
  @Test
  void shouldBlockBpmCompletionWithoutCorrelatedQualityReviewExecution() {
    service.onQualityReviewCompleted(
        new LandingQualityReviewedEvent(
            88L,
            "agent-task:30",
            "{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\",\"score\":91}"));

    verify(automaticActivityService, never())
        .completeFromExecution(any(), any(), any(), any(), any(), any(), any());
    verify(agentTaskService, never())
        .completeClaimedProcessTask(
            any(), any(), any(com.marketinghub.agenttask.CompleteAgentTaskRequest.class));
  }

  /**
   * Deve propagar landing, screenshots, anúncio e checkout para Psique avaliar evidências reais.
   */
  @Test
  void shouldIncludeCommercialEvidenceWhenCompletingBpmTask() {
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    experiment.setHtmlGeraLanding("<!doctype html><html><body>Agenda Cheia</body></html>");
    experiment.setAdCopy("{\"headline\":\"Feed profissional\"}");
    experiment.setAdImageBriefing("{\"proof\":\"antes e depois\"}");
    experiment.setFollowUpActionUrl("https://delivery.example/agenda-cheia");
    experiment.setCommercialCheckoutUrl("https://checkout.example/agenda-cheia");
    experiment.setUnitPrice(java.math.BigDecimal.valueOf(67));
    Product product = new Product();
    product.setId(9L);
    product.setName("Agenda Cheia");
    product.setSlug("agenda-cheia");
    product.setPdeExperienceJson(
        "{\"commercialBinding\":{\"experimentId\":88,\"priceBrl\":67,\"billingModel\":\"ONE_TIME\"}}");
    experiment.setProduct(product);
    GeraLandingStageExecution qualityReview = execution("quality-review-88", "CONCLUIDO");
    qualityReview.setStageCode("landing-page-quality-review");
    qualityReview.setAutonomousCycleId("agent-task:30");
    qualityReview.setModelResponse(
        "{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\",\"score\":90}");
    qualityReview.setCompletedAt(qualityReview.getExecutionRequestedAt().plusSeconds(1));
    qualityReview.setQualityReviewAudit(
        "{\"screenshots\":[{\"viewport\":\"mobile\",\"publicUrl\":\"https://evidence/mobile.jpg\"}]}");
    GeraLandingStageExecution technicalExecution = execution("landing-agent-88", "CONCLUIDO");
    technicalExecution.setAutonomousCycleId("agent-task:30");
    technicalExecution.setOpenAiModel("gpt-5.6-sol");
    technicalExecution.setExecutionReasoningEffort("high");
    technicalExecution.setAgentPromptPart("Núcleo de Dédalo.");
    technicalExecution.setActivityPromptPart("Gere a landing aprovada.");
    technicalExecution.setPrompt("Núcleo de Dédalo.\n\nGere a landing aprovada.");
    when(experimentRepository.findById(88L)).thenReturn(Optional.of(experiment));
    when(repository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                88L, "landing-page-quality-review", "agent-task:30"))
        .thenReturn(List.of(qualityReview));
    when(repository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                88L, "landing-generation-agent-v1", "agent-task:30"))
        .thenReturn(List.of(technicalExecution));
    when(approvedCreativeEvidenceService.resolve(88L))
        .thenReturn(
            java.util.Map.of(
                "status",
                "APPROVED",
                "creativePackageId",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));

    service.onQualityReviewCompleted(
        new LandingQualityReviewedEvent(
            88L,
            "agent-task:30",
            "{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\",\"score\":90}"));

    org.mockito.ArgumentCaptor<com.marketinghub.agenttask.CompleteAgentTaskRequest> request =
        org.mockito.ArgumentCaptor.forClass(
            com.marketinghub.agenttask.CompleteAgentTaskRequest.class);
    verify(agentTaskService)
        .completeClaimedProcessTask(
            org.mockito.ArgumentMatchers.eq("landing-generator"),
            org.mockito.ArgumentMatchers.eq(30L),
            request.capture());
    assertTrue(request.getValue().evidenceJson().contains("Agenda Cheia"));
    assertTrue(request.getValue().evidenceJson().contains("mobile.jpg"));
    assertTrue(request.getValue().evidenceJson().contains("checkout.example"));
    assertTrue(
        request.getValue().evidenceJson().contains("VALIDATED_FROM_PERSISTED_CANONICAL_BINDING"));
    assertFalse(request.getValue().evidenceJson().contains("delivery.example"));
    assertTrue(request.getValue().evidenceJson().contains("Feed profissional"));
    assertTrue(request.getValue().evidenceJson().contains("approvedCreativeEvidence"));
    assertTrue(
        request
            .getValue()
            .evidenceJson()
            .contains("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    assertEquals("high", request.getValue().executionAudit().reasoningEffort());
    assertEquals("Núcleo de Dédalo.", request.getValue().executionAudit().agentPromptPart());
    assertEquals(
        "Gere a landing aprovada.", request.getValue().executionAudit().activityPromptPart());
    assertEquals(
        "Núcleo de Dédalo.\n\nGere a landing aprovada.",
        request.getValue().executionAudit().promptSent());
  }

  /** Não deve acumular outra correção quando o mesmo ciclo já possui trabalho ativo. */
  @Test
  void shouldNotDuplicateRejectedQualityReviewWhileCorrectionIsActive() {
    when(repository.existsByExperimentIdAndStageCodeAndAutonomousCycleIdAndStatusIn(
            88L, "landing-generation-agent-v1", "cycle-88", List.of("INICIADO", "PROCESSANDO")))
        .thenReturn(true);

    service.onQualityReviewCompleted(
        new LandingQualityReviewedEvent(
            88L,
            "cycle-88",
            "{\"approvalRecommendation\":\"REGENERATE_BEFORE_PUBLICATION\",\"score\":78}"));

    verify(repository, never()).save(any(GeraLandingStageExecution.class));
  }

  /** Deve transformar a causa de Têmis em contexto explícito para a autonomia de Dédalo. */
  @Test
  void shouldEnqueueCreativeLandingCorrectionWithProtectedAuthority() {
    service.enqueueCreativeConvergenceCorrection(
        88L,
        "creative-convergence:14:landing",
        "PRODUCT_PROOF_MISSING",
        "Mostrar o produto digital real.",
        "Desktop e mobile comprovam posts, stories e legendas antes do CTA.");

    org.mockito.ArgumentCaptor<GeraLandingStageExecution> execution =
        org.mockito.ArgumentCaptor.forClass(GeraLandingStageExecution.class);
    verify(repository).save(execution.capture());
    assertEquals("creative-convergence:14:landing", execution.getValue().getAutonomousCycleId());
    assertTrue(execution.getValue().getPromptContent().contains("PRODUCT_PROOF_MISSING"));
    assertTrue(execution.getValue().getPromptContent().contains("não pode publicar"));
  }

  /** Deve materializar a tarefa BPM de Dédalo na fila técnica sem perder a correlação. */
  @Test
  void shouldActivateClaimedBpmTask() {
    when(agentTaskService.claimedProcessTask("landing-generator", 30L))
        .thenReturn(
            new AgentTaskPendingResponse(
                30L,
                "landing-generator",
                "landing-page-generation",
                2,
                "html",
                "Construir HTML com autonomia",
                "Experimento 88 — construir landing",
                "Criar candidata responsiva sem publicar.",
                "commercial-plan:2@v4",
                Instant.parse("2026-08-15T05:10:37Z"),
                "{\"completedActivities\":[]}"));

    service.activateProcessTask(30L);

    org.mockito.ArgumentCaptor<GeraLandingStageExecution> execution =
        org.mockito.ArgumentCaptor.forClass(GeraLandingStageExecution.class);
    verify(repository).save(execution.capture());
    assertEquals(88L, execution.getValue().getExperimentId());
    assertEquals("agent-task:30", execution.getValue().getAutonomousCycleId());
    assertTrue(execution.getValue().getPromptContent().contains("Construir HTML com autonomia"));
  }

  /** Deve reservar e materializar a atividade BPM em uma única transação do backend. */
  @Test
  void shouldClaimAndActivateBpmTaskAtomically() {
    AgentTaskPendingResponse task =
        new AgentTaskPendingResponse(
            30L,
            "landing-generator",
            "landing-page-generation",
            2,
            "html",
            "Construir HTML com autonomia",
            "Experimento 88 — construir landing",
            "Criar candidata responsiva sem publicar.",
            "commercial-plan:2@v4",
            Instant.parse("2026-08-15T05:10:37Z"),
            "{\"completedActivities\":[]}");
    when(agentTaskService.claimEligibleProcessTask("landing-generator"))
        .thenReturn(Optional.of(task));

    service.activateNextProcessTask();

    org.mockito.ArgumentCaptor<GeraLandingStageExecution> execution =
        org.mockito.ArgumentCaptor.forClass(GeraLandingStageExecution.class);
    verify(repository).save(execution.capture());
    assertEquals("agent-task:30", execution.getValue().getAutonomousCycleId());
  }

  /** Não deve recriar o briefing original enquanto a entrega concluída aguarda o Quality Review. */
  @Test
  void shouldNotRematerializeClaimedBpmTaskAwaitingQualityReview() {
    AgentTaskPendingResponse task =
        new AgentTaskPendingResponse(
            30L,
            "landing-generator",
            "landing-page-generation",
            2,
            "html",
            "Construir HTML com autonomia",
            "Experimento 88 — construir landing",
            "Criar candidata responsiva sem publicar.",
            "commercial-plan:2@v4",
            Instant.parse("2026-08-15T05:10:37Z"),
            "{\"completedActivities\":[]}");
    GeraLandingStageExecution completed = execution("completed-before-review", "CONCLUIDO");
    completed.setAutonomousCycleId("agent-task:30");
    when(agentTaskService.claimEligibleProcessTask("landing-generator"))
        .thenReturn(Optional.of(task));
    when(repository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                88L, "landing-generation-agent-v1", "agent-task:30"))
        .thenReturn(List.of(completed));

    service.activateNextProcessTask();

    verify(repository, never()).save(any(GeraLandingStageExecution.class));
  }

  /** Deve persistir e bloquear uma falha de aplicação sem devolver erro técnico ao executor. */
  @Test
  void shouldBlockBpmTaskWhenApplyingGeneratedLandingFails() {
    GeraLandingStageExecution execution = execution("job-apply-failure", "PROCESSANDO");
    execution.setAutonomousCycleId("agent-task:30");
    when(repository.findTopByIdJobOrderByExecutionRequestedAtDesc(
            "job-apply-failure".getBytes(StandardCharsets.UTF_8)))
        .thenReturn(Optional.of(execution));
    doThrow(new IllegalArgumentException("Checkout divergente do briefing"))
        .when(resultApplicationService)
        .apply(88L, "agent-task:30", "{\"decision\":\"generated\"}");

    service.complete(
        "job-apply-failure",
        new LandingAgentResultRequest(
            "{\"decision\":\"generated\"}",
            "Núcleo de Dédalo.\n\nCorrija a landing.",
            "Núcleo de Dédalo.",
            "Corrija a landing.",
            "response",
            "gpt-5.6-sol",
            "high",
            null,
            null,
            null,
            null,
            null));

    assertEquals("FALHA", execution.getStatus());
    assertTrue(execution.getErrorMessage().contains("Checkout divergente"));
    org.mockito.ArgumentCaptor<com.marketinghub.agenttask.FailAgentTaskRequest> request =
        org.mockito.ArgumentCaptor.forClass(com.marketinghub.agenttask.FailAgentTaskRequest.class);
    verify(agentTaskService)
        .failClaimedProcessTask(
            org.mockito.ArgumentMatchers.eq("landing-generator"),
            org.mockito.ArgumentMatchers.eq(30L),
            request.capture());
    assertEquals("high", request.getValue().executionAudit().reasoningEffort());
  }

  /** Deve persistir o esforço configurado junto à execução técnica antes do Quality Review. */
  @Test
  void shouldPersistReasoningEffortWithTechnicalExecution() {
    GeraLandingStageExecution execution = execution("job-reasoning-effort", "PROCESSANDO");
    when(repository.findTopByIdJobOrderByExecutionRequestedAtDesc(
            "job-reasoning-effort".getBytes(StandardCharsets.UTF_8)))
        .thenReturn(Optional.of(execution));

    service.complete(
        "job-reasoning-effort",
        new LandingAgentResultRequest(
            "{\"decision\":\"generated\"}",
            "Núcleo de Dédalo.\n\nCorrija a landing.",
            "Núcleo de Dédalo.",
            "Corrija a landing.",
            "{\"decision\":\"generated\"}",
            "gpt-5.6-sol",
            "high",
            100,
            20,
            50,
            java.math.BigDecimal.valueOf(0.01),
            null));

    assertEquals("high", execution.getExecutionReasoningEffort());
    verify(repository).save(execution);
  }

  /** Deve abrir nova transação ao persistir a fila depois do commit do Quality Review. */
  @Test
  void shouldPersistQualityReviewEventInNewTransaction() throws NoSuchMethodException {
    Transactional transactional =
        LandingGenerationAgentExecutionService.class
            .getMethod("onQualityReviewCompleted", LandingQualityReviewedEvent.class)
            .getAnnotation(Transactional.class);

    assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
  }

  /** Deve isolar a aplicação da landing para que sua rejeição não reverta o callback auditável. */
  @Test
  void shouldApplyGeneratedLandingInIndependentTransaction() throws NoSuchMethodException {
    Transactional transactional =
        LandingGenerationResultApplicationService.class
            .getMethod("apply", Long.class, String.class, String.class)
            .getAnnotation(Transactional.class);

    assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
  }

  /** Deve reservar uma pendência e preservar o experimento no snapshot. */
  @Test
  void shouldClaimPendingWithSegregatedContext() {
    GeraLandingStageExecution execution = execution("job-88", "INICIADO");
    when(repository.findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "INICIADO"))
        .thenReturn(List.of(execution));

    List<LandingAgentPendingResponse> result = service.claimPending(1);

    assertEquals(1, result.size());
    assertEquals(88L, result.getFirst().experimentId());
    assertEquals("PROCESSANDO", execution.getStatus());
    @SuppressWarnings("unchecked")
    List<java.util.Map<String, Object>> catalog =
        (List<java.util.Map<String, Object>>)
            result.getFirst().context().get("generationApproachCatalog");
    assertEquals(3, catalog.size());
    assertTrue((Boolean) catalog.getFirst().get("available"));
    assertFalse((Boolean) catalog.get(1).get("available"));
  }

  /** Deve reservar a fila mesmo quando o experimento possui campos opcionais nulos. */
  @Test
  void shouldClaimPendingWhenExperimentSnapshotHasNullOptionalFields() {
    GeraLandingStageExecution execution = execution("job-null-fields-88", "INICIADO");
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    experiment.setName("MAQA-H002-E001");
    when(experimentRepository.findById(88L)).thenReturn(Optional.of(experiment));
    when(repository.findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "INICIADO"))
        .thenReturn(List.of(execution));

    LandingAgentPendingResponse result = service.claimPending(1).getFirst();

    assertEquals("MAQA-H002-E001", result.context().get("experimentName"));
    assertFalse(result.context().containsKey("landingHtml"));
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> checkoutContract =
        (java.util.Map<String, Object>) result.context().get("checkoutContract");
    assertEquals("BLOCKED", checkoutContract.get("validationStatus"));
    assertEquals("PROCESSANDO", execution.getStatus());
  }

  /** Deve entregar a Dédalo a URL exata e a regra operacional do checkout congelado. */
  @Test
  void shouldExposeExplicitCheckoutContractInSnapshot() {
    GeraLandingStageExecution execution = execution("job-checkout-88", "INICIADO");
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    GeraSalesPagePublicationAudit publication = new GeraSalesPagePublicationAudit();
    publication.setCheckoutUrl("https://checkout.example/agenda-cheia?ref=88");
    when(experimentRepository.findById(88L)).thenReturn(Optional.of(experiment));
    when(publicationRepository.findTopByExperimentIdOrderByPublishedAtDesc(88L))
        .thenReturn(Optional.of(publication));
    when(repository.findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "INICIADO"))
        .thenReturn(List.of(execution));

    LandingAgentPendingResponse result = service.claimPending(1).getFirst();

    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> contract =
        (java.util.Map<String, Object>) result.context().get("checkoutContract");
    assertEquals("https://checkout.example/agenda-cheia?ref=88", contract.get("canonicalUrl"));
    assertTrue(contract.get("rule").toString().contains("Todo CTA"));
  }

  /** Deve entregar o checkout comercial de Rigel mesmo antes de existir publicação anterior. */
  @Test
  void shouldExposeCommercialCheckoutContractBeforeFirstPublication() {
    GeraLandingStageExecution execution = execution("job-checkout-89", "INICIADO");
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setCommercialCheckoutUrl("https://checkout.example/rigel");
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));
    when(repository.findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "INICIADO"))
        .thenReturn(List.of(execution));
    execution.setExperimentId(89L);

    LandingAgentPendingResponse result = service.claimPending(1).getFirst();

    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> contract =
        (java.util.Map<String, Object>) result.context().get("checkoutContract");
    assertEquals("https://checkout.example/rigel", contract.get("canonicalUrl"));
  }

  /** Deve congelar preço, degustação e continuidade do anúncio para proteger a oferta de Rigel. */
  @Test
  void shouldExposeCompleteCommercialOfferToLandingAgent() {
    GeraLandingStageExecution execution = execution("job-offer-89", "INICIADO");
    execution.setExperimentId(89L);
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setFreeReward("Sequência demonstrativa com três follow-ups respeitosos.");
    experiment.setUnitPrice(java.math.BigDecimal.valueOf(349));
    experiment.setAdCopy("Orçamento enviado. Cliente sumiu?");
    experiment.setAdImageBriefing("Conversa real e legível no WhatsApp.");
    when(commercialContextResolver.resolve(experiment))
        .thenReturn(
            java.util.Map.of(
                "targetAudience", "Prestadores de serviço que vendem por WhatsApp",
                "productFormat", "Serviço personalizado",
                "serviceExperienceContract",
                    java.util.Map.of("serviceScope", java.util.List.of("10 a 20 respostas"))));
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));
    when(repository.findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "INICIADO"))
        .thenReturn(List.of(execution));

    LandingAgentPendingResponse result = service.claimPending(1).getFirst();

    assertEquals(java.math.BigDecimal.valueOf(349), result.context().get("unitPriceBrl"));
    assertTrue(result.context().get("freeReward").toString().contains("três follow-ups"));
    assertTrue(result.context().get("adCopy").toString().contains("Cliente sumiu"));
    assertTrue(result.context().get("adImageBriefing").toString().contains("WhatsApp"));
    assertTrue(result.context().get("targetAudience").toString().contains("Prestadores"));
    assertTrue(result.context().get("serviceExperienceContract").toString().contains("10 a 20"));
  }

  /** Deve reabrir uma única vez o timeout terminal deixado por uma versão antiga do worker. */
  @Test
  void shouldRecoverLegacyTimeoutFailureOnce() {
    GeraLandingStageExecution timedOut = execution("job-timeout-88", "FALHA");
    timedOut.setCompletedAt(Instant.now());
    timedOut.setErrorMessage("Timeout do Codex do Agente Gerador de Landing");
    when(repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "FALHA"))
        .thenReturn(List.of(timedOut));
    when(repository.findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "INICIADO"))
        .thenReturn(List.of(timedOut));

    List<LandingAgentPendingResponse> result = service.claimPending(1);

    assertEquals(1, result.size());
    assertEquals("PROCESSANDO", timedOut.getStatus());
    assertEquals("LEGACY_TIMEOUT_RECOVERED_ONCE", timedOut.getErrorDetail());
    assertEquals(null, timedOut.getErrorMessage());
    assertEquals(null, timedOut.getCompletedAt());
  }

  /** Não reabre novamente um timeout que já consumiu sua retomada controlada. */
  @Test
  void shouldNotRecoverLegacyTimeoutTwice() {
    GeraLandingStageExecution timedOut = execution("job-timeout-88", "FALHA");
    timedOut.setErrorMessage("Timeout do Codex do Agente Gerador de Landing");
    timedOut.setErrorDetail("LEGACY_TIMEOUT_RECOVERED_ONCE");
    when(repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "FALHA"))
        .thenReturn(List.of(timedOut));

    List<LandingAgentPendingResponse> result = service.claimPending(1);

    assertTrue(result.isEmpty());
    assertEquals("FALHA", timedOut.getStatus());
  }

  /** Deve retomar homologação abandonada quando um build novo passa a consumir a fila. */
  @Test
  void shouldRecoverCommercialHomologationAfterExecutorDeploy() {
    GeraLandingStageExecution blocked = execution("job-cph-88", "PROCESSANDO");
    blocked.setExecutionRequestedAt(Instant.now().minusSeconds(300));
    blocked.setAutonomousCycleId("cph-2-cycle");
    blocked.setPromptContent(
        "{\"source\":\"COMMERCIAL_PLAN_JOURNEY_HOMOLOGATION\","
            + "\"recoveryPolicy\":\"RETRY_ON_EXECUTOR_DEPLOY\"}");
    blocked.setErrorDetail("CLAIMED_BY_BUILD:build-old");
    when(repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "PROCESSANDO"))
        .thenReturn(List.of(blocked));
    when(repository.findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "INICIADO"))
        .thenReturn(List.of(blocked));

    List<LandingAgentPendingResponse> result = service.claimPending(1, "build-new");

    assertEquals(1, result.size());
    assertEquals("PROCESSANDO", blocked.getStatus());
    assertEquals("CLAIMED_BY_BUILD:build-new", blocked.getErrorDetail());
    assertEquals(null, blocked.getCompletedAt());
    assertTrue(blocked.getQualityReviewAudit().contains("from=build-old|to=build-new"));
  }

  /** Deve retomar tarefa BPM reservada por um executor substituído durante o deploy. */
  @Test
  void shouldRecoverBpmTaskAfterExecutorDeploy() {
    GeraLandingStageExecution blocked = execution("job-bpm-30", "PROCESSANDO");
    blocked.setExecutionRequestedAt(Instant.now().minusSeconds(300));
    blocked.setAutonomousCycleId("agent-task:30");
    blocked.setPromptContent("{\"agentTaskId\":30}");
    blocked.setErrorDetail("CLAIMED_BY_BUILD:build-old");
    when(repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "PROCESSANDO"))
        .thenReturn(List.of(blocked));
    when(repository.findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "INICIADO"))
        .thenReturn(List.of(blocked));

    List<LandingAgentPendingResponse> result = service.claimPending(1, "build-new");

    assertEquals(1, result.size());
    assertEquals("PROCESSANDO", blocked.getStatus());
    assertEquals("CLAIMED_BY_BUILD:build-new", blocked.getErrorDetail());
    assertTrue(blocked.getQualityReviewAudit().contains("from=build-old|to=build-new"));
  }

  /** Não deve duplicar homologação que ainda pertence ao mesmo build ativo. */
  @Test
  void shouldNotRecoverCommercialHomologationWithinSameDeploy() {
    GeraLandingStageExecution active = execution("job-cph-88", "PROCESSANDO");
    active.setExecutionRequestedAt(Instant.now().minusSeconds(300));
    active.setPromptContent("{\"recoveryPolicy\":\"RETRY_ON_EXECUTOR_DEPLOY\"}");
    active.setErrorDetail("CLAIMED_BY_BUILD:build-current");
    when(repository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "PROCESSANDO"))
        .thenReturn(List.of(active));

    List<LandingAgentPendingResponse> result = service.claimPending(1, "build-current");

    assertTrue(result.isEmpty());
    assertEquals("PROCESSANDO", active.getStatus());
    assertEquals("CLAIMED_BY_BUILD:build-current", active.getErrorDetail());
  }

  /** Deve tornar callback repetido idempotente e não avançar novamente. */
  @Test
  void shouldIgnoreRepeatedCompletedCallback() {
    GeraLandingStageExecution execution = execution("job-88", "CONCLUIDO");
    when(repository.findTopByIdJobOrderByExecutionRequestedAtDesc(any()))
        .thenReturn(Optional.of(execution));
    LandingAgentResultRequest request =
        new LandingAgentResultRequest(
            "{}",
            "Núcleo de Dédalo.\n\nCorrija a landing.",
            "Núcleo de Dédalo.",
            "Corrija a landing.",
            "{}",
            "gpt-5.6-sol",
            "high",
            null,
            null,
            null,
            null,
            null);

    service.complete("job-88", request);

    org.mockito.Mockito.verifyNoInteractions(coordinator);
  }

  /** Cria uma execução mínima compatível com a fila. */
  private GeraLandingStageExecution execution(String id, String status) {
    return GeraLandingStageExecution.builder()
        .idJob(id.getBytes(StandardCharsets.UTF_8))
        .experimentId(88L)
        .stageCode("landing-generation-agent-v1")
        .promptContent("{\"score\":70}")
        .status(status)
        .executionRequestedAt(Instant.now())
        .createdAt(Instant.now())
        .build();
  }

  /** Cria uma aprovação visual correlacionada à tarefa BPM usada pelos testes de callback. */
  private GeraLandingStageExecution approvedQualityReview(String id, int score) {
    GeraLandingStageExecution execution = execution(id, "CONCLUIDO");
    execution.setStageCode("landing-page-quality-review");
    execution.setAutonomousCycleId("agent-task:30");
    execution.setModelResponse(
        "{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\",\"score\":" + score + "}");
    execution.setCompletedAt(execution.getExecutionRequestedAt().plusSeconds(1));
    execution.setCostUsd(java.math.BigDecimal.valueOf(0.146645));
    return execution;
  }
}
