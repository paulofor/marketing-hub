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
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.qualityreview.service.LandingQualityReviewedEvent;
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
  private LandingGenerationResultApplicationService resultApplicationService;
  private GeraSalesPagePublicationAuditRepository publicationRepository;
  private LandingGenerationAgentExecutionService service;

  /** Prepara dependências isoladas antes de cada cenário. */
  @BeforeEach
  void setUp() {
    repository = mock(GeraLandingStageExecutionRepository.class);
    coordinator = mock(LandingGenerationAgentCoordinator.class);
    experimentRepository = mock(ExperimentRepository.class);
    agentTaskService = mock(AgentTaskService.class);
    resultApplicationService = mock(LandingGenerationResultApplicationService.class);
    publicationRepository = mock(GeraSalesPagePublicationAuditRepository.class);
    service =
        new LandingGenerationAgentExecutionService(
            repository,
            coordinator,
            experimentRepository,
            publicationRepository,
            new ObjectMapper(),
            agentTaskService,
            resultApplicationService);
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
    verify(coordinator, never()).continueAfterQualityReview(any(), any(), any());
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
            "request",
            "response",
            "gpt-5.6-sol",
            null,
            null,
            null,
            null));

    assertEquals("FALHA", execution.getStatus());
    assertTrue(execution.getErrorMessage().contains("Checkout divergente"));
    verify(agentTaskService)
        .failClaimedProcessTask(
            org.mockito.ArgumentMatchers.eq("landing-generator"),
            org.mockito.ArgumentMatchers.eq(30L),
            any(com.marketinghub.agenttask.FailAgentTaskRequest.class));
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
    assertEquals("PROCESSANDO", execution.getStatus());
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
        new LandingAgentResultRequest("{}", "request", "{}", "gpt-5.6-sol", null, null, null, null);

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
}
