package com.marketinghub.geralanding.agent.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.qualityreview.service.LandingQualityReviewedEvent;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
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
  private LandingGenerationAgentExecutionService service;

  /** Prepara dependências isoladas antes de cada cenário. */
  @BeforeEach
  void setUp() {
    repository = mock(GeraLandingStageExecutionRepository.class);
    coordinator = mock(LandingGenerationAgentCoordinator.class);
    experimentRepository = mock(ExperimentRepository.class);
    service =
        new LandingGenerationAgentExecutionService(
            repository, coordinator, experimentRepository, new ObjectMapper());
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

  /** Deve abrir nova transação ao persistir a fila depois do commit do Quality Review. */
  @Test
  void shouldPersistQualityReviewEventInNewTransaction() throws NoSuchMethodException {
    Transactional transactional =
        LandingGenerationAgentExecutionService.class
            .getMethod("onQualityReviewCompleted", LandingQualityReviewedEvent.class)
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
